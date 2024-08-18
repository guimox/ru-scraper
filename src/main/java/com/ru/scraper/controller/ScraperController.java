package com.ru.scraper.controller;

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.ru.scraper.helper.Utils;
import com.ru.scraper.service.ScrapService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/scraper")
public class ScraperController {

    private final ScrapService scrapService;
    private final Utils utils;

    @Autowired
    public ScraperController(ScrapService scrapService, Utils utils) {
        this.scrapService = scrapService;
        this.utils = utils;
    }

    @GetMapping("/menu")
    public Object getScraperMenu() {
        try {
            ScheduledEvent event = new ScheduledEvent();
            event.setTime(new DateTime());
            System.out.println("TIME TRIGGERED: " + event.getTime().toString());
            DateTime jodaDateTime = event.getTime();
            LocalDateTime javaLocalDateTime = utils.convertToLocalDateTime(jodaDateTime);
            return scrapService.scrape(javaLocalDateTime);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}
