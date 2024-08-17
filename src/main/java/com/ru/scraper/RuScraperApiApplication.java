package com.ru.scraper;

import com.ru.scraper.service.ScrapService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
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
    public Function<Object, Object> scraperMenu() {
        return (input) -> {
            try {
                System.out.println("INPUT RECEIVED: " + input);
                return scrapService.scrape();
            } catch (Exception e) {
                e.printStackTrace();
                return "Error when starting the function: " + e.getMessage();
            }
        };
    }
}
