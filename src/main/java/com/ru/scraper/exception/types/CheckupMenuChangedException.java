package com.ru.scraper.exception.types;

import com.ru.scraper.data.response.ResponseMenu;

public class CheckupMenuChangedException extends RuntimeException {
    private final ResponseMenu menu;

    public CheckupMenuChangedException(String message, ResponseMenu menu) {
        super(message);
        this.menu = menu;
    }

    public ResponseMenu getMenu() {
        return menu;
    }
}
