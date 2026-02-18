package com.athena.parivarpocket.ui;

import com.athena.parivarpocket.model.JobOpportunity;
import com.athena.parivarpocket.model.JobApplication;
import com.athena.parivarpocket.model.Lesson;
import com.athena.parivarpocket.model.QuizResult;
import com.athena.parivarpocket.model.User;
import com.athena.parivarpocket.model.WalletEntry;
import com.athena.parivarpocket.model.WalletEntryType;
import com.athena.parivarpocket.service.DataRepository;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import com.google.gson.JsonObject;
import java.util.List;

public class StudentDashboardView extends VBox {
    private final DataRepository repository;
    private final User user;

    public StudentDashboardView(User user, DataRepository repository) {
        this.user = user;
        this.repository = repository;
        setSpacing(24);
        setPadding(new Insets(24));
        getStyleClass().add("dashboard-container");

        List<Lesson> lessons = repository.getLessons();
        long completedCount = lessons.stream().filter(l -> repository.isLessonCompleted(l.getId())).count();
        int modulesCompleted = (int) completedCount;
        
        int coins = 0;
        com.athena.parivarpocket.model.StudentProgress progress = repository.getStudentProgress(user.getEmail());
        if (progress != null) {
            coins = progress.getParivaarPoints();
        }
        
        java.util.Map<String, Double> quizStats = repository.getQuizStats(user);
        int maxScore = quizStats.get("max").intValue();
        int minScore = quizStats.get("min").intValue();
        int medianScore = quizStats.get("median").intValue();
        
        String badgeLabel = determineBadge(modulesCompleted);

        // Hero Section
        getChildren().add(buildWelcomePanel(user, modulesCompleted, lessons.size(), coins, badgeLabel));

        // Stats Grid (Learning & Wallet side-by-side)
        HBox summaryGrid = new HBox(24);
        summaryGrid.setAlignment(Pos.TOP_LEFT);
        
        VBox learningPanel = buildLearningProgress(modulesCompleted, lessons.size(), maxScore, minScore, medianScore);
        VBox walletPanel = buildWalletSummary(repository, user);
        
        learningPanel.setPrefWidth(400);
        walletPanel.setPrefWidth(400);
        
        HBox.setHgrow(learningPanel, Priority.ALWAYS);
        HBox.setHgrow(walletPanel, Priority.ALWAYS);

        summaryGrid.getChildren().addAll(learningPanel, walletPanel);
        getChildren().add(summaryGrid);

        // Jobs Section - Show all favorited jobs on dashboard
        List<JobOpportunity> favoritedJobs = repository.fetchFavoriteJobs(user);
        getChildren().add(buildBookmarkedJobs(favoritedJobs));

        // TEST 28: Applied Jobs section - show jobs with "Applied ✓" status
        VBox appliedSection = buildAppliedJobsSection();
        getChildren().add(appliedSection);

        // Load applied jobs asynchronously
        Task<List<JobApplication>> appsTask = new Task<>() {
            @Override
            protected List<JobApplication> call() {
                return repository.fetchJobApplications(user.getEmail());
            }
        };
        appsTask.setOnSucceeded(e -> {
            List<JobApplication> apps = appsTask.getValue();
            Platform.runLater(() -> populateAppliedJobs(appliedSection, apps));
        });
        new Thread(appsTask).start();
    }

    private Panel buildWelcomePanel(User user,
                                    int modulesCompleted,
                                    int moduleTotal,
                                    int coins,
                                    String badgeLabel) {
        Label greeting = new Label("Good " + getTimeOfDay() + ", " + user.getName().split(" ")[0]);
        greeting.getStyleClass().add("hero-greeting");
        
        Label subtitle = new Label("You're making great progress towards your financial goals.");
        subtitle.getStyleClass().add("hero-subtitle");

        VBox left = new VBox(8, greeting, subtitle);
        left.setAlignment(Pos.CENTER_LEFT);

        VBox pointsBox = createHeroMetric("ParivaarCoins", String.valueOf(coins), "Keep learning!");
        VBox badgeBox = createHeroMetric("Current Badge", badgeLabel, modulesCompleted + " modules done");
        
        HBox metrics = new HBox(32, pointsBox, badgeBox);
        metrics.setAlignment(Pos.CENTER_RIGHT);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox content = new HBox(24, left, spacer, metrics);
        content.setAlignment(Pos.CENTER_LEFT);
        
        Panel panel = new Panel(null, content);
        panel.getStyleClass().add("hero-banner");
        panel.setPadding(new Insets(32));
        return panel;
    }

