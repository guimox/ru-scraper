package com.ru.scraper.controller;

import com.ru.scraper.RuScraperApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@RestController
@RequestMapping("/api/scraper")
public class ScraperController {

    private final RuScraperApplication ruScraperApplication;

    public ScraperController(RuScraperApplication ruScraperApplication) {
        this.ruScraperApplication = ruScraperApplication;
    }

    @GetMapping("/menu")
    public Object getScraperMenu(@RequestParam(defaultValue = "1") int targetDateOffset) {
        try {
            Map<String, Object> eventPayload = new HashMap<>();

            String triggerTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
            eventPayload.put("time", triggerTime);
            eventPayload.put("targetDateOffset", targetDateOffset);
            eventPayload.put("executionType", "scheduled");
            eventPayload.put("runType", "BACKUP");
            System.out.println("Simulating EventBridge payload: " + eventPayload);

            Function<Map<String, Object>, ?> scraperFunction = ruScraperApplication.scraperMenu();
            return scraperFunction.apply(eventPayload);

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}