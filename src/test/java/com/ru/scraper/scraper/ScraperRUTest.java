package com.ru.scraper.scraper;

import com.ru.scraper.data.meal.MealOption;
import com.ru.scraper.data.response.MenuResult;
import com.ru.scraper.helper.ScraperHelper;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScraperRUTest {

    @Mock
    private ScraperHelper scraperHelper;

    @Mock
    private Document document;

    @Mock
    private Element element;

    @Mock
    private Connection connection;

    @Mock
    private Elements elements;

    private ScraperRU scraperRU;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
//        scraperRU = new ScraperRU(scraperHelper, "https://gxlpes.github.io/ru-scraper/src/main/resources/website/Restaurante%20Universit%C3%A1rio.html", LocalDate.of(2024, 8, 8));
    }

    @Test
    void testConnectScraper_SuccessfulConnection() throws Exception {
        String webURL = "https://gxlpes.github.io/ru-scraper/src/main/resources/website/Restaurante%20Universit%C3%A1rio.html";
        System.out.println(webURL + " ############");
        Document result = scraperRU.connectScraper(webURL);

        assertNotNull(result, "The document should not be null when the connection is successful.");
        assertEquals(document, result, "The returned document should be the same as the mocked document.");
    }

}
