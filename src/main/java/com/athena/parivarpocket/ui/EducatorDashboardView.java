package com.athena.parivarpocket.ui;

import com.athena.parivarpocket.model.StudentProgress;
import com.athena.parivarpocket.service.DataRepository;
import com.athena.parivarpocket.service.ReportService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class EducatorDashboardView extends VBox {
    private final DataRepository repository;
    private final List<StudentProgress> allStudents;
    private final FilteredList<StudentProgress> filteredStudents;

    public EducatorDashboardView(DataRepository repository, ReportService reportService) {
        this.repository = repository;
        List<StudentProgress> students = repository.getStudentsProgress();
        this.allStudents = new ArrayList<>(students);
        this.filteredStudents = new FilteredList<>(FXCollections.observableArrayList(allStudents), p -> true);

        setSpacing(20);
        setFillWidth(true);
        getChildren().add(buildMonitoringPanel(reportService));
        getChildren().add(buildPerformancePanel(reportService));
        getChildren().add(buildCourseAuthoringPanel());
    }

    private Panel buildMonitoringPanel(ReportService reportService) {
        TextField searchField = new TextField();
        searchField.setPromptText("Search students by name...");
        searchField.getStyleClass().add("monitoring-search");
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String lower = newValue == null ? "" : newValue.toLowerCase();
            filteredStudents.setPredicate(progress -> progress.getStudentName().toLowerCase().contains(lower));
        });

        Button searchButton = new Button("\uD83D\uDD0D");
        searchButton.getStyleClass().add("icon-button");
        searchButton.setOnAction(e -> searchField.requestFocus());

        Button exportAll = new Button("Export All Reports");
        exportAll.getStyleClass().add("monitoring-export-button");
        exportAll.setOnAction(e -> exportAllReports(reportService));

        HBox searchRow = new HBox(8, searchField, searchButton, exportAll);
        searchRow.getStyleClass().add("monitoring-search-row");
        searchRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        HBox metricsRow = new HBox(12,
                createMetricCard(String.valueOf(allStudents.size()), "Total Students"),
                createMetricCard(String.valueOf(performingWell()), "Performing Well"),
                createMetricCard(String.valueOf(allStudents.size() - performingWell()), "Need Attention"),
                createMetricCard(averageCompletion() + "%", "Avg Completion")
        );
        metricsRow.getStyleClass().add("monitoring-metrics");
        metricsRow.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(12, searchRow, metricsRow);
        content.setPadding(new Insets(0, 2, 10, 2));
        Panel panel = new Panel("Educator Dashboard - Student Monitoring", content);
        panel.getStyleClass().add("monitoring-panel");
        return panel;
    }

    private Panel buildPerformancePanel(ReportService reportService) {
        TableView<StudentProgress> table = new TableView<>(filteredStudents);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No students match that search."));
        table.getStyleClass().add("educator-table");
        table.setPrefHeight(320);

        table.getColumns().add(buildNameColumn());
        table.getColumns().add(buildLessonColumn());
        table.getColumns().add(buildQuizColumn());
        table.getColumns().add(buildWalletColumn());
        table.getColumns().add(buildJobColumn());
        table.getColumns().add(buildActionsColumn(reportService));

        Panel panel = new Panel("Student Performance Overview", table);
        panel.getStyleClass().add("performance-panel");
        return panel;
    }

    private TableColumn<StudentProgress, StudentProgress> buildNameColumn() {
        TableColumn<StudentProgress, StudentProgress> column = new TableColumn<>("Name");
        column.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(StudentProgress progress, boolean empty) {
                super.updateItem(progress, empty);
                if (empty || progress == null) {
                    setGraphic(null);
                    return;
                }
                Label name = new Label(progress.getStudentName());
                name.getStyleClass().add("student-name");

                VBox wrapper = new VBox(3, name);
                if (progress.getAlerts() > 0) {
                    Label alert = new Label("\u24D8 " + progress.getAlerts() + " alert" + (progress.getAlerts() > 1 ? "s" : ""));
                    alert.getStyleClass().add("alert-tag");
                    wrapper.getChildren().add(alert);
                }
                setGraphic(wrapper);
            }
        });
        return column;
    }

    private TableColumn<StudentProgress, StudentProgress> buildLessonColumn() {
        TableColumn<StudentProgress, StudentProgress> column = new TableColumn<>("Lessons Progress");
        column.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        column.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar();
            private final Label ratio = new Label();
            private final VBox layout = new VBox(6, ratio, bar);

            {
                bar.setPrefWidth(180);
                bar.getStyleClass().add("progress-bar");
            }

            @Override
            protected void updateItem(StudentProgress progress, boolean empty) {
                super.updateItem(progress, empty);
                if (empty || progress == null) {
                    setGraphic(null);
                    return;
                }
                double ratioValue = (double) progress.getModulesCompleted() / progress.getTotalModules();
                bar.setProgress(ratioValue);
                ratio.setText(progress.getModulesCompleted() + "/" + progress.getTotalModules());
                ratio.getStyleClass().add("lesson-ratio");
                setGraphic(layout);
            }
        });
        return column;
    }

    private TableColumn<StudentProgress, StudentProgress> buildQuizColumn() {
        TableColumn<StudentProgress, StudentProgress> column = new TableColumn<>("Quiz Avg");
        column.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        column.setCellFactory(col -> new TableCell<>() {
            private final Label value = new Label();
            private final Label helper = new Label();
            private final VBox layout = new VBox(4, value, helper);

            {
                value.getStyleClass().add("quiz-average");
                helper.getStyleClass().add("quiz-helper");
            }

            @Override
            protected void updateItem(StudentProgress progress, boolean empty) {
                super.updateItem(progress, empty);
                if (empty || progress == null) {
                    setGraphic(null);
                    return;
                }
                value.setText(Math.round(progress.getAverageScore()) + "%");
                helper.setText(progress.getQuizzesTaken() + " quizzes");
                setGraphic(layout);
            }
        });
        return column;
    }

    private TableColumn<StudentProgress, StudentProgress> buildWalletColumn() {
        TableColumn<StudentProgress, StudentProgress> column = new TableColumn<>("Wallet Health");
        column.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        column.setCellFactory(col -> new TableCell<>() {
            private final Label status = new Label();
            private final Label savings = new Label();
            private final VBox layout = new VBox(4, status, savings);

            {
                layout.setPrefWidth(120);
            }

            @Override
            protected void updateItem(StudentProgress progress, boolean empty) {
                super.updateItem(progress, empty);
                if (empty || progress == null) {
                    setGraphic(null);
                    return;
                }
                String flag = walletStatus(progress.getWalletHealthScore());
                status.setText(flag);
                status.getStyleClass().setAll("status-pill", flag.toLowerCase().replace(" ", "-"));
                savings.setText("₹" + progress.getWalletSavings() + " saved");
                savings.getStyleClass().add("wallet-savings");
                setGraphic(layout);
            }
        });
        return column;
    }

    private TableColumn<StudentProgress, StudentProgress> buildJobColumn() {
        TableColumn<StudentProgress, StudentProgress> column = new TableColumn<>("Job Activity");
        column.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        column.setCellFactory(col -> new TableCell<>() {
            private final Label applied = new Label();
            private final Label saved = new Label();
            private final VBox layout = new VBox(3, applied, saved);

            {
                layout.setPrefWidth(140);
            }

            @Override
            protected void updateItem(StudentProgress progress, boolean empty) {
                super.updateItem(progress, empty);
                if (empty || progress == null) {
                    setGraphic(null);
                    return;
                }
                applied.setText("\u2197 " + progress.getEmploymentApplications() + " applied");
                saved.setText("\u21BA " + progress.getJobSaves() + " saved");
                applied.getStyleClass().add("job-meta");
                saved.getStyleClass().add("job-meta");
                setGraphic(layout);
            }
        });
        return column;
    }

    private TableColumn<StudentProgress, StudentProgress> buildActionsColumn(ReportService reportService) {
        TableColumn<StudentProgress, StudentProgress> column = new TableColumn<>("Actions");
        column.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        column.setCellFactory(col -> new TableCell<>() {
            private final Button button = new Button("View Details");

            {
                button.getStyleClass().add("detail-button");
                button.setOnAction(event -> {
                    StudentProgress progress = getItem();
                    if (progress != null) {
                        showAlert(Alert.AlertType.INFORMATION, "Opening report for " + progress.getStudentName());
                    }
                });
            }

            @Override
            protected void updateItem(StudentProgress progress, boolean empty) {
                super.updateItem(progress, empty);
                setGraphic(empty || progress == null ? null : button);
            }
        });
        return column;
    }

    private void exportAllReports(ReportService reportService) {
        if (allStudents.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No student data available.");
            return;
        }
        for (StudentProgress progress : allStudents) {
            reportService.exportStudentReport(progress);
        }
        showAlert(Alert.AlertType.INFORMATION, "Exported " + allStudents.size() + " reports.");
    }

    private Panel buildCourseAuthoringPanel() {
        TextField lessonTitle = new TextField();
        lessonTitle.setPromptText("Lesson title");
        TextArea description = new TextArea();
        description.setPromptText("Lesson description");
        description.setWrapText(true);
        description.setPrefRowCount(3);

        ChoiceBox<String> difficulty = new ChoiceBox<>(FXCollections.observableArrayList("Beginner", "Intermediate", "Advanced"));
        difficulty.getSelectionModel().selectFirst();
        TextField courseUrl = new TextField();
        courseUrl.setPromptText("Course URL");

        TextField quizTitle = new TextField();
        quizTitle.setPromptText("Quiz title");
        Spinner<Integer> passingSpinner = new Spinner<>(0, 100, 3);
        passingSpinner.setEditable(true);
        Spinner<Integer> totalSpinner = new Spinner<>(1, 100, 5);
        totalSpinner.setEditable(true);

        TextField questionField = new TextField();
        questionField.setPromptText("Question text");
        TextArea optionsArea = new TextArea();
        optionsArea.setPromptText("Options (one per line)");
        optionsArea.setPrefRowCount(4);
        TextField correctIndex = new TextField();
        correctIndex.setPromptText("Correct option index (0-based)");
        Spinner<Integer> pointsSpinner = new Spinner<>(1, 10, 1);
        pointsSpinner.setEditable(true);

        Button createButton = new Button("Create lesson + quiz");
        createButton.getStyleClass().add("primary-button");
        createButton.setOnAction(e -> {
            String title = lessonTitle.getText().trim();
            String diff = difficulty.getValue();
            String desc = description.getText().trim();
            String url = courseUrl.getText().trim();
            String qTitle = quizTitle.getText().trim();
            String question = questionField.getText().trim();
            List<String> options = parseOptions(optionsArea);
            int passing = Math.max(0, passingSpinner.getValue());
            int total = Math.max(1, totalSpinner.getValue());
            int points = Math.max(1, pointsSpinner.getValue());
            int correct;
            try {
                correct = Integer.parseInt(correctIndex.getText().trim());
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, "Correct option must be a number.");
                return;
            }
            if (options.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Add at least one option.");
                return;
            }
            if (correct < 0 || correct >= options.size()) {
                showAlert(Alert.AlertType.WARNING, "Correct option index must match one of the provided options.");
                return;
            }
            boolean success = repository.createLessonWithQuiz(title, diff, desc, url, qTitle, passing, total, question, options, correct, points);
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Lesson and quiz created successfully.");
                lessonTitle.clear();
                description.clear();
                courseUrl.clear();
                quizTitle.clear();
                questionField.clear();
                optionsArea.clear();
                correctIndex.clear();
            } else {
                showAlert(Alert.AlertType.ERROR, "Unable to create lesson. Check you are signed in.");
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.add(new Label("Lesson title"), 0, 0);
        grid.add(lessonTitle, 1, 0);
        grid.add(new Label("Difficulty"), 0, 1);
        grid.add(difficulty, 1, 1);
        grid.add(new Label("Description"), 0, 2);
        grid.add(description, 1, 2);
        grid.add(new Label("Course URL"), 0, 3);
        grid.add(courseUrl, 1, 3);
        grid.add(new Label("Quiz title"), 0, 4);
        grid.add(quizTitle, 1, 4);
        grid.add(new Label("Passing marks"), 0, 5);
        grid.add(passingSpinner, 1, 5);
        grid.add(new Label("Total marks"), 0, 6);
        grid.add(totalSpinner, 1, 6);
        grid.add(new Label("Question"), 0, 7);
        grid.add(questionField, 1, 7);
        grid.add(new Label("Options"), 0, 8);
        grid.add(optionsArea, 1, 8);
        grid.add(new Label("Correct option index"), 0, 9);
        grid.add(correctIndex, 1, 9);
        grid.add(new Label("Points (per question)"), 0, 10);
        grid.add(pointsSpinner, 1, 10);

        GridPane.setHgrow(lessonTitle, Priority.ALWAYS);
        GridPane.setHgrow(description, Priority.ALWAYS);
        GridPane.setHgrow(courseUrl, Priority.ALWAYS);
        GridPane.setHgrow(quizTitle, Priority.ALWAYS);
        GridPane.setHgrow(questionField, Priority.ALWAYS);
        GridPane.setHgrow(optionsArea, Priority.ALWAYS);
        GridPane.setHgrow(correctIndex, Priority.ALWAYS);

        VBox wrapper = new VBox(10, grid, createButton);
        wrapper.setPadding(new Insets(12));
        Panel panel = new Panel("Add or update learning content", wrapper);
        panel.getStyleClass().add("course-authoring-panel");
        return panel;
    }

    private List<String> parseOptions(TextArea area) {
        if (area == null || area.getText().isBlank()) {
            return List.of();
        }
        List<String> options = new ArrayList<>();
        for (String line : area.getText().split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                options.add(trimmed);
            }
        }
        return options;
    }

    private VBox createMetricCard(String value, String label) {
        Label metricValue = new Label(value);
        metricValue.getStyleClass().add("metric-value");
        Label metricLabel = new Label(label);
        metricLabel.getStyleClass().add("metric-label");
        VBox card = new VBox(4, metricValue, metricLabel);
        card.getStyleClass().add("metric-card");
        return card;
    }

    private long performingWell() {
        return allStudents.stream()
                .filter(progress -> progress.getAverageScore() >= 85)
                .count();
    }

    private int averageCompletion() {
        return (int) Math.round(allStudents.stream()
                .mapToDouble(progress -> (double) progress.getModulesCompleted() / progress.getTotalModules())
                .average()
                .orElse(0) * 100);
    }

    private String walletStatus(double score) {
        if (score >= 90) {
            return "Excellent";
        }
        if (score >= 75) {
            return "Good";
        }
        return "Needs Attention";
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.showAndWait();
    }
}
