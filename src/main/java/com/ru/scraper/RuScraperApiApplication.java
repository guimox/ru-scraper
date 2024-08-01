package com.ru.scraper;

import com.ru.scraper.service.ScrapService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.Map;
import java.util.function.Function;

@SpringBootApplication
@ComponentScan(basePackages = "com.ru.scraper")
public class RuScraperApiApplication {
    private final ScrapService scrapService;

    public RuScraperApiApplication(ScrapService scrapService) {
        this.scrapService = scrapService;
    }

    public static void main(String[] args) {
        SpringApplication.run(RuScraperApiApplication.class, args);
    }

    @Bean
    public Function<Map<String, Object>, Object> scraperMenu() {
        return (input) -> {
            try {
                System.out.println("Received input: " + input);
                return scrapService.scrape();
            } catch (Exception e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
        };
    }}
