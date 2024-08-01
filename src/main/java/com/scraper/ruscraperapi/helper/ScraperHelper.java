package com.scraper.ruscraperapi.helper;

import org.springframework.stereotype.Component;

@Component
public class ScraperHelper implements IScraperHelper {

    public String translateMeal(String originalTitle) {
        return switch (originalTitle.toUpperCase()) {
            case "CAFÉ DA MANHÃ" -> "breakfast";
            case "ALMOÇO" -> "lunch";
            case "JANTAR" -> "dinner";
            default -> null;
        };
    }

    public String extractFileNameWithoutExtension(String url) {
        int lastIndexOfSlash = url.lastIndexOf('/');
        if (lastIndexOfSlash != -1) {
            String extractedPart = url.substring(lastIndexOfSlash + 1);
            int indexOfDot = extractedPart.lastIndexOf('.');
            if (indexOfDot != -1) return extractedPart.substring(0, indexOfDot);
        }
        return null;
    }

}
