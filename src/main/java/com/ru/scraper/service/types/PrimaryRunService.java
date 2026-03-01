package com.ru.scraper.service.types;

import com.ru.scraper.data.response.ResponseMenu;
import com.ru.scraper.data.scraper.RunType;
import com.ru.scraper.service.ScrapService;
import com.ru.scraper.store.service.ExecutionStateService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PrimaryRunService {

    private final ScrapService scrapService;
    private final ExecutionStateService executionStateService;

    public PrimaryRunService(ScrapService scrapService, ExecutionStateService executionStateService) {
        this.scrapService = scrapService;
        this.executionStateService = executionStateService;
    }

    public ResponseMenu executePrimaryRun(String ruCode, LocalDateTime targetDateTime, LocalDateTime triggerDateTime, RunType runType) {
        System.out.println("Starting " + runType + " scraping process for " + ruCode + "...");

        try {
            System.out.println("Trying to scrap the menu from the given date and time");
            ResponseMenu result = scrapService.scrape(targetDateTime);

            executionStateService.saveSuccessfulExecution(triggerDateTime, ruCode, runType.name(), result);

            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMsg = runType + " scraping interrupted for " + ruCode + ": " + e.getMessage();
            System.err.println(errorMsg);
            e.printStackTrace();

            executionStateService.saveFailedExecution(triggerDateTime, "Failed to check scraping state: " + e.getMessage(), ruCode, runType.name());

            throw new RuntimeException(errorMsg, e);

        } catch (Exception e) {
            String errorMsg = runType + " scraping failed for " + ruCode + ": " + e.getMessage();
            System.err.println(errorMsg);
            e.printStackTrace();

            executionStateService.saveFailedExecution(triggerDateTime, errorMsg, ruCode, runType.name());

            throw new RuntimeException(errorMsg, e);
        }
    }
}


