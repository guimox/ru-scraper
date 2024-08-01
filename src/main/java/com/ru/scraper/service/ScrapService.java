package com.ru.scraper.service;

import com.ru.scraper.data.meal.MealOption;
import com.ru.scraper.data.response.ResponseMenu;
import com.ru.scraper.exception.types.RuMenuNotFound;
import com.ru.scraper.factory.ResponseMenuBuilder;
import com.ru.scraper.helper.ScraperHelper;
import com.ru.scraper.scraper.ScraperRU;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScrapService implements IScrapService {

    @Value("${RU_CODE}")
    private String ruKey;
    private final ResponseMenuBuilder responseMenuBuilder;
    private final ScraperHelper scraperHelper;
    private final ScraperRU scraperRU;

    public ScrapService(ResponseMenuBuilder responseMenuBuilder, ScraperRU scraperRU, ScraperHelper scraperHelper) {
        this.responseMenuBuilder = responseMenuBuilder;
        this.scraperRU = scraperRU;
        this.scraperHelper = scraperHelper;
    }

    public ResponseMenu scrape() {
        Elements mealRows = scraperRU.parseTableHtml(ruKey);

        System.out.println(ruKey);

        if (mealRows == null) {
            throw new RuMenuNotFound("Menu not found with this date " + LocalDateTime.now());
        }

        Map<String, List<MealOption>> meals = new HashMap<>();
        List<MealOption> mealOptions = new ArrayList<>();
        List<String> served = new ArrayList<>();
        String mealPeriodTitle = null;
        System.out.println(mealRows);

        for (Element element : mealRows) {
            Element tdElement = element.select("td").first();
            String mealTitle = scraperHelper.translateMeal(tdElement.text());

            if (mealTitle != null) {
                scraperRU.updateMeals(meals, mealOptions, mealPeriodTitle);
                mealOptions = new ArrayList<>();
                mealPeriodTitle = mealTitle;
                served.add(mealTitle);
                continue;
            }

            scraperRU.processContentFromRow(tdElement.html(), mealOptions, scraperRU);
        }

        scraperRU.updateMeals(meals, mealOptions, mealPeriodTitle);
        return responseMenuBuilder.createResponseMenu(meals, served);
    }}
