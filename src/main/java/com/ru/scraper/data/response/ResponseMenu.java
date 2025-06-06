package com.ru.scraper.data.response;

import com.ru.scraper.data.meal.MealOption;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class ResponseMenu {

    private LocalDateTime date;
    private String imgMenu;
    private String ruName;
    private String ruUrl;
    private String ruCode;
    private List<String> served;
    private Map<String, List<MealOption>> meals;

    public String getImgMenu() {
        return imgMenu;
    }

    public void setImgMenu(String imgMenu) {
        this.imgMenu = imgMenu;
    }

    public String getRuName() {
        return ruName;
    }

    public void setRuName(String ruName) {
        this.ruName = ruName;
    }

    public String getRuUrl() {
        return ruUrl;
    }

    public void setRuUrl(String ruUrl) {
        this.ruUrl = ruUrl;
    }

    public String getRuCode() {
        return ruCode;
    }

    public void setRuCode(String ruCode) {
        this.ruCode = ruCode;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public Map<String, List<MealOption>> getMeals() {
        return meals;
    }

    public void setMeals(Map<String, List<MealOption>> meals) {
        this.meals = meals;
    }

    public List<String> getServed() {
        return served;
    }

    public void setServed(List<String> served) {
        this.served = served;
    }

    public void addMeal(String mealPeriod, List<MealOption> mealOptions) {
        this.meals.put(mealPeriod, mealOptions);
    }

}