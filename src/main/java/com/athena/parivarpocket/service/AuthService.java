package com.athena.parivarpocket.service;

import com.athena.parivarpocket.model.User;
import com.athena.parivarpocket.model.UserRole;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class AuthService {
    private final Map<String, String> credentialStore = new HashMap<>();
    private final Map<String, UserRole> roleStore = new HashMap<>();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,}$");

    public AuthService() {
        seedDemoAccounts();
    }

    public User login(String email, String password, UserRole requestedRole) {
        validateEmail(email);
        validatePassword(password);
        String storedPassword = credentialStore.get(normalise(email));
        if (storedPassword == null || !storedPassword.equals(password)) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        UserRole role = roleStore.get(normalise(email));
        if (role != requestedRole) {
            throw new IllegalArgumentException("Role mismatch. Please pick the right profile.");
        }
        String name = email.split("@")[0];
        return new User(email, capitalise(name), role);
    }

    public User register(String email, String password, UserRole role) {
        validateEmail(email);
        validatePassword(password);
        String key = normalise(email);
        if (credentialStore.containsKey(key)) {
            throw new IllegalArgumentException("Account already exists. Please sign in.");
        }
        credentialStore.put(key, password);
        roleStore.put(key, role);
        return new User(email, capitalise(email.split("@")[0]), role);
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Please enter a valid email address");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
    }

    private void seedDemoAccounts() {
        credentialStore.put("student@parivaar.org", "password123");
        roleStore.put("student@parivaar.org", UserRole.STUDENT);
        credentialStore.put("educator@parivaar.org", "password123");
        roleStore.put("educator@parivaar.org", UserRole.EDUCATOR);
    }

    private String normalise(String email) {
        return email.trim().toLowerCase();
    }

    private String capitalise(String value) {
        if (value.isEmpty()) return value;
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
