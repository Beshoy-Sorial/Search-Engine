package com.example.Backend.RobotsChecking;


import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;

public class RobotsChecker {
    private static final String[] USER_AGENTS = {"MyCrawlerBot", "Googlebot"};
    private static final Map<String, BaseRobotRules> rulesCache = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastAccessTimes = new ConcurrentHashMap<>();
    
    
    public boolean isAllowed(String urlToCheck) {
        try {
            URI uri = new URI(urlToCheck).normalize();
            String host = uri.getScheme() + "://" + uri.getHost();

            BaseRobotRules rules = rulesCache.computeIfAbsent(host, h -> {
                try {
                    URI robotsUri = new URI(uri.getScheme(), uri.getHost(), "/robots.txt", null);
                    URLConnection connection = robotsUri.toURL().openConnection();
                    connection.setRequestProperty("User-Agent", 
                        USER_AGENTS[ThreadLocalRandom.current().nextInt(USER_AGENTS.length)]);
                    connection.setConnectTimeout(10_000);
                    connection.setReadTimeout(10_000);

                    try (InputStream in = connection.getInputStream()) {
                        SimpleRobotRulesParser parser = new SimpleRobotRulesParser();
                        return parser.parseContent(
                            robotsUri.toString(),
                            in.readAllBytes(),
                            "text/plain",
                            USER_AGENTS[0]
                        );
                    }
                } catch (Exception ex) {
                    return ex instanceof FileNotFoundException ? 
                        new SimpleRobotRules(SimpleRobotRules.RobotRulesMode.ALLOW_ALL) :
                        new SimpleRobotRules(SimpleRobotRules.RobotRulesMode.ALLOW_NONE);
                }
            });

            if (rules.getCrawlDelay() > 0) {
                long lastAccess = lastAccessTimes.getOrDefault(host, 0L);
                long delay = rules.getCrawlDelay() * 1000L - (System.currentTimeMillis() - lastAccess);
                if (delay > 0) Thread.sleep(delay);
                lastAccessTimes.put(host, System.currentTimeMillis());
            }

            return rules.isAllowed(urlToCheck);
        } catch (Exception e) {
            return true; 
        }
    }
}