package com.aditya.parivarpocket.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class StudentActivity {
    private final String id;
    private final String activityType;
    private final Map<String, String> activityData;
    private final LocalDateTime createdAt;

    public StudentActivity(String id,
                           String activityType,
                           Map<String, String> activityData,
                           LocalDateTime createdAt) {
        this.id = id;
        this.activityType = activityType;
        this.activityData = activityData != null ? Map.copyOf(activityData) : Collections.emptyMap();
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getActivityType() {
        return activityType;
    }

    public Map<String, String> getActivityData() {
        return activityData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getSummary() {
        if (activityData.isEmpty()) {
            return "Activity recorded";
        }
        return activityData.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(" · "));
    }
}
