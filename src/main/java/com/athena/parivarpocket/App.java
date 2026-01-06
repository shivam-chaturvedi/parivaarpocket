package com.athena.parivarpocket;

import com.athena.parivarpocket.model.User;
import com.athena.parivarpocket.service.*;
import com.athena.parivarpocket.ui.LoginView;
import com.athena.parivarpocket.ui.MainLayout;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class App extends Application {
    private final AuthService authService = new AuthService();
    private final LocalStoreService localStoreService = new LocalStoreService();
    private final DataRepository repository = new DataRepository(localStoreService);
    private final OfflineSyncService offlineSyncService = new OfflineSyncService();
    private StackPane root;

    @Override
    public void start(Stage stage) {
        root = new StackPane();
        Scene scene = new Scene(root, 1280, 820);
        String css = App.class.getResource("/styles.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("ParivaarPocket • JavaFX");
        stage.setScene(scene);
        stage.show();
        showLogin();
    }

    private void showLogin() {
        LoginView loginView = new LoginView(authService, this::onLogin);
        root.getChildren().setAll(loginView.getView());
    }

    private void onLogin(User user) {
        MainLayout layout = new MainLayout(
                user,
                repository,
                offlineSyncService,
                this::showLogin);
        root.getChildren().setAll(layout.getView());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
