package com.athena.parivarpocket.model;

import java.time.LocalDateTime;

public class BudgetGoal {
    private final String id;
    private final String userEmail;
    private final double currentBudget;
    private final double targetSavings;
    private final LocalDateTime updatedAt;

    public BudgetGoal(String id,
                      String userEmail,
                      double currentBudget,
                      double targetSavings,
                      LocalDateTime updatedAt) {
        this.id = id;
        this.userEmail = userEmail;
        this.currentBudget = currentBudget;
        this.targetSavings = targetSavings;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public double getCurrentBudget() {
        return currentBudget;
    }

    public double getTargetSavings() {
        return targetSavings;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