    private VBox buildLearningProgress(int modulesCompleted,
                                        int moduleTotal,
                                        int maxScore,
                                        int minScore,
                                        int medianScore) {
        Label header = new Label("Learning Performance");
        header.getStyleClass().add("panel-header");
        
        double progress = moduleTotal > 0 ? (double) modulesCompleted / moduleTotal : 0;
        ProgressBar progressBar = new ProgressBar(progress);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("learning-progress-bar");

        Label progressLabel = new Label(modulesCompleted + " / " + moduleTotal + " Modules Completed (" + Math.round(progress * 100) + "%)");
        progressLabel.getStyleClass().add("progress-text");

        // Get quizzes taken from student progress for "live" stats
        int quizzesTaken = 0;
        com.athena.parivarpocket.model.StudentProgress progressObj = repository.getStudentProgress(this.user.getEmail());
        if (progressObj != null) {
            quizzesTaken = progressObj.getQuizzesTaken();
        }
        
        HBox stats = new HBox(0,
                createStatItem("Quizzes Taken", String.valueOf(quizzesTaken)),
                createVerticalSeparator(),
                createStatItem("Average Score", medianScore + "%"),
                createVerticalSeparator(),
                createStatItem("Best Score", maxScore + "%")
        );
        stats.setAlignment(Pos.CENTER);
        
        VBox content = new VBox(16, header, progressBar, progressLabel, stats);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("dashboard-card");
        return content; // Returning VBox to use directly in HBox layout
    }

