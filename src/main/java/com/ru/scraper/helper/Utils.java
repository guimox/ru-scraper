package com.ru.scraper.helper;

import com.ru.scraper.data.meal.MealOption;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class Utils {

    public String getFormattedDateTime(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM"));
    }

    public void updateMeals(Map<String, List<MealOption>> meals, List<MealOption> mealOptions, String mealPeriodTitle) {
        if (mealPeriodTitle != null && !mealOptions.isEmpty()) {
            meals.put(mealPeriodTitle, new ArrayList<>(mealOptions));
        }
    }

    public boolean isInternetAvailable() {
        try {
            final URL url = new URL("http://www.google.com");
            final URLConnection conn = url.openConnection();
            conn.connect();
            conn.getInputStream().close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public LocalDateTime convertToLocalDateTime (DateTime jodaDateTime) {
        return LocalDateTime.ofInstant(
                jodaDateTime.toDate().toInstant(),
                ZoneId.systemDefault()
        );
    }
}
