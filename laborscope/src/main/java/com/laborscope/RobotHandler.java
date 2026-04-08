package com.laborscope;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class RobotHandler {
    
    // Set up base http client
    private final OkHttpClient client = new OkHttpClient();
    public BaseRobotRules fetchRules(String baseUrl, String userAgent) throws IOException {
        // Set up the url and request variables to fetch the given website's robots.txt
        String robotsUrl = baseUrl.endsWith("/") ? baseUrl + "robots.txt" : baseUrl + "/robots.txt";
        Request request = new Request.Builder().url(robotsUrl).header("User-Agent", userAgent).build();
        try (Response response = client.newCall(request).execute()) {
            // Set up RobotsRulesParser to detect if the robots.txt fetch was successful
            SimpleRobotRulesParser parser = new SimpleRobotRulesParser();            
            if (!response.isSuccessful()) {
                return parser.failedFetch(response.code());
            }
            // Collect the data within the robots.txt and parse it
            byte[] content = response.body().bytes();
            String contentType = response.header("Content-Type", "text/plain");
            List<String> agentNames = Collections.singletonList(userAgent);
            // Return contents of robots.txt parsed
            return parser.parseContent(
                baseUrl,
                content,
                contentType,
                agentNames
            );
        }
    }
    public boolean isAllowed(BaseRobotRules rules, String url) {
        return rules.isAllowed(url);
    }
}