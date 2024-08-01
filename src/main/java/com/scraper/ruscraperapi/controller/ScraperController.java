package com.scraper.ruscraperapi.controller;

import com.scraper.ruscraperapi.service.ScrapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scraper")
public class ScraperController {

    private final ScrapService scrapService;

    @Autowired
    public ScraperController(ScrapService scrapService) {
        this.scrapService = scrapService;
    }

    @GetMapping("/menu")
    public Object getScraperMenu() {
        try {
            return scrapService.scrape();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}
