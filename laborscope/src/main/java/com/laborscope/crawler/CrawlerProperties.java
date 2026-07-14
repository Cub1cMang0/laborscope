package com.laborscope.crawler;

import com.laborscope.crawler.CrawlTarget;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;
import java.util.List;

@Component
@Data
@ConfigurationProperties(prefix = "crawler")
public class CrawlerProperties {
    private List<CrawlTarget> targets;
    private String userAgent;
}
