package com.ru.scraper.controller;

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.ru.scraper.RuScraperApplication;
import org.joda.time.DateTime;
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
    public Object getScraperMenu(@RequestParam(defaultValue = "0") int daysOffset) {
        try {
            ScheduledEvent event = new ScheduledEvent();
            event.setTime(new DateTime().plusDays(daysOffset));

            Function<ScheduledEvent, ?> scraperFunction = ruScraperApplication.scraperMenu();
            return scraperFunction.apply(event);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}