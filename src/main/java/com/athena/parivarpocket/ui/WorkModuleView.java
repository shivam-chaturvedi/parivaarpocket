package com.athena.parivarpocket.ui;

import com.athena.parivarpocket.model.JobOpportunity;
import com.athena.parivarpocket.model.User;
import com.athena.parivarpocket.service.DataRepository;
import com.athena.parivarpocket.service.RapidJobService;
import com.google.gson.JsonObject;
import javafx.application.Platform;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class WorkModuleView extends VBox {
    private final DataRepository repository;
    private final RapidJobService rapidJobService = new RapidJobService();
    private final TextField searchField = new TextField();
    private final ChoiceBox<String> localityFilter = new ChoiceBox<>();
    private final ChoiceBox<String> locationFilter = new ChoiceBox<>();
    private final ChoiceBox<String> categoryFilter = new ChoiceBox<>();
    private final ChoiceBox<String> hoursFilter = new ChoiceBox<>();
    private final ChoiceBox<String> limitFilter = new ChoiceBox<>();
    private final CheckBox paidOnly = new CheckBox("Paid roles only");
    private final CheckBox favoritesOnly = new CheckBox("Show Favorites Only");
    private final Button refreshButton = new Button("Refresh");
    private final ProgressIndicator loadingIndicator = new ProgressIndicator();
    private final VBox contentHolder = new VBox();
    private final AtomicBoolean rapidSyncInProgress = new AtomicBoolean();
    private final Label jobCountLabel = new Label("0 jobs loaded");

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

    private void ensureRapidSync() {
        CompletableFuture.runAsync(() -> {
            LocalDateTime lastSync = repository.getLatestJobSyncTime();
            LocalDateTime now = LocalDateTime.now();
            boolean shouldSync = false;
            if (lastSync == null) {
                shouldSync = true;
                System.out.println("[WorkModuleView] No previous sync found, triggering sync.");
            } else {
                long hours = ChronoUnit.HOURS.between(lastSync, now);
                System.out.println("[WorkModuleView] Last sync was " + hours + " hours ago.");
                if (hours >= 24) {
                    shouldSync = true;
                }
            }
            if (shouldSync) {
                Platform.runLater(() -> startRapidJobSync(allJobs.isEmpty()));
            }
        });
    }

    private void startRapidJobSync(boolean showLoader) {
        if (!rapidSyncInProgress.compareAndSet(false, true)) {
            return;
        }
        if (showLoader) {
            setRefreshing(true);
        }
        AtomicReference<Throwable> syncError = new AtomicReference<>();
        Task<List<JobOpportunity>> task = new Task<>() {
            @Override
            protected List<JobOpportunity> call() throws Exception {
                List<JobOpportunity> fetched = rapidJobService.fetchRecentWestBengalJobs();
                if (fetched.isEmpty()) {
                    return Collections.emptyList();
                }
                try {
                    return repository.syncJobs(fetched);
                } catch (Exception e) {
                    syncError.set(e);
                    System.err.println("[WorkModuleView] Unable to sync jobs to Supabase: " + e.getMessage());
                    return fetched;
                }
            }
        };
        task.setOnSucceeded(event -> {
            List<JobOpportunity> jobs = task.getValue();
            if (jobs != null && !jobs.isEmpty()) {
                updateJobListing(jobs);
            }
            Throwable error = syncError.getAndSet(null);
            if (error != null) {
                showErrorDialog("Unable to save jobs", error.getMessage());
            }
            rapidSyncInProgress.set(false);
            setRefreshing(false);
        });
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            System.err.println("[WorkModuleView] Rapid job refresh failed: " + (error != null ? error.getMessage() : "unknown"));
            showErrorDialog("Job refresh failed", error != null ? error.getMessage() : "Unable to fetch the latest jobs.");
            rapidSyncInProgress.set(false);
            setRefreshing(false);
        });
        new Thread(task).start();
    }

    private Node buildHeader() {
        Label title = new Label("Employment Opportunities");
        title.getStyleClass().add("work-page-title");
        HBox titleRow = new HBox(8, title, jobCountLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("Search by role, company, location...");
        searchField.setPrefWidth(360);
        searchField.getStyleClass().add("job-search-input");

        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("primary-button");
        searchBtn.setOnAction(e -> refreshListings());

        jobCountLabel.getStyleClass().add("job-count-label");
        jobCountLabel.setPadding(new Insets(6, 0, 0, 0));

        refreshButton.getStyleClass().addAll("outline-button", "refresh-button");
        refreshButton.setPrefSize(110, 32);
        refreshButton.setOnAction(e -> {
            if (!allJobs.isEmpty()) {
                Collections.shuffle(allJobs);
                updateJobListing(new ArrayList<>(allJobs));
                startRapidJobSync(false); // Silent background sync
            } else {
                refreshListings();
                startRapidJobSync(true); // Full sync with loader
            }
        });
        refreshButton.setTooltip(new Tooltip("Refresh the job feed and sync with Supabase"));

        localityFilter.getItems().add("All localities");
        localityFilter.getSelectionModel().selectFirst();
        localityFilter.getStyleClass().add("job-filter-choice");
        localityFilter.setOnAction(e -> refreshListings());

        locationFilter.getItems().add("All locations");
        locationFilter.getSelectionModel().selectFirst();
        locationFilter.getStyleClass().add("job-filter-choice");
        locationFilter.setOnAction(e -> refreshListings());

        categoryFilter.getItems().addAll("All categories", "Tutoring", "Delivery", "Retail", "Internship", "General");
        categoryFilter.getSelectionModel().selectFirst();
        categoryFilter.getStyleClass().add("job-filter-choice");
        categoryFilter.setOnAction(e -> refreshListings());

        hoursFilter.getItems().addAll("All hours", "Full-time", "Part-time", "Flexible");
        hoursFilter.getSelectionModel().selectFirst();
        hoursFilter.getStyleClass().add("job-filter-choice");
        hoursFilter.setOnAction(e -> refreshListings());

        limitFilter.getItems().addAll("Show 10", "Show 15", "Show All");
        limitFilter.getStyleClass().add("job-filter-choice");
        limitFilter.setValue("Show All");
        limitFilter.setOnAction(e -> refreshListings());

        paidOnly.setOnAction(e -> refreshListings());
        favoritesOnly.setOnAction(e -> refreshListings());

        HBox searchRow = new HBox(10, searchField, searchBtn, refreshButton, loadingIndicator);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        HBox filterRow1 = new HBox(10, localityFilter, locationFilter, categoryFilter, hoursFilter, limitFilter);
        filterRow1.setAlignment(Pos.CENTER_LEFT);

        HBox filterRow2 = new HBox(15, paidOnly, favoritesOnly);
        filterRow2.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(10, titleRow, searchRow, filterRow1, filterRow2);
        header.setPadding(new Insets(10, 6, 6, 6));
        header.getStyleClass().add("work-page-header");
        return header;
    }

    private void fetchJobs() {
        loadingIndicator.setVisible(true);
        Task<List<JobOpportunity>> task = new Task<>() {
            @Override
            protected List<JobOpportunity> call() throws Exception {
                List<JobOpportunity> cached = repository.getJobOpportunities();
                return cached != null ? cached : Collections.emptyList();
            }
        };
        task.setOnSucceeded(event -> {
            loadingIndicator.setVisible(false);
            List<JobOpportunity> cached = task.getValue();
            if (cached != null && !cached.isEmpty()) {
                updateJobListing(cached);
            } else {
                allJobs = new ArrayList<>();
                updateJobCount(0);
                populateFilters();
                refreshListings();
            }
            ensureRapidSync();
        });
        task.setOnFailed(event -> {
            loadingIndicator.setVisible(false);
            Throwable error = task.getException();
            contentHolder.getChildren().setAll(new Label("Unable to load job feed. Try again later."));
            showErrorDialog("Job feed unavailable", error != null ? error.getMessage() : "Please check your connection.");
            updateJobCount(0);
            ensureRapidSync();
        });
        new Thread(task).start();
    }

    private void populateFilters() {
        localityFilter.getItems().setAll("All localities");
        locationFilter.getItems().setAll("All locations");
        allJobs.stream()
                .map(JobOpportunity::getLocality)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .forEach(localityFilter.getItems()::add);
        allJobs.stream()
                .map(JobOpportunity::getLocation)
                .filter(l -> l != null && !l.isBlank())
                .distinct()
                .sorted()
                .forEach(locationFilter.getItems()::add);
        localityFilter.getSelectionModel().selectFirst();
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
        
        VBox list = new VBox(12);
        List<String> favIds = repository.fetchFavoriteJobIds(repository.getCurrentUser());
        
        addTableHeader(list);
        for (JobOpportunity job : filtered) {
            list.getChildren().add(buildJobRow(job, favIds.contains(job.getId())));
        }
        
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("job-scroll-pane");
        contentHolder.getChildren().setAll(scroll);
    }

    private Node buildJobRow(JobOpportunity job, boolean isFav) {
        VBox rowContainer = new VBox(8);
        rowContainer.getStyleClass().add("job-row-container");

        GridPane row = new GridPane();
        row.setHgap(16);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        setupColumnConstraints(row);

        row.add(jobTitleBlock(job), 0, 0);
        row.add(jobCompanyBlock(job), 1, 0);
        row.add(createSuitabilityBadge(job), 2, 0);
        row.add(createCell(job.getSalaryDescription(), "job-table-cell"), 3, 0);
        row.add(applyAction(job, isFav), 4, 0);

        VBox details = buildDetailsPanel(job);
        details.setVisible(false);
        details.setManaged(false);

        row.setOnMouseClicked(e -> {
            boolean visible = !details.isVisible();
            details.setVisible(visible);
            details.setManaged(visible);
            if (visible) {
                row.getStyleClass().add("job-row-expanded");
            } else {
                row.getStyleClass().remove("job-row-expanded");
            }
        });

        rowContainer.getChildren().addAll(row, details);
        return rowContainer;
    }

    private void setupColumnConstraints(GridPane table) {
        ColumnConstraints titleCol = new ColumnConstraints();
        titleCol.setPercentWidth(30);
        ColumnConstraints companyCol = new ColumnConstraints();
        companyCol.setPercentWidth(25);
        ColumnConstraints suitCol = new ColumnConstraints();
        suitCol.setPercentWidth(15);
        ColumnConstraints salaryCol = new ColumnConstraints();
        salaryCol.setPercentWidth(15);
        ColumnConstraints actionCol = new ColumnConstraints();
        actionCol.setPercentWidth(15);
        table.getColumnConstraints().setAll(titleCol, companyCol, suitCol, salaryCol, actionCol);
    }

    private void addTableHeader(VBox container) {
        GridPane header = new GridPane();
        header.setHgap(16);
        header.setPadding(new Insets(12, 16, 12, 16));
        setupColumnConstraints(header);
        header.add(createHeader("Role & details"), 0, 0);
        header.add(createHeader("Organisation"), 1, 0);
        header.add(createHeader("Suitability"), 2, 0);
        header.add(createHeader("Salary"), 3, 0);
        header.add(createHeader("Action"), 4, 0);
        container.getChildren().add(header);
    }

    private Node createSuitabilityBadge(JobOpportunity job) {
        String label = "Neutral";
        String style = "badge-neutral";
        
        String cat = job.getCategory();
        if ("Tutoring".equals(cat)) {
            label = "Excellent Match";
            style = "badge-success";
        } else if ("Delivery".equals(cat) || "Retail".equals(cat)) {
            label = "High Potential";
            style = "badge-info";
        } else if ("Internship".equals(cat)) {
            label = "Growth Path";
            style = "badge-warning";
        }

        Label badge = new Label(label);
        badge.getStyleClass().addAll("suitability-badge", style);
        HBox box = new HBox(badge);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private VBox buildDetailsPanel(JobOpportunity job) {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(0, 32, 16, 32));
        panel.getStyleClass().add("job-details-expand");

        panel.getChildren().add(new Separator());

        HBox top = new HBox(40);
        top.getChildren().addAll(
            detailItem("Required Skills", String.join(", ", job.getRequiredSkills())),
            detailItem("Working Hours", job.getWorkingHours())
        );

        VBox safety = new VBox(6, 
            new Label("Safety Guidance"),
            new Label(job.getSafetyGuidance())
        );
        safety.getChildren().get(0).getStyleClass().add("detail-label");
        safety.getChildren().get(1).getStyleClass().add("detail-value-long");

        VBox contact = new VBox(6, 
            new Label("Contact Information"),
            new Label(job.getContactInfo())
        );
        contact.getChildren().get(0).getStyleClass().add("detail-label");
        contact.getChildren().get(1).getStyleClass().add("detail-value-long");

        panel.getChildren().addAll(top, safety, contact);
        return panel;
    }

    private VBox detailItem(String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("detail-label");
        Label v = new Label(value);
        v.getStyleClass().add("detail-value");
        return new VBox(4, l, v);
    }

    private void updateJobListing(List<JobOpportunity> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            refreshListings();
            return;
        }
        LinkedHashMap<String, JobOpportunity> merged = new LinkedHashMap<>();
        for (JobOpportunity job : jobs) {
            if (job == null || job.getId() == null || job.getId().isBlank()) {
                continue;
            }
            merged.put(job.getId(), job);
        }
        for (JobOpportunity job : allJobs) {
            if (job == null || job.getId() == null || job.getId().isBlank()) {
                continue;
            }
            merged.putIfAbsent(job.getId(), job);
        }
        List<JobOpportunity> combined = new ArrayList<>(merged.values());
        allJobs = combined;
        updateJobCount(combined.size());
        populateFilters();
        limitFilter.setValue("Show All");
        refreshListings();
    }

    private void updateJobCount(int count) {
        int safeCount = Math.max(0, count);
        Platform.runLater(() -> jobCountLabel.setText(safeCount + " job" + (safeCount == 1 ? "" : "s") + " loaded"));
    }

    private void setRefreshing(boolean refreshing) {
        Platform.runLater(() -> {
            loadingIndicator.setVisible(refreshing);
            refreshButton.setDisable(refreshing);
        });
    }

    private List<JobOpportunity> filterJobs() {
        String term = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String locality = localityFilter.getValue();
        String location = locationFilter.getValue();
        String category = categoryFilter.getValue();
        String hours = hoursFilter.getValue();
        boolean onlyPaid = paidOnly.isSelected();
        boolean onlyFavs = favoritesOnly.isSelected();
        
        List<String> favIds = onlyFavs ? repository.fetchFavoriteJobIds(repository.getCurrentUser()) : null;
        
        List<JobOpportunity> filtered = allJobs.stream()
                .filter(job -> term.isBlank() || matchesSearch(job, term))
                .filter(job -> locality == null || locality.equals("All localities") || locality.equalsIgnoreCase(job.getLocality()))
                .filter(job -> location == null || location.equals("All locations") || location.equalsIgnoreCase(job.getLocation()))
                .filter(job -> category == null || category.equals("All categories") || category.equalsIgnoreCase(job.getCategory()))
                .filter(job -> hours == null || hours.equals("All hours") || hours.equalsIgnoreCase(job.getWorkingHours()))
                .filter(job -> !onlyPaid || job.hasSalary())
                .filter(job -> !onlyFavs || (favIds != null && favIds.contains(job.getId())))
                .toList();

        String limitVal = limitFilter.getValue();
        if (limitVal != null && !limitVal.equals("Show All")) {
            int limit = 10;
            if (limitVal.contains("15")) limit = 15;
            return filtered.stream().limit(limit).toList();
        }
        return filtered;
    }

    private boolean matchesSearch(JobOpportunity job, String term) {
        return contains(job.getTitle(), term) || contains(job.getCompany(), term) || contains(job.getLocation(), term);
    }

    private boolean contains(String source, String term) {
        return source != null && source.toLowerCase().contains(term);
    }

    private HBox applyAction(JobOpportunity job, boolean isFav) {
        Button apply = new Button("Apply Now");
        apply.getStyleClass().addAll("primary-button", "small-button", "job-apply-button");
        apply.setDisable(job.getId() == null || job.getId().isBlank());
        apply.setOnAction(e -> {
            handleJobApply(job);
            openJobLink(job);
        });

        Button favBtn = new Button(isFav ? "❤" : "♡");
        favBtn.getStyleClass().addAll("icon-button", "fav-button");
        if (isFav) favBtn.getStyleClass().add("fav-active");
        favBtn.setOnAction(e -> {
            repository.toggleFavorite(repository.getCurrentUser(), job.getId());
            favBtn.setText(repository.isFavorite(repository.getCurrentUser(), job.getId()) ? "❤" : "♡");
            if (favBtn.getText().equals("❤")) favBtn.getStyleClass().add("fav-active");
            else favBtn.getStyleClass().remove("fav-active");
            e.consume(); // Don't expand the row
        });

        HBox actions = new HBox(8, favBtn, apply);
        actions.setAlignment(Pos.CENTER_RIGHT);
        return actions;
    }

    private void openJobLink(JobOpportunity job) {
        if (job == null) return;
        String jobId = job.getId();
        String jobUrl = buildViewJobUrl(jobId);
        if (jobUrl.isBlank() || !Desktop.isDesktopSupported()) {
            return;
        }

        // Log view activity before opening the link
        User currentUser = repository.getCurrentUser();
        if (currentUser != null) {
            JsonObject activityData = new JsonObject();
            activityData.addProperty("job_id", jobId);
            activityData.addProperty("job_title", job.getTitle());
            activityData.addProperty("company", job.getCompany());
            activityData.addProperty("source", "work_module");
            repository.logStudentActivity(currentUser, "job_view", activityData);
        }

        try {
            Desktop.getDesktop().browse(new URI(jobUrl));
        } catch (IOException | URISyntaxException ex) {
            System.err.println("Unable to open job link: " + jobUrl);
        }
    }

    private String buildViewJobUrl(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return "";
        }
        return "https://in.indeed.com/viewjob?jk=" + jobId;
    }

    private void handleJobApply(JobOpportunity job) {
        User currentUser = repository.getCurrentUser();
        if (currentUser == null || job == null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            repository.recordJobApplication(currentUser, job);
            
            JsonObject activityData = new JsonObject();
            activityData.addProperty("job_id", job.getId());
            activityData.addProperty("job_title", job.getTitle());
            activityData.addProperty("company", job.getCompany());
            activityData.addProperty("location", job.getLocation());
            activityData.addProperty("locality", job.getLocality());
            activityData.addProperty("job_link", job.getJobLink());
            activityData.addProperty("formatted_relative_time", job.getFormattedRelativeTime());
            if (job.getSalaryMin() != null) {
                activityData.addProperty("salary_min", job.getSalaryMin());
            }
            if (job.getSalaryMax() != null) {
                activityData.addProperty("salary_max", job.getSalaryMax());
            }
            activityData.addProperty("salary_type", job.getSalaryType());
            repository.logStudentActivity(currentUser, "job_application_click", activityData);
        });
    }

    private void showErrorDialog(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    (message != null && !message.isBlank()) ? message : "Please try again later.",
                    ButtonType.OK);
            alert.setHeaderText(title);
            alert.show();
        });
    }

    private VBox jobTitleBlock(JobOpportunity job) {
        Label title = new Label(job.getTitle());
        title.getStyleClass().add("job-title");
        Label meta = new Label(job.getLocation() + " • " + job.getLocality());
        meta.getStyleClass().add("job-meta");
        return new VBox(2, title, meta);
    }

    private VBox jobCompanyBlock(JobOpportunity job) {
        Label company = new Label(job.getCompany());
        company.getStyleClass().add("job-company");
        Label posted = new Label(job.getFormattedRelativeTime());
        posted.getStyleClass().add("job-posted");
        return new VBox(2, company, posted);
    }

    private Label createCell(String text, String styleClass) {
        Label label = new Label(text);
        if (styleClass != null) label.getStyleClass().add(styleClass);
        return label;
    }

    private Label createHeader(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("job-table-header");
        return label;
    }
}
