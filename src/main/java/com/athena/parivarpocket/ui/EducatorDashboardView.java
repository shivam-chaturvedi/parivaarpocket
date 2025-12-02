package com.athena.parivarpocket.ui;

import com.athena.parivarpocket.model.StudentProgress;
import com.athena.parivarpocket.service.ReportService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class EducatorDashboardView extends VBox {
    private final List<StudentProgress> allStudents;
    private final FilteredList<StudentProgress> filteredStudents;

    public EducatorDashboardView(List<StudentProgress> students, ReportService reportService) {
        this.allStudents = new ArrayList<>(students);
        this.filteredStudents = new FilteredList<>(FXCollections.observableArrayList(allStudents), p -> true);

        setSpacing(20);
        setFillWidth(true);
        getChildren().add(buildMonitoringPanel(reportService));
        getChildren().add(buildPerformancePanel(reportService));
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
