package com.athena.parivarpocket.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Alert {
    private final String id;
    private final String userEmail;
    private final String category;
    private final String severity;
    private final String message;
    private final Map<String, String> metadata;
    private final LocalDateTime createdAt;
    private final boolean read;

    public Alert(String id,
                 String userEmail,
                 String category,
                 String severity,
                 String message,
                 Map<String, String> metadata,
                 LocalDateTime createdAt,
                 boolean read) {
        this.id = id;
        this.userEmail = userEmail;
        this.category = category;
        this.severity = severity;
        this.message = message;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
        this.createdAt = createdAt;
        this.read = read;
    }

    public String getId() {
        return id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getCategory() {
        return category;
    }

    public String getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public String getMetadataSummary() {
        if (metadata.isEmpty()) {
            return "";
        }
        return metadata.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }
}
