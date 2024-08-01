package com.scraper.ruscraperapi.scraper;

import com.scraper.ruscraperapi.data.meal.MealOption;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.Map;

public interface IScraperRU {

    MealOption createMealOption(String contentPart, ScraperRU scraperRU);

    Elements parseTableHtml(String ruCode);

    String extractTextFromHtml(String htmlContent);

    String extractImageName(Element imgElement, ScraperRU scraperRU);

    void updateMeals(Map<String, List<MealOption>> meals, List<MealOption> mealOptions, String mealPeriodTitle);

    void processContentFromRow(String htmlContent, List<MealOption> mealOptions, ScraperRU scraperRU);

}
