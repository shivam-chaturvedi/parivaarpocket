package com.aditya.parivarpocket.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class PageHeader extends HBox {
    public PageHeader(String title, String description) {
        this(title, description, null);
    }

    public PageHeader(String title, String description, Runnable onRefresh) {
        setPadding(new Insets(12, 0, 12, 0));
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("page-header");

        VBox textContainer = new VBox(6);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("page-header-title");
        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("page-header-description");
        textContainer.getChildren().addAll(titleLabel, descriptionLabel);

        getChildren().add(textContainer);

        if (onRefresh != null) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button refreshBtn = new Button(" \u21BB "); // Unicode refresh symbol
            refreshBtn.getStyleClass().add("refresh-icon-button");
            refreshBtn.setTooltip(new javafx.scene.control.Tooltip("Refresh data from server"));
            refreshBtn.setOnAction(e -> onRefresh.run());

            getChildren().addAll(spacer, refreshBtn);
        }
    }
}
