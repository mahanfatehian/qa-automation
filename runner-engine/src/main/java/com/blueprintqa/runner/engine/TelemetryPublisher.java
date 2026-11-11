package com.blueprintqa.runner.engine;

import com.blueprintqa.runner.config.RedisConfig;
import com.blueprintqa.runner.model.NodeTelemetryEvent;
import com.blueprintqa.runner.model.WorkflowJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TelemetryPublisher {
    private static final Logger log = LoggerFactory.getLogger(TelemetryPublisher.class);
    private final StringRedisTemplate redis;
    private final RedisConfig redisConfig;
    private final ObjectMapper mapper;

    public TelemetryPublisher(StringRedisTemplate redis, RedisConfig redisConfig, ObjectMapper mapper) {
        this.redis = redis;
        this.redisConfig = redisConfig;
        this.mapper = mapper;
    }

    public void publish(WorkflowJob job, String eventType, String nodeId, String nodeType, long durationMs, String message, String errorStack, Map<String, Object> output, String screenshot) {
        NodeTelemetryEvent event = new NodeTelemetryEvent();
        event.runId = job.runId;
        event.workflowId = job.workflowId;
        event.nodeId = nodeId == null ? "" : nodeId;
        event.nodeType = nodeType == null ? "" : nodeType;
        event.eventType = eventType;
        event.timestamp = Instant.now().toString();
        event.durationMs = durationMs;
        event.message = message == null ? "" : message;
        event.errorStack = errorStack == null ? "" : errorStack;
        event.outputData = output == null ? new LinkedHashMap<>() : output;
        event.screenshotBase64 = screenshot == null ? "" : screenshot;
        try {
            String json = mapper.writeValueAsString(event);
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    redis.convertAndSend(redisConfig.telemetryChannel(), json);
                    return;
                } catch (Exception ex) {
                    if (attempt == 3) {
                        log.error("telemetry publish failed for {}", job.runId, ex);
                    } else {
                        Thread.sleep(200L);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("telemetry serialize failed for {}", job.runId, ex);
        }
    }

    public Map<String, Object> safe(Map<String, Object> pins) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (pins == null) {
            return out;
        }
        for (Map.Entry<String, Object> entry : pins.entrySet()) {
            Object value = entry.getValue();
            if (entry.getKey().endsWith("_ref") || value instanceof ElementRef) {
                continue;
            } else if (value instanceof WebElement element) {
                out.put(entry.getKey(), summarize(element));
            } else if (value instanceof String text) {
                out.put(entry.getKey(), text.length() > 4000 ? text.substring(0, 4000) + "..." : text);
            } else if (value instanceof Number || value instanceof Boolean) {
                out.put(entry.getKey(), value);
            } else if (value != null) {
                out.put(entry.getKey(), String.valueOf(value));
            }
        }
        return out;
    }

    private String summarize(WebElement element) {
        try {
            String tag = element.getTagName();
            String text = element.getText();
            if (text != null && text.length() > 120) {
                text = text.substring(0, 120) + "...";
            }
            return tag + (text == null || text.isBlank() ? "" : ": " + text);
        } catch (Exception ex) {
            return "element";
        }
    }
}
