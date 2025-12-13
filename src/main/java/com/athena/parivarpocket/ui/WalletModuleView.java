package com.athena.parivarpocket.ui;

import com.athena.parivarpocket.model.BudgetGoal;
import com.athena.parivarpocket.model.BudgetRecommendation;
import com.athena.parivarpocket.model.User;
import com.athena.parivarpocket.model.WalletEntry;
import com.athena.parivarpocket.model.WalletEntryType;
import com.athena.parivarpocket.service.BudgetOptimizer;
import com.athena.parivarpocket.service.DataRepository;
import com.athena.parivarpocket.service.OfflineSyncService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WalletModuleView extends VBox {
    private final DataRepository repository;
    private final User user;
    private final BudgetOptimizer optimizer;
    private final OfflineSyncService offlineSyncService;
    private List<WalletEntry> walletEntries;
    private BudgetGoal budgetGoal;

    private final Label incomeValue = new Label();
    private final Label expensesValue = new Label();
    private final Label savingsValue = new Label();
    private final Label budgetAlertLabel = new Label();

    private final TextField currentBudgetField = new TextField();
    private final TextField targetSavingsField = new TextField();
    private final Label goalStatusLabel = new Label();

    private final TextField itemField = new TextField();
    private final TextField categoryField = new TextField();
    private final TextField amountField = new TextField();
    private final TextField purchaseNoteField = new TextField();
    private final DatePicker entryDatePicker = new DatePicker(LocalDate.now());
    private final ChoiceBox<String> entryTypeChoice = new ChoiceBox<>(FXCollections.observableArrayList("Expense", "Savings", "Income"));
    private final ChoiceBox<String> timeOfDayChoice = new ChoiceBox<>(FXCollections.observableArrayList("Morning", "Afternoon", "Evening", "Night"));

    private final ChoiceBox<String> timeframeFilter = new ChoiceBox<>(FXCollections.observableArrayList("Today", "This Week", "This Month", "This Year", "Custom Range"));
    private final ChoiceBox<String> typeFilter = new ChoiceBox<>(FXCollections.observableArrayList("All types", "Income", "Expense", "Savings"));
    private final ChoiceBox<String> timeFilter = new ChoiceBox<>(FXCollections.observableArrayList("Any time", "Morning", "Afternoon", "Evening", "Night"));
    private final DatePicker startPicker = new DatePicker();
    private final DatePicker endPicker = new DatePicker();
    private final VBox historyContainer = new VBox(8);
    private final Label historySummaryLabel = new Label();

    private final VBox recommendationsList = new VBox(8);
    private final Label potentialSavingsValue = new Label();
    private final Label yearlyImpactValue = new Label();
    private final VBox distributionList = new VBox(8);

    public WalletModuleView(DataRepository repository, User user, BudgetOptimizer optimizer, OfflineSyncService offlineSyncService) {
        this.repository = repository;
        this.user = user;
        this.optimizer = optimizer;
        this.offlineSyncService = offlineSyncService;
        this.walletEntries = new ArrayList<>(repository.loadWallet(user));
        this.budgetGoal = repository.getBudgetGoal(user);

        setSpacing(18);
        setPadding(new Insets(0, 0, 8, 0));

        getChildren().add(buildBudgetPanel());
        getChildren().add(buildGoalPanel());
        getChildren().add(buildEntryFormPanel());
        getChildren().add(buildTimelinePanel());
        getChildren().add(buildOptimizationPanel());
        getChildren().add(buildExpenseDistributionPanel());

        refreshAllSections();
    }

    private Panel buildBudgetPanel() {
        HBox metrics = new HBox(14,
                createMetric("Monthly Income", incomeValue),
                createMetric("Monthly Expenses", expensesValue),
                createMetric("Current Savings", savingsValue)
        );
        metrics.setAlignment(Pos.CENTER);

        budgetAlertLabel.getStyleClass().add("budget-alert");
        budgetAlertLabel.setWrapText(true);

        VBox content = new VBox(12, metrics, budgetAlertLabel);
        content.setPadding(new Insets(10));
        Panel panel = new Panel("Budget Snapshot", content);
        panel.getStyleClass().add("budget-panel");
        return panel;
    }

    private Panel buildGoalPanel() {
        currentBudgetField.getStyleClass().add("wallet-form-input");
        currentBudgetField.setPromptText("Current monthly budget (₹)");
        targetSavingsField.getStyleClass().add("wallet-form-input");
        targetSavingsField.setPromptText("Savings target (₹)");
        goalStatusLabel.getStyleClass().add("goal-status");

        Button saveGoal = new Button("Save goals");
        saveGoal.getStyleClass().add("primary-button");
        saveGoal.setOnAction(e -> {
            try {
                double current = Double.parseDouble(currentBudgetField.getText().trim());
                double target = Double.parseDouble(targetSavingsField.getText().trim());
                budgetGoal = repository.upsertBudgetGoal(user, current, target);
                updateGoalStatus();
                goalStatusLabel.setText("Budget goals updated.");
            } catch (NumberFormatException ex) {
                showAlert("Enter valid numeric values for budget and savings.");
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(new Label("Current budget"), 0, 0);
        grid.add(currentBudgetField, 1, 0);
        grid.add(new Label("Target savings"), 0, 1);
        grid.add(targetSavingsField, 1, 1);

        VBox content = new VBox(12, grid, saveGoal, goalStatusLabel);
        content.setPadding(new Insets(10));
        Panel panel = new Panel("Budget Planning & Targets", content);
        panel.getStyleClass().add("budget-panel");
        return panel;
    }

    private Panel buildEntryFormPanel() {
        itemField.setPromptText("Item or purpose (e.g. Groceries)");
        itemField.getStyleClass().add("wallet-form-input");
        categoryField.setPromptText("Category (Food, Transport... )");
        categoryField.getStyleClass().add("wallet-form-input");
        amountField.setPromptText("Amount (₹)");
        amountField.getStyleClass().add("wallet-form-input");
        purchaseNoteField.setPromptText("Notes (vendors, time, details)");
        purchaseNoteField.getStyleClass().add("wallet-form-input");
        entryDatePicker.getStyleClass().add("wallet-form-input");
        entryTypeChoice.getSelectionModel().select("Expense");
        entryTypeChoice.getStyleClass().add("wallet-form-choice");
        timeOfDayChoice.getSelectionModel().selectFirst();
        timeOfDayChoice.getStyleClass().add("wallet-form-choice");

        Button logEntry = new Button("Log today's purchase");
        logEntry.getStyleClass().add("primary-button");
        logEntry.setOnAction(e -> recordEntry());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(new Label("Item"), 0, 0);
        grid.add(itemField, 1, 0);
        grid.add(new Label("Category"), 0, 1);
        grid.add(categoryField, 1, 1);
        grid.add(new Label("Amount"), 0, 2);
        grid.add(amountField, 1, 2);
        grid.add(new Label("Date"), 0, 3);
        grid.add(entryDatePicker, 1, 3);
        grid.add(new Label("Type"), 0, 4);
        grid.add(entryTypeChoice, 1, 4);
        grid.add(new Label("Time of day"), 0, 5);
        grid.add(timeOfDayChoice, 1, 5);
        grid.add(new Label("Notes"), 0, 6);
        grid.add(purchaseNoteField, 1, 6);

        VBox content = new VBox(12, grid, logEntry);
        content.setPadding(new Insets(10));
        Panel panel = new Panel("Log Expenses & Savings", content);
        panel.getStyleClass().add("budget-panel");
        return panel;
    }

    private Panel buildTimelinePanel() {
        timeframeFilter.getSelectionModel().selectFirst();
        timeframeFilter.setOnAction(e -> {
            updateCustomRangeState();
            refreshHistoryPanel();
        });
        timeframeFilter.getStyleClass().add("wallet-form-choice");

        typeFilter.getSelectionModel().selectFirst();
        typeFilter.setOnAction(e -> refreshHistoryPanel());
        typeFilter.getStyleClass().add("wallet-form-choice");

        timeFilter.getSelectionModel().selectFirst();
        timeFilter.setOnAction(e -> refreshHistoryPanel());
        timeFilter.getStyleClass().add("wallet-form-choice");

        startPicker.setDisable(true);
        endPicker.setDisable(true);
        startPicker.valueProperty().addListener((obs, old, value) -> refreshHistoryPanel());
        endPicker.valueProperty().addListener((obs, old, value) -> refreshHistoryPanel());

        HBox filters = new HBox(12,
                new VBox(4, new Label("View by timeframe"), timeframeFilter),
                new VBox(4, new Label("Type"), typeFilter),
                new VBox(4, new Label("Time of day"), timeFilter),
                new VBox(4, new Label("Range Start"), startPicker),
                new VBox(4, new Label("Range End"), endPicker)
        );
        filters.setAlignment(Pos.CENTER_LEFT);

        historySummaryLabel.getStyleClass().add("history-summary");

        historyContainer.setPadding(new Insets(10));
        historyContainer.setStyle("-fx-background-color: transparent;");

        ScrollPane scrollPane = new ScrollPane(historyContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(260);
        scrollPane.setStyle("-fx-background: transparent;");
        scrollPane.getStyleClass().add("history-scroll");

        VBox content = new VBox(12, filters, historySummaryLabel, scrollPane);
        content.setPadding(new Insets(10));
        Panel panel = new Panel("Expense & Savings Timeline", content);
        panel.getStyleClass().add("budget-panel");
        return panel;
    }

    private Panel buildOptimizationPanel() {
        Label header = new Label("Optimization Recommendations");
        header.getStyleClass().add("optimization-title");

        recommendationsList.setPadding(new Insets(4, 0, 0, 0));

        HBox stats = new HBox(12,
                createOptimizationStat("Potential monthly savings", potentialSavingsValue),
                createOptimizationStat("Yearly impact", yearlyImpactValue)
        );
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getStyleClass().add("optimization-stats");

        VBox content = new VBox(10, header, recommendationsList, stats);
        content.setPadding(new Insets(10));
        Panel panel = new Panel("Budget Optimization Analysis", content);
        panel.getStyleClass().addAll("budget-panel", "optimization-panel");
        return panel;
    }

    private Panel buildExpenseDistributionPanel() {
        distributionList.setPadding(new Insets(4, 0, 0, 0));
        Panel panel = new Panel("Expense Distribution", distributionList);
        panel.getStyleClass().add("budget-panel");
        return panel;
    }

    private VBox createMetric(String label, Label valueLabel) {
        Label title = new Label(label);
        title.getStyleClass().add("budget-metric-label");
        valueLabel.getStyleClass().add("budget-metric-value");
        VBox box = new VBox(4, title, valueLabel);
        box.setPadding(new Insets(10));
        box.getStyleClass().add("budget-metric-card");
        return box;
    }

    private VBox createOptimizationStat(String titleText, Label valueLabel) {
        Label title = new Label(titleText);
        title.getStyleClass().add("optimization-stat-label");
        valueLabel.getStyleClass().add("optimization-stat-value");
        VBox box = new VBox(4, title, valueLabel);
        box.setPadding(new Insets(12));
        box.getStyleClass().add("optimization-stat-box");
        return box;
    }

    private void refreshAllSections() {
        refreshBudgetPanel();
        refreshGoalPanel();
        refreshHistoryPanel();
        refreshOptimizationPanel();
        refreshDistributionPanel();
    }

    private void refreshBudgetPanel() {
        double income = repository.calculateIncome(walletEntries);
        double expenses = repository.calculateExpenses(walletEntries);
        double savings = walletEntries.stream()
                .filter(e -> e.getType() == WalletEntryType.SAVINGS)
                .mapToDouble(WalletEntry::getAmount)
                .sum();

        incomeValue.setText("₹" + Math.round(income));
        expensesValue.setText("₹" + Math.round(expenses));
        savingsValue.setText("₹" + Math.round(savings));

        String alert = expenses >= income
                ? "Budget Alert! Your expenses equal or exceed your income. Cut back a little to protect savings."
                : "Expenses (₹" + Math.round(expenses) + ") are being tracked against your income (₹" + Math.round(income) + ").";
        budgetAlertLabel.setText(alert);
    }

    private void refreshGoalPanel() {
        if (budgetGoal != null) {
            currentBudgetField.setText(String.valueOf(Math.round(budgetGoal.getCurrentBudget())));
            targetSavingsField.setText(String.valueOf(Math.round(budgetGoal.getTargetSavings())));
            updateGoalStatus();
        } else {
            goalStatusLabel.setText("Set a goal to see guiding insights.");
        }
    }

    private void updateGoalStatus() {
        double expenses = repository.calculateExpenses(walletEntries);
        double target = budgetGoal != null ? budgetGoal.getTargetSavings() : 0;
        double delta = target - expenses;
        if (delta > 0) {
            goalStatusLabel.setText("Need ₹" + Math.round(delta) + " more to hit savings target.");
        } else {
            goalStatusLabel.setText("On track! You have reached your savings target.");
        }
    }

    private void refreshHistoryPanel() {
        List<WalletEntry> filtered = filterEntries();
        double income = filtered.stream()
                .filter(e -> e.getType() == WalletEntryType.INCOME)
                .mapToDouble(WalletEntry::getAmount)
                .sum();
        double expenses = filtered.stream()
                .filter(e -> e.getType() == WalletEntryType.EXPENSE)
                .mapToDouble(WalletEntry::getAmount)
                .sum();
        double savings = filtered.stream()
                .filter(e -> e.getType() == WalletEntryType.SAVINGS)
                .mapToDouble(WalletEntry::getAmount)
                .sum();

        historySummaryLabel.setText(String.format("Showing %d entries — Income ₹%d • Expenses ₹%d • Savings ₹%d",
                filtered.size(),
                Math.round(income),
                Math.round(expenses),
                Math.round(savings)));

        historyContainer.getChildren().clear();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM");
        for (WalletEntry entry : filtered) {
            Label dateLabel = new Label(entry.getDate().format(formatter));
            dateLabel.getStyleClass().add("history-date");
            Label detail = new Label(entry.getCategory() + " • " + entry.getNote());
            detail.getStyleClass().add("history-note");
            Label amount = new Label((entry.getType() == WalletEntryType.EXPENSE ? "-" : "+") + "₹" + Math.round(entry.getAmount()));
            amount.getStyleClass().add("history-amount");
            HBox row = new HBox(16, dateLabel, detail, amount);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("history-row");
            historyContainer.getChildren().add(row);
        }
    }

    private void refreshOptimizationPanel() {
        List<BudgetRecommendation> recommendations = optimizer.recommend(walletEntries);
        recommendationsList.getChildren().clear();
        recommendations.forEach(rec -> {
            Label bullet = new Label("• " + rec.getDetail());
            bullet.setWrapText(true);
            bullet.getStyleClass().add("optimization-bullet");
            recommendationsList.getChildren().add(bullet);
        });

        double expenses = repository.calculateExpenses(walletEntries);
        double currentBudget = budgetGoal != null ? budgetGoal.getCurrentBudget() : repository.calculateIncome(walletEntries);
        double potential = Math.max(0, currentBudget - expenses);
        potentialSavingsValue.setText("₹" + Math.round(potential));
        yearlyImpactValue.setText("₹" + Math.round(potential * 12));
    }

    private void refreshDistributionPanel() {
        Map<String, Double> totals = new LinkedHashMap<>();
        double total = walletEntries.stream().mapToDouble(WalletEntry::getAmount).sum();
        for (WalletEntry entry : walletEntries) {
            String category = entry.getCategory();
            if (category == null || category.isBlank()) {
                category = "Other";
            }
            totals.merge(category, entry.getAmount(), Double::sum);
        }

        distributionList.getChildren().clear();
        totals.entrySet().stream()
                .limit(6)
                .forEach(entry -> distributionList.getChildren().add(distributionRow(entry.getKey(),
                        total > 0 ? (entry.getValue() / total) * 100 : 0)));
    }

    private List<WalletEntry> filterEntries() {
        String timeframe = timeframeFilter.getValue();
        LocalDate start = null;
        LocalDate end = null;
        LocalDate today = LocalDate.now();
        if ("Today".equals(timeframe)) {
            start = today;
            end = today;
        } else if ("This Week".equals(timeframe)) {
            start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            end = start.plusDays(6);
        } else if ("This Month".equals(timeframe)) {
            start = today.withDayOfMonth(1);
            end = start.with(TemporalAdjusters.lastDayOfMonth());
        } else if ("This Year".equals(timeframe)) {
            start = today.withDayOfYear(1);
            end = today.with(TemporalAdjusters.lastDayOfYear());
        } else if ("Custom Range".equals(timeframe)) {
            start = startPicker.getValue();
            end = endPicker.getValue();
        }

        String type = typeFilter.getValue();
        String timeOfDay = timeFilter.getValue();
        LocalDate effectiveStart = start;
        LocalDate effectiveEnd = end;

        return walletEntries.stream()
                .filter(entry -> (effectiveStart == null || !entry.getDate().isBefore(effectiveStart)))
                .filter(entry -> (effectiveEnd == null || !entry.getDate().isAfter(effectiveEnd)))
                .filter(entry -> "All types".equals(type) || entry.getType().name().equalsIgnoreCase(type))
                .filter(entry -> timeOfDay == null || "Any time".equals(timeOfDay) || containsIgnoreCase(entry.getNote(), timeOfDay))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .collect(Collectors.toList());
    }

    private boolean containsIgnoreCase(String source, String needle) {
        return source != null && source.toLowerCase().contains(needle.toLowerCase());
    }

    private void updateCustomRangeState() {
        boolean custom = "Custom Range".equals(timeframeFilter.getValue());
        startPicker.setDisable(!custom);
        endPicker.setDisable(!custom);
    }

    private void recordEntry() {
        try {
            String item = itemField.getText().trim();
            String category = categoryField.getText().trim();
            String note = purchaseNoteField.getText().trim();
            if (!timeOfDayChoice.getValue().isBlank()) {
                note = timeOfDayChoice.getValue() + " • " + note;
            }
            double amount = Double.parseDouble(amountField.getText().trim());
            LocalDate date = entryDatePicker.getValue() != null ? entryDatePicker.getValue() : LocalDate.now();
            WalletEntryType type = switch (entryTypeChoice.getValue()) {
                case "Income" -> WalletEntryType.INCOME;
                case "Savings" -> WalletEntryType.SAVINGS;
                default -> WalletEntryType.EXPENSE;
            };
            WalletEntry entry = new WalletEntry(type,
                    category.isBlank() ? "General" : category,
                    amount,
                    (item.isBlank() ? note : item + " • " + note).trim(),
                    date);
            repository.addWalletEntry(user, entry);
            walletEntries.add(0, entry);
            refreshAllSections();
            clearEntryForm();
        } catch (NumberFormatException ex) {
            showAlert("Enter a valid numeric amount for the purchase.");
        }
    }

    private void clearEntryForm() {
        itemField.clear();
        categoryField.clear();
        amountField.clear();
        purchaseNoteField.clear();
        entryDatePicker.setValue(LocalDate.now());
        entryTypeChoice.getSelectionModel().select("Expense");
        timeOfDayChoice.getSelectionModel().selectFirst();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setHeaderText("Budget entry");
        alert.showAndWait();
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
}
