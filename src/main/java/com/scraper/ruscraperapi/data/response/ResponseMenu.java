package com.scraper.ruscraperapi.data.response;

import com.scraper.ruscraperapi.data.meal.MealOption;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ResponseMenu {

    private ZonedDateTime date;
    private String ruName;
    private String ruUrl;
    private String ruCode;
    private List<String> served;
    private Map<String, List<MealOption>> meals;

    public ResponseMenu() {
        this.meals = new HashMap<>();
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

    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(ZonedDateTime date) {
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