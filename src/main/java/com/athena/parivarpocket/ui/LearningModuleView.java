package com.athena.parivarpocket.ui;

import com.athena.parivarpocket.service.DataRepository;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class LearningModuleView extends VBox {
    public LearningModuleView(DataRepository repository) {
        setSpacing(18);
        setPadding(new Insets(0, 0, 8, 0));
        List<LearningModule> modules = List.of(
                new LearningModule("Budgeting Basics", 100, 100, false),
                new LearningModule("Smart Saving Strategies", 75, 150, false),
                new LearningModule("Understanding Income", 50, 120, false),
                new LearningModule("Expense Management", 0, 180, false),
                new LearningModule("Investment Fundamentals", 0, 200, true),
                new LearningModule("Financial Planning", 0, 250, true)
        );
        getChildren().add(buildLibraryPanel(modules));
        getChildren().add(buildProgressPanel(modules));
    }

    private Panel buildLibraryPanel(List<LearningModule> modules) {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPadding(new Insets(8, 0, 0, 0));
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(33.33);
        column.setHalignment(HPos.LEFT);
        column.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(column, column, column);
        int columnIndex = 0;
        int rowIndex = 0;
        for (LearningModule module : modules) {
            VBox card = createModuleCard(module);
            grid.add(card, columnIndex, rowIndex);
            columnIndex++;
            if (columnIndex >= 3) {
                columnIndex = 0;
                rowIndex++;
            }
        }
        Panel panel = new Panel("Financial Learning Library", grid);
        panel.getStyleClass().add("learning-panel");
        return panel;
    }

    private VBox createModuleCard(LearningModule module) {
        Label icon = new Label("\uD83D\uDCD6");
        icon.getStyleClass().add("learning-card-icon");
        Label title = new Label(module.title);
        title.getStyleClass().add("learning-card-title");
        HBox titleRow = new HBox(6, icon, title);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        ProgressBar bar = new ProgressBar(module.progress / 100.0);
        bar.setPrefHeight(10);
        bar.setStyle("-fx-accent: #111111;");

        Label progress = new Label(module.progress + "% Complete");
        progress.getStyleClass().add("learning-card-progress");

        HBox footer = new HBox(14);
        footer.setAlignment(Pos.CENTER_LEFT);
        Label coins = new Label("₹" + module.coins + " Coins");
        coins.getStyleClass().add("learning-card-coins");
        Button action = new Button(module.locked ? "Locked" : module.progress >= 100 ? "Continue" : module.progress == 0 ? "Start" : "Continue");
        action.getStyleClass().add(module.locked ? "outline-button" : "primary-button");
        action.setDisable(module.locked);
        footer.getChildren().addAll(coins, action);

        VBox card = new VBox(12, titleRow, bar, progress, footer);
        card.setPadding(new Insets(18));
        card.getStyleClass().add("learning-card");
        card.setPrefHeight(160);
        card.setMinHeight(160);
        card.setMaxWidth(Double.MAX_VALUE);
        if (module.locked) {
            card.getStyleClass().add("learning-card-locked");
        }
        return card;
    }

    private Panel buildProgressPanel(List<LearningModule> modules) {
        int coursesStarted = (int) modules.stream().filter(m -> m.progress > 0).count();
        int coursesCompleted = (int) modules.stream().filter(m -> m.progress >= 100).count();
        int totalCoins = modules.stream().mapToInt(m -> m.coins).sum();

        HBox stats = new HBox(12,
                createStatCard("Courses Started", String.valueOf(coursesStarted)),
                createStatCard("Courses Completed", String.valueOf(coursesCompleted)),
                createStatCard("Total Coins Earned", String.valueOf(totalCoins))
        );
        stats.setAlignment(Pos.CENTER);
        Panel panel = new Panel("Your Progress", stats);
        panel.getStyleClass().add("learning-progress-panel");
        return panel;
    }

    private VBox createStatCard(String title, String value) {
        Label label = new Label(title);
        label.getStyleClass().add("learning-stat-title");
        Label val = new Label(value);
        val.getStyleClass().add("learning-stat-value");
        VBox box = new VBox(4, val, label);
        box.setPadding(new Insets(12));
        box.getStyleClass().add("learning-stat-card");
        return box;
    }

    private record LearningModule(String title, int progress, int coins, boolean locked) {
    }
}
