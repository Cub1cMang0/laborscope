package com.laborscope;

// Jsoup imports to handle HTMLs
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

// BaseRobotRules in order to avoid IP block for not following robots.txt
import crawlercommons.robots.BaseRobotRules;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

// Imports Kafka producer to send jobs
import com.laborscope.kafka.CrawlJobProducer;
import com.laborscope.kafka.CrawlJobProducer.CrawlJob;

// Imports for page saving into PostgreSQL
import com.laborscope.model.CrawledPage;
import com.laborscope.repository.CrawledPageRepository;

// Imports for Redis url duplication checking
import org.springframework.data.redis.core.RedisTemplate;

// Basic java data types
import java.io.IOException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.time.LocalDateTime;

@Service
public class LaborScopeApplication {
    // Set up class variables and have them be dependecy injected
    private final RobotHandler robotsChecker;
    private final CrawlJobProducer crawlProducer;
    private final List<String[]> productData = Collections.synchronizedList(new ArrayList<>());
    private final int maxDepth;
    private final long DELAY_MS;
    private long lastRequestTime = 0;
    private static final String REDIS_KEY_PREFIX = "visited:url:";

    @Autowired
    private CrawledPageRepository crawledPageRepository;
    private RedisTemplate<String, Object> redisCacheTemplate;

    // Constructor
    public LaborScopeApplication(
            RobotHandler robotsChecker, 
            CrawlJobProducer crawlProducer,
            @Value("${crawler.max-depth:2}") int maxDepth,
            @Value("${crawler.request-delay-ms:1000}") long delayMs) {
        this.robotsChecker = robotsChecker;
        this.crawlProducer = crawlProducer;
        this.maxDepth = maxDepth;
        this.DELAY_MS = delayMs;
    }

    // Crawls the specified website
    public void startCrawl(CrawlJob job) {
        try {
            // Initialize web url to begin crawling (wikipedia is the dummy url for testing)
            String baseUrl = "https://en.wikipedia.org";
            String userAgent = "LaborScope/1.0";
            // Initialize robots.txt from the baseUrl.
            BaseRobotRules rules = robotsChecker.fetchRules(baseUrl, userAgent);
            // Crawl and export data
            crawl(job, rules);
        }
        catch (IOException e) {
            System.err.println("Failed to fetch robots.txt: " + e.getMessage());
        }
    }

    // ... retrives the HTML contents of the url (pretty self explanatory)
    private Document retrieveHTML(String url) {
        try {
            // Fetches the HTML contents while enforcing requests rate limit to avoid being IP blocked (bad)
            enforceRateLimit();
            return Jsoup.connect(url).userAgent("Mozilla/5.0 (Compatible; MyBot/1.0)").timeout(10000).get();
        } catch (IOException e) {
            System.out.println("Error fetching " + url + ": " + e.getMessage());
            return null;
        }
    }
    
    // Recursively crawls the webpage given while enforcing robots.txt to prevent causing issues to the website domain
    private void crawl(CrawlJob job, BaseRobotRules rules) {
        String url = job.url();
        int depth = job.depth();
        // Avoids non related http / https links
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return;
        }
        // Stops crawl when max depth is reached
        if (depth > maxDepth)
        {
            System.out.printf("Max depth of %d reached", maxDepth);
            return;
        }
        String redisKey = REDIS_KEY_PREFIX + url;
        boolean visited = Boolean.TRUE.equals(redisCacheTemplate.hasKey(redisKey));
        if (!visited)
        {
            visited = crawledPageRepository.existsByUrl(url);
            if (visited)
            {
                redisCacheTemplate.opsForValue().set(redisKey, "true");
            }
        }
        if (visited)
        {
            System.out.println("Skipping (Cache/DB hit): " + url);
            return;
        }
        // Prevents visiting urls defined in the robots.txt file
        if (rules != null && !robotsChecker.isAllowed(rules, url)) {
            System.out.println(url + " blocked by robots.txt");
            return;
        }
        System.out.println("Crawling: " + url);
        redisCacheTemplate.opsForValue().set(redisKey, "true");
        Document doc = retrieveHTML(url);
        if (doc != null) 
        {
            String[] doc_info = extractData(doc);
            String doc_title = doc_info[0];
            String doc_content = doc_info[1];
            CrawledPage page = new CrawledPage(url, doc_title, doc_content, LocalDateTime.now(), depth);
            crawledPageRepository.save(page);
            // Extracts pagination links on Wikipedia (this process is a placeholder until I get the distributrd system working)
            Elements paginationLinks = doc.select("div.mw-parser-output p a[href^='/wiki/']");
            for (Element link : paginationLinks) 
            {
                String nextUrl = link.absUrl("href");
                if (!nextUrl.isEmpty()) 
                {
                    if (!Boolean.TRUE.equals(redisCacheTemplate.hasKey(REDIS_KEY_PREFIX + nextUrl)))
                    {
                        crawlProducer.publish(nextUrl, depth + 1);
                    }
                }
            }
        }
    }

    // Extracts and formats the web-scraped data from the visited URL
    private String[] extractData(Document document) {
        String title = escapeCsv(document.select("h1#firstHeading").text());    
        String pageText = document.body().text();    
        return new String[] {title, pageText};
    }

    // Prevents early termination of paragraph data when parsing data into a CSV file 
    private String escapeCsv(String data) {
        if (data == null || data.isEmpty()) return "";
        String cleanData = data.replaceAll("\\[\\d+\\]", "");
        cleanData = cleanData.replace("\"", "\"\"");
        return "\"" + cleanData.trim() + "\"";
    }

    // Enforces request limit in order to avoid being IP blocked by the crawl-targeted website
    private void enforceRateLimit() {
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
    public void exportDataToCsv(String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.append("Page Title,Value\n");
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