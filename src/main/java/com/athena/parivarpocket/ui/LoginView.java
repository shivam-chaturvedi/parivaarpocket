package com.athena.parivarpocket.ui;

import com.athena.parivarpocket.App;
import com.athena.parivarpocket.model.User;
import com.athena.parivarpocket.model.UserRole;
import com.athena.parivarpocket.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.function.Consumer;

public class LoginView {
    private final BorderPane root = new BorderPane();
    private final AuthService authService;
    private boolean registering = false;

    public LoginView(AuthService authService, Consumer<User> onLogin) {
        this.authService = authService;
        root.setPadding(new Insets(40));

        Node form = buildForm(onLogin);
        Node left = buildIllustrationPanel();

        HBox card = new HBox(left, form);
        card.setSpacing(0);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("login-card");
        card.setPrefHeight(500);
        card.setMinHeight(500);
        card.setMaxHeight(500);
        card.setPrefWidth(920);
        card.setMinWidth(920);
        card.setMaxWidth(920);

        BorderPane.setAlignment(card, Pos.CENTER);
        root.setCenter(card);
    }

    public BorderPane getView() {
        return root;
    }

    private Node buildForm(Consumer<User> onLogin) {
        TextField emailField = new TextField();
        emailField.getStyleClass().add("login-input");
        emailField.setPromptText("student@parivaar.org");
        PasswordField passwordField = new PasswordField();
        passwordField.getStyleClass().add("login-input");
        passwordField.setPromptText("Password (min 6 characters)");

        ToggleGroup roleGroup = new ToggleGroup();
        ToggleButton studentBtn = new ToggleButton("Student");
        studentBtn.setToggleGroup(roleGroup);
        studentBtn.setUserData(UserRole.STUDENT);
        studentBtn.setSelected(true);
        studentBtn.getStyleClass().addAll("role-toggle", "role-toggle-active");

        ToggleButton educatorBtn = new ToggleButton("Educator");
        educatorBtn.setToggleGroup(roleGroup);
        educatorBtn.setUserData(UserRole.EDUCATOR);
        educatorBtn.getStyleClass().add("role-toggle");

        roleGroup.selectedToggleProperty().addListener((obs, old, toggle) -> {
            studentBtn.getStyleClass().remove("role-toggle-active");
            educatorBtn.getStyleClass().remove("role-toggle-active");
            if (toggle == studentBtn) {
                studentBtn.getStyleClass().add("role-toggle-active");
                emailField.setPromptText("student@parivaar.org");
            } else if (toggle == educatorBtn) {
                educatorBtn.getStyleClass().add("role-toggle-active");
                emailField.setPromptText("educator@gmail.com");
            }
        });

        HBox roleBox = new HBox(8, studentBtn, educatorBtn);
        roleBox.setAlignment(Pos.CENTER_LEFT);

        Label roleLabel = new Label("Role");
        roleLabel.setStyle("-fx-text-fill: #000000; -fx-font-weight: 600; -fx-font-size: 14px;");
        Label emailLabel = new Label("Email");
        emailLabel.setStyle("-fx-text-fill: #000000; -fx-font-weight: 600; -fx-font-size: 14px;");
        Label passwordLabel = new Label("Password");
        passwordLabel.setStyle("-fx-text-fill: #000000; -fx-font-weight: 600; -fx-font-size: 14px;");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("danger");
        errorLabel.setVisible(false);

        Button submitButton = new Button();
        submitButton.getStyleClass().add("primary-button");
        submitButton.setMaxWidth(Double.MAX_VALUE);

        Button switchButton = new Button();
        switchButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #000000;");
        switchButton.setOnAction(e -> {
            registering = !registering;
            submitButton.setText(registering ? "Create account" : "Sign in");
            switchButton.setText(registering ? "Back to sign in" : "New here? Create account");
            errorLabel.setVisible(false);
            updateRoleSelectionVisibility(roleLabel, roleBox);
        });

        submitButton.setText("Sign in");
        switchButton.setText("New here? Create account");

        ProgressIndicator loader = new ProgressIndicator();
        loader.setPrefSize(32, 32);
        loader.setVisible(false);
        loader.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        loader.getStyleClass().add("login-loader");
        HBox loaderRow = new HBox(loader);
        loaderRow.setAlignment(Pos.CENTER);
        loaderRow.setPadding(new Insets(4, 0, 0, 0));
        loaderRow.setVisible(false);
        loaderRow.setManaged(false);
        Consumer<Boolean> toggleLoading = isLoading -> {
            submitButton.setDisable(isLoading);
            switchButton.setDisable(isLoading);
            studentBtn.setDisable(isLoading);
            educatorBtn.setDisable(isLoading);
            emailField.setDisable(isLoading);
            passwordField.setDisable(isLoading);
            loaderRow.setVisible(isLoading);
            loaderRow.setManaged(isLoading);
        };

        submitButton.setOnAction(e -> {
            errorLabel.setVisible(false);
            toggleLoading.accept(true);
            String email = emailField.getText();
            String password = passwordField.getText();
            boolean targetRegistering = registering;
            Task<User> authTask = new Task<>() {
                @Override
                protected User call() {
                    if (targetRegistering) {
                        return authService.register(email, password, (UserRole) roleGroup.getSelectedToggle().getUserData());
                    }
                    return authService.login(email, password);
                }
            };
            authTask.setOnSucceeded(event -> {
                toggleLoading.accept(false);
                onLogin.accept(authTask.getValue());
            });
            authTask.setOnFailed(event -> {
                toggleLoading.accept(false);
                Throwable cause = authTask.getException();
                String message = cause == null ? "Authentication failed." : cause.getMessage();
                errorLabel.setText(message);
                errorLabel.setVisible(true);
                showAlert(message);
            });
            new Thread(authTask).start();
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(roleLabel, 0, 0);
        grid.add(roleBox, 1, 0);
        grid.add(emailLabel, 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(passwordLabel, 0, 2);
        grid.add(passwordField, 1, 2);

        Label formTitle = new Label("Sign In");
        formTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: #000000;");

        VBox fieldsBox = new VBox(12, grid);
        fieldsBox.getStyleClass().add("login-fields-box");

        VBox formBox = new VBox(24,
                formTitle,
                fieldsBox,
                submitButton,
                switchButton,
                errorLabel,
                loaderRow);
        formBox.setPadding(new Insets(40, 40, 40, 40));
        formBox.setMaxWidth(460);
        formBox.setAlignment(Pos.CENTER);

        updateRoleSelectionVisibility(roleLabel, roleBox);
        Panel panel = new Panel(null, formBox);
        panel.setPrefWidth(500);
        panel.setMinWidth(500);
        panel.setMaxWidth(500);
        panel.setPrefHeight(500);
        panel.setMinHeight(500);
        panel.setMaxHeight(500);
        return panel;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Check your inputs");
        alert.showAndWait();
    }

    private void updateRoleSelectionVisibility(Label roleLabel, HBox roleBox) {
        boolean visible = registering;
        roleLabel.setVisible(visible);
        roleLabel.setManaged(visible);
        roleBox.setVisible(visible);
        roleBox.setManaged(visible);
    }

    private Node buildIllustrationPanel() {
        Image image = new Image(App.class.getResource("/login.png").toExternalForm(), 200, 200, true, true);
        ImageView imageView = new ImageView(image);

        Label title = new Label("ParivaarPocket");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #000000;");
        Label subtitle = new Label("Financial Empowerment for Students");
        subtitle.setStyle("-fx-text-fill: #000000;");

        VBox box = new VBox(16, imageView, title, subtitle);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        box.setPrefWidth(420);
        box.setPrefHeight(500);
        box.setMinHeight(500);
        box.setMaxHeight(500);
        box.getStyleClass().add("login-left");
        return box;
    }
}
