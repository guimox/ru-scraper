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
    private Document mockDocument;

    @Mock
    private Element mockDateElement;

    @Mock
    private Element mockMenuElement;

    @Mock
    private Connection connection;

    @Mock
    private Elements elements;

    private ScraperRU scraperRU;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        scraperHelper = mock(ScraperHelper.class);
        String testUrl = "https://gxlpes.github.io/ru-scraper/src/main/resources/website/ru-website-1208-0908.html";
        document = Jsoup.connect(testUrl).execute().parse();
        scraperRU = new ScraperRU(scraperHelper, testUrl);
    }

    @Test
    void testConnectScraper_SuccessfulConnection() throws Exception {
        String webURL = "https://gxlpes.github.io/ru-scraper/src/main/resources/website/ru-website-1208-0908.html";
        Document result = scraperRU.connectScraper(webURL);
        assertNotNull(result, "The document should not be null when the connection is successful.");
    }

    @Test
    void testConnectScraper_UnsuccesfulConnection() throws Exception {
        String webURL = "https://gxlpes.github.io/ru-scraper/src/main/resources/website/ru-website-error.html";
        String expectedMessage = "Failed to retrieve content from the website after 4 attempts";
        Exception exception = assertThrows(RuntimeException.class, () -> {
            scraperRU.connectScraper(webURL);
        });
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void testParseTableHtml_WithImageMenu() throws InterruptedException {
        String formattedDate = "12/08";
        Element mockImgElement = mock(Element.class);

        when(mockDocument.selectFirst("p:contains(" + formattedDate + ")")).thenReturn(mockDateElement);
        when(mockDateElement.nextElementSibling()).thenReturn(mockMenuElement);
        when(mockMenuElement.selectFirst("figure.wp-block-image")).thenReturn(mock(Element.class));
        when(mockMenuElement.selectFirst("img")).thenReturn(mockImgElement);

        MenuResult result = scraperRU.parseTableHtml(mockDocument, formattedDate);

        assertNotNull(result);
        assertNull(result.getTableRows());
        assertEquals(mockImgElement, result.getImageElement());
    }

    @Test
    void testParseTableHtml_WithTableMenu() throws InterruptedException {
        String formattedDate = "12/08";
        Elements mockTableRows = mock(Elements.class);

        when(mockDocument.selectFirst("p:contains(" + formattedDate + ")")).thenReturn(mockDateElement);
        when(mockDateElement.nextElementSibling()).thenReturn(mockMenuElement);
        when(mockMenuElement.selectFirst("figure.wp-block-image")).thenReturn(null);
        when(mockMenuElement.selectFirst("figure.wp-block-table")).thenReturn(mock(Element.class));
        when(mockMenuElement.select("table tbody tr")).thenReturn(mockTableRows);

        MenuResult result = scraperRU.parseTableHtml(mockDocument, formattedDate);

        assertNotNull(result);
        assertNull(result.getImageElement());
        assertEquals(mockTableRows, result.getTableRows());
    }

    @Test
    void testParseTableHtml_NoHeaderFound() {
        String formattedDate = "12/08";

        when(mockDocument.selectFirst("p:contains(" + formattedDate + ")")).thenReturn(null);

        assertThrows(RuntimeException.class, () -> {
            scraperRU.parseTableHtml(mockDocument, formattedDate);
        }, "Should throw RuntimeException when no header is found");
    }

    @Test
    void testParseTableHtml_NoMenuFound() {
        String formattedDate = "12/08";

        when(mockDocument.selectFirst("p:contains(" + formattedDate + ")")).thenReturn(mockDateElement);
        when(mockDateElement.nextElementSibling()).thenReturn(mockMenuElement);
        when(mockMenuElement.selectFirst("figure.wp-block-image")).thenReturn(null);
        when(mockMenuElement.selectFirst("figure.wp-block-table")).thenReturn(null);

        assertThrows(RuntimeException.class, () -> {
            scraperRU.parseTableHtml(mockDocument, formattedDate);
        }, "Should throw RuntimeException when no menu is found");
    }
}