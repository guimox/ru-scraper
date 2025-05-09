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
                System.out.println("TIME TRIGGERED: " + input.getTime().toString());
                System.setProperty("java.net.preferIPv6Addresses", "true");
                DateTime jodaDateTime = input.getTime();
                LocalDateTime javaLocalDateTime = utils.convertToLocalDateTime(jodaDateTime);
                return scrapService.scrape(javaLocalDateTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Scraping interrupted", e);
            } catch (Exception e) {
                throw new RuntimeException("Scraping failed", e);
            }
        };
    }
}
