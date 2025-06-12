package com.ru.scraper;

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.ru.scraper.helper.Utils;
import com.ru.scraper.service.ScrapService;
import com.ru.scraper.store.service.ExecutionStateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.util.function.Function;

@SpringBootApplication
public class RuScraperApplication {

    private final ScrapService scrapService;
    private final Utils utils;
    private final ExecutionStateService executionStateService;

    @Value("${ru.code}")
    private String ruCode;

    public RuScraperApplication(ScrapService scrapService, Utils utils, ExecutionStateService executionStateService) {
        this.scrapService = scrapService;
        this.utils = utils;
        this.executionStateService = executionStateService;
    }

    public static void main(String[] args) {
        SpringApplication.run(RuScraperApplication.class, args);
    }

    @Bean
    public Function<ScheduledEvent, ?> scraperMenu() {
        return (input) -> {
            LocalDateTime currentDateTime = utils.convertToLocalDateTime(input.getTime());

            System.out.println("Current date: " + utils.getFormattedDateTime(currentDateTime));

            System.out.println("Processing for restaurant: " + ruCode);

            if (!executionStateService.isScrapingNeeded(ruCode, currentDateTime)) {
                String skipMessage = "Scraping skipped - already successful for " + ruCode + " on " + utils.getFormattedDateTime(currentDateTime);
                System.out.println(skipMessage);
                return "Skipped menu because one was already sent";
            }

            System.out.println("Starting scraping process for " + ruCode + "...");

            try {
                Object result = scrapService.scrape(currentDateTime);

                executionStateService.saveSuccessfulExecution(currentDateTime, ruCode);

                return result;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                String errorMsg = "Scraping interrupted for " + ruCode + ": " + e.getMessage();

                executionStateService.saveFailedExecution(currentDateTime, errorMsg, ruCode);

                throw new RuntimeException(errorMsg, e);

            } catch (Exception e) {
                String errorMsg = "Scraping failed for " + ruCode + ": " + e.getMessage();

                executionStateService.saveFailedExecution(currentDateTime, errorMsg, ruCode);

                throw new RuntimeException(errorMsg, e);
            }
        };
    }
}