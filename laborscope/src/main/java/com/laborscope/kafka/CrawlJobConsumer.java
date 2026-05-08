package com.laborscope.kafka;

import org.springframework.stereotype.Service;
import com.laborscope.LaborScopeApplication;
import org.springframework.kafka.annotation.KafkaListener;
import com.laborscope.kafka.CrawlJobProducer.CrawlJob;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class CrawlJobConsumer {

    // Crawler object
    @Autowired
    private LaborScopeApplication laborScopeCrawler;
    
    // Set uo KafkaListener and define consumption to crawl
    @KafkaListener(topics = "${crawler.kafka.topic-crawl-jobs}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(CrawlJob job)
    {
        System.out.println("KAFKA RECEIVED: " + job.url()); // If you don't see this, Kafka is the issue.
        laborScopeCrawler.startCrawl(job);
    }
}
