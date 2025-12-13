package com.athena.parivarpocket.service;

import com.athena.parivarpocket.model.User;
import com.athena.parivarpocket.model.UserRole;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

public class AuthService {
    private static final String SUPABASE_URL = "https://wfepviatoqylkfxtvupa.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndmZXB2aWF0b3F5bGtmeHR2dXBhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjUyNjI1OTQsImV4cCI6MjA4MDgzODU5NH0.tBymCssYhkShOQiRF7xtWcp9c1a_1lLEdL8Cz5LmitU";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,}$");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Gson gson = new Gson();
    private final ProfileService profileService = new ProfileService();

    public User login(String email, String password) {
        validateEmail(email);
        validatePassword(password);

        JsonObject loginBody = new JsonObject();
        loginBody.addProperty("email", email);
        loginBody.addProperty("password", password);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/auth/v1/token?grant_type=password"))
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(loginBody)))
                .build();

        String requestPayload = gson.toJson(loginBody);
        JsonObject payload = executeRequest(request, requestPayload);
        UserRole profileRole = profileService.fetchRoleByEmail(email);
        return createUserFromResponse(payload, profileRole);
    }

    public User register(String email, String password, UserRole role) {
        validateEmail(email);
        validatePassword(password);
        if (role == null) {
            throw new IllegalArgumentException("Please select a role");
        }

        JsonObject body = buildRegistrationBody(email, password, role);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/auth/v1/signup"))
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        JsonObject payload = executeRequest(request, gson.toJson(body));
        profileService.upsertProfile(email, role);
        return createUserFromResponse(payload, role);
    }

    private JsonObject executeRequest(HttpRequest request, String requestBody) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String rawBody = response.body();
            JsonObject payload = safeParse(rawBody);
            int status = response.statusCode();
            if (status >= 400) {
                logAuthExchange(request, requestBody, status, rawBody);
                throw new IllegalArgumentException(extractErrorMessage(payload, rawBody, "Supabase authentication failed"));
            }
            logAuthExchange(request, requestBody, status, rawBody);
            return payload;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to reach Supabase", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Authentication request was interrupted", e);
        }
    }

    private void logAuthExchange(HttpRequest request, String requestBody, int statusCode, String responseBody) {
        System.out.println("[AuthService] " + request.method() + " " + request.uri());
        if (requestBody != null) {
            System.out.println("[AuthService] Request payload: " + requestBody);
        }
        System.out.println("[AuthService] Response status: " + statusCode);
        if (responseBody != null) {
            System.out.println("[AuthService] Response payload: " + responseBody);
        }
    }

    private User createUserFromResponse(JsonObject payload, UserRole overrideRole) {
        if (payload == null) {
            throw new IllegalStateException("Unexpected response from Supabase");
        }
        if (!payload.has("user")) {
            throw new IllegalArgumentException(extractErrorMessage(payload, null, "Supabase did not return a user"));
        }
        JsonObject userJson = payload.getAsJsonObject("user");
        String email = valueOrThrow(userJson, "email", "Supabase did not return an email");
        String metadataRole = extractMetadataValue(userJson, "role");
        UserRole storedRole = UserRole.fromMetadata(metadataRole);
        UserRole resolvedRole = resolveRole(overrideRole, storedRole);
        String name = resolveDisplayName(userJson, email);
        String userId = valueOrThrow(userJson, "id", "Supabase did not return an id");
        String accessToken = payload.has("access_token") && !payload.get("access_token").isJsonNull()
                ? payload.get("access_token").getAsString()
                : null;
        return new User(userId, email, capitalise(name), resolvedRole, accessToken);
    }

    private UserRole resolveRole(UserRole overrideRole, UserRole metadataRole) {
        if (overrideRole != null) {
            if (metadataRole != null && metadataRole != overrideRole) {
                System.out.println("[AuthService] Profile role differs from stored metadata, using profile value.");
            }
            return overrideRole;
        }
        return metadataRole != null ? metadataRole : UserRole.STUDENT;
    }

    private JsonObject buildRegistrationBody(String email, String password, UserRole role) {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);
        JsonObject options = new JsonObject();
        JsonObject data = new JsonObject();
        data.addProperty("role", role.asMetadata());
        options.add("data", data);
        body.add("options", options);
        return body;
    }

    private String extractMetadataValue(JsonObject userJson, String key) {
        if (userJson.has("user_metadata")) {
            JsonObject metadata = userJson.getAsJsonObject("user_metadata");
            if (metadata.has(key)) {
                return metadata.get(key).getAsString();
            }
        }
        return null;
    }

    private String resolveDisplayName(JsonObject userJson, String email) {
        for (String key : new String[]{"full_name", "name"}) {
            String candidate = extractMetadataValue(userJson, key);
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        String prefix = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        return prefix;
    }

    private String extractErrorMessage(JsonObject payload, String rawBody, String fallback) {
        if (payload == null) {
            return fallbackWithBody(fallback, rawBody);
        }
        if (payload.has("error_description")) {
            return payload.get("error_description").getAsString();
        }
        if (payload.has("error")) {
            return payload.get("error").getAsString();
        }
        if (payload.has("message")) {
            return payload.get("message").getAsString();
        }
        if (payload.has("msg")) {
            return payload.get("msg").getAsString();
        }
        if (payload.has("details")) {
            return payload.get("details").getAsString();
        }
        return fallback;
    }

    private JsonObject safeParse(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return gson.fromJson(body, JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String fallbackWithBody(String fallback, String body) {
        if (body == null || body.isBlank()) {
            return fallback;
        }
        return fallback + ": " + body.trim();
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

    private String valueOrThrow(JsonObject source, String key, String message) {
        if (source == null || !source.has(key) || source.get(key).isJsonNull()) {
            throw new IllegalStateException(message);
        }
        return source.get(key).getAsString();
    }

    private String capitalise(String value) {
        if (value == null || value.isBlank()) return value;
        String trimmed = value.trim();
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

}
