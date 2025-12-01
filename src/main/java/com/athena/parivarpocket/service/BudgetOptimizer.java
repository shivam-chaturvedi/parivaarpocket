package com.athena.parivarpocket.service;

import com.athena.parivarpocket.model.BudgetRecommendation;
import com.athena.parivarpocket.model.WalletEntry;
import com.athena.parivarpocket.model.WalletEntryType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BudgetOptimizer {
    private final DataRepository repository;

    public BudgetOptimizer(DataRepository repository) {
        this.repository = repository;
    }

    public double budgetHealth(List<WalletEntry> entries) {
        double income = repository.calculateIncome(entries);
        double expenses = repository.calculateExpenses(entries);
        if (income == 0) {
            return 0;
        }
        double ratio = expenses / income;
        return Math.max(0, Math.min(100, 100 - (ratio * 50)));
    }

    public List<BudgetRecommendation> recommend(List<WalletEntry> entries) {
        List<BudgetRecommendation> recommendations = new ArrayList<>();
        double income = repository.calculateIncome(entries);
        double expenses = repository.calculateExpenses(entries);
        Map<String, Double> breakdown = repository.categoryBreakdown(entries);

        breakdown.entrySet().stream()
                .filter(e -> e.getValue() > income * 0.15)
                .forEach(e -> recommendations.add(new BudgetRecommendation(
                        "Trim " + e.getKey(),
                        "Reduce " + e.getKey().toLowerCase() + " by 10% to free ₹" + Math.round(e.getValue() * 0.1),
                        e.getValue() * 0.1
                )));

        if (expenses > income) {
            recommendations.add(new BudgetRecommendation(
                    "Balance Budget",
                    "Expenses exceed income. Move one non-essential expense to next month.",
                    expenses - income
            ));
        }

        double savings = entries.stream()
                .filter(e -> e.getType() == WalletEntryType.SAVINGS)
                .mapToDouble(WalletEntry::getAmount)
                .sum();
        if (savings < income * 0.2) {
            double delta = Math.round((income * 0.2) - savings);
            recommendations.add(new BudgetRecommendation(
                    "Boost Savings",
                    "Shift ₹" + delta + " to savings to hit a 20% savings rate.",
                    delta
            ));
        }

        if (recommendations.isEmpty()) {
            recommendations.add(new BudgetRecommendation(
                    "On Track",
                    "Great job! You are within budget and saving steadily.",
                    0
            ));
        }
        return recommendations;
    }
}
