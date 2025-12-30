package com.athena.parivarpocket.ui;

import com.athena.parivarpocket.model.BudgetGoal;
import com.athena.parivarpocket.model.User;
import com.athena.parivarpocket.model.WalletEntry;
import com.athena.parivarpocket.model.WalletEntryType;
import com.athena.parivarpocket.service.DataRepository;
import com.athena.parivarpocket.service.OfflineSyncService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
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

    private final ChoiceBox<String> timeframeFilter = new ChoiceBox<>(FXCollections.observableArrayList("Today", "This Week", "This Month", "This Year"));
    private final VBox historyContainer = new VBox(12);
    private final Label historySummaryLabel = new Label();

    private final PieChart expenseChart = new PieChart();
    private final VBox expenseLegend = new VBox(6);

    public WalletModuleView(DataRepository repository, User user, OfflineSyncService offlineSyncService) {
        this.repository = repository;
        this.user = user;
        this.offlineSyncService = offlineSyncService;
        this.walletEntries = new ArrayList<>(repository.loadWallet(user));
        this.budgetGoal = repository.getBudgetGoal(user);

        setSpacing(24);
        setPadding(new Insets(0, 0, 24, 0));
        getStyleClass().add("wallet-container");

        // Mobile-friendly vertical stack
        getChildren().addAll(
                buildBudgetPanel(),
                buildVisualizationPanel(),
                buildGoalPanel(),
                buildEntryFormPanel(),
                buildTimelinePanel()
        );

        refreshAllSections();
    }

    private Panel buildBudgetPanel() {
        HBox metrics = new HBox(0,
                createMetric("Income", incomeValue, "metric-income"),
                createSeparator(),
                createMetric("Expenses", expensesValue, "metric-expense"),
                createSeparator(),
                createMetric("Savings", savingsValue, "metric-savings")
        );
        metrics.setAlignment(Pos.CENTER);
        metrics.setSpacing(0);
        HBox.setHgrow(metrics, Priority.ALWAYS);

        budgetAlertLabel.getStyleClass().add("budget-alert");
        budgetAlertLabel.setWrapText(true);

        VBox content = new VBox(16, metrics, budgetAlertLabel);
        content.setPadding(new Insets(16));
        Panel panel = new Panel("Overview", content);
        panel.getStyleClass().add("wallet-card");
        return panel;
    }

    private Region createSeparator() {
        Region sep = new Region();
        sep.setPrefWidth(1);
        sep.setPrefHeight(40);
        sep.setStyle("-fx-background-color: #e0e0e0;");
        return sep;
    }

    private Panel buildGoalPanel() {
        currentBudgetField.getStyleClass().add("modern-input");
        currentBudgetField.setPromptText("Budget limit (₹)");
        targetSavingsField.getStyleClass().add("modern-input");
        targetSavingsField.setPromptText("Goal (₹)");
        goalStatusLabel.getStyleClass().add("goal-status");
        goalStatusLabel.setWrapText(true);

        Button saveGoal = new Button("Update Goals");
        saveGoal.getStyleClass().add("secondary-button");
        saveGoal.setMaxWidth(Double.MAX_VALUE);
        saveGoal.setOnAction(e -> {
            try {
                double current = Double.parseDouble(currentBudgetField.getText().trim());
                double target = Double.parseDouble(targetSavingsField.getText().trim());
                budgetGoal = repository.upsertBudgetGoal(user, current, target);
                updateGoalStatus();
                showAlert("Success", "Budget goals updated successfully.");
            } catch (NumberFormatException ex) {
                showAlert("Error", "Please enter valid numbers.");
            }
        });

        HBox inputs = new HBox(12, currentBudgetField, targetSavingsField);
        
        VBox content = new VBox(12, inputs, saveGoal, goalStatusLabel);
        content.setPadding(new Insets(16));
        Panel panel = new Panel("Targets", content);
        panel.getStyleClass().add("wallet-card");
        return panel;
    }

    private Panel buildEntryFormPanel() {
        itemField.setPromptText("What did you buy?");
        itemField.getStyleClass().add("modern-input");
        
        categoryField.setPromptText("Category");
        categoryField.getStyleClass().add("modern-input");
        
        amountField.setPromptText("₹ Amount");
        amountField.getStyleClass().add("modern-input");
        
        purchaseNoteField.setPromptText("Add a note...");
        purchaseNoteField.getStyleClass().add("modern-input");
        
        entryDatePicker.getStyleClass().add("modern-input");
        entryTypeChoice.getSelectionModel().select("Expense");
        entryTypeChoice.getStyleClass().add("modern-choice");
        timeOfDayChoice.getSelectionModel().selectFirst();
        timeOfDayChoice.getStyleClass().add("modern-choice");

        Button logEntry = new Button("Add Transaction");
        logEntry.getStyleClass().add("primary-button-lg");
        logEntry.setMaxWidth(Double.MAX_VALUE);
        logEntry.setOnAction(e -> recordEntry());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(16);
        
        grid.add(new Label("Type"), 0, 0);
        grid.add(entryTypeChoice, 1, 0);
        
        grid.add(new Label("Amount"), 0, 1);
        grid.add(amountField, 1, 1);
        
        grid.add(new Label("Details"), 0, 2);
        grid.add(itemField, 1, 2);
        
        grid.add(new Label("Category"), 0, 3);
        grid.add(categoryField, 1, 3);
        
        grid.add(new Label("Date"), 0, 4);
        grid.add(entryDatePicker, 1, 4);
        
        grid.add(new Label("Note"), 0, 5);
        grid.add(purchaseNoteField, 1, 5);

        VBox content = new VBox(20, grid, logEntry);
        content.setPadding(new Insets(20));
        Panel panel = new Panel("New Entry", content);
        panel.getStyleClass().add("wallet-card");
        return panel;
    }

    private Panel buildVisualizationPanel() {
        expenseChart.setLabelsVisible(false);
        expenseChart.setLegendVisible(false);
        expenseChart.setPrefHeight(220);
        expenseChart.setMaxHeight(220);

        StackPane chartContainer = new StackPane(expenseChart);
        chartContainer.setAlignment(Pos.CENTER);
        
        expenseLegend.setAlignment(Pos.CENTER);
        expenseLegend.setPadding(new Insets(10, 0, 0, 0));

        HBox layout = new HBox(20, chartContainer, expenseLegend);
        layout.setAlignment(Pos.CENTER_LEFT);
        
        VBox content = new VBox(10, layout);
        content.setPadding(new Insets(16));
        Panel panel = new Panel("Expense Breakdown", content);
        panel.getStyleClass().add("wallet-card");
        return panel;
    }

    private Panel buildTimelinePanel() {
        timeframeFilter.getSelectionModel().select("This Month");
        timeframeFilter.setOnAction(e -> refreshHistoryPanel());
        timeframeFilter.getStyleClass().add("modern-choice-sm");

        HBox header = new HBox(12, new Label("Recent Activity"), new Region(), timeframeFilter);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("timeline-header");

        historySummaryLabel.getStyleClass().add("history-summary");

        historyContainer.setPadding(new Insets(8, 0, 0, 0));
        ScrollPane scrollPane = new ScrollPane(historyContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        scrollPane.getStyleClass().add("transparent-scroll");

        VBox content = new VBox(12, header, historySummaryLabel, scrollPane);
        content.setPadding(new Insets(16));
        Panel panel = new Panel(null, content); // Custom header inside
        panel.getStyleClass().add("wallet-card");
        return panel;
    }

    private VBox createMetric(String label, Label valueLabel, String styleClass) {
        Label title = new Label(label);
        title.getStyleClass().add("metric-title");
        valueLabel.getStyleClass().addAll("metric-value", styleClass);
        VBox box = new VBox(4, valueLabel, title);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(0, 24, 0, 24));
        return box;
    }

    private void refreshAllSections() {
        refreshBudgetPanel();
        refreshGoalPanel();
        refreshHistoryPanel();
        refreshCharts();
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

        if (expenses >= income && income > 0) {
            budgetAlertLabel.setText("⚠️ Warning: Expenses exceed income.");
            budgetAlertLabel.getStyleClass().add("alert-danger");
        } else {
            double remaining = income - expenses;
            budgetAlertLabel.setText("You have ₹" + Math.round(remaining) + " remaining this month.");
            budgetAlertLabel.getStyleClass().remove("alert-danger");
        }
    }

    private void refreshGoalPanel() {
        if (budgetGoal != null) {
            currentBudgetField.setText(String.valueOf(Math.round(budgetGoal.getCurrentBudget())));
            targetSavingsField.setText(String.valueOf(Math.round(budgetGoal.getTargetSavings())));
            updateGoalStatus();
        } else {
            goalStatusLabel.setText("Set a budget and savings goal to track your progress.");
        }
    }

    private void updateGoalStatus() {
        double expenses = repository.calculateExpenses(walletEntries);
        double target = budgetGoal != null ? budgetGoal.getTargetSavings() : 0;
        double income = repository.calculateIncome(walletEntries);
        double currentSavings = income - expenses; // Simple calc
        
        double delta = target - currentSavings;
        if (delta > 0) {
            goalStatusLabel.setText("🎯 " + Math.round((currentSavings / target) * 100) + "% to savings goal (₹" + Math.round(delta) + " to go)");
        } else if (target > 0) {
            goalStatusLabel.setText("🎉 Savings goal reached!");
        }
    }

    private void refreshHistoryPanel() {
        List<WalletEntry> filtered = filterEntries();
        
        historySummaryLabel.setText(filtered.size() + " transactions in this period");
        historyContainer.getChildren().clear();
        
        if (filtered.isEmpty()) {
            Label empty = new Label("No transactions found.");
            empty.setStyle("-fx-text-fill: #999; -fx-padding: 20;");
            historyContainer.getChildren().add(empty);
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        
        for (WalletEntry entry : filtered) {
             HBox row = new HBox(12);
             row.setAlignment(Pos.CENTER_LEFT);
             row.getStyleClass().add("transaction-row");
             row.setPadding(new Insets(12));

             Label dateIcon = new Label(entry.getDate().format(formatter).replace(" ", "\n"));
             dateIcon.setAlignment(Pos.CENTER);
             dateIcon.getStyleClass().add("date-badge");
             dateIcon.setPrefWidth(50);

             VBox details = new VBox(2);
             Label title = new Label(entry.getCategory());
             title.getStyleClass().add("trans-title");
             Label subtitle = new Label(entry.getNote().isEmpty() ? entry.getType().toString() : entry.getNote());
             subtitle.getStyleClass().add("trans-subtitle");
             details.getChildren().addAll(title, subtitle);
             
             Region spacer = new Region();
             HBox.setHgrow(spacer, Priority.ALWAYS);

             String prefix = entry.getType() == WalletEntryType.EXPENSE ? "-" : "+";
             String colorClass = entry.getType() == WalletEntryType.EXPENSE ? "amount-neg" : "amount-pos";
             Label amount = new Label(prefix + "₹" + Math.round(entry.getAmount()));
             amount.getStyleClass().addAll("trans-amount", colorClass);

             row.getChildren().addAll(dateIcon, details, spacer, amount);
             historyContainer.getChildren().add(row);
        }
    }

    private void refreshCharts() {
        Map<String, Double> expensesByCategory = walletEntries.stream()
                .filter(e -> e.getType() == WalletEntryType.EXPENSE)
                .collect(Collectors.groupingBy(WalletEntry::getCategory, Collectors.summingDouble(WalletEntry::getAmount)));

        ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();
        expensesByCategory.forEach((cat, amt) -> chartData.add(new PieChart.Data(cat, amt)));
        
        expenseChart.setData(chartData);
        
        // Custom Legend
        expenseLegend.getChildren().clear();
        double totalExpense = expensesByCategory.values().stream().mapToDouble(Double::doubleValue).sum();
        
        int count = 0;
        for (Map.Entry<String, Double> entry : expensesByCategory.entrySet()) {
            if (count++ > 5) break; // Limit legend items
            double pct = (entry.getValue() / totalExpense) * 100;
            HBox item = new HBox(8);
            item.setAlignment(Pos.CENTER_LEFT);
            Region dot = new Region();
            dot.setPrefSize(12, 12);
            dot.setStyle("-fx-background-color: " + getColorForIndex(count) + "; -fx-background-radius: 6;");
            
            Label name = new Label(entry.getKey());
            name.getStyleClass().add("legend-label");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label val = new Label(String.format("%.1f%%", pct));
            val.getStyleClass().add("legend-value");
            
            item.getChildren().addAll(dot, name, spacer, val);
            expenseLegend.getChildren().add(item);
        }
    }
    
    // Naive color generator matching JavaFX defaults roughly
    private String getColorForIndex(int i) {
        String[] colors = {"#f3622d", "#fba71b", "#57b757", "#41a9c9", "#4258c9", "#9a42c8", "#c84164", "#888888"};
        return colors[i % colors.length];
    }

    private List<WalletEntry> filterEntries() {
        // Simplified filter logic for demo
        String timeframe = timeframeFilter.getValue();
        LocalDate now = LocalDate.now();
        LocalDate start = switch (timeframe) {
            case "This Week" -> now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "This Month" -> now.withDayOfMonth(1);
            case "This Year" -> now.withDayOfYear(1);
            default -> now; // "Today"
        };
        
        return walletEntries.stream()
                .filter(e -> !e.getDate().isBefore(start))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .collect(Collectors.toList());
    }

    private void recordEntry() {
        try {
            String item = itemField.getText().trim();
            String category = categoryField.getText().trim();
            String note = purchaseNoteField.getText().trim();
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
                    (item.isBlank() ? note : item + (note.isEmpty() ? "" : " • " + note)).trim(),
                    date);
            
            repository.addWalletEntry(user, entry);
            walletEntries.add(0, entry);
            
            refreshAllSections();
            clearEntryForm();
        } catch (NumberFormatException ex) {
            showAlert("Invalid Input", "Please enter a valid numeric amount.");
        } catch (Exception ex) {
            ex.printStackTrace(); // Log for debug
            showAlert("Error Adding Entry", "Failed to save entry: " + ex.getMessage());
        }
    }

    private void clearEntryForm() {
        itemField.clear();
        categoryField.clear();
        amountField.clear();
        purchaseNoteField.clear();
        entryDatePicker.setValue(LocalDate.now());
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(title);
        alert.show();
    }
}
