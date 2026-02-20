package com.aditya.parivarpocket.model;

import java.time.LocalDateTime;

public class JobApplication {
    private final String id;
    private final String jobId;
    private final String status;
    private final LocalDateTime appliedAt;

    public JobApplication(String id, String jobId, String status, LocalDateTime appliedAt) {
        this.id = id;
        this.jobId = jobId;
        this.status = status;
        this.appliedAt = appliedAt;
    }

    public String getId() { return id; }
    public String getJobId() { return jobId; }
    public String getStatus() { return status; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
}
