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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class WorkModuleView extends VBox {
    private final DataRepository repository;
    private final RapidJobService rapidJobService = new RapidJobService();
    private final TextField searchField = new TextField();
    private final Button refreshButton = new Button("Refresh");
    private final ProgressIndicator loadingIndicator = new ProgressIndicator();
    private final VBox contentHolder = new VBox();
    private final AtomicBoolean rapidSyncInProgress = new AtomicBoolean();
    
    private final Label availableJobsVal = new Label("0");
    private final Label appsVal = new Label("0");
    private final Label bookmarkedVal = new Label("0");

    // TEST 24: DB connection status label
    private final Label dbStatusLabel = new Label("● Connecting...");

    // TEST 28: Track applied jobs in this session
    private final Set<String> appliedJobIds = new HashSet<>();

    private List<JobOpportunity> allJobs = new ArrayList<>();

    public WorkModuleView(DataRepository repository) {
        this.repository = repository;
        setSpacing(0);
        setPadding(new Insets(0));
        loadingIndicator.setVisible(false);
        loadingIndicator.setPrefSize(24, 24);
        
        // Style the DB status label
        dbStatusLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11px; -fx-padding: 4 8;");
        
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
            } else {
                long hours = ChronoUnit.HOURS.between(lastSync, now);
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
                    return fetched;
                }
            }
        };
        task.setOnSucceeded(event -> {
            List<JobOpportunity> jobs = task.getValue();
            if (jobs != null && !jobs.isEmpty()) {
                updateJobListing(jobs);
            }
            rapidSyncInProgress.set(false);
            setRefreshing(false);
        });
        task.setOnFailed(event -> {
            rapidSyncInProgress.set(false);
            setRefreshing(false);
        });
        new Thread(task).start();
    }

    private Node buildHeader() {
        VBox header = new VBox(0);
        header.setPadding(new Insets(0, 0, 24, 0));

        VBox mainContainer = new VBox(20);
        mainContainer.getStyleClass().add("quiz-container"); // Reuse for border/white bg
        mainContainer.setPadding(new Insets(24));

        Label localTitle = new Label("Local Employment Opportunities");
        localTitle.getStyleClass().add("modal-header-title"); // Reuse header style
        
        // Search and Sort
        searchField.setPromptText("Search potential job opportunities available in West-Bengal");
        searchField.getStyleClass().add("job-search-input");
        searchField.setPrefWidth(600);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        
        Button searchBtn = new Button("\u2315"); // Magnifying glass
        searchBtn.getStyleClass().add("job-search-btn");
        searchBtn.setOnAction(e -> refreshListings());

        HBox searchBox = new HBox(searchField, searchBtn);
        searchBox.getStyleClass().add("job-search-box");
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setMinWidth(650);

        Label sortLabel = new Label("Descending Job Salaries Order");
        sortLabel.getStyleClass().add("job-sort-label");
        
        StackPane sortBox = new StackPane(sortLabel);
        sortBox.getStyleClass().add("job-sort-dropdown");
        sortBox.setPrefWidth(300);
        sortBox.setAlignment(Pos.CENTER);

        HBox controls = new HBox(20, searchBox, sortBox);
        controls.setAlignment(Pos.CENTER_LEFT);

        // Metrics
        HBox metrics = new HBox(20);
        metrics.getStyleClass().add("employment-metrics-container");
        metrics.getChildren().addAll(
            createMetricCard("Available Jobs", availableJobsVal),
            createMetricCard("Applications", appsVal),
            createMetricCard("Bookmarked", bookmarkedVal)
        );
        metrics.setAlignment(Pos.CENTER);
        for (Node n : metrics.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
        }

        // TEST 24: DB status row at bottom of header
        HBox statusRow = new HBox();
        statusRow.setAlignment(Pos.CENTER_RIGHT);
        statusRow.getChildren().add(dbStatusLabel);

        mainContainer.getChildren().addAll(localTitle, new Separator(), controls, metrics, statusRow);
        header.getChildren().addAll(mainContainer);
        return header;
    }

    private VBox createMetricCard(String label, Label valLabel) {
        VBox card = new VBox(4);
        card.getStyleClass().add("employment-metric-card");
        valLabel.getStyleClass().add("employment-metric-value");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("employment-metric-label");
        card.getChildren().addAll(valLabel, lbl);
        card.setAlignment(Pos.CENTER);
        return card;
    }

    private void fetchJobs() {
        setRefreshing(true);
        Task<List<JobOpportunity>> task = new Task<>() {
            @Override
            protected List<JobOpportunity> call() throws Exception {
                return repository.getJobOpportunities();
            }
        };
        task.setOnSucceeded(event -> {
            setRefreshing(false);
            List<JobOpportunity> cached = task.getValue();
            if (cached != null) {
                updateJobListing(cached);
            }
            // TEST 24: Show connected status on success
            Platform.runLater(() -> {
                dbStatusLabel.setText("● Connected");
                dbStatusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8;");
            });
            ensureRapidSync();
        });
        task.setOnFailed(event -> {
            setRefreshing(false);
            // TEST 24: Show disconnected status on failure
            Platform.runLater(() -> {
                dbStatusLabel.setText("● Disconnected");
                dbStatusLabel.setStyle("-fx-text-fill: #f44336; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8;");
            });
            ensureRapidSync();
        });
        new Thread(task).start();
    }

    private void refreshListings() {
        VBox list = new VBox(20);
        list.setPadding(new Insets(24));
        
        Label sectionTitle = new Label("Job Listings");
        sectionTitle.getStyleClass().add("modal-header-title");
        list.getChildren().addAll(sectionTitle, new Separator());

        if (allJobs.isEmpty()) {
            list.getChildren().add(new Label("No jobs available. Please refresh."));
            contentHolder.getChildren().setAll(list);
            return;
        }

        List<JobOpportunity> filtered = filterAndSortJobs();
        List<String> favIds = repository.fetchFavoriteJobIds(repository.getCurrentUser());
        
        for (JobOpportunity job : filtered) {
            list.getChildren().add(buildJobCard(job, favIds.contains(job.getId())));
        }
        
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        contentHolder.getChildren().setAll(scroll);
        
        updateMetrics(favIds.size());
    }

    private List<JobOpportunity> filterAndSortJobs() {
        String term = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<JobOpportunity> filtered = allJobs.stream()
                .filter(job -> term.isBlank() || 
                        job.getTitle().toLowerCase().contains(term) || 
                        job.getCompany().toLowerCase().contains(term))
                .collect(Collectors.toList());

        // Always sort by descending salary
        filtered.sort((a, b) -> Double.compare(b.getSalaryMax() != null ? b.getSalaryMax() : 0, 
                                              a.getSalaryMax() != null ? a.getSalaryMax() : 0));
        
        return filtered;
    }

    private Node buildJobCard(JobOpportunity job, boolean isFav) {
        VBox card = new VBox(12);
        card.getStyleClass().add("job-card-redesign");

        HBox topRow = new HBox(20);
        topRow.setAlignment(Pos.TOP_LEFT);

        VBox info = new VBox(6);
        Label title = new Label(job.getTitle());
        title.getStyleClass().add("job-card-title");
        
        HBox companyRow = new HBox(12);
        companyRow.setAlignment(Pos.CENTER_LEFT);
        Label company = new Label(job.getCompany());
        company.getStyleClass().add("job-card-company");
        
        Label catBadge = new Label(job.getCategory());
        catBadge.getStyleClass().add("job-card-category-badge");
        companyRow.getChildren().addAll(company, catBadge);

        Label meta = new Label(String.format("%s, %s    %s    (%s)", 
            job.getLocation(), job.getLocality(), job.getSalaryDescription(), job.getWorkingHours()));
        meta.getStyleClass().add("job-card-meta");

        info.getChildren().addAll(title, companyRow, meta);
        HBox.setHgrow(info, Priority.ALWAYS);

        VBox actions = new VBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        // TEST 28: Show "Applied ✓" if already applied, otherwise "Apply Now"
        boolean alreadyApplied = appliedJobIds.contains(job.getId());
        Button applyBtn = new Button(alreadyApplied ? "Applied \u2713" : "Apply Now");
        applyBtn.getStyleClass().add("job-apply-dark-btn");
        if (alreadyApplied) {
            applyBtn.setDisable(true);
            applyBtn.setStyle("-fx-opacity: 0.7;");
        }
        applyBtn.setOnAction(e -> {
            handleJobApply(job);
            appliedJobIds.add(job.getId());
            applyBtn.setText("Applied \u2713");
            applyBtn.setDisable(true);
            applyBtn.setStyle("-fx-opacity: 0.7;");
        });

        Button saveBtn = new Button(isFav ? "\uD83D\uDD16 Saved" : "\uD83D\uDD16 Save");
        saveBtn.getStyleClass().add("job-save-white-btn");
        saveBtn.setOnAction(e -> {
            repository.toggleFavorite(repository.getCurrentUser(), job.getId());
            refreshListings();
        });

        // TEST 26: "View Details" button to open job detail dialog
        Button detailBtn = new Button("View Details");
        detailBtn.getStyleClass().add("outline-button");
        detailBtn.setOnAction(e -> showJobDetailDialog(job));

        actions.getChildren().addAll(applyBtn, saveBtn, detailBtn);

        topRow.getChildren().addAll(info, actions);
        card.getChildren().add(topRow);

        return card;
    }

    /**
     * TEST 26: Show a detailed job dialog with all fields:
     * title, location, salary, category, required skills, working hours, safety guidance, contact info.
     */
    private void showJobDetailDialog(JobOpportunity job) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(job.getTitle());

        VBox root = new VBox();
        root.getStyleClass().add("quiz-container");

        // Header
        HBox header = new HBox(new Label(job.getTitle()));
        header.getStyleClass().add("modal-header");

        VBox body = new VBox(16);
        body.setPadding(new Insets(24));

        // Company and category
        Label companyLabel = new Label(job.getCompany());
        companyLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label categoryLabel = new Label("Category: " + job.getCategory());
        categoryLabel.setStyle("-fx-text-fill: #555;");

        Separator sep1 = new Separator();

        // Location
        addDetailRow(body, "\uD83D\uDCCD Location", job.getLocation() + (job.getLocality() != null && !job.getLocality().isBlank() ? ", " + job.getLocality() : ""));

        // Salary
        addDetailRow(body, "\uD83D\uDCB0 Salary", job.getSalaryDescription());

        // Working Hours
        addDetailRow(body, "\u23F0 Working Hours", job.getWorkingHours() != null && !job.getWorkingHours().isBlank() ? job.getWorkingHours() : "Not specified");

        // Required Skills
        String skills = job.getRequiredSkills() != null && !job.getRequiredSkills().isEmpty()
                ? String.join(", ", job.getRequiredSkills())
                : "Not specified";
        addDetailRow(body, "\uD83D\uDCCB Required Skills", skills);

        // Safety Guidance
        String safety = job.getSafetyGuidance() != null && !job.getSafetyGuidance().isBlank()
                ? job.getSafetyGuidance()
                : "Standard workplace safety practices apply.";
        addDetailRow(body, "\uD83D\uDEE1 Safety Guidance", safety);

        // Contact Info
        String contact = job.getContactInfo() != null && !job.getContactInfo().isBlank()
                ? job.getContactInfo()
                : "Apply via the link below.";
        addDetailRow(body, "\uD83D\uDCDE Contact", contact);

        Separator sep2 = new Separator();

        // Footer buttons
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);

        boolean alreadyApplied = appliedJobIds.contains(job.getId());
        Button applyBtn = new Button(alreadyApplied ? "Applied \u2713" : "Apply Now");
        applyBtn.getStyleClass().add("job-apply-dark-btn");
        if (alreadyApplied) {
            applyBtn.setDisable(true);
        }
        applyBtn.setOnAction(e -> {
            handleJobApply(job);
            appliedJobIds.add(job.getId());
            applyBtn.setText("Applied \u2713");
            applyBtn.setDisable(true);
            refreshListings();
        });

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("outline-button");
        closeBtn.setOnAction(e -> dialog.close());

        footer.getChildren().addAll(applyBtn, closeBtn);

        body.getChildren().addAll(0, List.of(companyLabel, categoryLabel, sep1));
        body.getChildren().addAll(sep2, footer);

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(500);

        root.getChildren().addAll(header, scroll);

        Scene scene = new Scene(root, 650, 580);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void addDetailRow(VBox container, String label, String value) {
        VBox row = new VBox(4);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label val = new Label(value != null ? value : "Not specified");
        val.setWrapText(true);
        val.setStyle("-fx-text-fill: #333; -fx-font-size: 13px;");
        row.getChildren().addAll(lbl, val);
        container.getChildren().add(row);
    }

    private void updateMetrics(int favCount) {
        availableJobsVal.setText(String.valueOf(allJobs.size()));
        appsVal.setText(String.valueOf(appliedJobIds.size()));
        bookmarkedVal.setText(String.valueOf(favCount));
    }

    private void updateJobListing(List<JobOpportunity> jobs) {
        LinkedHashMap<String, JobOpportunity> merged = new LinkedHashMap<>();
        for (JobOpportunity job : jobs) {
            if (job != null && job.getId() != null) merged.put(job.getId(), job);
        }
        for (JobOpportunity job : allJobs) {
            if (job != null && job.getId() != null) merged.putIfAbsent(job.getId(), job);
        }
        allJobs = new ArrayList<>(merged.values());
        Platform.runLater(this::refreshListings);
    }

    private void handleJobApply(JobOpportunity job) {
        User currentUser = repository.getCurrentUser();
        if (currentUser == null) return;
        
        CompletableFuture.runAsync(() -> {
            repository.recordJobApplication(currentUser, job);
            repository.logStudentActivity(currentUser, "job_application_click", new JsonObject());
        });

        if (Desktop.isDesktopSupported() && job.getJobLink() != null) {
            try {
                Desktop.getDesktop().browse(new URI("https://in.indeed.com/viewjob?jk=" + job.getId()));
            } catch (Exception ex) {}
        }
    }

    private void setRefreshing(boolean refreshing) {
        Platform.runLater(() -> {
            loadingIndicator.setVisible(refreshing);
        });
    }
}
