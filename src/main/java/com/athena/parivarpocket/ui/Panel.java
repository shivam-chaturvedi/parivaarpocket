package com.athena.parivarpocket.ui;

import javafx.scene.Node;
import javafx.scene.control.Label;

public class Panel extends Card {
    public Panel(String title, Node content) {
        super();
        getStyleClass().add("panel");
        if (title != null && !title.isBlank()) {
            Label header = new Label(title);
            header.getStyleClass().add("panel-header");
            header.setMaxWidth(Double.MAX_VALUE);
            getChildren().add(header);
        }
        getChildren().add(content);
    }
}
