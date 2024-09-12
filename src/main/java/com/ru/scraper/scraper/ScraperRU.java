package com.ru.scraper.scraper;

import com.ru.scraper.data.meal.MealOption;
import com.ru.scraper.data.response.MenuResult;
import com.ru.scraper.helper.ScraperHelper;
import com.ru.scraper.helper.Utils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

@Component
public class ScraperRU implements IScraperRU {

    private static final int TIMEOUT_CONNECTION = 40000; // 35 seconds
    private static final int RETRY_DELAY = 2000; // 1 second
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
                System.out.println("Response Headers: " + response.headers());

                if (response.statusCode() == 200) {
                    return response.parse();
                } else {
                    System.out.println("Unexpected HTTP status code: " + response.statusCode());
                    throw new RuntimeException("Failed to retrieve content from the website due to unexpected HTTP " + "status code: " + response.statusCode());
                }
            } catch (IOException e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String exceptionDetails = sw.toString();

                System.out.println("Failed to connect to " + webURL + " on attempt " + attempt);
                System.out.println("Exception Details: " + exceptionDetails);

                if (attempt >= MAX_RETRIES) {
                    throw new RuntimeException("Failed to retrieve content from the website after " + MAX_RETRIES + " attempts. Exception: " + exceptionDetails);
                }

                Thread.sleep(RETRY_DELAY);
            }
        }

        return null;
    }



    @Override
    public MenuResult parseTableHtml(Document htmlDocument, String formattedDate) throws InterruptedException {

        System.out.println("Trying to get a menu for the day " + formattedDate);
        Element titleContainingDate = htmlDocument.selectFirst("p:contains(" + formattedDate + ")");

        if (titleContainingDate == null) {
            titleContainingDate = htmlDocument.selectFirst("figcaption:contains(" + formattedDate + ")");
        }


        if (titleContainingDate == null) {
            titleContainingDate = htmlDocument.selectFirst("strong:contains(" + formattedDate + ")");
        }

        if (titleContainingDate == null) {
            throw new RuntimeException("No menu found with the given date " + formattedDate);
        }

        Element menuFromWeekday = titleContainingDate.nextElementSibling();
        System.out.println("Menu from weekday: " + menuFromWeekday);

        if (menuFromWeekday.selectFirst("figure.wp-block-image") != null) {
            Element imgElementMenu = menuFromWeekday.selectFirst("img");
            if (imgElementMenu != null) {
                return new MenuResult(imgElementMenu);
            }
        }

        if (menuFromWeekday.selectFirst("figure.wp-block-table") != null) {
            Elements tableRows = menuFromWeekday.select("table tbody tr");
            return new MenuResult(tableRows);
        }

        throw new RuntimeException("No menu found for the specified date.");
    }

    @Override
    public String extractTextFromHtml(String htmlContent) {
        return Jsoup.parse(htmlContent).text();
    }

    @Override
    public String extractImageName(Element imgElement) {
        String src = imgElement.attr("src");
        return scraperHelper.extractFileNameWithoutExtension(src);
    }

    @Override
    public void processContentFromRow(String htmlContent, List<MealOption> mealOptions) {
        String[] contentFromRow = htmlContent.split("<br>");
        for (String contentPart : contentFromRow) {
            MealOption mealOption = this.createMealOption(contentPart);
            if (mealOption != null) {
                mealOptions.add(mealOption);
            }
        }
    }

    @Override
    public MealOption createMealOption(String contentPart) {
        String text = this.extractTextFromHtml(contentPart);

        if (!text.isEmpty()) {
            MealOption mealOption = new MealOption();
            mealOption.setName(text);

            Elements imgElements = Jsoup.parse(contentPart).select("img");
            for (Element imgElement : imgElements) {
                String imageName = this.extractImageName(imgElement);
                mealOption.addIcon(imageName);
            }

            return mealOption;
        }

        return null;
    }
}
