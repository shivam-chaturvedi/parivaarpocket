package com.athena.parivarpocket.ui;

import com.athena.parivarpocket.model.JobOpportunity;
import com.athena.parivarpocket.service.DataRepository;
import com.athena.parivarpocket.service.RapidJobService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorkModuleView extends VBox {
    private final DataRepository repository;
    private final RapidJobService rapidJobService = new RapidJobService();
    private final TextField searchField = new TextField();
    private final ChoiceBox<String> categoryFilter = new ChoiceBox<>();
    private final ChoiceBox<String> locationFilter = new ChoiceBox<>();
    private final CheckBox verifiedOnly = new CheckBox("Verified jobs only");
    private final ProgressIndicator loadingIndicator = new ProgressIndicator();
    private final VBox contentHolder = new VBox();

    private List<JobOpportunity> allJobs = new ArrayList<>();

    public WorkModuleView(DataRepository repository) {
        this.repository = repository;
        setSpacing(18);
        setPadding(new Insets(0));
        loadingIndicator.setVisible(false);
        loadingIndicator.setPrefSize(32, 32);
        getChildren().addAll(buildHeader(), contentHolder);
        fetchJobs();
    }

    private Node buildHeader() {
        Label title = new Label("Employment Opportunities");
        title.getStyleClass().add("work-page-title");

        searchField.setPromptText("Search by role, company, location...");
        searchField.setPrefWidth(360);
        searchField.getStyleClass().add("job-search-input");

        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("primary-button");
        searchBtn.setOnAction(e -> refreshListings());

        categoryFilter.getItems().add("All categories");
        categoryFilter.getSelectionModel().selectFirst();
        categoryFilter.getStyleClass().add("job-filter-choice");

        locationFilter.getItems().add("All locations");
        locationFilter.getSelectionModel().selectFirst();
        locationFilter.getStyleClass().add("job-filter-choice");

        verifiedOnly.setOnAction(e -> refreshListings());

        HBox searchRow = new HBox(10, searchField, searchBtn, loadingIndicator);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        HBox filters = new HBox(10, categoryFilter, locationFilter, verifiedOnly);
        filters.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(10, title, searchRow, filters);
        header.setPadding(new Insets(10, 6, 6, 6));
        header.getStyleClass().add("work-page-header");
        return header;
    }

    private void fetchJobs() {
        loadingIndicator.setVisible(true);
        Task<List<JobOpportunity>> task = new Task<>() {
            @Override
            protected List<JobOpportunity> call() throws Exception {
                List<JobOpportunity> fetched = rapidJobService.fetchRecentWestBengalJobs();
                if (fetched.isEmpty()) {
                    return Collections.emptyList();
                }
                return fetched;
            }
        };
        task.setOnSucceeded(event -> {
            loadingIndicator.setVisible(false);
            allJobs = new ArrayList<>(task.getValue());
            populateFilters();
            refreshListings();
        });
        task.setOnFailed(event -> {
            loadingIndicator.setVisible(false);
            contentHolder.getChildren().setAll(new Label("Unable to load job feed. Try again later."));
        });
        new Thread(task).start();
    }

    private void populateFilters() {
        categoryFilter.getItems().setAll("All categories");
        locationFilter.getItems().setAll("All locations");
        allJobs.stream()
                .map(JobOpportunity::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .forEach(categoryFilter.getItems()::add);
        allJobs.stream()
                .map(JobOpportunity::getLocation)
                .filter(l -> l != null && !l.isBlank())
                .distinct()
                .sorted()
                .forEach(locationFilter.getItems()::add);
        categoryFilter.getSelectionModel().selectFirst();
        locationFilter.getSelectionModel().selectFirst();
    }

    private void refreshListings() {
        if (allJobs.isEmpty()) {
            contentHolder.getChildren().setAll(new Label("No jobs found yet. Refresh in a bit."));
            return;
        }
        List<JobOpportunity> filtered = filterJobs();
        if (filtered.isEmpty()) {
            contentHolder.getChildren().setAll(new Label("No listings matched your filters."));
            return;
        }
        Panel panel = buildListingsPanel(filtered);
        contentHolder.getChildren().setAll(panel);
    }

    private List<JobOpportunity> filterJobs() {
        String term = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String category = categoryFilter.getValue();
        String location = locationFilter.getValue();
        boolean onlyVerified = verifiedOnly.isSelected();
        return allJobs.stream()
                .filter(job -> term.isBlank() || matchesSearch(job, term))
                .filter(job -> category == null || category.equals("All categories") || category.equalsIgnoreCase(job.getCategory()))
                .filter(job -> location == null || location.equals("All locations") || location.equalsIgnoreCase(job.getLocation()))
                .filter(job -> !onlyVerified || job.getSuitabilityScore() >= 80)
                .toList();
    }

    private boolean matchesSearch(JobOpportunity job, String term) {
        return contains(job.getTitle(), term) || contains(job.getCompany(), term) || contains(job.getLocation(), term);
    }

    private boolean contains(String source, String term) {
        return source != null && source.toLowerCase().contains(term);
    }

    private Panel buildListingsPanel(List<JobOpportunity> jobs) {
        GridPane table = new GridPane();
        table.setHgap(16);
        table.setVgap(16);
        table.setPadding(new Insets(12, 0, 0, 0));

        ColumnConstraints titleCol = new ColumnConstraints();
        titleCol.setPercentWidth(35);
        ColumnConstraints companyCol = new ColumnConstraints();
        companyCol.setPercentWidth(20);
        ColumnConstraints hoursCol = new ColumnConstraints();
        hoursCol.setPercentWidth(15);
        ColumnConstraints payCol = new ColumnConstraints();
        payCol.setPercentWidth(15);
        ColumnConstraints actionCol = new ColumnConstraints();
        actionCol.setPercentWidth(15);
        table.getColumnConstraints().addAll(titleCol, companyCol, hoursCol, payCol, actionCol);

        addTableHeader(table);
        int row = 1;
        for (JobOpportunity job : jobs) {
            table.add(jobTitleBlock(job), 0, row);
            table.add(jobCompanyBlock(job), 1, row);
            table.add(createCell(job.getHours(), "job-table-cell"), 2, row);
            table.add(createCell(job.getPayRange(), "job-table-cell"), 3, row);
            table.add(applyAction(job), 4, row);
            row++;
        }
        Panel panel = new Panel("Job Listings", table);
        panel.getStyleClass().add("job-listings-panel");
        return panel;
    }

    private void addTableHeader(GridPane table) {
        table.add(createHeader("Role & details"), 0, 0);
        table.add(createHeader("Organisation"), 1, 0);
        table.add(createHeader("Hours"), 2, 0);
        table.add(createHeader("Pay"), 3, 0);
        table.add(createHeader("Action"), 4, 0);
    }

    private VBox jobTitleBlock(JobOpportunity job) {
        Label title = new Label(job.getTitle());
        title.getStyleClass().add("job-title");
        Label location = new Label(job.getLocation());
        location.getStyleClass().add("job-meta");
        Label suitability = new Label("Suitability: " + job.getSuitabilityScore() + "%");
        suitability.getStyleClass().add("job-meta-tight");
        VBox box = new VBox(4, title, location, suitability);
        box.getStyleClass().add("job-table-cell");
        return box;
    }

    private VBox jobCompanyBlock(JobOpportunity job) {
        Label company = new Label(job.getCompany());
        company.getStyleClass().add("job-company");
        Label category = new Label(job.getCategory());
        category.getStyleClass().add("job-meta-tight");
        VBox box = new VBox(4, company, category);
        box.getStyleClass().add("job-table-cell");
        return box;
    }

    private Label createHeader(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("job-table-header");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Label createCell(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER_LEFT);
        return label;
    }

    private Button applyAction(JobOpportunity job) {
        Button apply = new Button("Apply Now");
        apply.getStyleClass().add("primary-button");
        apply.setDisable(job.getJobUrl() == null || job.getJobUrl().isBlank());
        apply.setOnAction(e -> openJobLink(job.getJobUrl()));
        return apply;
    }

    private void openJobLink(String jobUrl) {
        if (jobUrl == null || jobUrl.isBlank() || !Desktop.isDesktopSupported()) {
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(jobUrl));
        } catch (IOException | URISyntaxException ex) {
            System.err.println("Unable to open job link: " + jobUrl);
        }
    }
}
