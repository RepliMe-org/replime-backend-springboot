package com.example.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitTestRunner implements CommandLineRunner {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void run(String... args) {

        rabbitTemplate.convertAndSend(
                "test.queue",
                "Hello RabbitMQ"
        );

        System.out.println("Message sent");
    }

}
