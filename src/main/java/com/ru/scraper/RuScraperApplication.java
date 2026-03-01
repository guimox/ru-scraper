package com.ru.scraper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ru.scraper.data.response.ResponseMenu;
import com.ru.scraper.data.scraper.RunType;
import com.ru.scraper.exception.types.CheckupMenuChangedException;
import com.ru.scraper.exception.types.CheckupNoChangesException;
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
    private final ObjectMapper objectMapper;

    @Value("${ru.code}")
    private String ruCode;

    public RuScraperApplication(ScrapService scrapService, Utils utils, ExecutionStateService executionStateService, ObjectMapper objectMapper) {
        this.scrapService = scrapService;
        this.utils = utils;
        this.executionStateService = executionStateService;
        this.objectMapper = objectMapper;
    }

    public static void main(String[] args) {
        SpringApplication.run(RuScraperApplication.class, args);
    }

    private void handleBackupRun(String ruCode, LocalDateTime targetDateTime, LocalDateTime triggerDateTime, RunType runType) {
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
            throw new IllegalStateException(skipMessage);
        }
        System.out.println("BACKUP scraping is needed, proceeding...");
    }

    private void performCheckupRun(String ruCode, LocalDateTime targetDateTime, LocalDateTime triggerDateTime, RunType runType) {
        System.out.println("Performing health check for RU: " + ruCode + " on " + utils.getFormattedDate(targetDateTime));
        try {
            System.out.println("Scraping menu for CHECKUP verification...");
            ResponseMenu currentMenu = scrapService.scrape(targetDateTime);

            System.out.println("Retrieving last successful menu from storage...");
            ResponseMenu lastMenu = executionStateService.getLastSuccessfulMenu(ruCode);

            boolean hasChanged = hasMenuChanged(currentMenu, lastMenu);

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

    private boolean hasMenuChanged(ResponseMenu currentMenu, ResponseMenu lastMenu) {
        if (lastMenu == null) {
            System.out.println("No previous menu found, considering this as a change");
            return true;
        }

        try {
            ResponseMenu currentCopy = cloneMenuWithoutDate(currentMenu);
            ResponseMenu lastCopy = cloneMenuWithoutDate(lastMenu);

            String currentJson = objectMapper.writeValueAsString(currentCopy);
            String lastJson = objectMapper.writeValueAsString(lastCopy);

            boolean changed = !currentJson.equals(lastJson);

            if (changed) {
                System.out.println("Menu content has changed");
                System.out.println("Current: " + currentJson);
                System.out.println("Last: " + lastJson);
            } else {
                System.out.println("Menu content is identical to the last stored version");
            }

            return changed;
        } catch (Exception e) {
            System.err.println("Error comparing menus: " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }

    private ResponseMenu cloneMenuWithoutDate(ResponseMenu menu) {
        if (menu == null) return null;

        ResponseMenu clone = new ResponseMenu();
        clone.setImgMenu(menu.getImgMenu());
        clone.setRuName(menu.getRuName());
        clone.setRuUrl(menu.getRuUrl());
        clone.setRuCode(menu.getRuCode());
        clone.setServed(menu.getServed());
        clone.setMeals(menu.getMeals());
        return clone;
    }

    @Bean
    public Function<Map<String, Object>, ?> scraperMenu() {
        return (input) -> {
            LocalDateTime triggerDateTime;
            LocalDateTime targetDateTime;
            RunType runType = RunType.PRIMARY;

            try {
                if (input.containsKey("time") && input.containsKey("targetDateOffset")) {
                    triggerDateTime = utils.convertToLocalDateTime(DateTime.parse((String) input.get("time")));
                    int offset = ((Number) input.get("targetDateOffset")).intValue();
                    targetDateTime = triggerDateTime.plusDays(offset);
                } else {
                    System.out.println("Using the default behaviour of getting the date now");
                    triggerDateTime = LocalDateTime.now();
                    targetDateTime = triggerDateTime.plusDays(1);
                }

                // Determine run type from input parameters
                if (input.containsKey("runType")) {
                    String runTypeStr = ((String) input.get("runType")).toUpperCase();
                    runType = RunType.valueOf(runTypeStr);
                } else if (input.containsKey("isBackup") && Boolean.TRUE.equals(input.get("isBackup"))) {
                    runType = RunType.BACKUP;
                }

                System.out.println("Run type: " + runType);
                System.out.println("Trigger time: " + utils.getFormattedDate(triggerDateTime));
                System.out.println("Target scraping date: " + utils.getFormattedDate(targetDateTime));

                switch (runType) {
                    case BACKUP:
                        try {
                            handleBackupRun(ruCode, targetDateTime, triggerDateTime, runType);
                        } catch (IllegalStateException e) {
                            return e.getMessage();
                        }
                        break;
                    case CHECKUP:
                        System.out.println("CHECKUP run - performing health check...");
                        try {
                            performCheckupRun(ruCode, targetDateTime, triggerDateTime, runType);
                        } catch (CheckupMenuChangedException e) {
                            return e.getMenu();
                        } catch (CheckupNoChangesException e) {
                            System.out.println("CHECKUP: " + e.getMessage());
                            return e.getMessage();
                        }
                        break;
                    case PRIMARY:
                    default:
                        System.out.println("PRIMARY run - proceeding without checking previous executions...");
                        break;
                }

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

            } catch (Exception e) {
                System.err.println("Fatal error in scraperMenu: " + e.getMessage());
                e.printStackTrace();
            }

            return null;
        };
    }
}