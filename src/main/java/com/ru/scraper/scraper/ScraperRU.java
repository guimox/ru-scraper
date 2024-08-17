package com.ru.scraper.scraper;

import com.ru.scraper.data.meal.MealOption;
import com.ru.scraper.data.response.MenuResult;
import com.ru.scraper.helper.ScraperHelper;
import com.ru.scraper.helper.Utils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ScraperRU implements IScraperRU {

    private static final int TIMEOUT_CONNECTION = 12000; // 12 seconds
    private static final int RETRY_DELAY = 1000; // 1 second
    private static final int MAX_RETRIES = 4;
    private final Utils utils;
    private final ScraperHelper scraperHelper;
    private final String ruUrl;

    public ScraperRU(Utils utils, ScraperHelper scraperHelper, @Value("${RU_URL}") String ruUrl) {
        this.utils = utils;
        this.scraperHelper = scraperHelper;
        this.ruUrl = ruUrl;
    }

    public Document connectScraper(String webURL) throws InterruptedException {
        if (!utils.isInternetAvailable()) {
            throw new RuntimeException("No internet connection available");
        }

        int attempt = 0;

        System.out.println("Internet is available");

        while (attempt < MAX_RETRIES) {
            try {
                attempt++;
                System.out.println("Trying to connect to " + webURL + " (attempt " + attempt + ")");

                Connection.Response response = Jsoup.connect(webURL).timeout(TIMEOUT_CONNECTION).execute();

                System.out.println("HTTP Status Code: " + response.statusCode());
                System.out.println("HTTP Status Message: " + response.statusMessage());
                System.out.println("Response Headers: " + response.headers());

                if (response.statusCode() == 200) {
                    return response.parse();
                } else {
                    System.out.println("Unexpected HTTP status code: " + response.statusCode());
                    throw new RuntimeException("Failed to retrieve content from the website due to unexpected HTTP " + "status code: " + response.statusCode());
                }
            } catch (IOException e) {
                System.out.println("Failed to connect to " + webURL + " on attempt " + attempt + ": " + e.getMessage());

                if (attempt >= MAX_RETRIES) {
                    throw new RuntimeException("Failed to retrieve content from the website after " + MAX_RETRIES + " attempts");
                }

                Thread.sleep(RETRY_DELAY);
            }
        }

        throw new RuntimeException("Failed to retrieve content from the website after " + MAX_RETRIES + " attempts");
    }


    @Override
    public MenuResult parseTableHtml(Document htmlDocument, String formattedDate) throws InterruptedException {

        System.out.println("Trying to get a menu for the day " + formattedDate);
        Element titleContainingDate = htmlDocument.selectFirst("p:contains(" + formattedDate + ")");

        if (titleContainingDate == null) {
            throw new RuntimeException("No title found with the given date");
        }

        Element menuFromWeekday = titleContainingDate.nextElementSibling();
        System.out.println("Menu from weekday: " + menuFromWeekday);

        if (menuFromWeekday.selectFirst("figure.wp-block-image") != null) {
            Element imgElementMenu = menuFromWeekday.selectFirst("img");
            if (imgElementMenu != null) {
                return new MenuResult(imgElementMenu);
            }
        }

        if (menuFromWeekday.selectFirst("figure.wp-block-table") != null) {
            Elements tableRows = menuFromWeekday.select("table tbody tr");
            return new MenuResult(tableRows);
        }

        throw new RuntimeException("No menu found for the specified date.");
    }

    @Override
    public String extractTextFromHtml(String htmlContent) {
        return Jsoup.parse(htmlContent).text();
    }

    @Override
    public String extractImageName(Element imgElement) {
        String src = imgElement.attr("src");
        return scraperHelper.extractFileNameWithoutExtension(src);
    }

    @Override
    public void processContentFromRow(String htmlContent, List<MealOption> mealOptions) {
        String[] contentFromRow = htmlContent.split("<br>");
        for (String contentPart : contentFromRow) {
            MealOption mealOption = this.createMealOption(contentPart);
            if (mealOption != null) {
                mealOptions.add(mealOption);
            }
        }
    }

    @Override
    public MealOption createMealOption(String contentPart) {
        String text = this.extractTextFromHtml(contentPart);

        if (!text.isEmpty()) {
            MealOption mealOption = new MealOption();
            mealOption.setName(text);

            Elements imgElements = Jsoup.parse(contentPart).select("img");
            for (Element imgElement : imgElements) {
                String imageName = this.extractImageName(imgElement);
                mealOption.addIcon(imageName);
            }

            return mealOption;
        }

        return null;
    }
}
