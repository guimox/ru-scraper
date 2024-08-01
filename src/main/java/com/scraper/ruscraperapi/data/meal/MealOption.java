package com.scraper.ruscraperapi.data.meal;

import java.util.ArrayList;
import java.util.List;

public class MealOption {

    private String name;
    private List<String> icons;

    public MealOption() {
        this.icons = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getIcons() {
        return icons;
    }

    public void setIcons(List<String> icons) {
        this.icons = icons;
    }

    public void addIcon(String icon) {
        icons.add(icon);
    }

}
