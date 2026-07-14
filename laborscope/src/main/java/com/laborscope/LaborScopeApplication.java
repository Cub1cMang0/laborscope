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

// Imports Kafka producer to send jobs
import com.laborscope.kafka.CrawlJobProducer;
import com.laborscope.kafka.NlpJobProducer;
import com.laborscope.kafka.CrawlJobProducer.CrawlJob;

// Imports for page saving into PostgreSQL
import com.laborscope.model.CrawledPage;
import com.laborscope.repository.CrawledPageRepository;

// Imports for crawler target configuration of data usage
import com.laborscope.crawler.CrawlerProperties;
import com.laborscope.crawler.CrawlTarget;

// Imports for Redis url duplication checking
import org.springframework.data.redis.core.RedisTemplate;

// Basic java data types
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
public class LaborScopeApplication {
    // Set up class variables and have them be dependecy injected
    private final RobotHandler robotsChecker;
    private final CrawlJobProducer crawlProducer;
    private final NlpJobProducer nlpProducer;
    private final CrawlerProperties crawlerProperties;
    private static final String REDIS_KEY_PREFIX = "visited:url:";
    @Autowired
    private RedisTemplate<String, String> redisCacheTemplate;
    @Autowired
    private CrawledPageRepository crawledPageRepository;

    // Constructor
    public LaborScopeApplication(
            RobotHandler robotsChecker, 
            CrawlJobProducer crawlProducer,
            NlpJobProducer nlpProducer,
            CrawlerProperties crawlerProperties) {
        this.robotsChecker = robotsChecker;
        this.crawlProducer = crawlProducer;
        this.nlpProducer = nlpProducer;
        this.crawlerProperties = crawlerProperties;
    }

    public record CrawlContext(int maxDepth, long delayMs, long lastRequestTime) {}

    // Crawls the specified website
    public void startCrawl(CrawlJob job) {
        try {
            // Set user agent
            String userAgent = crawlerProperties.getUserAgent();
            // Grab current target
            CrawlTarget target = crawlerProperties.getTargets().stream().filter(
                t -> t.getName().equals(job.targetName())).findFirst().orElseThrow();
            // Initialize web url to begin crawling (wikipedia is the dummy url for testing)
            String baseUrl = target.getBaseUrl();
            // Initialize robots.txt from the baseUrl.
            BaseRobotRules rules = robotsChecker.fetchRules(baseUrl, userAgent);
            // Create CrawlContext for each possible thread since this is a singleton class
            CrawlContext context = new CrawlContext(target.getMaxDepth(), target.getDelayMs(), 0);
            crawl(job, rules, target, context);
        }
        catch (IOException e) {
            System.err.println("Failed to fetch robots.txt: " + e.getMessage());
        }
    }

    // ... retrives the HTML contents of the url (pretty self explanatory)
    private Document retrieveHTML(String url, long delayMs, long lastRequestTime) {
        try {
            // Fetches the HTML contents while enforcing requests rate limit to avoid being IP blocked (bad)
            enforceRateLimit(delayMs, lastRequestTime);
            return Jsoup.connect(url).userAgent("Mozilla/5.0 (Compatible; MyBot/1.0)").timeout(10000).get();
        } catch (IOException e) {
            // Print out any IO Errors encountered 
            System.out.println("Error fetching " + url + ": " + e.getMessage());
            return null;
        }
    }
    
    // Recursively crawls the webpage given while enforcing robots.txt to prevent causing issues to the website domain
    private void crawl(CrawlJob job, BaseRobotRules rules, CrawlTarget target, CrawlContext crawlContext) {
        String url = job.url();
        int depth = job.depth();
        String targetName = job.targetName();
        int maxDepth = crawlContext.maxDepth();
        long delayMs = crawlContext.delayMs();
        long lastRequestTime = crawlContext.lastRequestTime();
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
        // Construct Redis Key
        String redisKey = REDIS_KEY_PREFIX + url;
        // Check if the given URL has been visited using the Redis Key
        boolean visited = Boolean.TRUE.equals(redisCacheTemplate.hasKey(redisKey));
        if (!visited)
        {
            // Check to ensure that the actual repository hasn't crawled the given url
            visited = crawledPageRepository.existsByUrl(url);
            if (visited)
            {
                // Add the given URL to the Redis Cache Template to ensure future visit check
                redisCacheTemplate.opsForValue().set(redisKey, "true");
            }
        }
        // Skip the URL because it's already been crawled
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
        // Set the URL as visited since it's going to be processed shortly
        redisCacheTemplate.opsForValue().set(redisKey, "true");
        // Retrieve HTML contents
        Document doc = retrieveHTML(url, delayMs, lastRequestTime);
        if (doc != null) 
        {
            Map<String, String> doc_info = extractData(doc, target.getSelectors());
            // Extracts title from page
            String doc_title = doc_info.get("title");
            // Extracts readable, clean body page content from page
            String doc_content = doc_info.get("description");
            // Extracts the link selector to extract pagination links later
            String linkSelector = target.getLinkSelector();
            // Construct a CrawledPage objects from all the necessary data
            CrawledPage page = new CrawledPage(url, doc_title, doc_content, LocalDateTime.now(), depth);
            // Save the page into the repository to be stored in PostgreSQL
            CrawledPage savedPage = crawledPageRepository.save(page);
            // Use the saved page's id and url to publish and be processed by Python and HuggingFace
            nlpProducer.publish(savedPage.getId(), url);
            // Extracts pagination links on the give target website
            Elements paginationLinks = doc.select(linkSelector);
            for (Element link : paginationLinks) 
            {
                // Fetch the next url to crawl
                String nextUrl = link.absUrl("href");
                // Proceed with next urls that belong to the same website
                if (nextUrl.startsWith(target.getBaseUrl()))
                {
                    // Extract article name
                    String urlPath = nextUrl.substring(target.getBaseUrl().length());
                    // Skip pages if the title contains an anchor hash
                    if (!urlPath.contains("#")) 
                    {
                        // Ensure the next url published isn't already in redis cache
                        if (!Boolean.TRUE.equals(redisCacheTemplate.hasKey(REDIS_KEY_PREFIX + nextUrl)))
                        {
                            // Publish the next url to crawl
                            crawlProducer.publish(nextUrl, depth + 1, targetName);
                        }
                    }
                }
            }
        }
    }

    // Extracts and formats the web-scraped data from the visited URL
    private Map<String, String> extractData(Document document, Map<String, String> selectors) {
        Map<String, String> extractedSelectors = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : selectors.entrySet()) {
            String val = document.select(entry.getValue()).text();
            extractedSelectors.put(entry.getKey(), val.isEmpty() ? "" : val);
        }
        return extractedSelectors;
    }

    // Enforces request limit in order to avoid being IP blocked by the crawl-targeted website
    private void enforceRateLimit(long DELAY_MS, long lastRequestTime) {
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
}