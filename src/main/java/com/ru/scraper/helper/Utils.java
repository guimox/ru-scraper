package com.ru.scraper.helper;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class Utils {

    public String getFormattedDate(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM"));
    }
}
