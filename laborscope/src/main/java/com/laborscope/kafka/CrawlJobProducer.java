package com.laborscope.kafka;

import org.springframework.stereotype.Service;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;

@Service
public class CrawlJobProducer {
    // Initialize variables required for creating a kafka producer
    private final String crawlTopic;
    private final int maxDepth;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Utilize record crawlJob to ensure depth limit enforcement to prevent IP blocking
    public record CrawlJob(String url, int depth) {}

    // Create CrawlJobProducer initializer
    public CrawlJobProducer(
            // Grab the crawl topic and depth from application.yml
            @Value("${crawler.kafka.topic-crawl-jobs}") String crawlTopic,
            @Value("${crawler.max-depth}") int maxDepth,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.crawlTopic = crawlTopic;
        this.maxDepth = maxDepth;
        this.kafkaTemplate = kafkaTemplate;
    }

    // Topic seed url publisher for the kafka consumer
    public void publish(String url, int depth)
    {
        // Ensure any job published doesn't go beyond the max depth
        if (depth <= maxDepth)
        {
            // Send the topic and CrawlJob
            kafkaTemplate.send(crawlTopic, new CrawlJob(url, depth));
        }
    }
}
