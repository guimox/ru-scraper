package com.ru.scraper.scraper;

import com.ru.scraper.data.meal.MealOption;
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

    @Value("${RU_URL}")
    private String ruUrl;
    private final ScraperHelper scraperHelper;

    public ScraperRU(ScraperHelper scraperHelper) {
        this.scraperHelper = scraperHelper;
    }

    private Document connectScraper(String webURL) {
        try {
            return Jsoup.connect(webURL).get();
        } catch (IOException e) {
            System.err.println("Failed to retrieve menu: " + e.getMessage());
        }
        return null;
    }

    @Override
    public Elements parseTableHtml(String ruCode) {
        Document htmlDocument = this.connectScraper(ruUrl);
        String localDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM"));
        Element titleContainingDate = htmlDocument.selectFirst("p:contains(" + localDate + ")");
        System.out.println(ruUrl);
        System.out.println(localDate);
        Element menuFromWeekday = titleContainingDate.nextElementSibling();
        return menuFromWeekday.select("table tbody tr");
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
