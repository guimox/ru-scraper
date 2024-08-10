package com.ru.scraper.factory;

import com.ru.scraper.data.meal.MealOption;
import com.ru.scraper.data.response.ResponseMenu;

import java.util.List;
import java.util.Map;

public interface IResponseMenuBuilder {
    ResponseMenu createResponseMenu(Map<String, List<MealOption>> meals, List<String> served);
    ResponseMenu createResponseMenuWithImg(String imgMenu);
}
