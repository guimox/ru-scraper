package com.ru.scraper;

import com.ru.scraper.helper.Utils;
import com.ru.scraper.service.ScrapService;
import com.ru.scraper.store.service.ExecutionStateService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;

@SpringBootApplication
public class RuScraperApplication {

    private final ScrapService scrapService;
    private final Utils utils;
    private final ExecutionStateService executionStateService;
    private final Environment environment;

    @Value("${ru.code}")
    private String ruCode;

    public RuScraperApplication(ScrapService scrapService, Utils utils, ExecutionStateService executionStateService, Environment environment) {
        this.scrapService = scrapService;
        this.utils = utils;
        this.executionStateService = executionStateService;
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(RuScraperApplication.class, args);
    }

    @Bean
    public Function<Map<String, Object>, ?> scraperMenu() {
        return (input) -> {
            LocalDateTime triggerDateTime;
            LocalDateTime targetDateTime;

            if (!utils.isInternetAvailable()) {
                throw new RuntimeException("No internet connection available");
            }

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

                boolean isLocalProfile = environment.matchesProfiles("local");

                if (isLocalProfile) {
                    System.out.println("Running in local profile - skipping DynamoDB state checks");
                } else {
                    System.out.println("Checking if scraping is needed...");
                    boolean scrapingNeeded;

                    try {
                        scrapingNeeded = executionStateService.isScrapingNeeded(ruCode, targetDateTime);
                    } catch (Exception e) {
                        System.err.println("Error checking scraping state: " + e.getMessage());
                        e.printStackTrace();

                        executionStateService.saveFailedExecution(triggerDateTime,
                                "Failed to check scraping state: " + e.getMessage(), ruCode);

                        throw new RuntimeException("Failed to check scraping state", e);
                    }

                    if (!scrapingNeeded) {
                        String skipMessage = "Scraping skipped - already successful for " + ruCode + " on " + utils.getFormattedDate(targetDateTime);
                        System.out.println(skipMessage);
                        return skipMessage;
                    }

                    System.out.println("Scraping is needed, proceeding...");
                }

            try {
                return (Object) scrapService.scrape(targetDateTime);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        };
    }
}