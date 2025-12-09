package com.athena.parivarpocket.model;

import java.util.Objects;

public class User {
    private final String id;
    private final String email;
    private final String name;
    private final UserRole role;
    private final String accessToken;

    public User(String id, String email, String name, UserRole role, String accessToken) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.role = role;
        this.accessToken = accessToken;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public UserRole getRole() {
        return role;
    }

    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }
}
