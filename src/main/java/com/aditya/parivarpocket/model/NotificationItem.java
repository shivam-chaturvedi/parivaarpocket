package com.aditya.parivarpocket.model;

import java.time.LocalDate;

public class NotificationItem {
    private final String title;
    private final String description;
    private final String severity;
    private final LocalDate date;

    public NotificationItem(String title, String description, String severity, LocalDate date) {
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getSeverity() {
        return severity;
    }

    public LocalDate getDate() {
        return date;
    }
}