    private VBox buildWalletSummary(DataRepository repository, User user) {
        List<WalletEntry> entries = repository.loadWallet(user);
        double income = repository.calculateIncome(entries);
        double expenses = repository.calculateExpenses(entries);
        double totalSavings = repository.calculateTotalSavings(entries);
        
        com.athena.parivarpocket.model.BudgetGoal goal = repository.getBudgetGoal(user);
        double savingsGoal = (goal != null && goal.getTargetSavings() > 0) ? goal.getTargetSavings() : 5000;
        double budgetLimit = (goal != null && goal.getCurrentBudget() > 0) ? goal.getCurrentBudget() : income;

        Label header = new Label("Wallet Summary");
        header.getStyleClass().add("panel-header");

        // Savings Goal Progress
        Label savingsLabel = new Label("Current Savings Goal Progress");
        savingsLabel.getStyleClass().add("progress-label");
        
        double savingsProgress = savingsGoal > 0 ? Math.min(Math.max(totalSavings, 0) / savingsGoal, 1.0) : 0;
        
        ProgressBar savingsBar = new ProgressBar(savingsProgress);
        savingsBar.setMaxWidth(Double.MAX_VALUE);
        savingsBar.getStyleClass().add("dashboard-progress-bar");
        
        Label savingsCount = new Label("₹" + Math.round(totalSavings) + " / ₹" + Math.round(savingsGoal));
        savingsCount.getStyleClass().add("progress-count");
        
        HBox savingsRow = new HBox(12, savingsBar, savingsCount);
        savingsRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(savingsBar, Priority.ALWAYS);

        // Budget Status Summary
        double balance = income - expenses;
        String statusText = balance >= 0 ? "Net Balance: ₹" + Math.round(balance) : "Deficit: ₹" + Math.round(Math.abs(balance));
        Label statusLabel = new Label(statusText);
        statusLabel.setStyle(balance >= 0 ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;" : "-fx-text-fill: #d32f2f; -fx-font-weight: bold;");

        HBox stats = new HBox(0,
                createStatItem("Income", "₹" + Math.round(income)),
                createVerticalSeparator(),
                createStatItem("Expenses", "₹" + Math.round(expenses)),
                createVerticalSeparator(),
                createStatItem("Budget", "₹" + Math.round(budgetLimit))
        );
        stats.setAlignment(Pos.CENTER);
        
        VBox content = new VBox(12, header, savingsLabel, savingsRow, statusLabel, stats);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("dashboard-card");
        return content;
    }

    private Panel buildBookmarkedJobs(List<JobOpportunity> jobs) {
        if (jobs.isEmpty()) {
            VBox emptyBox = new VBox(new Label("No bookmarked opportunities. Explore and save jobs from the Work tab!"));
            emptyBox.setPadding(new Insets(24));
            emptyBox.getStyleClass().add("dashboard-card");
            return new Panel("Bookmarked Opportunities", emptyBox);
        }
        
        VBox list = new VBox(16);
        jobs.forEach(job -> {
             HBox row = new HBox(16);
             row.setAlignment(Pos.CENTER_LEFT);
             
             VBox info = new VBox(4);
             Label title = new Label(job.getTitle());
             title.getStyleClass().add("job-title");
             Label meta = new Label(job.getLocation() + " • " + job.getCompany());
             meta.getStyleClass().add("job-meta");
             info.getChildren().addAll(title, meta);
             
             Region spacer = new Region();
             HBox.setHgrow(spacer, Priority.ALWAYS);
             
             Button apply = new Button("Apply");
             apply.getStyleClass().add("apply-button");
             apply.setOnAction(e -> {
                 String indeedLink = "https://in.indeed.com/viewjob?jk=" + job.getId();
                 
                 // Log dashboard job view activity
                 User currentUser = repository.getCurrentUser();
                 if (currentUser != null) {
                     com.google.gson.JsonObject activityData = new com.google.gson.JsonObject();
                     activityData.addProperty("job_id", job.getId());
                     activityData.addProperty("job_title", job.getTitle());
                     activityData.addProperty("company", job.getCompany());
                     activityData.addProperty("source", "dashboard");
                     repository.logStudentActivity(currentUser, "job_view_from_dashboard", activityData);
                 }

                 try {
                     java.awt.Desktop.getDesktop().browse(new java.net.URI(indeedLink));
                 } catch (java.io.IOException | java.net.URISyntaxException ex) {
                     ex.printStackTrace();
                 }
             });
             
             Button remove = new Button("Remove");
             remove.getStyleClass().add("remove-button");
             remove.setOnAction(e -> {
                 User currentUser = repository.getCurrentUser();
                 if (currentUser != null) {
                     repository.removeFavoriteJob(currentUser.getId(), job.getId());
                     // Refresh the dashboard by removing from UI
                     list.getChildren().remove(row);
                 }
             });
             
             HBox buttons = new HBox(8, apply, remove);
             buttons.setAlignment(Pos.CENTER_RIGHT);
             
             row.getChildren().addAll(info, spacer, buttons);
             list.getChildren().add(row);
        });
        
        VBox content = new VBox(16, list);
        content.setPadding(new Insets(20));
        Panel panel = new Panel("Bookmarked Opportunities", content);
        panel.getStyleClass().add("dashboard-card");
        return panel;
    }

    /** TEST 28: Applied Jobs section placeholder — populated async */
    private VBox buildAppliedJobsSection() {
        VBox section = new VBox(12);
        section.getStyleClass().add("dashboard-card");
        section.setPadding(new Insets(20));
        Label header = new Label("Applied Jobs");
        header.getStyleClass().add("panel-header");
        Label loading = new Label("Loading applied jobs...");
        loading.setStyle("-fx-text-fill: #888;");
        section.getChildren().addAll(header, loading);
        return section;
    }

    private void populateAppliedJobs(VBox section, List<JobApplication> apps) {
        section.getChildren().clear();
        Label header = new Label("Applied Jobs");
        header.getStyleClass().add("panel-header");
        section.getChildren().add(header);

        if (apps == null || apps.isEmpty()) {
            Label empty = new Label("No job applications yet. Apply from the Work tab!");
            empty.setStyle("-fx-text-fill: #888;");
            section.getChildren().add(empty);
            return;
        }

        for (JobApplication app : apps) {
            HBox row = new HBox(16);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.setStyle("-fx-background-color: #f9f9f9; -fx-background-radius: 8; -fx-border-color: #eee; -fx-border-width: 1;");

            VBox info = new VBox(4);
            Label jobId = new Label("Job ID: " + app.getJobId());
            jobId.setStyle("-fx-font-weight: bold;");
            Label dateLabel = new Label("Applied: " + (app.getAppliedAt() != null ? app.getAppliedAt().toLocalDate() : "Unknown"));
            dateLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
            info.getChildren().addAll(jobId, dateLabel);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // TEST 28: Show "Applied ✓" status badge
            Label statusBadge = new Label("Applied ✓");
            statusBadge.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 11px;");

            row.getChildren().addAll(info, spacer, statusBadge);
            section.getChildren().add(row);
        }
    }

    private VBox createHeroMetric(String title, String value, String subtitle) {
        Label val = new Label(value);
        val.getStyleClass().add("hero-stat-value");
        Label tit = new Label(title);
        tit.getStyleClass().add("hero-stat-title");
        Label sub = new Label(subtitle);
        sub.getStyleClass().add("hero-stat-sub");
        
        VBox box = new VBox(2, val, tit, sub);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private VBox createStatItem(String label, String value) {
        Label val = new Label(value);
        val.getStyleClass().add("stat-item-value");
        Label lb = new Label(label);
        lb.getStyleClass().add("stat-item-label");
        VBox box = new VBox(4, val, lb);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(100);
        return box;
    }

    private Region createVerticalSeparator() {
        Region sep = new Region();
        sep.setPrefWidth(1);
        sep.setPrefHeight(32);
        sep.setStyle("-fx-background-color: #eee;");
        return sep;
    }

    private String determineBadge(int modulesCompleted) {
        if (modulesCompleted >= 10) return "Silver Scholar";
        if (modulesCompleted >= 7) return "Bronze Scholar";
        return "Rising Learner";
    }

    private String getTimeOfDay() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 12) return "Morning";
        if (hour < 17) return "Afternoon";
        return "Evening";
    }
}
