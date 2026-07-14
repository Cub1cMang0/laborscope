package com.laborscope.crawler;

import lombok.Data;
import java.util.Map;

@Data
public class CrawlTarget {
    private String name;
    private String baseUrl;
    private String seedUrl;
    private String linkSelector;
    private Map<String, String> selectors;
    private int delayMs;
    private int maxDepth;
}
