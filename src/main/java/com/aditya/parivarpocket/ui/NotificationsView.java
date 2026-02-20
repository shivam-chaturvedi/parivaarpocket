package com.aditya.parivarpocket.ui;

import com.aditya.parivarpocket.model.Alert;
import com.aditya.parivarpocket.service.DataRepository;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class NotificationsView extends VBox {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final DataRepository repository;
    private final Label alertCountLabel = new Label("Loading alerts...");
    private final VBox cardsContainer = new VBox(12);

    public NotificationsView(DataRepository repository) {
        this.repository = repository;
        setSpacing(18);
        setPadding(new Insets(0));
        getChildren().add(buildAlertsPanel());
        loadAlertEntries();
    }

    private Node buildAlertsPanel() {
        Label sectionTitle = new Label("Budget & Wallet Alerts");
        sectionTitle.getStyleClass().add("section-header");
        alertCountLabel.getStyleClass().add("notification-count");
        HBox header = new HBox(10, sectionTitle, alertCountLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        cardsContainer.setPadding(new Insets(0));
        cardsContainer.setFillWidth(true);

        VBox content = new VBox(12, header, cardsContainer);
        content.setPadding(new Insets(16));
        content.getStyleClass().add("alerts-panel");
        return content;
    }

    private void loadAlertEntries() {
        cardsContainer.getChildren().setAll(new Label("Loading alerts..."));
        CompletableFuture.supplyAsync(() -> {
            try {
                return repository.fetchAllAlerts();
            } catch (Exception e) {
                System.err.println("[NotificationsView] Unable to load alerts: " + e.getMessage());
                return Collections.<Alert>emptyList();
            }
        }).thenAccept(this::renderAlerts);
    }

    private void renderAlerts(List<Alert> alerts) {
        Platform.runLater(() -> {
            cardsContainer.getChildren().clear();
            int size = alerts.size();
            alertCountLabel.setText(size + " alert" + (size == 1 ? "" : "s"));
            if (alerts.isEmpty()) {
                Label none = new Label("No alerts recorded yet.");
                none.getStyleClass().add("alert-empty");
                cardsContainer.getChildren().add(none);
                return;
            }
            for (Alert alert : alerts) {
                cardsContainer.getChildren().add(createAlertCard(alert));
            }
        });
    }

    private Node createAlertCard(Alert alert) {
        HBox card = new HBox(12);
        card.getStyleClass().addAll("alert-card", severityClass(alert.getSeverity()));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(Double.MAX_VALUE);
        card.setPadding(new Insets(14));

        Label icon = new Label(severityIcon(alert.getSeverity()));
        icon.getStyleClass().add("alert-card-icon");

        VBox text = new VBox(4);
        Label title = new Label(alert.getMessage());
        title.getStyleClass().add("alert-card-title");
        Label category = new Label(alert.getCategory());
        category.getStyleClass().add("alert-card-category");
        Label time = new Label(relativeTime(alert.getCreatedAt()));
        time.getStyleClass().add("alert-card-time");
        text.getChildren().addAll(title, category, time);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        javafx.scene.control.Button readBtn = new javafx.scene.control.Button("✓ Mark Read");
        readBtn.getStyleClass().add("outline-button");
        readBtn.setOnAction(e -> {
            CompletableFuture.runAsync(() -> repository.markAlertAsReadForEducator(repository.getCurrentUser(), alert.getId()))
                .thenRun(() -> Platform.runLater(() -> {
                    cardsContainer.getChildren().remove(card);
                    int remaining = cardsContainer.getChildren().size();
                    alertCountLabel.setText(remaining + " alert" + (remaining == 1 ? "" : "s"));
                    if (remaining == 0) {
                        Label none = new Label("No alerts recorded yet.");
                        none.getStyleClass().add("alert-empty");
                        cardsContainer.getChildren().add(none);
                    }
                }));
        });

        card.getChildren().addAll(icon, text, spacer, readBtn);
        return card;
    }

    private String severityIcon(String severity) {
        return switch (severity == null ? "" : severity.toLowerCase(Locale.ROOT)) {
            case "warning" -> "\u26A0";
            case "danger" -> "\u26D4";
            case "info" -> "\u2139";
            default -> "\u26A1";
        };
    }

    private String severityClass(String severity) {
        return "alert-card-" + (severity == null ? "info" : severity.toLowerCase(Locale.ROOT));
    }

    private String relativeTime(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "";
        }
        Duration duration = Duration.between(timestamp, LocalDateTime.now());
        if (duration.isNegative() || duration.isZero()) {
            return "Just now";
        }
        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        }
        long hours = duration.toHours();
        if (hours < 24) {
            return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        }
        long days = duration.toDays();
        return days + " day" + (days == 1 ? "" : "s") + " ago";
    }
}
