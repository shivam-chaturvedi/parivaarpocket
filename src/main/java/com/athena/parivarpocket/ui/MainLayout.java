package com.athena.parivarpocket.ui;

import com.athena.parivarpocket.model.MainTab;
import com.athena.parivarpocket.model.User;
import com.athena.parivarpocket.model.UserRole;
import com.athena.parivarpocket.service.DataRepository;
import com.athena.parivarpocket.service.OfflineSyncService;
import com.athena.parivarpocket.service.ReportService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.concurrent.CompletableFuture;

public class MainLayout {
    private final BorderPane root = new BorderPane();
    private final User user;
    private final DataRepository repository;
    private final OfflineSyncService offlineSyncService;
    private final ReportService reportService;
    private MainTab activeTab = MainTab.DASHBOARD;
    private final Runnable onLogout;
    private final StackPane centerWrapper = new StackPane();
    private final ProgressIndicator loader = new ProgressIndicator();
    private boolean dataLoaded = false;

    public MainLayout(User user,
                      DataRepository repository,
                      OfflineSyncService offlineSyncService,
                      ReportService reportService,
                      Runnable onLogout) {
        this.user = user;
        this.repository = repository;
        this.offlineSyncService = offlineSyncService;
        this.reportService = reportService;
        this.onLogout = onLogout;
        loader.setMaxSize(48, 48);
        centerWrapper.getChildren().add(loader);
        root.setCenter(centerWrapper);
        render();
        prefetchData(user);
    }

    public BorderPane getView() {
        return root;
    }

    private void render() {
        SidebarView sidebar = new SidebarView(user, activeTab, this::setActiveTab, onLogout, offlineSyncService);
        root.setLeft(sidebar);
        root.setTop(null);
        root.setCenter(centerWrapper);
        refreshContent();
    }

    private Node buildScrollableContent(MainTab tab) {
        String title = titleFor(tab);
        String desc = descriptionFor(tab);
        VBox container = new VBox(18, new PageHeader(title, desc), contentForTab(tab));
        container.setPadding(new Insets(16, 24, 24, 24));
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("content-scroll");
        return scrollPane;
    }

    private String titleFor(MainTab tab) {
        return switch (tab) {
            case DASHBOARD -> user.getRole() == UserRole.STUDENT ? "Student Dashboard" : "Educator Dashboard";
            case LEARNING -> "Financial learning";
            case WORK -> "Employment opportunities";
            case WALLET -> "Budget management";
            case NOTIFICATIONS -> "Alerts & reminders";
        };
    }

    private String descriptionFor(MainTab tab) {
        return switch (tab) {
            case DASHBOARD -> "Overview of your progress and activities";
            case LEARNING -> "Interactive lessons, quizzes, and ParivaarCoins";
            case WORK -> "Verified, safe local jobs updated every 24 hours";
            case WALLET -> "Record income, expenses, savings, and get alerts";
            case NOTIFICATIONS -> "Alerts from educators, wallet, and job board";
        };
    }

    private void setActiveTab(MainTab tab) {
        if (this.activeTab == tab) {
            return;
        }
        this.activeTab = tab;
        render();
    }

    private Node contentForTab(MainTab tab) {
        return switch (tab) {
            case DASHBOARD -> user.getRole() == UserRole.STUDENT
                    ? new StudentDashboardView(user, repository)
                    : new EducatorDashboardView(repository, reportService);
            case LEARNING -> new LearningModuleView(repository);
            case WORK -> new WorkModuleView(repository);
            case WALLET -> new WalletModuleView(repository, user, offlineSyncService);
            case NOTIFICATIONS -> new NotificationsView(repository.getNotifications(user), offlineSyncService);
        };
    }

    private void refreshContent() {
        if (!dataLoaded) {
            showLoading();
            return;
        }
        showContent(buildScrollableContent(activeTab));
    }

    private void showLoading() {
        centerWrapper.getChildren().setAll(loader);
    }

    private void showContent(Node content) {
        centerWrapper.getChildren().setAll(content);
    }

    private void prefetchData(User user) {
        CompletableFuture.runAsync(() -> {
            repository.prefetchAll(user);
            Platform.runLater(() -> {
                dataLoaded = true;
                render();
            });
        });
    }
}
