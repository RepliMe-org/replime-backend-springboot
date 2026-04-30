package com.example.demo.configs.seeders;

import com.example.demo.entities.ChatbotCategory;
import com.example.demo.entities.MessageClass;
import com.example.demo.entities.utils.MessageClassType;
import com.example.demo.repos.ChatbotCategoryRepo;
import com.example.demo.repos.MessageClassRepo;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1) // ensures this seeder runs first, if there are order dependencies
public class ChatbotDataSeeder implements Seeder {

    private final ChatbotCategoryRepo categoryRepo;
    private final MessageClassRepo messageClassRepo;

    public ChatbotDataSeeder(ChatbotCategoryRepo categoryRepo, MessageClassRepo messageClassRepo) {
        this.categoryRepo = categoryRepo;
        this.messageClassRepo = messageClassRepo;
    }

    @Override
    public void seed() {
        if (categoryRepo.count() == 0) {
            ChatbotCategory gaming = ChatbotCategory.builder().name("Gaming").build();
            ChatbotCategory tech = ChatbotCategory.builder().name("Technology").build();
            ChatbotCategory education = ChatbotCategory.builder().name("Education").build();
            ChatbotCategory lifestyle = ChatbotCategory.builder().name("Lifestyle").build();

            categoryRepo.saveAll(List.of(gaming, tech, education, lifestyle));

            if (messageClassRepo.count() == 0) {
                MessageClass review = MessageClass.builder()
                        .name("Review")
                        .category(tech)
                        .type(MessageClassType.SYSTEM)
                        .build();

                MessageClass tutorial = MessageClass.builder()
                        .name("Tutorial")
                        .category(education)
                        .type(MessageClassType.SYSTEM)
                        .build();

                MessageClass gameplay = MessageClass.builder()
                        .name("Gameplay")
                        .category(gaming)
                        .type(MessageClassType.SYSTEM)
                        .build();

                MessageClass vlog = MessageClass.builder()
                        .name("Vlog")
                        .category(lifestyle)
                        .type(MessageClassType.SYSTEM)
                        .build();

                messageClassRepo.saveAll(List.of(review, tutorial, gameplay, vlog));
            }

            System.out.println("Seeded database with default Chatbot Categories and Message Classes.");
        }
    }
}

