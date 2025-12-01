package com.athena.parivarpocket.ui;

import com.athena.parivarpocket.model.NotificationItem;
import com.athena.parivarpocket.service.OfflineSyncService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class NotificationsView extends VBox {
    public NotificationsView(List<NotificationItem> notifications, OfflineSyncService offlineSyncService) {
        setSpacing(18);
        setPadding(new Insets(0));
        getChildren().add(buildNotificationsPanel(notifications));
        getChildren().add(buildSyncPanel(offlineSyncService));
    }

    private Panel buildNotificationsPanel(List<NotificationItem> notifications) {
        Label countLabel = new Label(notifications.size() + " unread notifications");
        countLabel.getStyleClass().add("notification-count");
        Button markAll = new Button("Mark all as read");
        markAll.getStyleClass().add("outline-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, countLabel, spacer, markAll);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox list = new VBox(12);
        notifications.forEach(note -> list.getChildren().add(createNotificationCard(note)));

        VBox content = new VBox(10, header, list);
        content.setPadding(new Insets(4, 0, 0, 0));
        Panel panel = new Panel("Alerts & Notifications", content);
        panel.getStyleClass().add("alerts-panel");
        return panel;
    }

    private Card createNotificationCard(NotificationItem note) {
        Label icon = new Label(getIcon(note.getSeverity()));
        icon.getStyleClass().add("notification-icon");
        Label title = new Label(note.getTitle());
        title.getStyleClass().add("notification-card-title");
        Label desc = new Label(note.getDescription());
        desc.getStyleClass().add("notification-card-body");
        desc.setWrapText(true);
        Label time = new Label(note.getDate().toString());
        time.getStyleClass().add("notification-card-time");

        Region dot = new Region();
        dot.getStyleClass().add("notification-dot");

        VBox text = new VBox(4, title, desc, time);
        HBox row = new HBox(12, icon, text, dot);
        HBox.setHgrow(text, Priority.ALWAYS);
        row.setAlignment(Pos.CENTER_LEFT);

        Card card = new Card();
        card.getStyleClass().add("notification-card");
        card.getChildren().add(row);
        return card;
    }

    private Panel buildSyncPanel(OfflineSyncService offlineSyncService) {
        Label status = new Label(syncStatus(offlineSyncService));
        Button syncBtn = new Button("Sync now");
        syncBtn.getStyleClass().add("primary-button");
        syncBtn.setOnAction(e -> {
            int synced = offlineSyncService.syncNow();
            status.setText("Synced " + synced + " pending items.");
        });
        HBox row = new HBox(10, status, syncBtn);
        row.setPadding(new Insets(8, 0, 0, 0));
        Panel panel = new Panel("Sync status", row);
        panel.getStyleClass().add("alerts-panel");
        return panel;
    }

    private String getIcon(String severity) {
        return switch (severity) {
            case "warning" -> "\u26A0";
            case "success" -> "\u2714";
            case "error" -> "\u26D4";
            default -> "\u2139";
        };
    }

    private String syncStatus(OfflineSyncService offlineSyncService) {
        if (offlineSyncService.getPendingOperations().isEmpty()) {
            return "No pending sync. Offline mode: " + (offlineSyncService.isOfflineMode() ? "ON" : "OFF");
        }
        return offlineSyncService.getPendingOperations().size() + " item(s) queued for sync.";
    }
}
