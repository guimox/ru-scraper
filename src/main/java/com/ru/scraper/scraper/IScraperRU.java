package com.ru.scraper.scraper;

import com.ru.scraper.data.meal.MealOption;
import com.ru.scraper.data.response.MenuResult;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.Map;

public interface IScraperRU {

    MealOption createMealOption(String contentPart, ScraperRU scraperRU);

    MenuResult parseTableHtml(String ruCode) throws InterruptedException;

    String extractTextFromHtml(String htmlContent);

    String extractImageName(Element imgElement, ScraperRU scraperRU);

    void updateMeals(Map<String, List<MealOption>> meals, List<MealOption> mealOptions, String mealPeriodTitle);

    void processContentFromRow(String htmlContent, List<MealOption> mealOptions, ScraperRU scraperRU);

}
