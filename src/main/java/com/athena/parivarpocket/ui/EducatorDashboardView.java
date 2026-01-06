package com.athena.parivarpocket.ui;

import com.athena.parivarpocket.model.JobApplication;
import com.athena.parivarpocket.model.Lesson;
import com.athena.parivarpocket.model.LessonCompletion;
import com.athena.parivarpocket.model.QuizAttempt;
import com.athena.parivarpocket.model.StudentActivity;
import com.athena.parivarpocket.model.StudentProfile;
import com.athena.parivarpocket.model.StudentProgress;
import com.athena.parivarpocket.model.WalletEntry;
import com.athena.parivarpocket.model.UserRole;
import com.athena.parivarpocket.service.DataRepository;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class EducatorDashboardView extends VBox {
    private final DataRepository repository;
    private final ObservableList<StudentProfile> allStudents = FXCollections.observableArrayList();
    private final FilteredList<StudentProfile> filteredStudents = new FilteredList<>(allStudents, p -> true);
    private final TableView<StudentProfile> studentTable = new TableView<>();
    private final Map<String, StudentProgress> progressCache = new HashMap<>();
    private final Label totalStudentsValue = createStatLabel("0");
    private final Label performingWellValue = createStatLabel("0");
    private final Label needsHelpValue = createStatLabel("0");
    private final Label avgCompletionValue = createStatLabel("0%");
    private final PieChart walletHealthPie = new PieChart();
    private final XYChart.Series<String, Number> savingsSeries = new XYChart.Series<>();

    public EducatorDashboardView(DataRepository repository) {
        this.repository = repository;

        setSpacing(24);
        setFillWidth(true);
        setPadding(new Insets(24));
        getStyleClass().add("dashboard-container");

        Label title = new Label("Educator Dashboard");
        title.getStyleClass().add("dashboard-title");
        Label subtitle = new Label("Overview of your progress and activities");
        subtitle.getStyleClass().add("dashboard-subtitle");
        getChildren().addAll(title, subtitle);

        getChildren().addAll(
                buildStudentMonitoringSection(),
                buildAnalyticsSection(),
                buildPerformanceSection()
        );

        refreshMonitoringStats();
        loadStudentProfiles();
    }

    private Node buildStudentMonitoringSection() {
        VBox container = new VBox(16);
        container.getStyleClass().add("section-container");
        container.setPadding(new Insets(16));

        Label sectionTitle = new Label("Educator Dashboard - Student Monitoring");
        sectionTitle.getStyleClass().add("section-header");

        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        TextField search = new TextField();
        search.setPromptText("Search students by name or email...");
        search.getStyleClass().add("modern-search-bar");
        search.setPrefWidth(400);
        search.textProperty().addListener((obs, old, val) -> {
            String lower = val == null ? "" : val.toLowerCase(Locale.ROOT);
            filteredStudents.setPredicate(profile -> {
                if (lower.isBlank()) return true;
                StudentProgress progress = progressForProfile(profile);
                String name = progress != null && progress.getStudentName() != null
                        ? progress.getStudentName()
                        : profile.getEmail();
                return name.toLowerCase(Locale.ROOT).contains(lower)
                        || profile.getEmail().toLowerCase(Locale.ROOT).contains(lower);
            });
        });

        Button searchBtn = new Button("\uD83D\uDD0D");
        searchBtn.getStyleClass().add("icon-button-bw");
        searchBtn.setOnAction(e -> search.requestFocus());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(search, searchBtn, spacer);

        HBox stats = new HBox(16);
        stats.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(stats, Priority.ALWAYS);

        stats.getChildren().addAll(
                createStatCard(totalStudentsValue, "Total Students"),
                createStatCard(performingWellValue, "Performing Well"),
                createStatCard(needsHelpValue, "Need Attention"),
                createStatCard(avgCompletionValue, "Avg Completion")
        );

        container.getChildren().addAll(sectionTitle, topBar, stats);
        return container;
    }

    private Node createStatCard(Label valueLabel, String title) {
        VBox card = new VBox(8);
        card.getStyleClass().add("stat-card-bw");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title-bw");

        card.getChildren().addAll(valueLabel, titleLabel);
        return card;
    }

    private Label createStatLabel(String initialValue) {
        Label label = new Label(initialValue);
        label.getStyleClass().add("stat-value-bw");
        return label;
    }

    private Node buildAnalyticsSection() {
        HBox container = new HBox(20);
        container.setPrefHeight(300);

        VBox chart1 = new VBox(10);
        chart1.getStyleClass().addAll("section-container", "bw-chart");
        chart1.setPadding(new Insets(16));
        HBox.setHgrow(chart1, Priority.ALWAYS);

        Label l1 = new Label("Student Wallet Health Distribution");
        l1.getStyleClass().add("chart-title");

        walletHealthPie.setLabelsVisible(true);
        walletHealthPie.setLegendSide(javafx.geometry.Side.RIGHT);

        chart1.getChildren().addAll(l1, walletHealthPie);

        VBox chart2 = new VBox(10);
        chart2.getStyleClass().addAll("section-container", "bw-chart");
        chart2.setPadding(new Insets(16));
        HBox.setHgrow(chart2, Priority.ALWAYS);

        Label l2 = new Label("Average Savings vs Target");
        l2.getStyleClass().add("chart-title");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> bar = new BarChart<>(xAxis, yAxis);
        bar.setLegendVisible(false);
        bar.setBarGap(10);

        savingsSeries.setName("Savings");
        bar.getData().add(savingsSeries);
        chart2.getChildren().addAll(l2, bar);

        container.getChildren().addAll(chart1, chart2);
        return container;
    }

    private void refreshMonitoringStats() {
        List<StudentProgress> progressList = allStudents.stream()
                .map(this::progressForProfile)
                .filter(Objects::nonNull)
                .toList();
        int total = allStudents.size();
        long performingWell = progressList.stream()
                .filter(progress -> progress.getWalletHealthScore() >= 75)
                .count();
        long needAttention = progressList.stream()
                .filter(progress -> progress.getWalletHealthScore() < 50)
                .count();

        DoubleSummaryStatistics completionStats = progressList.stream()
                .mapToDouble(progress -> progress.getTotalModules() == 0
                        ? 0
                        : (double) progress.getModulesCompleted() / progress.getTotalModules() * 100)
                .summaryStatistics();
        double avgCompletion = completionStats.getCount() == 0 ? 0 : completionStats.getAverage();

        totalStudentsValue.setText(String.valueOf(total));
        performingWellValue.setText(String.valueOf(performingWell));
        needsHelpValue.setText(String.valueOf(needAttention));
        avgCompletionValue.setText(String.format(Locale.ROOT, "%d%%", Math.round(avgCompletion)));

        updateAnalyticsCharts(progressList);
    }

    private void updateAnalyticsCharts(List<StudentProgress> progressList) {
        long excellent = progressList.stream()
                .filter(progress -> progress.getWalletHealthScore() >= 80)
                .count();
        long good = progressList.stream()
                .filter(progress -> progress.getWalletHealthScore() >= 50 && progress.getWalletHealthScore() < 80)
                .count();
        long poor = progressList.stream()
                .filter(progress -> progress.getWalletHealthScore() < 50)
                .count();

        walletHealthPie.getData().setAll(
                new PieChart.Data("Excellent", excellent),
                new PieChart.Data("Good", good),
                new PieChart.Data("Needs Help", poor)
        );

        double avgSavings = progressList.stream()
                .mapToInt(StudentProgress::getWalletSavings)
                .average()
                .orElse(0);

        savingsSeries.getData().setAll(
                new XYChart.Data<>("Class Average", avgSavings),
                new XYChart.Data<>("Target", 2000)
        );
    }

    private void loadStudentProfiles() {
        studentTable.setPlaceholder(new Label("Loading students..."));
        CompletableFuture.runAsync(() -> {
            List<StudentProfile> profiles = Collections.emptyList();
            try {
                List<StudentProfile> fetched = repository.getStudentProfiles();
                if (fetched != null) {
                    profiles = fetched;
                }
            } catch (Exception e) {
                System.err.println("[EducatorDashboardView] Unable to load student profiles: " + e.getMessage());
            }
            List<StudentProfile> studentsOnly = new ArrayList<>();
            for (StudentProfile profile : profiles) {
                if (isStudentProfile(profile)) {
                    studentsOnly.add(profile);
                }
            }
            Platform.runLater(() -> {
                progressCache.clear();
                allStudents.setAll(studentsOnly);
                refreshMonitoringStats();
                if (studentsOnly.isEmpty()) {
                    studentTable.setPlaceholder(new Label("No students found. Try again later."));
                }
            });
        });
    }

    private Node buildPerformanceSection() {
        VBox container = new VBox(16);
        container.getStyleClass().add("section-container");
        container.setPadding(new Insets(16));

        Label sectionTitle = new Label("Student Performance Overview");
        sectionTitle.getStyleClass().add("section-header");

        studentTable.setItems(filteredStudents);
        studentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        studentTable.getStyleClass().add("bw-table");
        studentTable.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        studentTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(StudentProfile item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                    return;
                }
                int index = Math.max(0, getIndex());
                boolean even = index % 2 == 0;
                setStyle(
                        "-fx-background-color: " + (even ? "#ffffff" : "#f7f8ff") + ";" +
                                "-fx-border-color: transparent;" +
                                "-fx-border-width: 0;" +
                                "-fx-padding: 10 0;"
                );
            }
        });

        TableColumn<StudentProfile, StudentProfile> nameCol = new TableColumn<>("Student");
        nameCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(StudentProfile profile, boolean empty) {
                super.updateItem(profile, empty);
                if (empty || profile == null) {
                    setGraphic(null);
                    return;
                }
                StudentProgress progress = progressForProfile(profile);
                VBox box = new VBox(4);
                Label nameLabel = new Label(progress != null && progress.getStudentName() != null
                        ? progress.getStudentName()
                        : profile.getEmail());
                nameLabel.getStyleClass().add("cell-name-bw");
                box.getChildren().add(nameLabel);
                Label roleLabel = new Label(displayRoleLabel(profile));
                roleLabel.getStyleClass().add("cell-subtext-bw");
                box.getChildren().add(roleLabel);
                if (progress != null && progress.getAlerts() > 0) {
                    Label alert = new Label("\u26A0 " + progress.getAlerts() + " alert" + (progress.getAlerts() > 1 ? "s" : ""));
                    alert.getStyleClass().add("cell-subtext-bw");
                    box.getChildren().add(alert);
                }
                setGraphic(box);
            }
        });

        TableColumn<StudentProfile, StudentProfile> lessonsCol = new TableColumn<>("Lessons Progress");
        lessonsCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        lessonsCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(StudentProfile profile, boolean empty) {
                super.updateItem(profile, empty);
                if (empty || profile == null) {
                    setGraphic(null);
                    return;
                }
                StudentProgress progress = progressForProfile(profile);
                int completed = progress != null ? progress.getModulesCompleted() : 0;
                int total = progress != null ? progress.getTotalModules() : 0;
                double ratio = total == 0 ? 0 : (double) completed / total;
                VBox box = new VBox(4);
                box.setAlignment(Pos.CENTER);
                Label progressText = new Label(completed + "/" + total);
                progressText.getStyleClass().add("cell-text-bw");
                ProgressBar pb = new ProgressBar(ratio);
                pb.getStyleClass().add("bw-progress-bar");
                pb.setPrefWidth(120);
                box.getChildren().addAll(progressText, pb);
                setGraphic(box);
            }
        });

        TableColumn<StudentProfile, StudentProfile> quizCol = new TableColumn<>("Quiz Avg");
        quizCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        quizCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(StudentProfile profile, boolean empty) {
                super.updateItem(profile, empty);
                if (empty || profile == null) {
                    setGraphic(null);
                    return;
                }
                StudentProgress progress = progressForProfile(profile);
                String labelText = "—";
                if (progress != null) {
                    double average = progress.getAverageScore();
                    if (average <= 1) {
                        labelText = Math.round(average * 100) + "%";
                    } else {
                        labelText = Math.round(average) + "%";
                    }
                }
                VBox box = new VBox(4);
                box.setAlignment(Pos.CENTER);
                Label avg = new Label(labelText);
                avg.getStyleClass().add("cell-highlight-bw");
                box.getChildren().add(avg);
                setGraphic(box);
            }
        });

        TableColumn<StudentProfile, StudentProfile> walletCol = new TableColumn<>("Wallet Health");
        walletCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        walletCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(StudentProfile profile, boolean empty) {
                super.updateItem(profile, empty);
                if (empty || profile == null) {
                    setGraphic(null);
                    return;
                }
                StudentProgress progress = progressForProfile(profile);
                VBox box = new VBox(4);
                box.setAlignment(Pos.CENTER);
                String status = progress != null ? getHealthLabel(progress.getWalletHealthScore()) : "No data";
                Label badge = new Label(status);
                badge.getStyleClass().addAll("bw-badge", "bw-badge-" + status.toLowerCase().replace(" ", "-"));
                Label savings = new Label(progress != null ? "₹" + progress.getWalletSavings() + " saved" : "Data missing");
                savings.getStyleClass().add("cell-subtext-bw");
                box.getChildren().addAll(badge, savings);
                setGraphic(box);
            }
        });

        TableColumn<StudentProfile, StudentProfile> jobCol = new TableColumn<>("Job Activity");
        jobCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        jobCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(StudentProfile profile, boolean empty) {
                super.updateItem(profile, empty);
                if (empty || profile == null) {
                    setGraphic(null);
                    return;
                }
                StudentProgress progress = progressForProfile(profile);
                int applied = progress != null ? progress.getEmploymentApplications() : 0;
                VBox box = new VBox(4);
                box.setAlignment(Pos.CENTER_LEFT);
                Label appliedLabel = new Label("\u2197 " + applied + " applied");
                appliedLabel.getStyleClass().add("cell-text-bw");
                box.getChildren().add(appliedLabel);
                setGraphic(box);
            }
        });

        TableColumn<StudentProfile, StudentProfile> actionCol = new TableColumn<>("Actions");
        actionCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        actionCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(StudentProfile profile, boolean empty) {
                super.updateItem(profile, empty);
                if (empty || profile == null) {
                    setGraphic(null);
                    return;
                }
                HBox box = new HBox(8);
                box.setAlignment(Pos.CENTER_LEFT);

                Button viewBtn = new Button("View");
                viewBtn.getStyleClass().add("action-button-bw");
                viewBtn.setOnAction(e -> showStudentDetail(profile));
                box.getChildren().add(viewBtn);
                setGraphic(box);
            }
        });

        studentTable.getColumns().addAll(nameCol, lessonsCol, quizCol, walletCol, jobCol, actionCol);
        container.getChildren().addAll(sectionTitle, studentTable);
        return container;
    }

    private String getHealthLabel(double score) {
        if (score >= 80) return "Excellent";
        if (score >= 50) return "Good";
        return "Needs Attention";
    }

    private boolean isStudentProfile(StudentProfile profile) {
        if (profile == null) {
            return false;
        }
        UserRole role = UserRole.fromMetadata(profile.getRole());
        return role == null || role == UserRole.STUDENT;
    }

    private String displayRoleLabel(StudentProfile profile) {
        if (profile == null) {
            return "Student";
        }
        UserRole role = UserRole.fromMetadata(profile.getRole());
        if (role == null) {
            return "Student";
        }
        return role == UserRole.EDUCATOR ? "Educator" : "Student";
    }

    private StudentProgress progressForProfile(StudentProfile profile) {
        if (profile == null || profile.getEmail() == null) {
            return null;
        }
        String key = profile.getEmail().toLowerCase(Locale.ROOT);
        return progressCache.computeIfAbsent(key, repository::getProgressForEmail);
    }

    private StudentProgress progressForEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String key = email.toLowerCase(Locale.ROOT);
        return progressCache.computeIfAbsent(key, repository::getProgressForEmail);
    }

    private String displayStudentName(String email) {
        StudentProgress progress = progressForEmail(email);
        if (progress != null && progress.getStudentName() != null && !progress.getStudentName().isBlank()) {
            return progress.getStudentName();
        }
        return email != null ? email : "Student";
    }

    private void showStudentDetail(StudentProfile profile) {
        StudentProgress progress = progressForProfile(profile);
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Student Analysis: " + (progress != null ? progress.getStudentName() : profile.getEmail()));
        dialog.getDialogPane().getStyleClass().add("bw-dialog");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("bw-tabs");

        Tab overviewTab = new Tab("Overview");
        overviewTab.setContent(buildOverviewTab(profile, progress));
        overviewTab.setClosable(false);

        Tab learningTab = new Tab("Learning");
        learningTab.setContent(buildLearningTab(profile));
        learningTab.setClosable(false);

        Tab jobTab = new Tab("Jobs");
        jobTab.setContent(buildJobHistoryTab(profile));
        jobTab.setClosable(false);

        Tab financeTab = new Tab("Wallet");
        financeTab.setContent(buildFinancialTab(profile));
        financeTab.setClosable(false);

        Tab activityTab = new Tab("Activity");
        activityTab.setContent(buildActivityTab(profile, progress));
        activityTab.setClosable(false);

        tabs.getTabs().addAll(overviewTab, learningTab, jobTab, activityTab, financeTab);

        VBox container = new VBox(tabs);
        container.setPrefSize(700, 500);
        dialog.getDialogPane().setContent(container);
        dialog.showAndWait();
    }

    private Node buildOverviewTab(StudentProfile profile, StudentProgress progress) {
        VBox layout = new VBox(12);
        layout.setPadding(new Insets(16));

        Label emailLabel = new Label(profile.getEmail());
        emailLabel.getStyleClass().add("cell-text-bw");
        Label roleLabel = new Label("Role: " + (profile.getRole() != null ? profile.getRole() : "student"));
        roleLabel.getStyleClass().add("cell-subtext-bw");

        HBox stats = new HBox(18);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
                createOverviewMetric("Modules", progress != null ? (progress.getModulesCompleted() + "/" + progress.getTotalModules()) : "N/A"),
                createOverviewMetric("Wallet Health", progress != null ? String.format("%.0f%%", progress.getWalletHealthScore()) : "N/A"),
                createOverviewMetric("Quizzes", progress != null ? String.valueOf(progress.getQuizzesTaken()) : "0")
        );

        if (profile.getCreatedAt() != null) {
            String created = DateTimeFormatter.ofPattern("dd MMM yyyy").format(profile.getCreatedAt());
            Label createdLabel = new Label("Joined " + created);
            createdLabel.getStyleClass().add("cell-subtext-bw");
            layout.getChildren().addAll(emailLabel, roleLabel, createdLabel, stats);
        } else {
            layout.getChildren().addAll(emailLabel, roleLabel, stats);
        }
        return layout;
    }

    private Node createOverviewMetric(String title, String value) {
        VBox card = new VBox(2);
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("cell-name-bw");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("cell-subtext-bw");
        card.getChildren().addAll(valueLabel, titleLabel);
        return card;
    }

    private Node buildLearningTab(StudentProfile profile) {
        VBox layout = new VBox(12);
        layout.setPadding(new Insets(16));

        Label lessonsHeader = new Label("Completed Lessons");
        lessonsHeader.getStyleClass().add("section-header");
        ListView<HBox> lessonsList = new ListView<>();
        lessonsList.setPlaceholder(new Label("Loading lessons..."));
        lessonsList.setPrefHeight(160);

        Label quizHeader = new Label("Quiz Attempts");
        quizHeader.getStyleClass().add("section-header");
        ListView<HBox> quizList = new ListView<>();
        quizList.setPlaceholder(new Label("Loading quiz attempts..."));
        quizList.setPrefHeight(160);

        CompletableFuture.runAsync(() -> {
            List<LessonCompletion> completions = repository.fetchLessonCompletionsByEmail(profile.getEmail());
            List<QuizAttempt> attempts = repository.fetchQuizAttemptsByEmail(profile.getEmail());
            Platform.runLater(() -> {
                lessonsList.getItems().clear();
                if (completions.isEmpty()) {
                    lessonsList.setPlaceholder(new Label("No lessons completed yet."));
                } else {
                    completions.forEach(completion -> lessonsList.getItems().add(createCompletionRow(completion)));
                }
                quizList.getItems().clear();
                if (attempts.isEmpty()) {
                    quizList.setPlaceholder(new Label("No quiz attempts recorded."));
                } else {
                    attempts.forEach(attempt -> quizList.getItems().add(createQuizRow(attempt)));
                }
            });
        });

        layout.getChildren().addAll(lessonsHeader, lessonsList, quizHeader, quizList);
        return layout;
    }

    private Node buildJobHistoryTab(StudentProfile profile) {
        VBox list = new VBox(12);
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("transparent-scroll");
        Label loading = new Label("Loading applications...");
        loading.getStyleClass().add("placeholder-text");
        list.getChildren().add(loading);

        CompletableFuture.runAsync(() -> {
            if (profile.getEmail() == null || profile.getEmail().isBlank()) {
                Platform.runLater(() -> {
                    list.getChildren().clear();
                    list.getChildren().add(new Label("No email linked. Cannot fetch job history."));
                });
                return;
            }
            List<JobApplication> apps = repository.fetchJobApplications(profile.getEmail());
            Platform.runLater(() -> {
                list.getChildren().clear();
                if (apps.isEmpty()) {
                    list.getChildren().add(new Label("No job applications found."));
                } else {
                    apps.forEach(app -> list.getChildren().add(
                            createJobItem("Job ID: " + app.getJobId(), app.getStatus(), app.getAppliedAt().toLocalDate().toString())));
                }
            });
        });

        VBox layout = new VBox(16, new Label("Job Application History"), scroll);
        layout.setPadding(new Insets(16));
        return layout;
    }

    private Node buildActivityTab(StudentProfile profile, StudentProgress progress) {
        VBox layout = new VBox(12);
        layout.setPadding(new Insets(16));

        Label header = new Label("Activity Feed");
        header.getStyleClass().add("section-header");

        int modulesCompleted = progress != null ? progress.getModulesCompleted() : 0;
        int totalModules = progress != null ? progress.getTotalModules() : 0;
        int jobsApplied = progress != null ? progress.getEmploymentApplications() : 0;
        String moduleText = totalModules == 0 ? modulesCompleted + "/" + totalModules : modulesCompleted + "/" + totalModules;
        String jobText = String.valueOf(jobsApplied);
        String walletText = progress != null ? "₹" + progress.getWalletSavings() : "₹0";

        HBox metricRow = new HBox(18);
        metricRow.setAlignment(Pos.CENTER_LEFT);
        metricRow.getChildren().addAll(
                createOverviewMetric("Courses Completed", moduleText),
                createOverviewMetric("Jobs Applied", jobText),
                createOverviewMetric("Wallet Savings", walletText)
        );

        ListView<HBox> activityList = new ListView<>();
        activityList.setPlaceholder(new Label("Loading activity stream..."));
        activityList.setPrefHeight(260);

        CompletableFuture.runAsync(() -> {
            List<StudentActivity> activities = repository.fetchStudentActivities(profile.getEmail());
            Platform.runLater(() -> {
                activityList.getItems().clear();
                if (activities.isEmpty()) {
                    activityList.setPlaceholder(new Label("No recorded activity yet."));
                } else {
                    activities.forEach(activity -> activityList.getItems().add(createActivityRow(activity)));
                }
            });
        });

        layout.getChildren().addAll(header, metricRow, activityList);
        return layout;
    }

    private HBox createActivityRow(StudentActivity activity) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8));

        Label typeLabel = new Label(formatActivityType(activity.getActivityType()));
        typeLabel.getStyleClass().add("cell-name-bw");

        Label summary = new Label(activity.getSummary());
        summary.getStyleClass().add("cell-subtext-bw");

        VBox detailBox = new VBox(2);
        detailBox.getChildren().add(new Label(activity.getActivityType()));
        if (!activity.getActivityData().isEmpty()) {
            activity.getActivityData().forEach((key, value) -> {
                Label detail = new Label(key + ": " + value);
                detail.getStyleClass().add("cell-subtext-bw");
                detailBox.getChildren().add(detail);
            });
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String timestamp = activity.getCreatedAt() != null
                ? activity.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
                : "";
        Label timeLabel = new Label(timestamp);
        timeLabel.getStyleClass().add("cell-subtext-bw");

        row.getChildren().addAll(typeLabel, detailBox, spacer, timeLabel);
        return row;
    }

    private String formatActivityType(String type) {
        if (type == null || type.isBlank()) {
            return "Activity";
        }
        StringBuilder builder = new StringBuilder();
        for (String token : type.split("[_\\s]+")) {
            if (token.isBlank()) {
                continue;
            }
            builder.append(token.substring(0, 1).toUpperCase(Locale.ROOT));
            if (token.length() > 1) {
                builder.append(token.substring(1).toLowerCase(Locale.ROOT));
            }
            builder.append(" ");
        }
        return builder.toString().trim();
    }

    private Node buildFinancialTab(StudentProfile profile) {
        VBox layout = new VBox(16);
        layout.setPadding(new Insets(16));

        if (profile.getEmail() == null || profile.getEmail().isBlank()) {
            layout.getChildren().add(new Label("No email linked to this student. Cannot fetch wallet data."));
            return layout;
        }

        ListView<HBox> txList = new ListView<>();
        Label placeholder = new Label("Loading transactions...");
        placeholder.getStyleClass().add("placeholder-text");
        txList.setPlaceholder(placeholder);
        txList.setPrefHeight(300);

        CompletableFuture.runAsync(() -> {
            List<WalletEntry> walletEntries = repository.fetchWalletByEmail(profile.getEmail());
            Platform.runLater(() -> {
                if (walletEntries.isEmpty()) {
                    placeholder.setText("No wallet transactions found.");
                } else {
                    walletEntries.forEach(entry -> txList.getItems().add(createTransactionRow(entry)));
                }
            });
        });

        layout.getChildren().addAll(new Label("Recent Wallet Transactions"), txList);
        return layout;
    }

    private HBox createTransactionRow(WalletEntry entry) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8));

        VBox info = new VBox(2);
        String descText = entry.getNote() != null && !entry.getNote().isEmpty()
                ? entry.getNote()
                : entry.getCategory();
        Label desc = new Label(descText);
        desc.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");
        Label date = new Label(entry.getDate().toString());
        date.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        info.getChildren().addAll(desc, date);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean isIncome = entry.getType() == com.athena.parivarpocket.model.WalletEntryType.INCOME;
        Label amt = new Label((isIncome ? "+" : "-") + " ₹" + entry.getAmount());
        amt.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (isIncome ? "black" : "#666") + ";");

        row.getChildren().addAll(info, spacer, amt);
        return row;
    }

    private HBox createCompletionRow(LessonCompletion completion) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6));
        Label title = new Label(lessonTitleFor(completion.getLessonId()));
        title.setStyle("-fx-font-weight: bold;");
        Label date = new Label("Completed: " + completion.getCompletedAt().toLocalDate());
        date.setStyle("-fx-text-fill: #666;");
        row.getChildren().addAll(title, date);
        return row;
    }

    private String lessonTitleFor(String lessonId) {
        if (lessonId == null || lessonId.isBlank()) {
            return "Lesson";
        }
        for (Lesson lesson : repository.getLessons()) {
            if (lessonId.equals(lesson.getId())) {
                return lesson.getTitle();
            }
        }
        return lessonId;
    }

    private HBox createQuizRow(QuizAttempt attempt) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6));
        Label label = new Label("Quiz ID: " + attempt.getQuizId());
        label.setStyle("-fx-font-weight: bold;");
        double percent = attempt.getMaxScore() == 0
                ? 0
                : (double) attempt.getScore() / attempt.getMaxScore() * 100;
        Label score = new Label(attempt.getScore() + " / " + attempt.getMaxScore() + " (" + Math.round(percent) + "%)");
        Label status = new Label(attempt.isPassed() ? "Passed" : "Failed");
        status.setStyle("-fx-text-fill: " + (attempt.isPassed() ? "#0b7d5d" : "#c2410c") + "; -fx-font-weight: bold;");
        Label date = new Label(attempt.getCreatedAt().toLocalDate().toString());
        date.setStyle("-fx-text-fill: #666;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(label, score, spacer, status, date);
        return row;
    }

    private HBox createJobItem(String title, String status, String date) {
        HBox card = new HBox(12);
        card.setStyle("-fx-border-color: #ccc; -fx-border-width: 0 0 1 0; -fx-padding: 8;");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(4);
        Label t = new Label(title);
        t.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");
        Label d = new Label(date);
        d.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        info.getChildren().addAll(t, d);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label s = new Label(status);
        s.setStyle("-fx-border-color: black; -fx-border-radius: 4; -fx-padding: 2 6; -fx-font-size: 10px;");

        card.getChildren().addAll(info, spacer, s);
        return card;
    }
}
