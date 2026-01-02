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
import java.util.ArrayList;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class RapidJobService {
    private static final String BASE_URL = "https://indeed12.p.rapidapi.com/jobs/search";
    private static final String HOST = "indeed12.p.rapidapi.com";
    private static final String KEY = "79faf3d171mshd01253d058122d2p1c9b03jsnb2abc2fee2ad";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public List<JobOpportunity> fetchRecentWestBengalJobs() throws IOException, InterruptedException {
        String params = "?query=jobs&location=West%20Bengal,%20India&locality=in&start=0&sort=date";
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
        System.out.println("[RapidJobService] Received " + hits.size() + " total hits from API.");
        List<JobOpportunity> jobs = new ArrayList<>();
        int filteredOut = 0;
        for (JsonElement element : hits) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject jobJson = element.getAsJsonObject();
            String title = getAsString(jobJson, "title", "Job opportunity");
            String location = getAsString(jobJson, "location", "West Bengal, India");
            String company = getAsString(jobJson, "company_name", "Hiring partner");
            String locality = getAsString(jobJson, "locality", "India");
            String relativeTime = getAsString(jobJson, "formatted_relative_time", "");
            
            // Robust ID extraction
            String jobId = getAsString(jobJson, "id", "");
            if (jobId.isBlank()) jobId = getAsString(jobJson, "job_id", "");
            if (jobId.isBlank()) jobId = getAsString(jobJson, "jk", "");
            if (jobId.isBlank()) jobId = getAsString(jobJson, "job_key", "");
            
            if (jobId == null || jobId.isBlank()) {
                jobId = java.util.UUID.randomUUID().toString();
            }
            String jobLink = deriveJobLink(jobId);
            long pubDateTsMilli = getAsLong(jobJson, "pub_date_ts_milli", 0L);
            JsonObject salary = jobJson.has("salary") && jobJson.get("salary").isJsonObject()
                    ? jobJson.getAsJsonObject("salary")
                    : null;
            Double salaryMin = salary != null ? getAsDouble(salary, "min") : null;
            Double salaryMax = salary != null ? getAsDouble(salary, "max") : null;
            String salaryType = salary != null ? getAsString(salary, "type", "") : "";

            String category = extractCategory(title);
            List<String> skills = extractSkills(title);
            String hours = extractHours(title);
            String safety = extractSafety(title);
            String contact = extractContact(title);

            JobOpportunity job = new JobOpportunity(
                    jobId,
                    title,
                    company,
                    location,
                    locality,
                    jobLink,
                    pubDateTsMilli,
                    relativeTime,
                    salaryMin,
                    salaryMax,
                    salaryType,
                    category,
                    skills,
                    hours,
                    safety,
                    contact
            );
            jobs.add(job);
            if (jobs.size() >= 50) {
                break;
            }
        }
        System.out.println("[RapidJobService] Sync complete. Kept " + jobs.size() + " jobs from API.");
        return jobs;
    }

    private String extractCategory(String title) {
        String t = title.toLowerCase();
        if (t.contains("tutor") || t.contains("teacher") || t.contains("faculty") || t.contains("lecturer") || t.contains("educator")) return "Tutoring";
        if (t.contains("delivery") || t.contains("rider") || t.contains("driver") || t.contains("courier") || t.contains("zomato") || t.contains("swiggy")) return "Delivery";
        if (t.contains("sales") || t.contains("retail") || t.contains("store") || t.contains("cashier") || t.contains("counter") || t.contains("executive")) return "Retail";
        if (t.contains("intern") || t.contains("trainee") || t.contains("fresher")) return "Internship";
        return "General";
    }

    private List<String> extractSkills(String title) {
        String t = title.toLowerCase();
        List<String> skills = new ArrayList<>();
        if (t.contains("sales") || t.contains("marketing")) skills.add("Sales");
        if (t.contains("tutor") || t.contains("teach")) skills.add("Teaching");
        if (t.contains("delivery") || t.contains("drive")) skills.add("Driving");
        if (t.contains("comm") || t.contains("english") || t.contains("hindi")) skills.add("Communication");
        if (t.contains("computer") || t.contains("excel") || t.contains("data")) skills.add("Basic IT");
        if (skills.isEmpty()) skills.add("General");
        return skills;
    }

    private String extractHours(String title) {
        String t = title.toLowerCase();
        if (t.contains("part time") || t.contains("part-time") || t.contains("hourly")) return "Part-time";
        if (t.contains("full time") || t.contains("full-time") || t.contains("day shift")) return "Full-time";
        if (t.contains("remote") || t.contains("work from home") || t.contains("flexible")) return "Flexible";
        return "Full-time"; // Default
    }

    private String extractSafety(String title) {
        String t = title.toLowerCase();
        if (t.contains("police") || t.contains("verification") || t.contains("background")) return "Police verification or background check may be required.";
        if (t.contains("safe") || t.contains("female") || t.contains("women")) return "Verified safe workplace for all candidates.";
        return "Always verify employer identity before sharing personal documents.";
    }

    private String extractContact(String title) {
        String t = title.toLowerCase();
        if (t.contains("call") || t.contains("contact") || t.contains("phone")) return "Direct contact details available in the full listing.";
        if (t.contains("mail") || t.contains("email")) return "Email application encouraged.";
        return "Use the 'Apply Now' button to reach the hiring manager via Indeed.";
    }

    private String getAsString(JsonObject json, String key, String fallback) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return fallback;
    }

    private long getAsLong(JsonObject json, String key, long fallback) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            try {
                return json.get(key).getAsLong();
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    private Double getAsDouble(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            try {
                return json.get(key).getAsDouble();
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String deriveJobLink(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return "";
        }
        return "https://in.indeed.com/viewjob?jk=" + jobId;
    }
}
