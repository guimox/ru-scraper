package com.ru.scraper;

import com.ru.scraper.service.ScrapService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@SpringBootApplication
public class RuScraperApiApplication {
    private final ScrapService scrapService;

    public RuScraperApiApplication(ScrapService scrapService) {
        this.scrapService = scrapService;
    }

    public static void main(String[] args) {
        SpringApplication.run(RuScraperApiApplication.class, args);
    }

    @Bean
    public Supplier<Object> scraperMenu() {
        return () -> {
            try {
                System.out.println("Scraper function triggered at: " + LocalDateTime.now());
                return scrapService.scrape();
            } catch (Exception e) {
                System.out.println("Error occurred at: " + System.currentTimeMillis());
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
        };
    }
}
