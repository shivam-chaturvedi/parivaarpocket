package com.athena.parivarpocket.ui;

import com.athena.parivarpocket.App;
import com.athena.parivarpocket.model.MainTab;
import com.athena.parivarpocket.model.User;
import com.athena.parivarpocket.model.UserRole;
import com.athena.parivarpocket.service.OfflineSyncService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SidebarView extends VBox {
    public SidebarView(User user,
                       MainTab activeTab,
                       Consumer<MainTab> onTabSelected,
                       Runnable onLogout,
                       OfflineSyncService offlineSyncService) {
        getStyleClass().add("sidebar");
        setPrefWidth(220);
        setPadding(new Insets(24, 16, 24, 16));
        setSpacing(28);
        setAlignment(Pos.TOP_LEFT);

        ImageView logoImage = sidebarIcon("logo.png", 32);
        Label brandTitle = new Label("PP");
        brandTitle.getStyleClass().add("sidebar-brand-title");
        Label brandSubtitle = new Label("Parivaar Pocket");
        brandSubtitle.getStyleClass().add("sidebar-brand-subtitle");
        VBox brandText = new VBox(2, brandTitle, brandSubtitle);
        brandText.setAlignment(Pos.CENTER_LEFT);
        HBox brandContent = new HBox(10, logoImage, brandText);
        brandContent.setAlignment(Pos.CENTER_LEFT);
        Button brandButton = new Button();
        brandButton.setGraphic(brandContent);
        brandButton.setMaxWidth(Double.MAX_VALUE);
        brandButton.getStyleClass().addAll("sidebar-item", "sidebar-brand");
        brandButton.setOnAction(e -> onTabSelected.accept(MainTab.DASHBOARD));
        getChildren().add(brandButton);

        VBox buttons = new VBox(8);
        buttons.setAlignment(Pos.TOP_LEFT);
        buttons.setFillWidth(true);

        Map<MainTab, String> tabLabels = new LinkedHashMap<>();
        tabLabels.put(MainTab.LEARNING, "Learning");
        tabLabels.put(MainTab.WORK, "Work");
        tabLabels.put(MainTab.WALLET, "Wallet");
        tabLabels.put(MainTab.NOTIFICATIONS, "Alerts");

        tabLabels.forEach((tab, label) -> buttons.getChildren().add(createTabButton(tab, label, activeTab, onTabSelected)));
        getChildren().add(buttons);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        getChildren().add(spacer);

        Button logoutBtn = createSidebarAction("logout.png", "Logout", onLogout);
        Label roleLabel = new Label(user.getRole() == UserRole.STUDENT ? "Student Profile" : "Educator Profile");
        roleLabel.getStyleClass().add("sidebar-role");
        int pending = offlineSyncService.getPendingOperations().size();
        String syncStatus = pending == 0 ? "All synced" : pending + " pending sync";
        Label syncLabel = new Label(syncStatus);
        syncLabel.getStyleClass().add("sidebar-sync");

        VBox footer = new VBox(6, logoutBtn, roleLabel, syncLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("sidebar-footer");
        getChildren().add(footer);
    }

    private Button createTabButton(MainTab tab, String label, MainTab activeTab, Consumer<MainTab> onTabSelected) {
        ImageView icon = switch (tab) {
            case LEARNING -> sidebarIcon("learning.png", 24);
            case WORK -> sidebarIcon("work.png", 24);
            case WALLET -> sidebarIcon("wallet.png", 24);
            case NOTIFICATIONS -> sidebarIcon("alerts.png", 24);
            case DASHBOARD -> sidebarIcon("learning.png", 24);
        };
        Label text = new Label(label);
        text.getStyleClass().add("sidebar-label");
        HBox content = new HBox(12, icon, text);
        content.setAlignment(Pos.CENTER_LEFT);

        Button button = new Button();
        button.setGraphic(content);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("sidebar-item");
        if (tab == activeTab) {
            button.getStyleClass().add("sidebar-item-active");
        }
        button.setOnAction(e -> onTabSelected.accept(tab));
        return button;
    }

    private Button createSidebarAction(String iconName, String labelText, Runnable action) {
        ImageView icon = sidebarIcon(iconName, 18);
        Label text = new Label(labelText);
        text.getStyleClass().add("sidebar-label");
        HBox content = new HBox(10, icon, text);
        content.setAlignment(Pos.CENTER_LEFT);
        Button button = new Button();
        button.setGraphic(content);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().addAll("sidebar-item", "sidebar-logout");
        button.setOnAction(e -> action.run());
        return button;
    }

    private ImageView sidebarIcon(String fileName, int size) {
        Image image = new Image(App.class.getResource("/sidebar/" + fileName).toExternalForm(), size, size, true, true);
        ImageView view = new ImageView(image);
        return view;
    }
}
