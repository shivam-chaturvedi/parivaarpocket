package com.athena.parivarpocket.ui;

import com.athena.parivarpocket.model.StudentProgress;
import com.athena.parivarpocket.service.ReportService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.List;

public class EducatorDashboardView extends VBox {
    public EducatorDashboardView(List<StudentProgress> students, ReportService reportService) {
        setSpacing(12);
        getChildren().add(buildSummary(students));
        getChildren().add(buildTable(students, reportService));
    }

    private Panel buildSummary(List<StudentProgress> students) {
        int totalQuizzes = students.stream().mapToInt(StudentProgress::getQuizzesTaken).sum();
        double avgScore = students.stream().mapToDouble(StudentProgress::getAverageScore).average().orElse(0);
        int totalModules = students.stream().mapToInt(StudentProgress::getModulesCompleted).sum();

        Label quizzes = metric("Quizzes", String.valueOf(totalQuizzes));
        Label average = metric("Average score", Math.round(avgScore) + "%");
        Label modules = metric("Modules done", String.valueOf(totalModules));

        HBox row = new HBox(16, quizzes, average, modules);
        row.setPadding(new Insets(10));
        return new Panel("Cohort overview", row);
    }

    private Panel buildTable(List<StudentProgress> students, ReportService reportService) {
        TableView<StudentProgress> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(students));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        table.getColumns().add(column("Student", StudentProgress::getStudentName));
        table.getColumns().add(column("Modules", sp -> sp.getModulesCompleted() + "/" + sp.getTotalModules()));
        table.getColumns().add(column("Quizzes", sp -> String.valueOf(sp.getQuizzesTaken())));
        table.getColumns().add(column("Avg score", sp -> String.format("%.1f%%", sp.getAverageScore())));
        table.getColumns().add(column("Wallet health", sp -> String.format("%.0f%%", sp.getWalletHealthScore())));
        table.getColumns().add(column("ParivaarPoints", sp -> String.valueOf(sp.getParivaarPoints())));
        table.getColumns().add(column("Employment", sp -> sp.getEmploymentApplications() + " apps"));

        Button exportBtn = new Button("Export selected report");
        exportBtn.getStyleClass().add("primary-button");
        exportBtn.setOnAction(e -> {
            StudentProgress selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "Select a student first.");
                return;
            }
            Path report = reportService.exportStudentReport(selected);
            showAlert(Alert.AlertType.INFORMATION, "Report saved to: " + report.toAbsolutePath());
        });

        VBox box = new VBox(10, table, exportBtn);
        return new Panel("Students and alerts", box);
    }

    private <T> TableColumn<StudentProgress, String> column(String title, java.util.function.Function<StudentProgress, String> mapper) {
        TableColumn<StudentProgress, String> col = new TableColumn<>(title);
        col.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(mapper.apply(data.getValue())));
        return col;
    }

    private Label metric(String title, String value) {
        Label label = new Label(title + "\n" + value);
        label.setStyle("-fx-font-weight: 700; -fx-padding: 10;");
        label.getStyleClass().add("panel");
        return label;
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.showAndWait();
    }
}
