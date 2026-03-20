package com.laborscope;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class RobotHandler {
    private final OkHttpClient client = new OkHttpClient();
    public BaseRobotRules fetchRules(String baseUrl, String userAgent) throws IOException {
        String robotsUrl = baseUrl.endsWith("/") ? baseUrl + "robots.txt" : baseUrl + "/robots.txt";
        Request request = new Request.Builder().url(robotsUrl).header("User-Agent", userAgent).build();
        try (Response response = client.newCall(request).execute()) {
            SimpleRobotRulesParser parser = new SimpleRobotRulesParser();            
            if (!response.isSuccessful()) {
                return parser.failedFetch(response.code());
            }
            byte[] content = response.body().bytes();
            String contentType = response.header("Content-Type", "text/plain");
            List<String> agentNames = Collections.singletonList(userAgent);
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