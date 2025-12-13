package com.athena.parivarpocket.service;

import com.athena.parivarpocket.model.JobOpportunity;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RapidJobService {
    private static final String BASE_URL = "https://indeed12.p.rapidapi.com/jobs/search";
    private static final String HOST = "indeed12.p.rapidapi.com";
    private static final String KEY = "30a5e6fbe9msh906939815ecd3fep1db2b4jsn37a899dab0b7";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public List<JobOpportunity> fetchRecentWestBengalJobs() throws IOException, InterruptedException {
        String params = "?query=software%20developer&location=West%20Bengal,%20India&locality=in&start=0&sort=date";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + params))
                .header("X-RapidAPI-Host", HOST)
                .header("x-rapidapi-key", KEY)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[RapidJobService] job fetch status=" + response.statusCode() + " body=" + response.body());
        if (response.statusCode() >= 400) {
            throw new IOException("Unable to fetch jobs: " + response.body());
        }
        JsonObject payload = gson.fromJson(response.body(), JsonObject.class);
        JsonArray hits = payload != null && payload.has("hits") ? payload.getAsJsonArray("hits") : new JsonArray();
        List<JobOpportunity> jobs = new ArrayList<>();
        for (JsonElement element : hits) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject jobJson = element.getAsJsonObject();
            if (!postedWithin24Hours(jobJson)) {
                continue;
            }
            String title = getAsString(jobJson, "title", "Job opportunity");
            String location = getAsString(jobJson, "location", "West Bengal, India");
            String company = getAsString(jobJson, "company_name", "Hiring partner");
            String relativeTime = getAsString(jobJson, "formatted_relative_time", "");
            String jobLink = getAsString(jobJson, "link", "");
            String jobUrl = jobLink.startsWith("http") ? jobLink : "https://indeed.com" + jobLink;
            JobOpportunity job = new JobOpportunity(
                    title,
                    company,
                    location,
                    "Local",
                    relativeTime,
                    "Competitive",
                    Collections.emptyList(),
                    "Updated " + relativeTime,
                    "",
                    jobUrl,
                    90
            );
            jobs.add(job);
            if (jobs.size() >= 50) {
                break;
            }
        }
        return jobs;
    }

    private boolean postedWithin24Hours(JsonObject json) {
        String relative = getAsString(json, "formatted_relative_time", "").toLowerCase();
        if (relative.contains("hour")) {
            return true;
        }
        if (relative.contains("day")) {
            try {
                String number = relative.split("day")[0].trim();
                if (number.isBlank()) {
                    return true;
                }
                int days = Integer.parseInt(number.replaceAll("\\D", ""));
                return days <= 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    private String getAsString(JsonObject json, String key, String fallback) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return fallback;
    }
}
