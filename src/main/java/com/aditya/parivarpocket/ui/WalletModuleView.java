package com.aditya.parivarpocket.ui;

import com.aditya.parivarpocket.model.BudgetGoal;
import com.aditya.parivarpocket.model.User;
import com.aditya.parivarpocket.model.WalletEntry;
import com.aditya.parivarpocket.model.WalletEntryType;
import com.aditya.parivarpocket.service.DataRepository;
import com.aditya.parivarpocket.service.OfflineSyncService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import com.google.gson.JsonObject;
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

    private final Label incomeValue = new Label("₹0");
    private final Label expensesValue = new Label("₹0");
    private final Label savingsValue = new Label("₹0");
    private final Label balanceValue = new Label("₹0");
    
    // Status section labels
    private final Label budgetStatusTitle = new Label("Budget Status");
    private final Label budgetStatusMsg = new Label("Analyzing your budget...");
    private final VBox optimizationItems = new VBox(8);

    // Form fields for Record New Entry (moved to separate dialog or kept as hidden?)
    // Mockup doesn't show the form, but shows a button to trigger it.
    // I will keep the form logic but trigger it via a modal to match the design.

    public WalletModuleView(DataRepository repository, User user, OfflineSyncService offlineSyncService) {
        this.repository = repository;
        this.user = user;
        this.offlineSyncService = offlineSyncService;
        this.walletEntries = new ArrayList<>(repository.loadWallet(user));
        this.budgetGoal = repository.getBudgetGoal(user);

        setSpacing(0);
        setPadding(new Insets(0));
        getStyleClass().add("wallet-container");

        getChildren().addAll(buildHeader(), buildMainContent());
        refreshAllSections();
    }

    private Node buildHeader() {
        return new Region(); // Page header is now handled by MainLayout centrally
    }

    private Node buildMainContent() {
        VBox main = new VBox(24);
        main.setPadding(new Insets(0, 24, 24, 24));

        VBox simulatedWallet = new VBox(20);
        simulatedWallet.getStyleClass().add("quiz-container");
        simulatedWallet.setPadding(new Insets(0));

        Label sectionHeader = new Label("Simulated Wallet - Monthly Budget");
        sectionHeader.getStyleClass().add("modal-header-title");
        sectionHeader.setPadding(new Insets(12, 16, 12, 16));
        
        StackPane headerBg = new StackPane(sectionHeader);
        headerBg.setAlignment(Pos.CENTER_LEFT);
        headerBg.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");

        // Metrics Row
        HBox metricsRow = new HBox(16);
        metricsRow.getStyleClass().add("wallet-metric-container");
        metricsRow.getChildren().addAll(
            createMetricBox("Monthly Income", incomeValue),
            createMetricBox("Monthly Expenses", expensesValue),
            createMetricBox("Current Savings", savingsValue)
        );
        for (Node n : metricsRow.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        // Status Box
        VBox statusContainer = new VBox();
        statusContainer.getStyleClass().add("budget-status-container");
        
        HBox statusBox = new HBox(20);
        statusBox.getStyleClass().add("budget-status-box");
        statusBox.setAlignment(Pos.CENTER_LEFT);

        VBox statusText = new VBox(4);
        budgetStatusTitle.getStyleClass().add("budget-status-title");
        budgetStatusMsg.getStyleClass().add("budget-status-message");
        budgetStatusMsg.setWrapText(true);
        statusText.getChildren().addAll(budgetStatusTitle, budgetStatusMsg);
        HBox.setHgrow(statusText, Priority.ALWAYS);

        Button recordBtn = new Button("Record New Entry");
        recordBtn.getStyleClass().add("wallet-record-btn");
        recordBtn.setOnAction(e -> showRecordEntryDialog());

        statusBox.getChildren().addAll(statusText, recordBtn);
        statusContainer.getChildren().add(statusBox);

        simulatedWallet.getChildren().addAll(headerBg, metricsRow, statusContainer);

        // Optimization Section
        VBox optimization = new VBox(12);
        optimization.getStyleClass().add("optimization-container");
        
        Label optTitle = new Label("Optimization Recommendations");
        optTitle.getStyleClass().add("optimization-title");
        
        optimizationItems.getChildren().clear();
        optimization.getChildren().addAll(optTitle, optimizationItems);

        main.getChildren().addAll(simulatedWallet, optimization);

        ScrollPane scroll = new ScrollPane(main);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private VBox createMetricBox(String label, Label val) {
        VBox box = new VBox(4);
        box.getStyleClass().add("wallet-metric-box");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("wallet-metric-label");
        val.getStyleClass().add("wallet-metric-value");
        box.getChildren().addAll(lbl, val);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void refreshAllSections() {
        this.walletEntries = new ArrayList<>(repository.loadWallet(user));
        // Exclude Education/coin entries so metrics only reflect real rupee transactions
        List<WalletEntry> realEntries = walletEntries.stream()
                .filter(e -> !"Education".equalsIgnoreCase(e.getCategory()))
                .collect(java.util.stream.Collectors.toList());
        double income = repository.calculateIncome(realEntries);
        double expenses = repository.calculateExpenses(realEntries);
        double totalSavings = repository.calculateTotalSavings(realEntries);
        double netBalance = income - expenses;
        
        // Fetch latest budget goal
        this.budgetGoal = repository.getBudgetGoal(user);
        double budgetLimit = (budgetGoal != null) ? budgetGoal.getCurrentBudget() : income;

        incomeValue.setText("₹" + Math.round(income));
        expensesValue.setText("₹" + Math.round(expenses));
        savingsValue.setText("₹" + Math.round(totalSavings));
        balanceValue.setText("₹" + Math.round(netBalance));
        
        // TEST 36: Three-state balance color: green (surplus >10%), yellow (stable 0-10%), red (deficit)
        double surplusRatio = income > 0 ? netBalance / income : (netBalance >= 0 ? 1 : -1);
        if (netBalance < 0) {
            // Deficit — red
            balanceValue.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        } else if (surplusRatio <= 0.10) {
            // Stable — yellow (within 10% margin)
            balanceValue.setStyle("-fx-text-fill: #F57C00; -fx-font-weight: bold;");
        } else {
            // Surplus — green
            balanceValue.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        }

        // Update status - compare expenses against budget limit (if set) or income
        double balance = budgetLimit - expenses;
        if (balance < 0) {
            budgetStatusTitle.setText(String.format("Budget = ₹%d — Budget Deficit!", Math.round(budgetLimit)));
            budgetStatusMsg.setText(String.format("Your expenses (₹%d) exceed your set budget (₹%d). Consider reducing spending.", 
                Math.round(expenses), Math.round(budgetLimit)));
            budgetStatusTitle.setStyle("-fx-text-fill: #d32f2f;");
        } else if (surplusRatio <= 0.10) {
            budgetStatusTitle.setText(String.format("Budget = ₹%d — Budget Stable", Math.round(budgetLimit)));
            budgetStatusMsg.setText(String.format("You are close to your budget limit. Expenses (₹%d) are near your limit (₹%d).",
                Math.round(expenses), Math.round(budgetLimit)));
            budgetStatusTitle.setStyle("-fx-text-fill: #F57C00;");
        } else {
            budgetStatusTitle.setText(String.format("Budget = ₹%d — Budget Surplus!", Math.round(budgetLimit)));
            budgetStatusMsg.setText(String.format("You are within your budget. Your expenses (₹%d) are within your set limit (₹%d).", 
                Math.round(expenses), Math.round(budgetLimit)));
            budgetStatusTitle.setStyle("-fx-text-fill: #2e7d32;");
        }

        // Update Recommendations
        updateRecommendations(income, expenses);
    }

    private void updateRecommendations(double income, double expenses) {
        optimizationItems.getChildren().clear();
        
        addRecommendation("Reduce ", "Food & Groceries", " spending by ₹400 through meal planning and bulk purchases");
        addRecommendation("Save ₹150 on ", "Transportation", " by using public transport more often");
        addRecommendation("Cut ", "Entertainment", " expenses by ₹200 with free community activities");
    }

    private void addRecommendation(String part1, String boldPart, String part2) {
        HBox item = new HBox(8);
        item.setAlignment(Pos.TOP_LEFT);
        Label bullet = new Label("•");
        bullet.getStyleClass().add("optimization-bullet");
        
        javafx.scene.text.Text t1 = new javafx.scene.text.Text(part1);
        t1.getStyleClass().add("optimization-item");
        
        javafx.scene.text.Text t2 = new javafx.scene.text.Text(boldPart);
        t2.getStyleClass().add("optimization-item");
        t2.setStyle("-fx-font-weight: bold;");
        
        javafx.scene.text.Text t3 = new javafx.scene.text.Text(part2);
        t3.getStyleClass().add("optimization-item");
        
        javafx.scene.text.TextFlow textFlow = new javafx.scene.text.TextFlow(t1, t2, t3);
        
        item.getChildren().addAll(bullet, textFlow);
        optimizationItems.getChildren().add(item);
    }

    private void showRecordEntryDialog() {
        Stage stage = new Stage();
        stage.setTitle("Record New Entry");
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox form = new VBox(20);
        form.setPadding(new Insets(24));
        form.getStyleClass().add("quiz-container");

        TextField itemField = new TextField();
        itemField.setPromptText("What did you buy?");
        itemField.getStyleClass().add("modern-input");
        
        TextField categoryField = new TextField();
        categoryField.setPromptText("Category (e.g., Food, Transport)");
        categoryField.getStyleClass().add("modern-input");
        
        TextField amountField = new TextField();
        amountField.setPromptText("₹ Amount");
        amountField.getStyleClass().add("modern-input");
        
        ChoiceBox<String> typeChoice = new ChoiceBox<>(FXCollections.observableArrayList("Expense", "Savings", "Income", "Budget"));
        typeChoice.getSelectionModel().select("Expense");
        typeChoice.getStyleClass().add("modern-choice");
        typeChoice.setMaxWidth(Double.MAX_VALUE);

        Button saveBtn = new Button("Save Entry");
        saveBtn.getStyleClass().add("job-apply-dark-btn");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> {
            try {
                double amt = Double.parseDouble(amountField.getText().trim());
                WalletEntryType type = switch (typeChoice.getValue()) {
                    case "Income" -> WalletEntryType.INCOME;
                    case "Savings" -> WalletEntryType.SAVINGS;
                    case "Budget" -> WalletEntryType.BUDGET;
                    default -> WalletEntryType.EXPENSE;
                };
                
                if (type == WalletEntryType.BUDGET) {
                    repository.upsertBudgetGoal(user, amt, 0);
                }
                
                WalletEntry entry = new WalletEntry(type, 
                    categoryField.getText().isEmpty() ? "General" : categoryField.getText(),
                    amt, itemField.getText(), LocalDate.now());
                
                repository.addWalletEntry(user, entry);
                walletEntries.add(0, entry);
                refreshAllSections();
                stage.close();

                // TEST 37: Check for overspending after adding entry and log alert for educator
                if (type == WalletEntryType.EXPENSE) {
                    double currentIncome = repository.calculateIncome(walletEntries);
                    double currentExpenses = repository.calculateExpenses(walletEntries);
                    if (currentExpenses > currentIncome && currentIncome > 0) {
                        JsonObject meta = new JsonObject();
                        meta.addProperty("income", currentIncome);
                        meta.addProperty("expenses", currentExpenses);
                        meta.addProperty("deficit", currentExpenses - currentIncome);
                        repository.logAlert(user, "Overspending",
                            "Student's expenses (\u20b9" + Math.round(currentExpenses) + ") exceed income (\u20b9" + Math.round(currentIncome) + ")",
                            "warning", meta);
                    }
                }
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Invalid amount").show();
            }
        });

        form.getChildren().addAll(
            new Label("Entry Type"), typeChoice,
            new Label("Amount"), amountField,
            new Label("Category"), categoryField,
            new Label("Reference/Item"), itemField,
            saveBtn
        );

        Scene scene = new Scene(form, 400, 500);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
}
