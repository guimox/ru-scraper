package com.ru.scraper;

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.ru.scraper.helper.Utils;
import com.ru.scraper.service.ScrapService;
import org.joda.time.DateTime;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.util.function.Function;

@SpringBootApplication
public class RuScraperApplication {

    private final ScrapService scrapService;
    private final Utils utils;

    public RuScraperApplication(ScrapService scrapService, Utils utils) {
        this.scrapService = scrapService;
        this.utils = utils;
    }

    public static void main(String[] args) {
        SpringApplication.run(RuScraperApplication.class, args);
    }

    @Bean
    public Function<ScheduledEvent, ?> scraperMenu() {
        return (input) -> {
            try {
                LocalDateTime currentDateTime = utils.convertToLocalDateTime(input.getTime());
                LocalDateTime nextDay = currentDateTime.plusDays(1);
                System.out.println("Current date: " + utils.getFormattedDate(currentDateTime));
                System.out.println("Next day date: " + utils.getFormattedDate(nextDay));
                return scrapService.scrape(nextDay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Scraping interrupted", e);
            } catch (Exception e) {
                throw new RuntimeException("Scraping failed", e);
            }
        };
    }
}
