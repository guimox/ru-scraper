package com.ru.scraper.scraper;

import com.ru.scraper.data.meal.MealOption;
import com.ru.scraper.data.response.MenuResult;
import com.ru.scraper.helper.ScraperHelper;
import com.ru.scraper.helper.Utils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node; // Import Node
import org.jsoup.nodes.TextNode; // Import TextNode
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@Component
public class ScraperRU implements IScraperRU {

    private static final int TIMEOUT_CONNECTION = 40000; // 40 seconds
    private static final int RETRY_DELAY = 2000; // 2 seconds
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
                // System.out.println("Response Headers: " + response.headers()); // Optional: Less verbose logging

                if (response.statusCode() == 200) {
                    return response.parse();
                } else {
                    System.err.println("Unexpected HTTP status code: " + response.statusCode());
                    throw new RuntimeException("Failed to retrieve content from the website due to unexpected HTTP " + "status code: " + response.statusCode());
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
        // Should not be reached if MAX_RETRIES > 0
        throw new RuntimeException("Failed to connect to " + webURL + " after " + MAX_RETRIES + " attempts.");
    }

    @Override
    public MenuResult parseTableHtml(Document htmlDocument, String formattedDate) throws InterruptedException {

        System.out.println("Trying to get a menu for the day " + formattedDate);
        // Prioritize more specific selectors first
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

        // --- Try finding the menu table/image relative to the date element ---
        Element potentialMenuContainer = null;

        // 1. Check immediate next sibling
        Element nextSibling = titleContainingDate.nextElementSibling();
        if (isValidMenuContainer(nextSibling)) {
            potentialMenuContainer = nextSibling;
            System.out.println("Menu found via immediate next sibling.");
        }

        // 2. Check parent's next sibling (common if date is inside a <p> or <strong>)
        if (potentialMenuContainer == null) {
            Element parent = titleContainingDate.parent();
            if (parent != null) {
                Element parentsNextSibling = parent.nextElementSibling();
                if (isValidMenuContainer(parentsNextSibling)) {
                    potentialMenuContainer = parentsNextSibling;
                    System.out.println("Menu found via parent's next sibling.");
                } else {
                    // Sometimes the table is nested further, e.g., <p><strong>Date</strong></p> <div> <figure><table>...
                    Element parentsNextSiblingNext = parentsNextSibling != null ? parentsNextSibling.nextElementSibling() : null;
                    if (isValidMenuContainer(parentsNextSiblingNext)) {
                        potentialMenuContainer = parentsNextSiblingNext;
                        System.out.println("Menu found via parent's next->next sibling.");
                    }
                }
            }
        }

        // 3. More Robust Search: Look for the *first* figure sibling *after* the date's container
        if (potentialMenuContainer == null) {
            Element searchStartElement = titleContainingDate.parent() != null ? titleContainingDate.parent() : titleContainingDate;
            Element sibling = searchStartElement.nextElementSibling();
            while (sibling != null) {
                if (isValidMenuContainer(sibling)) {
                    potentialMenuContainer = sibling;
                    System.out.println("Menu found via subsequent sibling search.");
                    break;
                }
                // Check if the sibling *contains* the menu figure
                Element containedFigure = sibling.selectFirst("figure.wp-block-table, figure.wp-block-image");
                if (isValidMenuContainer(containedFigure)) {
                    potentialMenuContainer = containedFigure;
                    System.out.println("Menu found nested in subsequent sibling.");
                    break;
                }
                sibling = sibling.nextElementSibling();
            }
        }


        if (potentialMenuContainer == null) {
            throw new RuntimeException("Could not locate a valid menu table or image container following the date element for " + formattedDate);
        }

        // Check for image menu first (less common but possible)
        Element imgElement = potentialMenuContainer.selectFirst("figure.wp-block-image img, img"); // Check inside figure or directly if figure is the img container
        if (imgElement != null && potentialMenuContainer.selectFirst("table") == null) { // Ensure it's not a table with an unrelated image
            System.out.println("Found menu image: " + imgElement.attr("src"));
            return new MenuResult(imgElement);
        }

        // Check for table menu
        Element tableElement = potentialMenuContainer.selectFirst("table");
        if (tableElement != null) {
            Elements tableRows = tableElement.select("tbody tr");
            if (!tableRows.isEmpty()) {
                System.out.println("Found menu table with " + tableRows.size() + " rows.");
                return new MenuResult(tableRows);
            } else {
                throw new RuntimeException("Found a table structure but no rows (tbody tr) within it for " + formattedDate);
            }
        }

        throw new RuntimeException("Located a potential menu container, but it didn't contain a recognizable image or table menu for " + formattedDate);
    }

