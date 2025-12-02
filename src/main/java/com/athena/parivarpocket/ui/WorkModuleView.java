package com.athena.parivarpocket.ui;

import com.athena.parivarpocket.model.JobOpportunity;
import com.athena.parivarpocket.service.OfflineSyncService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class WorkModuleView extends VBox {
    public WorkModuleView(List<JobOpportunity> jobs, OfflineSyncService offlineSyncService) {
        setSpacing(18);
        setPadding(new Insets(0));

        getChildren().add(buildOverviewPanel(jobs));
        getChildren().add(buildMonthlyBudgetPanel());
        getChildren().add(buildListingsPanel(jobs, offlineSyncService));
    }

    private Panel buildOverviewPanel(List<JobOpportunity> jobs) {
        TextField search = new TextField();
        search.setPromptText("Search jobs by title, company, or location...");
        search.setPrefWidth(360);
        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("primary-button");
        search.getStyleClass().add("card-text-input");
        ChoiceBox<String> category = new ChoiceBox<>();
        category.getItems().addAll("All Categories", "Tutoring", "Delivery", "Retail", "Internship");
        category.getSelectionModel().selectFirst();
        category.getStyleClass().add("card-choice-box");
        HBox searchRow = new HBox(10, search, searchBtn, category);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        HBox stats = new HBox(10,
                createSummaryCard(String.valueOf(jobs.size()), "Available Jobs"),
                createSummaryCard(String.valueOf((int) jobs.stream().filter(j -> j.getSuitabilityScore() >= 80).count()), "Verified Safe"),
                createSummaryCard("3", "Bookmarked")
        );
        stats.setAlignment(Pos.CENTER);
        stats.setPadding(new Insets(6, 0, 0, 0));

        VBox content = new VBox(12, searchRow, stats);
        Panel panel = new Panel("Local Employment Opportunities", content);
        panel.getStyleClass().add("work-overview-panel");
        return panel;
    }

    private Panel buildMonthlyBudgetPanel() {
        List<BudgetLine> lines = List.of(
                new BudgetLine("Food & Groceries", "₹2,000", "₹2,200", "₹1,800", "Over by ₹200"),
                new BudgetLine("Transportation", "₹800", "₹900", "₹750", "Over by ₹100"),
                new BudgetLine("Education", "₹1,500", "₹1,500", "₹1,500", "√ On Track"),
                new BudgetLine("Utilities", "₹600", "₹700", "₹600", "Over by ₹100"),
                new BudgetLine("Entertainment", "₹400", "₹500", "₹300", "Over by ₹100")
        );

        VBox table = new VBox();
        table.setSpacing(0);
        table.getStyleClass().add("monthly-budget-table");
        table.getChildren().add(headerRow());
        for (int i = 0; i < lines.size(); i++) {
            table.getChildren().add(dataRow(lines.get(i), i));
        }
        table.getChildren().add(totalRow());

        VBox content = new VBox(table);
        content.setPadding(new Insets(8, 0, 0, 0));
        Panel panel = new Panel("Monthly Budget Breakdown", content);
        panel.getStyleClass().add("monthly-budget-panel");
        return panel;
    }

    private VBox createSummaryCard(String value, String label) {
        Label number = new Label(value);
        number.getStyleClass().add("work-stat-value");
        Label caption = new Label(label);
        caption.getStyleClass().add("work-stat-label");
        VBox box = new VBox(2, number, caption);
        box.setPadding(new Insets(10));
        box.getStyleClass().add("work-stat-card");
        box.setPrefWidth(180);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private HBox headerRow() {
        HBox row = new HBox();
        row.getStyleClass().addAll("monthly-budget-row", "monthly-budget-header-row");
        row.getChildren().addAll(
                headerCell("Category"),
                headerCell("Planned Budget"),
                headerCell("Actual Spending"),
                headerCell("Optimized Budget"),
                headerCell("Status")
        );
        return row;
    }

    private HBox dataRow(BudgetLine line, int index) {
        HBox row = new HBox();
        row.getStyleClass().add("monthly-budget-row");
        if (index % 2 == 1) {
            row.getStyleClass().add("monthly-budget-row-alt");
        }
        row.getChildren().addAll(
                dataCell(line.category, true),
                dataCell(line.planned, false),
                dataCell(line.actual, false),
                dataCell(line.optimized, false),
                statusCell(line.status)
        );
        return row;
    }

    private HBox totalRow() {
        HBox row = new HBox();
        row.getStyleClass().addAll("monthly-budget-row", "monthly-budget-total-row");
        row.getChildren().addAll(
                totalCell("TOTAL"),
                totalCell("₹5,300"),
                totalCell("₹5,800"),
                totalCell("₹4,950"),
                new Label()
        );
        return row;
    }

    private Label headerCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().addAll("monthly-budget-cell", "monthly-budget-header");
        label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);
        return label;
    }

    private Label dataCell(String text, boolean isCategory) {
        Label label = new Label(text);
        label.getStyleClass().add("monthly-budget-cell");
        if (isCategory) {
            label.getStyleClass().add("monthly-budget-cell-category");
            label.setAlignment(Pos.CENTER_LEFT);
        } else {
            label.setAlignment(Pos.CENTER);
        }
        label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);
        return label;
    }

    private Label statusCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("monthly-status-pill");
        label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);
        return label;
    }

    private Label totalCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("monthly-budget-total-cell");
        label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);
        return label;
    }

    private Panel buildListingsPanel(List<JobOpportunity> jobs, OfflineSyncService offlineSyncService) {
        VBox list = new VBox(12);
        jobs.forEach(job -> list.getChildren().add(createJobCard(job, offlineSyncService)));
        Panel panel = new Panel("Job Listings", list);
        panel.getStyleClass().add("job-listings-panel");
        return panel;
    }

    private Card createJobCard(JobOpportunity job, OfflineSyncService offlineSyncService) {
        Label title = new Label(job.getTitle());
        title.getStyleClass().add("job-title");
        Label company = new Label(job.getCompany());
        company.getStyleClass().add("job-company");
        Label meta = new Label(job.getLocation() + " • " + job.getHours() + " • " + job.getPayRange());
        meta.getStyleClass().add("job-meta");

        HBox badges = new HBox(6,
                badge("Verified by Parivaar"),
                badge(job.getSuitabilityScore() >= 80 ? "High Suitability" : "Medium Suitability"),
                badge(job.getCategory())
        );
        badges.setPadding(new Insets(4, 0, 4, 0));

        VBox details = new VBox(4, title, badges, company, meta);
        details.setAlignment(Pos.CENTER_LEFT);

        Button apply = new Button("Apply Now");
        apply.getStyleClass().add("primary-button");
        apply.setOnAction(e -> {
            if (offlineSyncService.isOfflineMode()) {
                offlineSyncService.queueOperation("Apply to " + job.getTitle());
            }
        });
        Button save = new Button("Save");
        save.getStyleClass().add("outline-button");
        HBox actions = new HBox(10, apply, save);
        actions.setAlignment(Pos.CENTER_RIGHT);

        HBox inner = new HBox(details, actions);
        HBox.setHgrow(details, Priority.ALWAYS);
        inner.setSpacing(12);
        inner.setAlignment(Pos.CENTER_LEFT);
        Card card = new Card();
        card.getStyleClass().add("job-card");
        card.setPadding(new Insets(12));
        card.getChildren().add(inner);
        return card;
    }

    private Label badge(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("job-badge");
        return label;
    }

    private record BudgetLine(String category, String planned, String actual, String optimized, String status) {
    }
}
