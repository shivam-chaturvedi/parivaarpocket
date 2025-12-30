package com.athena.parivarpocket.model;

import java.util.List;

public class JobOpportunity {
    private final String id;
    private final String title;
    private final String company;
    private final String location;
    private final String category;
    private final String hours;
    private final String payRange;
    private final List<String> requiredSkills;
    private final String safetyNotes;
    private final String contact;
    private final String jobUrl;
    private final int suitabilityScore;

    public JobOpportunity(String id, String title, String company, String location, String category, String hours, String payRange,
                          List<String> requiredSkills, String safetyNotes, String contact, String jobUrl, int suitabilityScore) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.category = category;
        this.hours = hours;
        this.payRange = payRange;
        this.requiredSkills = requiredSkills;
        this.safetyNotes = safetyNotes;
        this.contact = contact;
        this.jobUrl = jobUrl;
        this.suitabilityScore = suitabilityScore;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCompany() {
        return company;
    }

    public String getLocation() {
        return location;
    }

    public String getCategory() {
        return category;
    }

    public String getHours() {
        return hours;
    }

    public String getPayRange() {
        return payRange;
    }

    public List<String> getRequiredSkills() {
        return requiredSkills;
    }

    public String getSafetyNotes() {
        return safetyNotes;
    }

    public String getContact() {
        return contact;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public int getSuitabilityScore() {
        return suitabilityScore;
    }
}
