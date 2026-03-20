package com.laborscope;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import crawlercommons.robots.BaseRobotRules;

import java.io.IOException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LaborScopeApplication {
    private static Set<String> visitedUrls = new HashSet<>();
    private static List<String[]> productData = new ArrayList<>();
    private static final RobotHandler robotsChecker = new RobotHandler();
    private static int maxDepth = 2;
    private static long lastRequestTime = 0;
    private static final long DELAY_MS = 1000;

    // Crawls the specified website
    public static void main(String[] args) {
        try {
            String baseUrl = "https://en.wikipedia.org";
            String userAgent = "WebCrawler/1.0";
            BaseRobotRules rules = robotsChecker.fetchRules(baseUrl, userAgent);
            String seedUrl = "https://en.wikipedia.org/wiki/Kingdom_Hearts";
            crawl(seedUrl, 1, rules);
            exportDataToCsv("wikidata.csv");
        }
        catch (IOException e) {
            System.err.println("Failed to fetch robots.txt: " + e.getMessage());
        }
    }

    // ... retrives the HTML contents of the url (pretty self explanatory)
    private static Document retrieveHTML(String url) {
        try {
            enforceRateLimit();
            return Jsoup.connect(url).userAgent("Mozilla/5.0 (Compatible; MyBot/1.0)").timeout(10000).get();
        } catch (IOException e) {
            System.out.println("Error fetching " + url + ": " + e.getMessage());
            return null;
        }
    }
    
    // Recursively crawls the webpage given while enforcing robots.txt to prevent causing issues to the website domain
    private static void crawl(String url, int depth, BaseRobotRules rules) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return;
        }
        if (depth > maxDepth || visitedUrls.contains(url)) {
            return;
        }
        if (rules != null && !robotsChecker.isAllowed(rules, url)) {
            System.out.println(url + " blocked by robots.txt");
            return;
        }
        System.out.println("Crawling: " + url);
        visitedUrls.add(url);
        Document doc = retrieveHTML(url);
        if (doc != null) {
            extractData(doc);
            Elements paginationLinks = doc.select("div.mw-parser-output p a[href^='/wiki/']");
            for (Element link : paginationLinks) {
                String nextUrl = link.absUrl("href");
                if (!nextUrl.isEmpty() && !visitedUrls.contains(nextUrl)) {
                    crawl(nextUrl, depth + 1, rules);
                }
            }
        }
    }

    // Extracts and formats the web-scraped data from the visited URL
    private static void extractData(Document document) {
        String title = escapeCsv(document.select("h1#firstHeading").text());        
        Element introElem = document.select("div.mw-parser-output > p:not(.mw-empty-elt)").first();
        String intro = (introElem != null) ? escapeCsv(introElem.text()) : "";
        if (!intro.isEmpty()) {
            productData.add(new String[]{title, "Summary/Intro", intro});
        }
        Elements infoboxRows = document.select("table.infobox tr");
        for (Element row : infoboxRows) {
            Element label = row.selectFirst("th.infobox-label");
            Element data = row.selectFirst("td.infobox-data");
            if (label != null && data != null) {
                String key = escapeCsv(label.text());
                String value = escapeCsv(data.text());
                productData.add(new String[]{title, key, value});
            }
        }
    }

    // Prevents early termination of paragraph data when parsing data into a CSV file 
    private static String escapeCsv(String data) {
        if (data == null || data.isEmpty()) return "";
        String cleanData = data.replaceAll("\\[\\d+\\]", "");
        cleanData = cleanData.replace("\"", "\"\"");
        return "\"" + cleanData.trim() + "\"";
    }

    // Enforces request limit in order to avoid being IP blocked by the crawl-targeted website
    private static void enforceRateLimit() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTime;
        if (timeSinceLastRequest < DELAY_MS) {
            try {
                Thread.sleep(DELAY_MS - timeSinceLastRequest);
            } catch (InterruptedException e) {
                System.err.println("Rate limiter interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTime = System.currentTimeMillis();
    }

    // Formats JSoup HTML-Parsed page information
    private static void exportDataToCsv(String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.append("Page Title,Attribute,Value\n");
            for (String[] row : productData) {
                writer.append(String.join(",", row)).append("\n");
            }
            System.out.println("--- Export Complete ---");
            System.out.println("Data successfully saved to: " + fileName);
        } catch (IOException e) {
            System.err.println("Error writing to CSV: " + e.getMessage());
        }
    }
}