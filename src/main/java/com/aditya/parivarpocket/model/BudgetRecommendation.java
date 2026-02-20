package com.aditya.parivarpocket.model;

public class BudgetRecommendation {
    private final String title;
    private final String detail;
    private final double impact;

    public BudgetRecommendation(String title, String detail, double impact) {
        this.title = title;
        this.detail = detail;
        this.impact = impact;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public double getImpact() {
        return impact;
    }
}
