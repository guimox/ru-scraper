package com.ru.scraper.service.others;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ru.scraper.data.response.ResponseMenu;
import org.springframework.stereotype.Service;

@Service
public class MenuComparisonService {

    private final ObjectMapper objectMapper;

    public MenuComparisonService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean hasMenuChanged(ResponseMenu currentMenu, ResponseMenu lastMenu) {
        if (lastMenu == null) {
            System.out.println("No previous menu found, considering this as a change");
            return true;
        }

        try {
            ResponseMenu currentCopy = cloneMenuWithoutDate(currentMenu);
            ResponseMenu lastCopy = cloneMenuWithoutDate(lastMenu);

            String currentJson = objectMapper.writeValueAsString(currentCopy);
            String lastJson = objectMapper.writeValueAsString(lastCopy);

            boolean changed = !currentJson.equals(lastJson);

            if (changed) {
                System.out.println("Menu content has changed");
                System.out.println("Current: " + currentJson);
                System.out.println("Last: " + lastJson);
            } else {
                System.out.println("Menu content is identical to the last stored version");
            }

            return changed;
        } catch (Exception e) {
            System.err.println("Error comparing menus: " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }

    private ResponseMenu cloneMenuWithoutDate(ResponseMenu menu) {
        if (menu == null) return null;

        ResponseMenu clone = new ResponseMenu();
        clone.setImgMenu(menu.getImgMenu());
        clone.setRuName(menu.getRuName());
        clone.setRuUrl(menu.getRuUrl());
        clone.setRuCode(menu.getRuCode());
        clone.setServed(menu.getServed());
        clone.setMeals(menu.getMeals());
        return clone;
    }
}

