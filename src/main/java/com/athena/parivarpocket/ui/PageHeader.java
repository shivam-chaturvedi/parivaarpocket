package com.athena.parivarpocket.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class PageHeader extends VBox {
    public PageHeader(String title, String description) {
        setSpacing(6);
        setPadding(new Insets(12, 0, 12, 0));
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("page-header");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("page-header-title");
        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("page-header-description");

        getChildren().addAll(titleLabel, descriptionLabel);
    }
}
