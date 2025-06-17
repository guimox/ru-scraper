package com.ru.scraper;

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ru.scraper.service.ScrapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class ScraperRUTest {

    @Mock
    private ScrapService scrapService;

    @InjectMocks
    private RuScraperApplication application;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testScraperMenuFunction() throws Exception {
        // Create a test event
        ObjectMapper objectMapper = new ObjectMapper();
        String json = "{ \"time\": \"2024-08-17T00:00:00Z\" }";
        Map<String, Object> eventPayload = new HashMap<>();

        String triggerTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
        eventPayload.put("time", triggerTime);

        // Get the function and apply the event
        Function<Map<String, Object>, ?> scraperFunction  = application.scraperMenu();
        Object result = scraperFunction.apply(eventPayload);

        LocalDateTime localDateTime = LocalDateTime.now();

        // Verify the interactions and the result
        verify(scrapService, times(1)).scrape(localDateTime);
        assertNotNull(result);
    }
}
