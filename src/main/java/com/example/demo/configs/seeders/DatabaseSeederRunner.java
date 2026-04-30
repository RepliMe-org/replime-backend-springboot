package com.example.demo.configs.seeders;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runner that executes all implementing classes of the Seeder interface upon application startup.
 */
@Component
public class DatabaseSeederRunner implements CommandLineRunner {

    private final List<Seeder> seeders;

    public DatabaseSeederRunner(List<Seeder> seeders) {
        this.seeders = seeders;
    }

    @Override
    public void run(String... args) {
        for (Seeder seeder : seeders) {
            seeder.seed();
        }
        System.out.println("Database seeding process completed.");
    }
}

