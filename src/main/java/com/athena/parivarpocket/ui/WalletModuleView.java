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
import javafx.scene.layout.GridPane;
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
        getChildren().add(buildBreakdownPanel());
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

    private Panel buildBreakdownPanel() {
        List<BudgetLine> lines = List.of(
                new BudgetLine("Food & Groceries", "₹2,000", "₹2,200", "₹1,800", "Over by ₹200"),
                new BudgetLine("Transportation", "₹800", "₹900", "₹750", "Over by ₹100"),
                new BudgetLine("Education", "₹1,500", "₹1,500", "₹1,500", "√ On Track"),
                new BudgetLine("Utilities", "₹600", "₹700", "₹600", "Over by ₹100"),
                new BudgetLine("Entertainment", "₹400", "₹500", "₹300", "Over by ₹100")
        );

        GridPane table = new GridPane();
        table.setHgap(0);
        table.setVgap(0);
        table.addRow(0,
                header("Category"),
                header("Planned Budget"),
                header("Actual Spending"),
                header("Optimized Budget"),
                header("Status")
        );
        for (int i = 0; i < lines.size(); i++) {
            BudgetLine line = lines.get(i);
            table.addRow(i + 1,
                    cell(line.category, i % 2 == 1, i),
                    cell(line.planned, i % 2 == 1, i),
                    cell(line.actual, i % 2 == 1, i),
                    cell(line.optimized, i % 2 == 1, i),
                    statusCell(line.status, i % 2 == 1, i)
            );
        }

        HBox totals = new HBox(12,
                totalCell("TOTAL", "Planned: ₹5,300"),
                totalCell("ACTUAL", "₹5,800"),
                totalCell("OPTIMIZED", "₹4,950")
        );
        totals.setAlignment(Pos.CENTER_LEFT);
        totals.setPadding(new Insets(6, 0, 0, 0));

        VBox content = new VBox(6, table, totals);
        content.setPadding(new Insets(8, 0, 0, 0));
        Panel panel = new Panel("Monthly Budget Breakdown", content);
        panel.getStyleClass().add("budget-panel");
        return panel;
    }

    private Panel buildOptimizationPanel(List<WalletEntry> entries) {
        List<BudgetRecommendation> recommendations = optimizer.recommend(entries);
        VBox list = new VBox(6);
        recommendations.forEach(rec -> {
            Label title = new Label(rec.getTitle());
            title.getStyleClass().add("budget-recommendation-title");
            Label detail = new Label(rec.getDetail());
            detail.setWrapText(true);
            Label impact = new Label("Impact: ₹" + Math.round(rec.getImpact()));
            impact.getStyleClass().add("budget-impacts");
            VBox block = new VBox(4, title, detail, impact);
            block.setPadding(new Insets(8));
            block.getStyleClass().add("budget-recommendation");
            list.getChildren().add(block);
        });
        HBox stats = new HBox(12,
                createSimpleStat("Potential Monthly Savings", "₹750"),
                createSimpleStat("Yearly Impact", "₹9,000")
        );
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.setPadding(new Insets(8, 0, 0, 0));
        Button export = new Button("Export Budget Report (PDF)");
        export.getStyleClass().add("export-button");

        VBox content = new VBox(12, list, stats, export);
        content.setPadding(new Insets(10, 0, 0, 0));
        Panel panel = new Panel("Budget Optimization Analysis", content);
        panel.getStyleClass().add("budget-panel");
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

    private Label header(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("budget-table-header");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Label cell(String text, boolean dim, int rowIndex) {
        Label label = new Label(text);
        label.getStyleClass().add(dim ? "budget-table-cell dim" : "budget-table-cell");
        label.getStyleClass().add(rowIndex % 2 == 0 ? "budget-table-row-even" : "budget-table-row-odd");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Label statusCell(String text, boolean dim, int rowIndex) {
        Label label = new Label(text);
        label.getStyleClass().add(dim ? "budget-status dim" : "budget-status");
        label.getStyleClass().add(rowIndex % 2 == 0 ? "budget-table-row-even" : "budget-table-row-odd");
        return label;
    }

    private VBox totalCell(String title, String value) {
        Label label = new Label(title);
        label.getStyleClass().add("budget-total-label");
        Label val = new Label(value);
        val.getStyleClass().add("budget-total-value");
        VBox box = new VBox(2, val, label);
        box.setPadding(new Insets(6));
        return box;
    }

    private VBox createSimpleStat(String title, String value) {
        Label label = new Label(title);
        label.getStyleClass().add("budget-simple-label");
        Label val = new Label(value);
        val.getStyleClass().add("budget-simple-value");
        VBox box = new VBox(4, label, val);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: #f3f3f3; -fx-border-color: #d5d5d5; -fx-border-width: 1;");
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
        return row;
    }

    private record BudgetLine(String category, String planned, String actual, String optimized, String status) {
    }

    private record DistributionLine(String category, double percentage) {
    }
}
