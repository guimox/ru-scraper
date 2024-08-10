package com.ru.scraper.factory;

import com.ru.scraper.data.meal.MealOption;
import com.ru.scraper.data.response.ResponseMenu;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Component
public class ResponseMenuBuilder implements IResponseMenuBuilder {

    @Value("${RU_CODE}")
    private String ruKey;

    @Value("${RU_URL}")
    private String ruUrl;

    @Value("${RU_NAME}")
    private String ruName;

    public ResponseMenu buildBaseMenu() {
        ResponseMenu responseMenu = new ResponseMenu();
        responseMenu.setDate(ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")));
        responseMenu.setRuCode(ruKey);
        responseMenu.setRuName(ruName);
        responseMenu.setRuUrl(ruUrl);
        return responseMenu;
    }

    @Override
    public ResponseMenu createResponseMenu(Map<String, List<MealOption>> meals, List<String> served) {
        ResponseMenu responseMenu = buildBaseMenu();
        responseMenu.setMeals(meals);
        responseMenu.setServed(served);
        return responseMenu;
    }

    @Override
    public ResponseMenu createResponseMenuWithImg(String imgMenu) {
        ResponseMenu responseMenu = buildBaseMenu();
        responseMenu.setImgMenu(imgMenu);
        return responseMenu;
    }

}
