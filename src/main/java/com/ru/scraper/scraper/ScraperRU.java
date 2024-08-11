package com.ru.scraper.scraper;

import com.ru.scraper.data.meal.MealOption;
import com.ru.scraper.data.response.MenuResult;
import com.ru.scraper.helper.ScraperHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ScraperRU implements IScraperRU {

    private static final int MAX_RETRIES = 3;
    private static final int TIMEOUT = 30000; // 30 seconds
    private static final int RETRY_DELAY = 1000; // 1 second

    private final ScraperHelper scraperHelper;
    private final LocalDate currentDate;
    private final String ruUrl;

    public ScraperRU(ScraperHelper scraperHelper,
                     @Value("${RU_URL}") String ruUrl,
                     @Value("#{T(java.time.LocalDate).now()}") LocalDate currentDate) {
        this.scraperHelper = scraperHelper;
        this.ruUrl = ruUrl;
        this.currentDate = currentDate;
    }

    public Document connectScraper(String webURL) throws InterruptedException {
        int attempt = 0;

        while (attempt < MAX_RETRIES) {
            try {
                attempt++;
                System.out.println("Trying to connect to " + webURL + " (attempt " + attempt + ")");
                return Jsoup.connect(webURL).timeout(TIMEOUT).get();
            } catch (IOException e) {
                System.out.println("Failed to connect to " + webURL + " on attempt " + attempt + ": " + e.getMessage());

                if (attempt >= MAX_RETRIES) {
                    throw new RuntimeException("Failed to retrieve content from " + webURL + " after " + MAX_RETRIES + " attempts", e);
                }

                Thread.sleep(RETRY_DELAY);
            }
        }

        throw new RuntimeException("Unexpected error occurred while trying to retrieve content from " + webURL);
    }

    @Override
    public MenuResult parseTableHtml(String ruCode) throws InterruptedException {
        Document htmlDocument = this.connectScraper(ruUrl);

        String formattedDate = currentDate.format(DateTimeFormatter.ofPattern("dd/MM"));
        System.out.println("Trying to get a menu for the day " + formattedDate);

        Element titleContainingDate = htmlDocument.selectFirst("p:contains(" + formattedDate + ")");

        Element menuFromWeekday = titleContainingDate.nextElementSibling();
        System.out.println("Menu from weekday: " + menuFromWeekday);

        if (menuFromWeekday.selectFirst("figure.wp-block-image") != null) {
            Element imgElement = menuFromWeekday.selectFirst("img");
            if (imgElement != null) {
                return new MenuResult(imgElement);
            }
        } else if (menuFromWeekday.selectFirst("figure.wp-block-table") != null) {
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
    public String extractImageName(Element imgElement, ScraperRU scraperRU) {
        String src = imgElement.attr("src");
        return scraperHelper.extractFileNameWithoutExtension(src);
    }

    @Override
    public void updateMeals(Map<String, List<MealOption>> meals, List<MealOption> mealOptions, String mealPeriodTitle) {
        if (mealPeriodTitle != null && !mealOptions.isEmpty()) {
            meals.put(mealPeriodTitle, new ArrayList<>(mealOptions));
        }
    }

    @Override
    public void processContentFromRow(String htmlContent, List<MealOption> mealOptions, ScraperRU scraperRU) {
        String[] contentFromRow = htmlContent.split("<br>");
        for (String contentPart : contentFromRow) {
            MealOption mealOption = createMealOption(contentPart, scraperRU);
            if (mealOption != null) {
                mealOptions.add(mealOption);
            }
        }
    }

    @Override
    public MealOption createMealOption(String contentPart, ScraperRU scraperRU) {
        String text = scraperRU.extractTextFromHtml(contentPart);

        if (!text.isEmpty()) {
            MealOption mealOption = new MealOption();
            mealOption.setName(text);

            Elements imgElements = Jsoup.parse(contentPart).select("img");
            for (Element imgElement : imgElements) {
                String imageName = scraperRU.extractImageName(imgElement, scraperRU);
                mealOption.addIcon(imageName);
            }

            return mealOption;
        }

        return null;
    }
}
