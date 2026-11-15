package com.laborscope;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.laborscope.kafka.CrawlJobProducer;

@RestController
public class CrawlController {

    private static final String WIKIPEDIA_ORIGIN = "https://en.wikipedia.org";
    private static final String REDIS_KEY_PREFIX = "visited:url:";

    private final CrawlJobProducer crawlJobProducer;
    private final RedisTemplate<String, String> redisCacheTemplate;
    private final String defaultSeedUrl;

    public CrawlController(
            CrawlJobProducer crawlJobProducer,
            RedisTemplate<String, String> redisCacheTemplate,
            @Value("${crawler.default-seed-url:https://en.wikipedia.org/wiki/Computer_science}") String defaultSeedUrl) {
        this.crawlJobProducer = crawlJobProducer;
        this.redisCacheTemplate = redisCacheTemplate;
        this.defaultSeedUrl = defaultSeedUrl;
    }

    @PostMapping("/crawl")
    public ResponseEntity<String> startCrawl(@RequestParam(required = false) String url) {
        boolean useDefault = url == null || url.isBlank();
        String constructedSeed = normalizeWikiaUrl(useDefault ? defaultSeedUrl : url);
        if (constructedSeed == null) {
            return ResponseEntity.badRequest()
                    .body("Only English Wikipedia URLs are supported, e.g. " + WIKIPEDIA_ORIGIN + "/wiki/Apache_Kafka\n");
        }
        if (Boolean.TRUE.equals(redisCacheTemplate.hasKey(REDIS_KEY_PREFIX + constructedSeed))) {
            return ResponseEntity.status(409)
                    .body("Already crawled: " + constructedSeed + "\nTo start fresh, stop the app and run: docker compose down -v\n");
        }
        // The seed is depth 0; crawler.max-depth is the number of link hops allowed from it
        crawlJobProducer.publish(constructedSeed, 0);
        return ResponseEntity.accepted()
                .body("Crawl started from " + constructedSeed + (useDefault ? " (default seed)" : "") + "\n");
    }

    private static String normalizeWikiaUrl(String input) {
        try {
            URI uri = URI.create(input.trim());
            String decodedPath = uri.getPath();
            String rawPath = uri.getRawPath();
            boolean isValid = "https".equals(uri.getScheme())
                    && "en.wikipedia.org".equalsIgnoreCase(uri.getHost())
                    && uri.getPort() == -1
                    && decodedPath != null
                    && decodedPath.startsWith("/wiki/")
                    && decodedPath.length() > "/wiki/".length()
                    && !decodedPath.substring("/wiki/".length()).contains(":");
            return isValid ? WIKIPEDIA_ORIGIN + rawPath : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}