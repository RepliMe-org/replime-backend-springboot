package com.example.demo.configs;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${replime.ingestion.rabbitmq.exchange}")
    private String exchange;

    @Value("${replime.ingestion.rabbitmq.queue}")
    private String queue;

    @Value("${replime.ingestion.rabbitmq.dlq}")
    private String dlq;

    @Value("${replime.ingestion.rabbitmq.dlx}")
    private String dlx;

    @Value("${replime.ingestion.rabbitmq.routing-key}")
    private String routingKey;

    @Value("${replime.ingestion.rabbitmq.dlq-routing-key}")
    private String dlqRoutingKey;

    // ── Exchanges ────────────────────────────────────────────────

    /**
     * Main exchange: Spring Boot publishes here.
     * Routes messages to replime.video.index queue.
     *
     * Analogy: the sorting room at the post office.
     * Letters (messages) arrive here and get routed to the right mailbox.
     */
    @Bean
    public DirectExchange videoExchange() {
        return ExchangeBuilder
                .directExchange(exchange)
                .durable(true)  // survives RabbitMQ restart
                .build();
    }

    /**
     * Dead letter exchange: receives messages that fail permanently.
     * RabbitMQ routes here automatically when a message is NACK'd
     * from the main queue with requeue=false.
     *
     * Analogy: the "return to sender" bin.
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder
                .directExchange(dlx)
                .durable(true)
                .build();
    }

    // ── Queues ───────────────────────────────────────────────────

    /**
     * Main queue: FastAPI worker consumes from here.
     *
     * The two x-dead-letter arguments are the critical wiring:
     * they tell RabbitMQ "if a message cannot be delivered
     * (NACK without requeue, or TTL expired), forward it to
     * the DLX automatically — no code needed."
     */
    @Bean
    public Queue videoIndexQueue() {
        return QueueBuilder
                .durable(queue)
                .withArgument("x-dead-letter-exchange",    dlx)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .withArgument("x-message-ttl", 86_400_000) // 24h max lifetime
                .build();
    }

    /**
     * Dead Letter Queue: holds permanently failed video jobs.
     * A DLQ consumer (in FastAPI) watches this and sends a
     * final webhook to Spring Boot marking the video DEAD.
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
                .durable(dlq)
                .build();
    }

    // ── Bindings ─────────────────────────────────────────────────

    @Bean
    public Binding videoQueueBinding() {
        return BindingBuilder
                .bind(videoIndexQueue())
                .to(videoExchange())
                .with(routingKey);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(dlqRoutingKey);
    }

    // ── Serialization ─────────────────────────────────────────────

    /**
     * Converts Java objects → JSON automatically when publishing.
     * Without this, Spring would send binary Java-serialized bytes
     * which Python cannot read.
     */
    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory cf) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cf);
        factory.setMessageConverter(messageConverter());
        return factory;
    }
}
