package com.ru.scraper.scraper;

import com.ru.scraper.data.meal.MealOption;
import com.ru.scraper.data.response.MenuResult;
import com.ru.scraper.helper.ScraperHelper;
import com.ru.scraper.helper.Utils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ScraperRU implements IScraperRU {

    private static final int TIMEOUT_CONNECTION = 40000;
    private static final int RETRY_DELAY = 2000;
    private static final int MAX_RETRIES = 4;
    private final Utils utils;
    private final ScraperHelper scraperHelper;
    private final String ruUrl;

    public ScraperRU(Utils utils, ScraperHelper scraperHelper, @Value("${RU_URL}") String ruUrl) {
        this.scraperHelper = scraperHelper;
        this.utils = utils;
        this.ruUrl = ruUrl;
    }

    public Document connectScraper(String webURL) throws InterruptedException {
        System.setProperty("java.net.preferIPv6Addresses", "true");

        if (!utils.isInternetAvailable()) {
            throw new RuntimeException("No internet connection available");
        }

        try {
            InetAddress localHost = InetAddress.getLocalHost();
            System.out.println("Local IP Address: " + localHost.getHostAddress());
        } catch (UnknownHostException e) {
            System.out.println("Unable to retrieve the local IP address");
        }

        int attempt = 0;
        System.out.println("Internet is available");

        while (attempt < MAX_RETRIES) {
            try {
                attempt++;
                System.out.println("Trying to connect to " + webURL + " (attempt " + attempt + ")");

                Connection.Response response = Jsoup.connect(webURL)
                        .timeout(TIMEOUT_CONNECTION)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                        .header("Accept-Encoding", "gzip, deflate, br, zstd")
                        .header("Accept-Language", "en-US,en;q=0.9,pt;q=0.8")
                        .header("Cache-Control", "max-age=0")
                        .header("Connection", "keep-alive")
                        .header("Host", "pra.ufpr.br")
                        .header("Referer", "https://www.google.com/")
                        .header("Sec-CH-UA", "\"Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\", \"Google Chrome\";v=\"128\"")
                        .header("Sec-CH-UA-Mobile", "?0")
                        .header("Sec-CH-UA-Platform", "\"Windows\"")
                        .header("Sec-Fetch-Dest", "document")
                        .header("Sec-Fetch-Mode", "navigate")
                        .header("Sec-Fetch-Site", "cross-site")
                        .header("Sec-Fetch-User", "?1")
                        .header("Upgrade-Insecure-Requests", "1")
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                        .execute();

                System.out.println("HTTP Status Code: " + response.statusCode());
                System.out.println("HTTP Status Message: " + response.statusMessage());

                if (response.statusCode() == 200) {
                    return response.parse();
                } else {
                    System.err.println("Unexpected HTTP status code: " + response.statusCode());
                    throw new RuntimeException("Failed to retrieve content from the website due to unexpected HTTP status code: " + response.statusCode());
                }
            } catch (IOException e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String exceptionDetails = sw.toString();

                System.err.println("Failed to connect to " + webURL + " on attempt " + attempt);
                System.err.println("Exception Details: " + exceptionDetails);

                if (attempt >= MAX_RETRIES) {
                    throw new RuntimeException("Failed to retrieve content from the website after " + MAX_RETRIES + " attempts. Exception: " + exceptionDetails);
                }

                Thread.sleep(RETRY_DELAY);
            }
        }
        throw new RuntimeException("Failed to connect to " + webURL + " after " + MAX_RETRIES + " attempts.");
    }

    @Override
    public MenuResult parseTableHtml(Document htmlDocument, String formattedDate) throws InterruptedException {
        System.out.println("Trying to get a menu for the day " + formattedDate);

        // Find the element containing the date
        Element titleContainingDate = htmlDocument.selectFirst("p > strong:contains(" + formattedDate + ")");
        if (titleContainingDate == null) {
            titleContainingDate = htmlDocument.selectFirst("strong:contains(" + formattedDate + ")");
        }
        if (titleContainingDate == null) {
            titleContainingDate = htmlDocument.selectFirst("figcaption:contains(" + formattedDate + ")");
        }
        if (titleContainingDate == null) {
            titleContainingDate = htmlDocument.selectFirst("p:contains(" + formattedDate + ")");
        }

        if (titleContainingDate == null) {
            throw new RuntimeException("No menu found with the given date " + formattedDate);
        }

        // First, check if the next sibling of the date element is a valid menu container
        Element potentialMenuContainer = null;
        Element nextSibling = titleContainingDate.nextElementSibling();
        if (isValidMenuContainer(nextSibling)) {
            potentialMenuContainer = nextSibling;
        }

        // If not found, check if the next sibling of the parent element is a valid menu container
        if (potentialMenuContainer == null) {
            Element parent = titleContainingDate.parent();
            if (parent != null) {
                Element parentsNextSibling = parent.nextElementSibling();
                if (isValidMenuContainer(parentsNextSibling)) {
                    potentialMenuContainer = parentsNextSibling;
                }
            }
        }

        // If no valid menu container found, throw an exception
        if (potentialMenuContainer == null) {
            throw new RuntimeException("Menu not found for date " + formattedDate);
        }

        // Process the found menu container
        Element imgElement = potentialMenuContainer.selectFirst("figure.wp-block-image img, img");
        if (imgElement != null && potentialMenuContainer.selectFirst("table") == null) {
            return new MenuResult(imgElement);
        }

        Element tableElement = potentialMenuContainer.selectFirst("table");
        if (tableElement != null) {
            Elements tableRows = tableElement.select("tbody tr");
            if (!tableRows.isEmpty()) {
                return new MenuResult(tableRows);
            } else {
                throw new RuntimeException("Found a table structure but no rows (tbody tr) within it for " + formattedDate);
            }
        }

        throw new RuntimeException("Located a potential menu container, but it didn't contain a recognizable image or table menu for " + formattedDate);
    }

    // Helper method to check if text contains a date format like DD/MM/YYYY
    private boolean containsDateFormat(String text) {
        // This regex matches common date formats like 01/05/2025, 1/5/2025
        return text.matches(".*\\d{1,2}/\\d{1,2}/\\d{4}.*");
    }

    @Override
    public void processContentFromRow(String htmlContent, List<MealOption> mealOptions) {
        Document doc = Jsoup.parseBodyFragment(htmlContent);
        Element rowHtml = doc.body();

        StringBuilder currentMealText = new StringBuilder();
        List<String> currentIcons = new ArrayList<>();

        // First pass: collect all text and icons
        for (Node node : rowHtml.childNodes()) {
            if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                String text = textNode.getWholeText().trim();
                text = text.replaceAll("\\s+", " ").trim();
                if (text.startsWith("\"") && text.endsWith("\"") && text.length() > 1) {
                    text = text.substring(1, text.length() - 1).trim();
                }

                if (!text.isEmpty()) {
                    // Check if this looks like a continuation of previous text
                    // by examining if it starts with a lowercase letter or specific connectors
                    boolean isContinuation = text.startsWith("e ") || text.startsWith("com ") ||
                            text.startsWith("ou ") || text.startsWith("de ") ||
                            text.startsWith("à ") || text.startsWith("ao ") ||
                            (text.length() > 0 && Character.isLowerCase(text.charAt(0)));

                    if (isContinuation && currentMealText.length() > 0) {
                        // This is a continuation of the previous text
                        currentMealText.append(" ").append(text);
                    } else {
                        // This is a new meal item
                        if (currentMealText.length() > 0) {
                            // Add the previous meal option to the list
                            MealOption mealOption = new MealOption();
                            mealOption.setName(currentMealText.toString());
                            for (String icon : currentIcons) {
                                mealOption.addIcon(icon);
                            }
                            mealOptions.add(mealOption);

                            // Reset for the new meal
                            currentMealText = new StringBuilder();
                            currentIcons = new ArrayList<>();
                        }

                        currentMealText.append(text);
                    }
                }
            } else if (node instanceof Element) {
                Element element = (Element) node;
                if (element.tagName().equalsIgnoreCase("img")) {
                    String iconDescription = extractIconDescription(element);
                    currentIcons.add(iconDescription);
                }
            }
        }

        // Add the last meal option if there's any
        if (currentMealText.length() > 0) {
            MealOption mealOption = new MealOption();
            mealOption.setName(currentMealText.toString());
            for (String icon : currentIcons) {
                mealOption.addIcon(icon);
            }
            mealOptions.add(mealOption);
        }
    }

    public String extractIconDescription(Element imgElement) {
        String title = imgElement.attr("title");

        if (!title.trim().isEmpty()) {
            return title.trim();
        }

        String alt = imgElement.attr("alt");
        if (!alt.trim().isEmpty()) {
            return alt.trim();
        }

        System.out.println("Warning: Icon missing title and alt attributes. Falling back to filename.");
        return scraperHelper.extractFileNameWithoutExtension(imgElement.attr("src"));
    }

    private boolean isValidMenuContainer(Element element) {
        if (element == null) return false;
        boolean isFigure = element.tagName().equalsIgnoreCase("figure");
        boolean hasTable = element.selectFirst("table") != null;
        boolean hasImage = element.selectFirst("img") != null;
        if (isFigure && (element.hasClass("wp-block-table") || element.hasClass("wp-block-image"))) return true;
        if (hasTable || (hasImage && !hasTable)) return true;
        if (element.tagName().equalsIgnoreCase("table")) return true;
        if (element.tagName().equalsIgnoreCase("div")) {
            return element.selectFirst("figure.wp-block-table, figure.wp-block-image, table") != null;
        }
        return false;
    }
}
