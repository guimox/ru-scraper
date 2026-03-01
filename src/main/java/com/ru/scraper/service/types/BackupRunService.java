package com.ru.scraper.service.types;

import com.ru.scraper.data.scraper.RunType;
import com.ru.scraper.helper.Utils;
import com.ru.scraper.store.service.ExecutionStateService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BackupRunService {

    private final ExecutionStateService executionStateService;
    private final Utils utils;

    public BackupRunService(ExecutionStateService executionStateService, Utils utils) {
        this.executionStateService = executionStateService;
        this.utils = utils;
    }

    public void executeBackupRun(String ruCode, LocalDateTime targetDateTime, LocalDateTime triggerDateTime, RunType runType) {
        System.out.println("Checking if BACKUP scraping is needed...");
        boolean scrapingNeeded;

        try {
            scrapingNeeded = executionStateService.isScrapingNeeded(ruCode, targetDateTime);
        } catch (Exception e) {
            System.err.println("Error checking scraping state: " + e.getMessage());
            e.printStackTrace();

            executionStateService.saveFailedExecution(triggerDateTime,
                    "Failed to check scraping state: " + e.getMessage(), ruCode, runType.name());

            throw new RuntimeException("Failed to check scraping state", e);
        }

        if (!scrapingNeeded) {
            String skipMessage = "BACKUP scraping skipped - already successful PRIMARY run found for " +
                    ruCode + " on " + utils.getFormattedDate(targetDateTime);
            System.out.println(skipMessage);
            throw new BackupNotNeedException(skipMessage);
        }

        System.out.println("BACKUP scraping is needed, proceeding...");
    }

    public static class BackupNotNeedException extends RuntimeException {
        public BackupNotNeedException(String message) {
            super(message);
        }
    }
}

