package com.laborscope.kafka;

import org.springframework.stereotype.Service;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;

@Service
public class CrawlJobProducer {
    // Initialize variables required for creating a kafka producer
    private final String topic;
    private final int maxDepth;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Utilize record crawlJob to ensure depth limit enforcement to prevent IP blocking
    public record CrawlJob(String url, int depth) {}

    public CrawlJobProducer(
            @Value("${spring.kafka.topic-crawl-jobs}") String topic,
            @Value("${spring.crawler.max-depth}") int maxDepth,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.topic = topic;
        this.maxDepth = maxDepth;
        this.kafkaTemplate = kafkaTemplate;
    }

    // Topic seed url publisher for the kafka consumer
    public void publish(String url, int depth)
    {
        if (depth <= maxDepth)
        {
            kafkaTemplate.send(topic, new CrawlJob(url, depth));
        }
    }
}
