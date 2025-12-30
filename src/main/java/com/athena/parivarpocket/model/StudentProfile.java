package com.athena.parivarpocket.model;

import java.time.LocalDateTime;

public class StudentProfile {
    private final String id;
    private final String email;
    private final String role;
    private final LocalDateTime createdAt;

    public StudentProfile(String id, String email, String role, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
