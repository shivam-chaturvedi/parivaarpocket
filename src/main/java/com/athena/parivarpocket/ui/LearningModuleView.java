package com.athena.parivarpocket.ui;

import com.athena.parivarpocket.model.Lesson;
import com.athena.parivarpocket.model.QuizDefinition;
import com.athena.parivarpocket.model.QuizQuestion;
import com.athena.parivarpocket.model.QuizAttempt;
import com.athena.parivarpocket.model.User;
import com.athena.parivarpocket.service.DataRepository;
import com.google.gson.JsonObject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LearningModuleView extends VBox {
    private final DataRepository repository;

    public LearningModuleView(DataRepository repository) {
        this.repository = repository;
        setSpacing(18);
        setPadding(new Insets(0, 0, 8, 0));
        refreshContent();
    }

    private void refreshContent() {
        List<Lesson> lessons = repository.getLessons();
        if (lessons.isEmpty()) {
            Label placeholder = new Label("Learning content is syncing. Please wait while we load lessons from Supabase.");
            placeholder.setWrapText(true);
            placeholder.setStyle("-fx-text-fill: #5b5b5b;");
            placeholder.setPadding(new Insets(30));
            Panel panel = new Panel("Learning Library", placeholder);
            panel.getStyleClass().add("learning-panel");
            getChildren().setAll(panel, new Panel("Learning Snapshot", new Label("Progress will appear here once lessons load.")));
            return;
        }
        Panel libraryPanel = buildLibraryPanel(lessons);
        Panel progressPanel = buildProgressPanel(lessons);
        getChildren().setAll(libraryPanel, progressPanel);
    }

    private Panel buildLibraryPanel(List<Lesson> lessons) {
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(18);
        grid.setPadding(new Insets(8, 0, 0, 0));
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(50);
        column.setHgrow(Priority.ALWAYS);
        column.setHalignment(HPos.LEFT);
        grid.getColumnConstraints().addAll(column, column);

        int columnIndex = 0;
        int rowIndex = 0;
        for (Lesson lesson : lessons) {
            VBox card = createLessonCard(lesson);
            grid.add(card, columnIndex, rowIndex);
            GridPane.setFillWidth(card, true);
            columnIndex++;
            if (columnIndex >= 2) {
                columnIndex = 0;
                rowIndex++;
            }
        }
        Panel panel = new Panel("Learning Library", grid);
        panel.getStyleClass().add("learning-panel");
        return panel;
    }

    private VBox createLessonCard(Lesson lesson) {
        boolean completed = repository.isLessonCompleted(lesson.getId());
        Label title = new Label(lesson.getTitle());
        title.getStyleClass().add("learning-card-title");

        Label difficulty = new Label(lesson.getDifficulty());
        difficulty.getStyleClass().add("learning-card-difficulty");
        HBox titleRow = new HBox(10, title, difficulty);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);

        Label description = new Label(lesson.getDescription());
        description.setWrapText(true);
        description.setMaxWidth(Double.MAX_VALUE);
        description.getStyleClass().add("learning-card-description");

        ProgressBar progressBar = null;
        Label progressMeta = null;
        // keep disabled for now as we don't want to show partial progress
        if (lesson.getQuizzesTotal() > 0 && false) {
            double ratio = Math.min(1, (double) lesson.getQuizzesCompleted() / lesson.getQuizzesTotal());
            progressBar = new ProgressBar(ratio);
            progressBar.setPrefHeight(8);
            progressBar.setStyle("-fx-accent: #111111;");
            progressMeta = new Label(lesson.getQuizzesCompleted() + " / " + lesson.getQuizzesTotal() + " quizzes completed");
            progressMeta.getStyleClass().add("learning-card-progress");
        }

        Label statusLabel = new Label(completed ? "Module Completed" : "Not completed yet");
        statusLabel.getStyleClass().add(completed ? "learning-status-complete" : "learning-status-pending");

        Button viewCourse = new Button("View Course");
        viewCourse.getStyleClass().add("primary-button");
        viewCourse.setDisable(lesson.getCourseUrl() == null || lesson.getCourseUrl().isBlank());
        viewCourse.setMaxWidth(Double.MAX_VALUE);
        viewCourse.setOnAction(event -> openCourseLink(lesson.getCourseUrl()));

        VBox quizSection = buildQuizSection(lesson, completed);
        List<Node> cardChildren = new ArrayList<>(List.of(titleRow, description));
        if (progressBar != null && progressMeta != null) {
            cardChildren.add(progressBar);
            cardChildren.add(progressMeta);
        }
        cardChildren.add(statusLabel);
        cardChildren.add(quizSection);
        cardChildren.add(viewCourse);
        VBox card = new VBox(10, cardChildren.toArray(new Node[0]));
        card.setPadding(new Insets(18));
        card.getStyleClass().add("learning-card");
        card.setPrefHeight(260);
        card.setMinHeight(260);
        card.setMaxWidth(Double.MAX_VALUE);
        if (completed) {
            card.getStyleClass().add("learning-card-complete");
        }
        return card;
    }

    private VBox buildQuizSection(Lesson lesson, boolean completed) {
        List<QuizDefinition> quizzes = repository.getQuizzesForLesson(lesson.getId());
        if (quizzes.isEmpty()) {
            Label hint = new Label("No quizzes attached yet. Learn the course to unlock assessments.");
            hint.getStyleClass().add("learning-quiet");
            return new VBox(hint);
        }
        VBox quizBox = new VBox(6);
        Label header = new Label("Quizzes");
        header.getStyleClass().add("learning-quiz-header");
        quizBox.getChildren().add(header);
        for (QuizDefinition quiz : quizzes) {
            HBox row = new HBox(16);
            Label quizTitle = new Label(quiz.getTitle() + " • " + quiz.getDifficulty());
            quizTitle.getStyleClass().add("learning-quiz-title");
            Button takeQuiz = new Button("Take Quiz");
            takeQuiz.getStyleClass().add("outline-button");
            takeQuiz.setDisable(completed);
            takeQuiz.setOnAction(event -> showQuizDialog(lesson, quiz));
            row.getChildren().addAll(quizTitle, takeQuiz);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(quizTitle, Priority.ALWAYS);
            quizBox.getChildren().add(row);
        }
        return quizBox;
    }

    private Panel buildProgressPanel(List<Lesson> lessons) {
        int inProgress = (int) lessons.stream()
                .filter(l -> l.getProgressPercent() > 0 && l.getProgressPercent() < 100)
                .count();
        int completed = (int) lessons.stream()
                .filter(l -> repository.isLessonCompleted(l.getId()))
                .count();
        double average = lessons.isEmpty() ? 0 : lessons.stream()
                .mapToInt(Lesson::getProgressPercent)
                .average()
                .orElse(0);

        HBox stats = new HBox(14,
                createStatCard("In-progress", String.valueOf(inProgress)),
                createStatCard("Completed", String.valueOf(completed)),
                createStatCard("Avg completion", Math.round(average) + "%")
        );
        stats.setAlignment(Pos.CENTER);
        Panel panel = new Panel("Learning Snapshot", stats);
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

    private void openCourseLink(String courseUrl) {
        if (courseUrl == null || courseUrl.isBlank() || !Desktop.isDesktopSupported()) {
            return;
        }

        // Log course view activity
        User user = repository.getCurrentUser();
        if (user != null) {
            JsonObject activityData = new JsonObject();
            activityData.addProperty("course_url", courseUrl);
            activityData.addProperty("source", "learning_module");
            repository.logStudentActivity(user, "course_view", activityData);
        }

        try {
            Desktop.getDesktop().browse(new URI(courseUrl));
        } catch (IOException | URISyntaxException e) {
            System.err.println("Unable to open course link: " + courseUrl);
        }
    }

    private void showQuizDialog(Lesson lesson, QuizDefinition quiz) {
        User user = repository.getCurrentUser();
        if (user == null) {
            showFeedback("Quiz unavailable", "Please sign in to take quizzes.");
            return;
        }
        List<QuizQuestion> questions = repository.getQuestionsForQuiz(quiz.getId());
        if (questions.isEmpty()) {
            showFeedback("Quiz is empty", "We could not find questions for this quiz yet.");
            return;
        }

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Quiz • " + quiz.getTitle());

        Label questionLabel = new Label();
        questionLabel.getStyleClass().add("quiz-question-title");
        questionLabel.setWrapText(true);
        questionLabel.setMaxWidth(520);

        VBox optionsBox = new VBox(8);
        optionsBox.setPrefWidth(520);

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("outline-button");
        cancelButton.setPrefSize(120, 40);
        cancelButton.setOnAction(e -> dialog.close());

        Button nextButton = new Button("Next");
        nextButton.getStyleClass().add("primary-button");
        nextButton.setPrefSize(140, 40);
        nextButton.setDisable(true);

        HBox actions = new HBox(10, cancelButton, nextButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("quiz-actions");

        VBox body = new VBox(18, questionLabel, optionsBox, actions);
        body.setPadding(new Insets(20));
        body.getStyleClass().add("quiz-dialog-body");

        Scene scene = new Scene(body, 560, 320);
        dialog.setScene(scene);

        List<Integer> responses = new ArrayList<>(Collections.nCopies(questions.size(), -1));
        final int[] currentIndex = {0};
        ObjectProperty<ToggleGroup> activeGroup = new SimpleObjectProperty<>();

        Runnable updateQuestion = () -> {
            QuizQuestion question = questions.get(currentIndex[0]);
            questionLabel.setText((currentIndex[0] + 1) + ". " + question.getQuestion());
            optionsBox.getChildren().clear();
            ToggleGroup toggleGroup = new ToggleGroup();
            for (int i = 0; i < question.getOptions().size(); i++) {
                RadioButton option = new RadioButton(question.getOptions().get(i));
                option.getStyleClass().add("quiz-option");
                option.setWrapText(true);
                option.setMaxWidth(Double.MAX_VALUE);
                option.setToggleGroup(toggleGroup);
                option.setUserData(i);
                optionsBox.getChildren().add(option);
            }
            toggleGroup.selectedToggleProperty().addListener((obs, old, value) -> nextButton.setDisable(value == null));
            activeGroup.set(toggleGroup);
            nextButton.setText(currentIndex[0] == questions.size() - 1 ? "Submit" : "Next");
            nextButton.setDisable(toggleGroup.getSelectedToggle() == null);
        };

        updateQuestion.run();

        nextButton.setOnAction(e -> {
            ToggleGroup group = activeGroup.get();
            if (group == null || group.getSelectedToggle() == null) {
                return;
            }
            int selected = (Integer) group.getSelectedToggle().getUserData();
            responses.set(currentIndex[0], selected);
            if (currentIndex[0] < questions.size() - 1) {
                currentIndex[0]++;
                updateQuestion.run();
                return;
            }
            dialog.close();
            processQuizCompletion(lesson, quiz, questions, user, responses);
        });

        dialog.showAndWait();
    }

    private void processQuizCompletion(Lesson lesson,
                                       QuizDefinition quiz,
                                       List<QuizQuestion> questions,
                                       User user,
                                       List<Integer> responses) {
        int score = 0;
        int maxScore = 0;
        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion question = questions.get(i);
            int points = question.getPoints();
            maxScore += points;
            Integer selected = responses.get(i);
            if (selected != null && selected == question.getCorrectOption()) {
                score += points;
            }
        }
        int totalMarks = Math.max(maxScore, 1);
        int passingTarget = Math.min(Math.max(quiz.getPassingMarks(), 0), totalMarks);
        boolean passed = score >= passingTarget;
        QuizAttempt attempt = repository.recordQuizAttempt(
                user,
                quiz,
                score,
                totalMarks,
                passed,
                new ArrayList<>(responses));
        
        int pointsAwarded = 0;
        if (passed && attempt != null) {
            repository.saveLessonCompletion(user, lesson, attempt);
            // Award a flat 100 ParivaarCoins for lesson completion
            pointsAwarded = 100;
            String reason = "Completed Lesson: " + lesson.getTitle();
            repository.awardParivaarPoints(user, pointsAwarded, reason);
        }
        
        String pointMsg = pointsAwarded > 0 ? String.format(" and earned %d ParivaarCoins!", pointsAwarded) : ".";
        String message = String.format("You scored %d / %d. %s%s",
                score,
                totalMarks,
                passed ? "Module marked complete" : "Review the course material and try again",
                passed ? pointMsg : "");
        showFeedback(passed ? "Quiz passed" : "Quiz submitted", message);
        JsonObject activityData = new JsonObject();
        activityData.addProperty("lesson_id", lesson.getId());
        activityData.addProperty("lesson_title", lesson.getTitle());
        activityData.addProperty("quiz_id", quiz.getId());
        activityData.addProperty("quiz_title", quiz.getTitle());
        activityData.addProperty("score", score);
        activityData.addProperty("max_score", totalMarks);
        activityData.addProperty("passed", passed);
        if (attempt != null) {
            activityData.addProperty("quiz_attempt_id", attempt.getId());
        }
        repository.logStudentActivity(user, "quiz_attempt", activityData);
        refreshContent();
    }

    private void showFeedback(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
