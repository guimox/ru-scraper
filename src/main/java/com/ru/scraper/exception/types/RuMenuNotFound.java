package com.ru.scraper.exception.types;

import java.io.Serial;

public class RuMenuNotFound extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1l;

    public RuMenuNotFound(String message) {
        super(message);
    }
}
