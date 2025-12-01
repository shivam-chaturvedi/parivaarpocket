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
}
