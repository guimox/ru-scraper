package com.ru.scraper;

import com.ru.scraper.helper.Utils;
import com.ru.scraper.service.ScrapService;
import com.ru.scraper.store.service.ExecutionStateService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.util.Map;
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
    public Function<Map<String, Object>, ?> scraperMenu() {
        return (input) -> {
            LocalDateTime triggerDateTime;
            LocalDateTime targetDateTime;

            try {
                if (input.containsKey("time") && input.containsKey("targetDateOffset")) {
                    triggerDateTime = utils.convertToLocalDateTime(DateTime.parse((String) input.get("time")));
                    int offset = ((Number) input.get("targetDateOffset")).intValue();
                    targetDateTime = triggerDateTime.plusDays(offset);
                    System.out.println("New approach using input transformer worked");
                } else {
                    System.out.println("Using the default behaviour of getting the date now");
                    triggerDateTime = LocalDateTime.now();
                    targetDateTime = triggerDateTime.plusDays(1);
                }

                System.out.println("Trigger time: " + utils.getFormattedDate(triggerDateTime));
                System.out.println("Target scraping date: " + utils.getFormattedDate(targetDateTime));

                System.out.println("Checking if scraping is needed...");
                boolean scrapingNeeded;
                try {
                    scrapingNeeded = executionStateService.isScrapingNeeded(ruCode, targetDateTime);
                } catch (Exception e) {
                    System.err.println("Error checking scraping state: " + e.getMessage());
                    e.printStackTrace();

                    // Save the error state
                    executionStateService.saveFailedExecution(triggerDateTime,
                            "Failed to check scraping state: " + e.getMessage(), ruCode);

                    // Re-throw to stop execution
                    throw new RuntimeException("Failed to check scraping state", e);
                }

                if (!scrapingNeeded) {
                    String skipMessage = "Scraping skipped - already successful for " + ruCode + " on " + utils.getFormattedDate(targetDateTime);
                    System.out.println(skipMessage);
                    return skipMessage; // Return the actual message instead of hardcoded string
                }

                System.out.println("Scraping is needed, proceeding...");
                System.out.println("Starting scraping process for " + ruCode + "...");

                try {
                    System.out.println("Trying to scrap the menu from the given date and time");
                    Object result = scrapService.scrape(targetDateTime);

                    executionStateService.saveSuccessfulExecution(triggerDateTime, ruCode);

                    return result;

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    String errorMsg = "Scraping interrupted for " + ruCode + ": " + e.getMessage();
                    System.err.println(errorMsg);
                    e.printStackTrace();

                    executionStateService.saveFailedExecution(triggerDateTime, errorMsg, ruCode);

                    throw new RuntimeException(errorMsg, e);

                } catch (Exception e) {
                    String errorMsg = "Scraping failed for " + ruCode + ": " + e.getMessage();
                    System.err.println(errorMsg);
                    e.printStackTrace();

                    executionStateService.saveFailedExecution(triggerDateTime, errorMsg, ruCode);

                    throw new RuntimeException(errorMsg, e);
                }

            } catch (Exception e) {
                System.err.println("Fatal error in scraperMenu: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        };
    }}