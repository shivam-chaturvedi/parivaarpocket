package com.athena.parivarpocket.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SupabaseClient {
    private static final String BASE_URL = "https://wfepviatoqylkfxtvupa.supabase.co";
    private static final String REST_ENDPOINT = BASE_URL + "/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndmZXB2aWF0b3F5bGtmeHR2dXBhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjUyNjI1OTQsImV4cCI6MjA4MDgzODU5NH0.tBymCssYhkShOQiRF7xtWcp9c1a_1lLEdL8Cz5LmitU";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    public JsonArray fetchTable(String table) {
        return fetchTable(table, null, null);
    }

    public JsonArray fetchTable(String table, String queryParams, String bearerToken) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(buildFetchUri(table, queryParams)))
                .header("apikey", API_KEY)
                .header("Accept", "application/json")
                .GET();
        if (bearerToken != null && !bearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        } else {
            builder.header("Authorization", "Bearer " + API_KEY);
        }
        HttpRequest request = builder.build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Supabase table fetch failed (" + table + "): " + response.body());
            }
            return parseArray(response.body());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to reach Supabase for table " + table, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Supabase request was interrupted", e);
        }
    }

    private String buildFetchUri(String table, String queryParams) {
        StringBuilder builder = new StringBuilder(REST_ENDPOINT).append(table).append("?select=*");
        if (queryParams != null && !queryParams.isBlank()) {
            if (!queryParams.startsWith("&")) {
                builder.append("&");
            }
            builder.append(queryParams);
        }
        return builder.toString();
    }

    private String buildWriteUri(String table, String queryParams) {
        StringBuilder builder = new StringBuilder(REST_ENDPOINT).append(table);
        if (queryParams != null && !queryParams.isBlank()) {
            if (!queryParams.startsWith("?")) {
                builder.append("?");
            }
            builder.append(queryParams);
        }
        return builder.toString();
    }

    public JsonArray insertRecord(String table, String queryParams, JsonElement payload, String bearerToken) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(buildWriteUri(table, queryParams)))
                .header("apikey", API_KEY)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates,return=representation");
        JsonElement normalizedPayload = payload;
        if ("jobs".equals(table) && payload != null && payload.isJsonObject()) {
            System.err.println("[SupabaseClient] jobs insert received JsonObject, wrapping into JsonArray");
            JsonArray array = new JsonArray();
            array.add(payload.getAsJsonObject());
            normalizedPayload = array;
        }
        builder.POST(HttpRequest.BodyPublishers.ofString(gson.toJson(normalizedPayload)));
        if (bearerToken != null && !bearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        } else {
            builder.header("Authorization", "Bearer " + API_KEY);
        }
        HttpRequest request = builder.build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Supabase insert failed (" + table + "): " + response.body());
            }
            return parseArray(response.body());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to reach Supabase for table " + table, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Supabase request was interrupted", e);
        }
    }

    public void deleteRecord(String table, String queryParams, String bearerToken) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(buildWriteUri(table, queryParams)))
                .header("apikey", API_KEY)
                .DELETE();
        
        if (bearerToken != null && !bearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        } else {
            builder.header("Authorization", "Bearer " + API_KEY);
        }
        
        HttpRequest request = builder.build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400 && response.statusCode() != 404) {
                throw new IllegalStateException("Supabase delete failed (" + table + "): " + response.body());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to reach Supabase for deletion in table " + table, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Supabase delete request was interrupted", e);
        }
    }

    private JsonArray parseArray(String body) {
        if (body == null || body.isBlank()) {
            return new JsonArray();
        }
        JsonElement element = gson.fromJson(body, JsonElement.class);
        if (element == null || !element.isJsonArray()) {
            return new JsonArray();
        }
        return element.getAsJsonArray();
    }
}

