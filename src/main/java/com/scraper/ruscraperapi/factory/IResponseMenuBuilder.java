package com.scraper.ruscraperapi.factory;

import com.scraper.ruscraperapi.data.meal.MealOption;
import com.scraper.ruscraperapi.data.response.ResponseMenu;

import java.util.List;
import java.util.Map;

public interface IResponseMenuBuilder {
    ResponseMenu createResponseMenu(Map<String, List<MealOption>> meals, List<String> served);
}
