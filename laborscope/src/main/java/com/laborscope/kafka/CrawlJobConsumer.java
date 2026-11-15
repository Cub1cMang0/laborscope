package com.laborscope.kafka;

import org.springframework.stereotype.Service;
import com.laborscope.CrawlerService;
import org.springframework.kafka.annotation.KafkaListener;
import com.laborscope.kafka.CrawlJobProducer.CrawlJob;

@Service
public class CrawlJobConsumer {

    // Crawler object
    private final CrawlerService laborScopeCrawler;

    public CrawlJobConsumer(CrawlerService laborScopeCrawler) {
        this.laborScopeCrawler = laborScopeCrawler;
    }
    
    // Set uo KafkaListener and define consumption to crawl
    @KafkaListener(topics = "${crawler.kafka.topic-crawl-jobs}", groupId = "${spring.kafka.consumer.group-id}", properties = "auto.offset.reset=earliest")
    public void consume(CrawlJob job)
    {
        System.out.println("KAFKA RECEIVED: " + job.url()); // If you don't see this, Kafka is the issue.
        laborScopeCrawler.startCrawl(job);
    }
}
