package com.athena.parivarpocket.ui;

import com.athena.parivarpocket.model.BudgetRecommendation;
import com.athena.parivarpocket.model.User;
import com.athena.parivarpocket.model.WalletEntry;
import com.athena.parivarpocket.service.BudgetOptimizer;
import com.athena.parivarpocket.service.DataRepository;
import com.athena.parivarpocket.service.OfflineSyncService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class WalletModuleView extends VBox {
    private final DataRepository repository;
    private final User user;
    private final BudgetOptimizer optimizer;
    private final OfflineSyncService offlineSyncService;

    public WalletModuleView(DataRepository repository, User user, BudgetOptimizer optimizer, OfflineSyncService offlineSyncService) {
        this.repository = repository;
        this.user = user;
        this.optimizer = optimizer;
        this.offlineSyncService = offlineSyncService;

        setSpacing(18);
        setPadding(new Insets(0, 0, 8, 0));

        List<WalletEntry> entries = repository.loadWallet(user);
        getChildren().add(buildBudgetPanel(entries));
        getChildren().add(buildOptimizationPanel(entries));
        getChildren().add(buildExpenseDistribution(entries));
    }

    private Panel buildBudgetPanel(List<WalletEntry> entries) {
        double income = repository.calculateIncome(entries);
        double expenses = repository.calculateExpenses(entries);
        double savings = entries.stream()
                .filter(e -> e.getType().name().equals("SAVINGS"))
                .mapToDouble(WalletEntry::getAmount)
                .sum();

        HBox metrics = new HBox(14,
                createMetric("Monthly Income", "₹" + Math.round(income)),
                createMetric("Monthly Expenses", "₹" + Math.round(expenses)),
                createMetric("Current Savings", "₹" + Math.round(savings))
        );
        metrics.setAlignment(Pos.CENTER);

        String alertMessage = expenses >= income
                ? "Budget Alert! Your expenses equal or exceed income. Reduce expenses to avoid shortfall."
                : "Budget Alert! Your expenses (₹" + Math.round(expenses) + ") are approaching your income (₹" + Math.round(income) + "). Consider reducing spending to increase savings.";
        Label alert = new Label(alertMessage);
        alert.getStyleClass().add("budget-alert");
        alert.setWrapText(true);

        VBox content = new VBox(12, metrics, alert);
        content.setPadding(new Insets(10));
        Panel panel = new Panel("Simulated Wallet - Monthly Budget", content);
        panel.getStyleClass().add("budget-panel");
        return panel;
    }

    private Panel buildOptimizationPanel(List<WalletEntry> entries) {
        List<BudgetRecommendation> recommendations = optimizer.recommend(entries);
        VBox recommendationsBox = new VBox(8);
        Label header = new Label("Optimization Recommendations");
        header.getStyleClass().add("optimization-title");
        recommendationsBox.getChildren().add(header);
        recommendations.forEach(rec -> {
            Label bullet = new Label("• " + rec.getDetail());
            bullet.setWrapText(true);
            bullet.getStyleClass().add("optimization-bullet");
            recommendationsBox.getChildren().add(bullet);
        });
        recommendationsBox.getStyleClass().add("optimization-recommendations");

        HBox stats = new HBox(12,
                createOptimizationStat("Potential Monthly Savings", "₹750"),
                createOptimizationStat("Yearly Impact", "₹9,000")
        );
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getStyleClass().add("optimization-stats");
        stats.setPadding(new Insets(8, 0, 0, 0));

        Button export = new Button("Export Budget Report (PDF)");
        export.getStyleClass().add("optimization-export-button");

        VBox content = new VBox(12, recommendationsBox, stats, export);
        content.setPadding(new Insets(10, 0, 0, 0));
        Panel panel = new Panel("Budget Optimization Analysis", content);
        panel.getStyleClass().addAll("budget-panel", "optimization-panel");
        return panel;
    }

    private Panel buildExpenseDistribution(List<WalletEntry> entries) {
        List<DistributionLine> lines = List.of(
                new DistributionLine("Food & Groceries", 37.9),
                new DistributionLine("Transportation", 15.5),
                new DistributionLine("Education", 25.9),
                new DistributionLine("Utilities", 12.1),
                new DistributionLine("Entertainment", 8.6)
        );
        VBox list = new VBox(8);
        list.getStyleClass().add("expense-distribution-list");
        lines.forEach(line -> list.getChildren().add(distributionRow(line.category, line.percentage)));

        Panel panel = new Panel("Expense Distribution", list);
        panel.getStyleClass().add("budget-panel");
        return panel;
    }

    private VBox createMetric(String label, String value) {
        Label title = new Label(label);
        title.getStyleClass().add("budget-metric-label");
        Label val = new Label(value);
        val.getStyleClass().add("budget-metric-value");
        VBox box = new VBox(4, title, val);
        box.setPadding(new Insets(10));
        box.getStyleClass().add("budget-metric-card");
        return box;
    }

    private VBox createOptimizationStat(String title, String value) {
        Label label = new Label(title);
        label.getStyleClass().add("optimization-stat-label");
        Label val = new Label(value);
        val.getStyleClass().add("optimization-stat-value");
        VBox box = new VBox(4, label, val);
        box.setPadding(new Insets(12));
        box.getStyleClass().add("optimization-stat-box");
        return box;
    }

    private HBox distributionRow(String label, double percentage) {
        Label title = new Label(label);
        title.getStyleClass().add("budget-distribution-label");
        StackPane bar = new StackPane();
        bar.setPrefWidth(260);
        bar.setPrefHeight(14);
        bar.getStyleClass().add("distribution-backdrop");
        Region fill = new Region();
        fill.getStyleClass().add("distribution-fill");
        fill.setPrefWidth(2.3 * percentage);
        fill.setPrefHeight(14);
        bar.getChildren().add(fill);
        Label pct = new Label(String.format("%.1f%%", percentage));
        pct.getStyleClass().add("budget-distribution-pct");
        HBox row = new HBox(12, title, bar, pct);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 4, 0));
        row.getStyleClass().add("expense-distribution-row");
        return row;
    }

    private record DistributionLine(String category, double percentage) {
    }
}
