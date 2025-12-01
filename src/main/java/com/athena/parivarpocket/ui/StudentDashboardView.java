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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class StudentDashboardView extends VBox {
    public StudentDashboardView(User user, DataRepository repository) {
        setSpacing(20);
        setPadding(new Insets(16));

        List<Lesson> lessons = repository.getLessons();
        int modulesCompleted = (int) lessons.stream().filter(l -> l.getProgressPercent() >= 70).count();
        List<QuizResult> quizResults = repository.getQuizResults();
        int quizzesTaken = quizResults.size();
        int averageScore = (int) Math.round(quizResults.stream().mapToInt(QuizResult::getScore).average().orElse(0));
        int bestScore = quizResults.stream().mapToInt(QuizResult::getScore).max().orElse(0);
        int coins = quizResults.stream().mapToInt(QuizResult::getCoinsAwarded).sum();
        String badgeLabel = determineBadge(modulesCompleted);

        getChildren().add(buildWelcomePanel(user, modulesCompleted, lessons.size(), coins, badgeLabel));
        getChildren().add(buildLearningProgress(modulesCompleted, lessons.size(), quizzesTaken, averageScore, bestScore));
        getChildren().add(buildWalletSummary(repository, user));
        getChildren().add(buildBookmarkedJobs(repository.getJobOpportunities().subList(0, 3)));
    }

    private Panel buildWelcomePanel(User user,
                                    int modulesCompleted,
                                    int moduleTotal,
                                    int coins,
                                    String badgeLabel) {
        Label title = new Label("Welcome, " + user.getName());
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800;");
        Label subtitle = new Label("Keep up the great work!");
        subtitle.setStyle("-fx-text-fill: #3a3a3a; -fx-font-size: 13px;");

        VBox left = new VBox(6, title, subtitle);

        VBox coinsBox = createHeroMetric("ParivaarCoins", String.valueOf(coins), "Coin balance");
        VBox badgeBox = createHeroMetric("Current Badge", badgeLabel, "Level unlocked");
        HBox metrics = new HBox(14, coinsBox, badgeBox);
        metrics.setAlignment(Pos.CENTER_RIGHT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox content = new HBox(16, left, spacer, metrics);
        content.setAlignment(Pos.CENTER_LEFT);
        Panel panel = new Panel("Student Dashboard", content);
        panel.getStyleClass().add("hero-panel");
        return panel;
    }

    private Panel buildLearningProgress(int modulesCompleted,
                                        int moduleTotal,
                                        int quizzesTaken,
                                        int averageScore,
                                        int bestScore) {
        Label label = new Label("Modules Completed");
        label.setStyle("-fx-font-weight: 600; -fx-text-fill: #2a2a2a;");
        Label ratio = new Label(modulesCompleted + " / " + moduleTotal);
        ratio.setStyle("-fx-font-weight: 700; -fx-font-size: 14px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(8, label, spacer, ratio);
        header.setAlignment(Pos.CENTER_LEFT);

        ProgressBar progressBar = new ProgressBar((double) modulesCompleted / moduleTotal);
        progressBar.setPrefHeight(12);
        progressBar.setPrefWidth(520);
        progressBar.setStyle("-fx-accent: #000000;");

        HBox stats = new HBox(12,
                createDashboardStat("Quizzes Taken", String.valueOf(quizzesTaken), null),
                createDashboardStat("Average Score", averageScore + "%", null),
                createDashboardStat("Best Score", bestScore + "%", null)
        );
        stats.setAlignment(Pos.CENTER);

        VBox content = new VBox(12, header, progressBar, stats);
        return new Panel("Learning Progress", content);
    }

    private Panel buildWalletSummary(DataRepository repository, User user) {
        List<WalletEntry> entries = repository.loadWallet(user);
        double income = repository.calculateIncome(entries);
        double expenses = repository.calculateExpenses(entries);
        double goal = 5000;
        double savings = entries.stream()
                .filter(e -> e.getType() == WalletEntryType.SAVINGS)
                .mapToDouble(WalletEntry::getAmount)
                .sum();
        double progress = Math.min(1, savings / goal);

        Label progressTitle = new Label("Savings Goal Progress");
        progressTitle.setStyle("-fx-font-weight: 600; -fx-text-fill: #2a2a2a;");
        Label progressValue = new Label("₹" + Math.round(savings) + " / ₹" + Math.round(goal));
        progressValue.setStyle("-fx-font-weight: 700;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox progressHeader = new HBox(8, progressTitle, spacer, progressValue);
        progressHeader.setAlignment(Pos.CENTER_LEFT);

        ProgressBar savingsBar = new ProgressBar(progress);
        savingsBar.setPrefHeight(12);
        savingsBar.setStyle("-fx-accent: #000000;");

        HBox stats = new HBox(12,
                createDashboardStat("Monthly Income", "₹" + Math.round(income), "Income recorded"),
                createDashboardStat("Monthly Expenses", "₹" + Math.round(expenses), "Expenses recorded")
        );
        stats.setAlignment(Pos.CENTER);

        VBox content = new VBox(14, progressHeader, savingsBar, stats);
        Panel panel = new Panel("Wallet Summary", content);
        panel.getStyleClass().add("wallet-panel");
        return panel;
    }

    private Panel buildBookmarkedJobs(List<JobOpportunity> jobs) {
        VBox box = new VBox(12);
        jobs.forEach(job -> {
            Label title = new Label(job.getTitle());
            title.setStyle("-fx-font-weight: 700; -fx-font-size: 15px;");
            Label meta = new Label(job.getLocation() + " • " + job.getCategory());
            meta.getStyleClass().add("bookmark-details-meta");
            Label hours = new Label(job.getHours());
            hours.getStyleClass().add("bookmark-details-meta");

            VBox details = new VBox(2, title, meta, hours);
            details.setAlignment(Pos.CENTER_LEFT);

            Button applyBtn = new Button("Apply");
            applyBtn.getStyleClass().add("primary-button");
            Button removeBtn = new Button("Remove");
            removeBtn.getStyleClass().add("outline-button");
            HBox actions = new HBox(8, applyBtn, removeBtn);
            actions.setAlignment(Pos.CENTER_RIGHT);

            HBox row = new HBox(24, details, actions);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(details, Priority.ALWAYS);
            row.getStyleClass().add("bookmark-card");
            box.getChildren().add(row);
        });
        return new Panel("Bookmarked Opportunities", box);
    }

    private VBox createHeroMetric(String title, String value, String subtitle) {
        Label label = new Label(title);
        label.getStyleClass().add("hero-metric-label");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("hero-metric-value");
        Label sub = new Label(subtitle);
        sub.getStyleClass().add("hero-metric-subtitle");
        VBox box = new VBox(4, label, valueLabel, sub);
        box.getStyleClass().add("hero-metric-card");
        return box;
    }

    private VBox createDashboardStat(String title, String value, String helper) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dashboard-stat-title");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("dashboard-stat-value");
        VBox box = new VBox(3, titleLabel, valueLabel);
        if (helper != null) {
            Label helperLabel = new Label(helper);
            helperLabel.getStyleClass().add("dashboard-stat-helper");
            box.getChildren().add(helperLabel);
        }
        box.getStyleClass().add("dashboard-stat-card");
        return box;
    }

    private String determineBadge(int modulesCompleted) {
        if (modulesCompleted >= 10) {
            return "Silver Scholar";
        }
        if (modulesCompleted >= 7) {
            return "Bronze Scholar";
        }
        return "Rising Learner";
    }
}
