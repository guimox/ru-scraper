package com.ru.scraper;

import com.ru.scraper.data.scraper.RunType;
import com.ru.scraper.helper.Utils;
import com.ru.scraper.service.others.RunTypeOrchestrator;
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

    private final Utils utils;
    private final RunTypeOrchestrator runTypeOrchestrator;

    @Value("${ru.code}")
    private String ruCode;

    public RuScraperApplication(Utils utils, RunTypeOrchestrator runTypeOrchestrator) {
        this.utils = utils;
        this.runTypeOrchestrator = runTypeOrchestrator;
    }

    public static void main(String[] args) {
        SpringApplication.run(RuScraperApplication.class, args);
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

                return runTypeOrchestrator.executeRun(runType, ruCode, targetDateTime, triggerDateTime);

            } catch (Exception e) {
                System.err.println("Fatal error in scraperMenu: " + e.getMessage());
                e.printStackTrace();
            }

            return null;
        };
    }
}