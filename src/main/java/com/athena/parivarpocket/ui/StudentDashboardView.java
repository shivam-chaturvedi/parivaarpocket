package com.athena.parivarpocket.ui;

import com.athena.parivarpocket.model.JobOpportunity;
import com.athena.parivarpocket.model.Lesson;
import com.athena.parivarpocket.model.QuizResult;
import com.athena.parivarpocket.model.User;
import com.athena.parivarpocket.model.WalletEntry;
import com.athena.parivarpocket.model.WalletEntryType;
import com.athena.parivarpocket.service.DataRepository;
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
import java.util.List;

public class StudentDashboardView extends VBox {
    public StudentDashboardView(User user, DataRepository repository) {
        setSpacing(24);
        setPadding(new Insets(24));
        getStyleClass().add("dashboard-container");

        List<Lesson> lessons = repository.getLessons();
        long completedCount = lessons.stream().filter(l -> repository.isLessonCompleted(l.getId())).count();
        int modulesCompleted = (int) completedCount;
        
        int coins = modulesCompleted * 100;
        
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

        HBox stats = new HBox(0,
                createStatItem("Max Score", maxScore + "%"),
                createVerticalSeparator(),
                createStatItem("Min Score", minScore + "%"),
                createVerticalSeparator(),
                createStatItem("Median", medianScore + "%")
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
        double currentBalance = income - expenses;
        
        Label header = new Label("Wallet Overview");
        header.getStyleClass().add("panel-header");

        Label balanceLabel = new Label("₹" + Math.round(currentBalance));
        balanceLabel.getStyleClass().add("balance-value");
        Label balanceTitle = new Label("Current Balance");
        balanceTitle.getStyleClass().add("balance-title");
        
        VBox balanceBox = new VBox(4, balanceTitle, balanceLabel);
        balanceBox.setAlignment(Pos.CENTER);
        balanceBox.setPadding(new Insets(0, 0, 12, 0));

        HBox stats = new HBox(0,
                createStatItem("Income", "₹" + Math.round(income)),
                createVerticalSeparator(),
                createStatItem("Expenses", "₹" + Math.round(expenses))
        );
        stats.setAlignment(Pos.CENTER);
        
        VBox content = new VBox(12, header, balanceBox, stats);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("dashboard-card");
        return content;
    }

    private Panel buildBookmarkedJobs(List<JobOpportunity> jobs) {
        if (jobs.isEmpty()) {
            VBox emptyBox = new VBox(new Label("No recent opportunities. Check the Work tab!"));
            emptyBox.setPadding(new Insets(24));
            emptyBox.getStyleClass().add("dashboard-card");
            return new Panel("Recent Opportunities", emptyBox);
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
             
             Button apply = new Button("View");
             apply.getStyleClass().add("small-button");
             apply.setOnAction(e -> {
                 if (job.getJobLink() != null && !job.getJobLink().isBlank()) {
                     try {
                         Desktop.getDesktop().browse(new URI(job.getJobLink()));
                     } catch (IOException | URISyntaxException ex) {
                         ex.printStackTrace();
                     }
                 }
             });
             
             row.getChildren().addAll(info, spacer, apply);
             list.getChildren().add(row);
        });
        
        VBox content = new VBox(16, list);
        content.setPadding(new Insets(20));
        Panel panel = new Panel("Bookmarked Opportunities", content);
        panel.getStyleClass().add("dashboard-card");
        // Remove default panel header style if wrapper handles it, but Panel class adds it.
        // We might want to customize, but standard Panel is fine for now.
        return panel;
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
