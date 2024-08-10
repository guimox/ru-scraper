package com.ru.scraper.scraper;

import com.ru.scraper.data.meal.MealOption;
import com.ru.scraper.exception.MenuResult;
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
    @Value("${RU_URL}")
    private String ruUrl;

    public ScraperRU(ScraperHelper scraperHelper) {
        this.scraperHelper = scraperHelper;
    }


    private Document connectScraper(String webURL) throws InterruptedException {
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

                Thread.sleep(RETRY_DELAY); // Delay before retrying
            }
        }

        throw new RuntimeException("Unexpected error occurred while trying to retrieve content from " + webURL);
    }

    @Override
    public MenuResult parseTableHtml(String ruCode) throws InterruptedException {
        Document htmlDocument = this.connectScraper(ruUrl);
        String localDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM"));
        System.out.println("Trying to get a menu for the day " + localDate);
        Element titleContainingDate = htmlDocument.selectFirst("p:contains(" + localDate + ")");

        Element menuFromWeekday = titleContainingDate.nextElementSibling();

        System.out.println("Menu from weekday: " + menuFromWeekday);

        Element imgElement = menuFromWeekday.selectFirst("img");
        if (imgElement != null) {
            return new MenuResult(imgElement);
        }

        Elements tableRows = menuFromWeekday.select("table tbody tr");
        return new MenuResult(tableRows);
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
