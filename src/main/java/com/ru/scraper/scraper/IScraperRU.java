package com.ru.scraper.scraper;

import com.ru.scraper.data.meal.MealOption;
import com.ru.scraper.data.response.MenuResult;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface IScraperRU {

    MealOption createMealOption(String contentPart);

    MenuResult parseTableHtml(Document htmlDocument, String formattedDate) throws InterruptedException;

    String extractTextFromHtml(String htmlContent);

    String extractImageName(Element imgElement);

    void processContentFromRow(String htmlContent, List<MealOption> mealOptions);

}
