package com.blueprintqa.runner.engine;

import com.blueprintqa.runner.config.RedisConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Semaphore;

@Component
public class RedisJobConsumer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(RedisJobConsumer.class);
    private final StringRedisTemplate redis;
    private final RedisConfig redisConfig;
    private final DagExecutorService executor;
    private final Semaphore waiting;

    public RedisJobConsumer(StringRedisTemplate redis, RedisConfig redisConfig, DagExecutorService executor, @Value("${blueprintqa.runner-queue:64}") int queueDepth) {
        this.redis = redis;
        this.redisConfig = redisConfig;
        this.executor = executor;
        this.waiting = new Semaphore(Math.max(1, queueDepth));
    }

    @Override
    public void run(ApplicationArguments args) {
        Thread.ofVirtual().name("blueprint-consumer").start(() -> {
            log.info("listening on {}", redisConfig.queue());
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String payload = pop();
                    if (payload != null) {
                        waiting.acquire();
                        String job = payload;
                        Thread.ofVirtual().name("blueprint-run").start(() -> {
                            try {
                                executor.executeJob(job);
                            } finally {
                                waiting.release();
                            }
                        });
                    }
                } catch (Exception ex) {
                    log.error("queue read failed", ex);
                    sleep(2000L);
                }
            }
        });
    }

    private String pop() {
        return redis.execute((RedisCallback<String>) connection -> {
            byte[] key = redisConfig.queue().getBytes(StandardCharsets.UTF_8);
            List<byte[]> result = connection.bLPop(5, key);
            if (result == null || result.size() < 2 || result.get(1) == null) {
                return null;
            }
            return new String(result.get(1), StandardCharsets.UTF_8);
        });
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
