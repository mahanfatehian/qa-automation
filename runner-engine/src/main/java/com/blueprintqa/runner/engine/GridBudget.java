package com.blueprintqa.runner.engine;

import com.blueprintqa.runner.config.SeleniumConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
public class GridBudget {
    private static final Logger log = LoggerFactory.getLogger(GridBudget.class);
    private final Semaphore permits;
    private final int hardware;
    private final StringRedisTemplate redis;
    private volatile int limit;

    public GridBudget(SeleniumConfig selenium, ObjectMapper mapper, StringRedisTemplate redis, @Value("${blueprintqa.runner-slots:0}") int configured) {
        int discovered = discover(selenium.gridUrl(), mapper);
        this.hardware = Math.max(1, configured > 0 ? configured : discovered);
        this.redis = redis;
        this.limit = this.hardware;
        this.permits = new Semaphore(this.hardware);
        log.info("browser slot budget {}", this.hardware);
    }

    public int capacity() {
        refresh();
        return limit;
    }

    private void refresh() {
        int wanted = readWanted();
        int next = wanted > 0 ? Math.min(hardware, wanted) : hardware;
        synchronized (this) {
            int extra = next - limit;
            if (extra > 0) {
                permits.release(extra);
            } else {
                while (extra < 0 && permits.tryAcquire()) {
                    extra++;
                }
            }
            limit = next;
        }
    }

    private int readWanted() {
        try {
            String raw = redis.opsForValue().get("studio:settings");
            if (raw == null || raw.isBlank()) {
                return 0;
            }
            JsonNode node = new ObjectMapper().readTree(raw).path("browser_slots");
            JsonNode root = new ObjectMapper().readTree(raw);
            readyMs = Math.max(0, root.path("ready_ms").asInt(readyMs));
            return root.path("browser_slots").asInt(0);
        } catch (Exception ex) {
            return 0;
        }
    }

    public static volatile int readyMs = 800;

    public void acquire() {
        refresh();
        try {
            if (!permits.tryAcquire(120, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Grid is full. This run waited 120s for a browser slot.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a browser slot");
        }
    }

    public boolean tryAcquire(long seconds) {
        try {
            return permits.tryAcquire(Math.max(0, seconds), TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void release() {
        permits.release();
    }

    private int discover(String gridUrl, ObjectMapper mapper) {
        String url = gridUrl == null ? "" : gridUrl.trim();
        if (url.isBlank()) {
            url = "http://selenium-hub:4444";
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url + "/status")).timeout(Duration.ofSeconds(5)).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode nodes = mapper.readTree(response.body()).path("value").path("nodes");
                int slots = 0;
                if (nodes.isArray()) {
                    for (JsonNode node : nodes) {
                        int max = node.path("maxSessions").asInt(0);
                        if (max <= 0 && node.path("slots").isArray()) {
                            max = node.path("slots").size();
                        }
                        slots += Math.max(0, max);
                    }
                }
                if (slots > 0) {
                    return slots;
                }
            } catch (Exception ex) {
                log.warn("grid status attempt {} failed: {}", attempt, ex.getMessage());
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.warn("could not read grid capacity, using 4 slots");
        return 4;
    }
}
