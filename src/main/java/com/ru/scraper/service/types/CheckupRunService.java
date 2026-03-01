package com.ru.scraper.service.types;

import com.ru.scraper.data.response.ResponseMenu;
import com.ru.scraper.data.scraper.RunType;
import com.ru.scraper.exception.types.CheckupMenuChangedException;
import com.ru.scraper.exception.types.CheckupNoChangesException;
import com.ru.scraper.helper.Utils;
import com.ru.scraper.service.others.MenuComparisonService;
import com.ru.scraper.service.ScrapService;
import com.ru.scraper.store.service.ExecutionStateService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CheckupRunService {

    private final ScrapService scrapService;
    private final ExecutionStateService executionStateService;
    private final MenuComparisonService menuComparisonService;
    private final Utils utils;

    public CheckupRunService(ScrapService scrapService, ExecutionStateService executionStateService,
                            MenuComparisonService menuComparisonService, Utils utils) {
        this.scrapService = scrapService;
        this.executionStateService = executionStateService;
        this.menuComparisonService = menuComparisonService;
        this.utils = utils;
    }

    public void executeCheckupRun(String ruCode, LocalDateTime targetDateTime, LocalDateTime triggerDateTime, RunType runType) {
        System.out.println("Performing health check for RU: " + ruCode + " on " + utils.getFormattedDate(targetDateTime));
        try {
            System.out.println("Scraping menu for CHECKUP verification...");
            ResponseMenu currentMenu = scrapService.scrape(targetDateTime);

            System.out.println("Retrieving last successful menu from storage...");
            ResponseMenu lastMenu = executionStateService.getLastSuccessfulMenu(ruCode);

            boolean hasChanged = menuComparisonService.hasMenuChanged(currentMenu, lastMenu);

            if (hasChanged) {
                System.out.println("CHECKUP detected CHANGES in menu for " + ruCode + " on " + utils.getFormattedDate(targetDateTime));
                executionStateService.saveSuccessfulExecution(triggerDateTime, ruCode, runType.name(), currentMenu);
                throw new CheckupMenuChangedException("CHECKUP detected CHANGES in menu for " + ruCode + " on " + utils.getFormattedDate(targetDateTime), currentMenu);
            } else {
                System.out.println("CHECKUP status: No changes detected in menu for " + ruCode + " on " + utils.getFormattedDate(targetDateTime));
                String noChangesMessage = "CHECKUP not needed - menu unchanged for " + ruCode + " on " + utils.getFormattedDate(targetDateTime);
                throw new CheckupNoChangesException(noChangesMessage);
            }
        } catch (CheckupMenuChangedException | CheckupNoChangesException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error during CHECKUP run: " + e.getMessage());
            e.printStackTrace();

            executionStateService.saveFailedExecution(triggerDateTime, "CHECKUP failed: " + e.getMessage(), ruCode, runType.name());
            throw new RuntimeException("CHECKUP run failed", e);
        }
    }
}

