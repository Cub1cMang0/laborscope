package com.laborscope.kafka;

import org.springframework.stereotype.Service;
import com.laborscope.LaborScopeApplication;
import org.springframework.kafka.annotation.KafkaListener;
import com.laborscope.kafka.CrawlJobProducer.CrawlJob;

@Service
public class CrawlJobConsumer {

    // Crawler object
    private LaborScopeApplication laborScopeCrawler;
    
    // Set uo KafkaListener and define consumption to crawl
    @KafkaListener(topics = "${spring.kakfa.topic-crawl-jobs}", groupId = "${spring.kafka.group-id}")
    public void consume(CrawlJob job)
    {
        laborScopeCrawler.startCrawl(job);
    }
}
