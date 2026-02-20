package com.aditya.parivarpocket.model;

public enum UserRole {
    STUDENT,
    EDUCATOR;

    public String asMetadata() {
        return name().toLowerCase();
    }

    public static UserRole fromMetadata(String value) {
        if (value == null) {
            return null;
        }
        switch (value.trim().toLowerCase()) {
            case "student":
                return STUDENT;
            case "educator":
                return EDUCATOR;
            default:
                return null;
        }
    }
}
