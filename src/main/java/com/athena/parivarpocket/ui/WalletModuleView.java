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
    private final VBox transactionList = new VBox(12);


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
            createMetricBox("Current Savings", savingsValue),
            createMetricBox("Net Balance", balanceValue)
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

        // Transaction List (New "Report" Section)
        VBox transactionsCont = new VBox(12);
        transactionsCont.getStyleClass().add("optimization-container"); // Reuse container style or create new
        transactionsCont.setStyle(transactionsCont.getStyle() + "; -fx-background-color: #ffffff;");
        
        Label transTitle = new Label("Recent Transactions");
        transTitle.getStyleClass().add("optimization-title");
        
        transactionList.setPadding(new Insets(10, 0, 0, 0));
        transactionsCont.getChildren().addAll(transTitle, transactionList);

        main.getChildren().addAll(simulatedWallet, optimization, transactionsCont);
        
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
        double income = repository.calculateIncome(walletEntries);
        double expenses = repository.calculateExpenses(walletEntries);
        double totalSavings = repository.calculateTotalSavings(walletEntries);
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
        updateTransactionList();
    }

    private void updateTransactionList() {
        transactionList.getChildren().clear();
        if (walletEntries.isEmpty()) {
            transactionList.getChildren().add(new Label("No transactions yet."));
            return;
        }

        // Filter out Education category (quiz coins) - they're not real money
        List<WalletEntry> realMoneyEntries = walletEntries.stream()
                .filter(entry -> !"Education".equalsIgnoreCase(entry.getCategory()))
                .limit(10)
                .collect(Collectors.toList());
        
        if (realMoneyEntries.isEmpty()) {
            transactionList.getChildren().add(new Label("No wallet transactions yet."));
            return;
        }

        // Show last 10 real money entries
        realMoneyEntries.forEach(entry -> {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.setStyle("-fx-background-color: #f9f9f9; -fx-background-radius: 8; -fx-border-color: #eee; -fx-border-width: 1;");

            Label date = new Label(entry.getDate().toString());
            date.setStyle("-fx-font-size: 11; -fx-text-fill: #888;");
            date.setPrefWidth(80);

            Label type = new Label(entry.getType().toString());
            type.setStyle("-fx-font-weight: bold; -fx-font-size: 11;");
            type.setPrefWidth(70);

            Label note = new Label(entry.getNote() != null && !entry.getNote().isEmpty() ? entry.getNote() : entry.getCategory());
            note.setStyle("-fx-font-size: 13;");
            HBox.setHgrow(note, Priority.ALWAYS);

            Label amount = new Label("₹" + Math.round(entry.getAmount()));
            amount.setStyle("-fx-font-weight: bold;");
            if (entry.getType() == WalletEntryType.EXPENSE) {
                amount.setStyle(amount.getStyle() + "; -fx-text-fill: #d32f2f;");
            } else if (entry.getType() == WalletEntryType.INCOME) {
                amount.setStyle(amount.getStyle() + "; -fx-text-fill: #2e7d32;");
            }

            row.getChildren().addAll(date, type, note, amount);
            transactionList.getChildren().add(row);
        });
    }

    private void updateRecommendations(double income, double expenses) {
        optimizationItems.getChildren().clear();

        // TEST 40-41: Only show tips when there are real issues; hide section when budget is healthy
        boolean hasIssues = false;

        // Dynamic Recommendations based on categories
        Map<String, Double> cats = walletEntries.stream()
            .filter(e -> e.getType() == WalletEntryType.EXPENSE)
            .collect(Collectors.groupingBy(WalletEntry::getCategory, Collectors.summingDouble(WalletEntry::getAmount)));

        // Only show food tip if actually overspending on food
        if (cats.containsKey("Food & Groceries") && income > 0 && cats.get("Food & Groceries") > income * 0.3) {
            addRecommendation("Reduce Food & Groceries spending by ₹400 through meal planning and bulk purchases");
            hasIssues = true;
        }

        if (cats.containsKey("Transportation") && cats.get("Transportation") > 500) {
            addRecommendation("Save ₹150 on Transportation by using public transport more often");
            hasIssues = true;
        }

        if (cats.containsKey("Entertainment") && income > 0 && cats.get("Entertainment") > income * 0.15) {
            addRecommendation("Cut Entertainment expenses by ₹200 with free community activities");
            hasIssues = true;
        }

        // Overall overspending tip
        if (income > 0 && expenses > income) {
            addRecommendation("Your total expenses exceed your income. Review and cut non-essential spending.");
            hasIssues = true;
        }

        // TEST 41: When budget is healthy, show a positive message instead of tips
        if (!hasIssues) {
            if (walletEntries.isEmpty()) {
                addRecommendation("Start recording your income and expenses to get personalized optimization tips.");
            } else {
                addRecommendation("\u2705 Your budget is healthy! Keep up the great financial habits.");
            }
        }
    }

    private void addRecommendation(String text) {
        HBox item = new HBox(8);
        item.setAlignment(Pos.TOP_LEFT);
        Label bullet = new Label("•");
        bullet.getStyleClass().add("optimization-bullet");
        Label lbl = new Label(text);
        lbl.getStyleClass().add("optimization-item");
        lbl.setWrapText(true);
        item.getChildren().addAll(bullet, lbl);
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