    /**
     * Checks if an element is likely a container for the menu (either table or image).
     */
    private boolean isValidMenuContainer(Element element) {
        if (element == null) {
            return false;
        }
        // Check if it's a figure containing a table or an image
        boolean isFigure = element.tagName().equalsIgnoreCase("figure");
        boolean hasTable = element.selectFirst("table") != null;
        boolean hasImage = element.selectFirst("img") != null;

        if (isFigure && (element.hasClass("wp-block-table") || element.hasClass("wp-block-image"))) {
            return true;
        }
        // Sometimes the table/image is directly nested without a figure
        if (hasTable || (hasImage && !hasTable)) { // Prioritize table if both exist
            return true;
        }

        // Check if the element *is* the table itself
        if (element.tagName().equalsIgnoreCase("table")) {
            return true;
        }

        // Check if it's a div that *contains* the figure/table (another common pattern)
        if(element.tagName().equalsIgnoreCase("div")) {
            return element.selectFirst("figure.wp-block-table, figure.wp-block-image, table") != null;
        }


        return false;
    }


    /**
     * Extracts a descriptive name for an icon, preferring 'title' then 'alt'.
     * @param imgElement The Jsoup Element representing the <img> tag.
     * @return A descriptive string for the icon.
     */
    public String extractIconDescription(Element imgElement) {
        String title = imgElement.attr("title");
        if (title != null && !title.trim().isEmpty()) {
            return title.trim();
        }
        String alt = imgElement.attr("alt");
        if (alt != null && !alt.trim().isEmpty()) {
            return alt.trim();
        }
        // Fallback: Use the helper to get filename if description is missing
        System.out.println("Warning: Icon missing title and alt attributes. Falling back to filename.");
        return scraperHelper.extractFileNameWithoutExtension(imgElement.attr("src"));
    }

    /**
     * Processes the HTML content of a table cell (td) to extract meal options.
     * It iterates through child nodes (text and images) to group them correctly.
     *
     * @param htmlContent The inner HTML content of a table cell (e.g., from td.html()).
     * @param mealOptions The list to add extracted MealOption objects to.
     */
    @Override
    public void processContentFromRow(String htmlContent, List<MealOption> mealOptions) {
        // Parse the HTML content of the cell as a body fragment
        // Using parseBodyFragment prevents Jsoup from adding extra <html><head><body> tags
        Document doc = Jsoup.parseBodyFragment(htmlContent);
        Element body = doc.body(); // Get the body element containing the parsed content

        MealOption currentMealOption = null;

        // Iterate through the direct children nodes of the parsed content
        for (Node node : body.childNodes()) {
            if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                // Get the literal text, trim leading/trailing whitespace
                String text = textNode.getWholeText().trim();

                // Further clean the text: replace multiple spaces with one, remove quotes
                text = text.replaceAll("\\s+", " ").trim();
                if (text.startsWith("\"") && text.endsWith("\"") && text.length() > 1) {
                    text = text.substring(1, text.length() - 1).trim();
                }

                // If the cleaned text is not empty, it marks a new meal item
                if (!text.isEmpty()) {
                    // If we were building a previous meal option, add it to the list now.
                    if (currentMealOption != null) {
                        mealOptions.add(currentMealOption);
                    }
                    // Start a new meal option
                    currentMealOption = new MealOption();
                    currentMealOption.setName(text);
                }
            } else if (node instanceof Element) {
                Element element = (Element) node;
                // Check if the element is an image (icon)
                if (element.tagName().equalsIgnoreCase("img")) {
                    // If we find an image and we have a current meal option being built,
                    // add the icon description to it.
                    if (currentMealOption != null) {
                        String iconDescription = extractIconDescription(element); // Use the dedicated method
                        currentMealOption.addIcon(iconDescription);
                    } else {
                        // This case means an icon appeared before any text in the cell,
                        // or directly after a previous item was finalized. Log it.
                        System.out.println("Warning: Found icon '" + extractIconDescription(element) + "' without preceding meal text in current cell fragment.");
                    }
                } else if (element.tagName().equalsIgnoreCase("br")) {
                    // <br> tags might exist but are generally ignored in this node-traversal logic.
                    // Separation is primarily handled by encountering new non-empty TextNodes.
                    // If a <br> consistently separated items that weren't text, logic could be added here.
                }
                // Handle other potential elements within the <td> if needed
            }
        }

        // After iterating through all nodes, add the last meal option if it exists
        if (currentMealOption != null) {
            mealOptions.add(currentMealOption);
        }
    }
}