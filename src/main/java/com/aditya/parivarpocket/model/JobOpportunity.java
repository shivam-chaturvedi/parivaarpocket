package com.aditya.parivarpocket.model;

public class JobOpportunity {
    private final String id;
    private final String title;
    private final String company;
    private final String location;
    private final String locality;
    private final String jobLink;
    private final long pubDateTsMilli;
    private final String formattedRelativeTime;
    private final Double salaryMin;
    private final Double salaryMax;
    private final String salaryType;
    private final String category;
    private final java.util.List<String> requiredSkills;
    private final String workingHours;
    private final String safetyGuidance;
    private final String contactInfo;

    public JobOpportunity(String id,
                          String title,
                          String company,
                          String location,
                          String locality,
                          String jobLink,
                          long pubDateTsMilli,
                          String formattedRelativeTime,
                          Double salaryMin,
                          Double salaryMax,
                          String salaryType,
                          String category,
                          java.util.List<String> requiredSkills,
                          String workingHours,
                          String safetyGuidance,
                          String contactInfo) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.locality = locality;
        this.jobLink = jobLink;
        this.pubDateTsMilli = pubDateTsMilli;
        this.formattedRelativeTime = formattedRelativeTime;
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        this.salaryType = salaryType;
        this.category = category;
        this.requiredSkills = requiredSkills != null ? requiredSkills : java.util.Collections.emptyList();
        this.workingHours = workingHours;
        this.safetyGuidance = safetyGuidance;
        this.contactInfo = contactInfo;
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

    public String getLocality() {
        return locality;
    }

    public String getJobLink() {
        return jobLink;
    }

    public long getPubDateTsMilli() {
        return pubDateTsMilli;
    }

    public String getFormattedRelativeTime() {
        return formattedRelativeTime;
    }

    public Double getSalaryMin() {
        return salaryMin;
    }

    public Double getSalaryMax() {
        return salaryMax;
    }

    public String getSalaryType() {
        return salaryType;
    }

    public String getCategory() {
        return category;
    }

    public java.util.List<String> getRequiredSkills() {
        return requiredSkills;
    }

    public String getWorkingHours() {
        return workingHours;
    }

    public String getSafetyGuidance() {
        return safetyGuidance;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public boolean hasSalary() {
        return (salaryMin != null && salaryMin > 0) || (salaryMax != null && salaryMax > 0);
    }

    public String getSalaryDescription() {
        if (!hasSalary()) {
            return "Not disclosed";
        }
        StringBuilder builder = new StringBuilder();
        if (salaryMin != null && salaryMax != null) {
            if (salaryMin.equals(salaryMax)) {
                builder.append("₹").append(Math.round(salaryMin));
            } else {
                builder.append("₹").append(Math.round(salaryMin)).append(" - ₹").append(Math.round(salaryMax));
            }
        } else if (salaryMin != null) {
            builder.append("From ₹").append(Math.round(salaryMin));
        } else {
            builder.append("Up to ₹").append(Math.round(salaryMax));
        }
        if (salaryType != null && !salaryType.isBlank()) {
            builder.append(" ").append(salaryType.toLowerCase());
        }
        return builder.toString();
    }
}
