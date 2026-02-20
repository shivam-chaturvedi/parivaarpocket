package com.aditya.parivarpocket.ui;

import javafx.geometry.Insets;
import javafx.scene.layout.VBox;

public class Card extends VBox {
    public Card() {
        getStyleClass().add("card");
        setPadding(new Insets(12));
        setSpacing(10);
    }
}
