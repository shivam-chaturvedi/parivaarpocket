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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
        setSpacing(24);
        setPadding(new Insets(0, 0, 24, 0));
        refreshContent();
    }

    private void refreshContent() {
        getChildren().clear();
        
        List<Lesson> lessons = repository.getLessons();
        if (lessons.isEmpty()) {
            getChildren().add(new Label("Loading financial learning modules..."));
            return;
        }

        // Library Section
        VBox libraryContainer = new VBox();
        libraryContainer.getStyleClass().add("learning-container");
        
        HBox libHeader = new HBox(new Label("Financial Learning Library"));
        libHeader.getStyleClass().add("learning-section-header");
        
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(24));
        
        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(33.33);
        grid.getColumnConstraints().addAll(col, col, col);

        int row = 0;
        int column = 0;
        for (Lesson lesson : lessons) {
            grid.add(createModuleCard(lesson), column, row);
            column++;
            if (column >= 3) {
                column = 0;
                row++;
            }
        }
        
        libraryContainer.getChildren().addAll(libHeader, grid);
        getChildren().add(libraryContainer);

        // Progress Section
        VBox progressContainer = new VBox();
        progressContainer.getStyleClass().add("learning-container");
        
        HBox progressHeader = new HBox(new Label("Your Progress"));
        progressHeader.getStyleClass().add("learning-section-header");
        
        User user = repository.getCurrentUser();
        int coursesDone = (int) lessons.stream().filter(l -> repository.isLessonCompleted(l.getId())).count();
        
        // Fetch student progress for "live" stats
        int quizzesDone = 0;
        int parivaarCoins = 0;
        if (user != null) {
            com.athena.parivarpocket.model.StudentProgress progress = repository.getStudentProgress(user.getEmail());
            if (progress != null) {
                quizzesDone = progress.getQuizzesTaken();
                parivaarCoins = progress.getParivaarPoints();
            }
        }

        HBox statsGrid = new HBox(20);
        statsGrid.setPadding(new Insets(24));
        statsGrid.setAlignment(Pos.CENTER);
        
        statsGrid.getChildren().addAll(
            createMetricCard(String.valueOf(coursesDone), "Courses Done"),
            createMetricCard(String.valueOf(quizzesDone), "Quizzes Done"),
            createMetricCard(String.valueOf(parivaarCoins), "Total ParivaarCoins")
        );
        HBox.setHgrow(statsGrid.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(statsGrid.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(statsGrid.getChildren().get(2), Priority.ALWAYS);

        progressContainer.getChildren().addAll(progressHeader, statsGrid);
        getChildren().add(progressContainer);
    }

    private VBox createModuleCard(Lesson lesson) {
        VBox card = new VBox(16);
        card.getStyleClass().add("learning-module-card");
        
        Label title = new Label(lesson.getTitle());
        title.getStyleClass().add("learning-module-title");
        title.setWrapText(true);
        title.setMinHeight(50);
        
        // Progress bar logic
        boolean completed = repository.isLessonCompleted(lesson.getId());
        double progressVal = completed ? 1.0 : (lesson.getQuizzesTotal() > 0 ? (double)lesson.getQuizzesCompleted() / lesson.getQuizzesTotal() : 0);
        
        ProgressBar bar = new ProgressBar(progressVal);
        bar.getStyleClass().add("thick-progress-bar");
        bar.setMaxWidth(Double.MAX_VALUE);
        
        Label percent = new Label((int)(progressVal * 100) + "%");
        percent.getStyleClass().add("percent-label");
        
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        
        Button startBtn = new Button("Start");
        startBtn.getStyleClass().add("start-button");
        startBtn.setOnAction(e -> showModuleModal(lesson));
        
        HBox footer = new HBox(startBtn);
        footer.setAlignment(Pos.BOTTOM_RIGHT);
        
        card.getChildren().addAll(title, bar, percent, spacer, footer);
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox createMetricCard(String value, String label) {
        VBox card = new VBox(8);
        card.getStyleClass().add("learning-metric-card");
        Label val = new Label(value);
        val.getStyleClass().add("learning-metric-value");
        Label tit = new Label(label);
        tit.getStyleClass().add("learning-metric-title");
        card.getChildren().addAll(val, tit);
        return card;
    }

    private void showModuleModal(Lesson lesson) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(lesson.getTitle());

        VBox root = new VBox();
        
        // Modal Header
        HBox header = new HBox(new Label(lesson.getTitle()));
        header.getStyleClass().add("modal-header");
        
        // Modal Body
        VBox body = new VBox(24);
        body.getStyleClass().add("modal-body");
        body.setPrefWidth(800);

        Label overviewTitle = new Label("Module Overview");
        overviewTitle.getStyleClass().add("modal-section-title");

        VBox iconBox = new VBox();
        iconBox.getStyleClass().add("modal-icon-box");
        iconBox.setAlignment(Pos.CENTER);
        
        try {
            Image iconImg = new Image(getClass().getResourceAsStream("/financial-icons.png"));
            ImageView iv = new ImageView(iconImg);
            iv.setFitHeight(120);
            iv.setPreserveRatio(true);
            iconBox.getChildren().add(iv);
        } catch (Exception e) {
            iconBox.getChildren().add(new Label("[ Financial Icons ]"));
        }

        VBox contentBox = new VBox(16);
        contentBox.getStyleClass().add("modal-box");
        Label desc = new Label(lesson.getDescription());
        desc.getStyleClass().add("modal-content-text");
        desc.setWrapText(true);
        contentBox.getChildren().addAll(iconBox, desc);

        VBox topicsSection = new VBox(12);
        Label topicsTitle = new Label("Key Topics Covered:");
        topicsTitle.getStyleClass().add("modal-important-title");
        
        VBox topicsList = new VBox(8);
        String[] topics = {
            "Understanding core financial concepts",
            "Practical application techniques",
            "Real-world examples and case studies",
            "Best practices for success"
        };
        for (String t : topics) {
            Label item = new Label("• " + t);
            item.getStyleClass().add("modal-topic-item");
            topicsList.getChildren().add(item);
        }
        
        VBox topicsBox = new VBox(topicsList);
        topicsBox.getStyleClass().add("modal-box");
        topicsSection.getChildren().addAll(topicsTitle, topicsBox);

        VBox importantBox = new VBox(8);
        importantBox.getStyleClass().add("modal-important-box");
        Label impTitle = new Label("Important Points to Remember:");
        impTitle.getStyleClass().add("modal-important-title");
        Label impText = new Label("Always track your spending, set realistic goals, and review your progress regularly.");
        impText.setWrapText(true);
        importantBox.getChildren().addAll(impTitle, impText);

        // Footer with Finished, Quiz and Close buttons
        HBox modalFooter = new HBox(12);
        modalFooter.setAlignment(Pos.CENTER_RIGHT);
        
        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("outline-button");
        closeBtn.setOnAction(e -> stage.close());

        // TEST 18: "Finished" button — awards coins and shows congratulations message
        User user = repository.getCurrentUser();
        Button finishedBtn = new Button("Finished");
        finishedBtn.getStyleClass().add("primary-button");
        finishedBtn.setOnAction(e -> {
            stage.close();
            if (user != null && !repository.isLessonCompleted(lesson.getId())) {
                repository.awardParivaarPoints(user, 100, "Completed module: " + lesson.getTitle());
                JsonObject activityData = new JsonObject();
                activityData.addProperty("lesson_id", lesson.getId());
                activityData.addProperty("action", "module_finished");
                repository.logStudentActivity(user, "module_finished", activityData);
            }
            Alert congrats = new Alert(Alert.AlertType.INFORMATION);
            congrats.setHeaderText("Well Done!");
            congrats.setContentText("Fantastic work! You are an excellent learner.");
            congrats.showAndWait();
            refreshContent();
        });
        
        List<QuizDefinition> quizzes = repository.getQuizzesForLesson(lesson.getId());
        if (!quizzes.isEmpty()) {
            Button quizBtn = new Button("Take Quiz");
            quizBtn.getStyleClass().add("primary-button");
            quizBtn.setOnAction(e -> {
                stage.close();
                showQuizDialog(lesson, quizzes.get(0));
            });
            modalFooter.getChildren().add(quizBtn);
        }
        modalFooter.getChildren().addAll(finishedBtn, closeBtn);

        body.getChildren().addAll(overviewTitle, contentBox, topicsSection, importantBox, modalFooter);
        
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(600);
        
        root.getChildren().addAll(header, scroll);
        
        Scene scene = new Scene(root, 800, 700);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
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
        dialog.setTitle("Quiz Challenge");

        VBox root = new VBox();
        root.getStyleClass().add("quiz-container");

        // Header
        HBox header = new HBox(new Label("Quiz Challenge"));
        header.getStyleClass().add("modal-header");

        VBox content = new VBox(20);
        content.setPadding(new Insets(24));

        // Question Info, Difficulty label, and Coins info
        Label qCountLabel = new Label();
        qCountLabel.getStyleClass().add("quiz-question-count");
        Label coinRewardLabel = new Label("+50 Coins per correct answer");
        coinRewardLabel.getStyleClass().add("quiz-coins-info");

        // TEST 23: Difficulty label in top right corner
        Label difficultyLabel = new Label();
        difficultyLabel.getStyleClass().add("quiz-difficulty-label");
        difficultyLabel.setStyle("-fx-font-weight: bold; -fx-padding: 2 8; -fx-border-radius: 4; -fx-background-radius: 4;");
        
        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);
        HBox metaRow = new HBox(8, qCountLabel, coinRewardLabel, metaSpacer, difficultyLabel);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        ProgressBar bar = new ProgressBar(0);
        bar.getStyleClass().add("thick-progress-bar");
        bar.setMaxWidth(Double.MAX_VALUE);

        // Question Box
        VBox qBox = new VBox();
        qBox.getStyleClass().add("quiz-question-box");
        Label questionTxt = new Label();
        questionTxt.getStyleClass().add("quiz-question-txt");
        questionTxt.setWrapText(true);
        qBox.getChildren().add(questionTxt);

        VBox optionsList = new VBox(12);

        // Feedback Box
        VBox feedbackBox = new VBox(4);
        feedbackBox.getStyleClass().add("quiz-feedback-container");
        feedbackBox.setVisible(false);
        Label feedbackTitle = new Label();
        feedbackTitle.getStyleClass().add("quiz-feedback-title");
        feedbackTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        Label feedbackSub = new Label();
        feedbackSub.getStyleClass().add("quiz-feedback-sub");
        feedbackBox.getChildren().addAll(feedbackTitle, feedbackSub);

        // Footer Actions
        Button nextBtn = new Button("Next");
        nextBtn.getStyleClass().add("primary-button");
        nextBtn.setDisable(true);
        nextBtn.setPrefSize(120, 40);
        
        HBox footer = new HBox(nextBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);

        content.getChildren().addAll(metaRow, bar, qBox, optionsList, feedbackBox, footer);
        root.getChildren().addAll(header, content);

        Scene scene = new Scene(root, 700, 650);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialog.setScene(scene);

        List<Integer> responses = new ArrayList<>(Collections.nCopies(questions.size(), -1));
        final int[] currentIndex = {0};
        final int[] correctAnswers = {0};

        char[] labels = {'A', 'B', 'C', 'D'};

        Runnable updateQuestion = () -> {
            int idx = currentIndex[0];
            QuizQuestion q = questions.get(idx);
            qCountLabel.setText(String.format("Question %d of %d", idx + 1, questions.size()));
            bar.setProgress((double)(idx) / questions.size());
            questionTxt.setText(q.getQuestion());
            optionsList.getChildren().clear();
            feedbackBox.setVisible(false);
            nextBtn.setDisable(true);
            nextBtn.setText(idx == questions.size() - 1 ? "Finish" : "Next");

            // TEST 23: Assign difficulty based on question position
            int total = questions.size();
            String difficulty;
            String diffColor;
            if (total <= 1) {
                difficulty = "Easy";
                diffColor = "#4CAF50";
            } else if (idx < total / 3) {
                difficulty = "Easy";
                diffColor = "#4CAF50";
            } else if (idx < (total * 2) / 3) {
                difficulty = "Medium";
                diffColor = "#FF9800";
            } else {
                difficulty = "Hard";
                diffColor = "#f44336";
            }
            difficultyLabel.setText(difficulty);
            difficultyLabel.setStyle("-fx-font-weight: bold; -fx-padding: 2 10; -fx-border-radius: 4; "
                    + "-fx-background-radius: 4; -fx-background-color: " + diffColor + "; -fx-text-fill: white;");

            for (int i = 0; i < q.getOptions().size(); i++) {
                final int optIdx = i;
                HBox optContainer = new HBox(12);
                optContainer.getStyleClass().add("quiz-option-container");
                optContainer.setAlignment(Pos.CENTER_LEFT);
                optContainer.setPickOnBounds(true);
                optContainer.setMaxWidth(Double.MAX_VALUE);
                
                Label lbl = new Label(labels[i % 4] + ".");
                lbl.getStyleClass().add("quiz-option-label");
                lbl.setMouseTransparent(true);
                
                Label txt = new Label(q.getOptions().get(i));
                txt.getStyleClass().add("quiz-option-text");
                txt.setWrapText(true);
                txt.setMouseTransparent(true);
                
                optContainer.getChildren().addAll(lbl, txt);
                
                optContainer.setOnMouseClicked(e -> {
                    if (responses.get(currentIndex[0]) != -1) return; // Already answered
                    
                    responses.set(currentIndex[0], optIdx);
                    boolean correct = optIdx == q.getCorrectOption();
                    boolean rewarded = false;
                    
                    if (correct) {
                        correctAnswers[0]++;
                        // Award coins per question if not already rewarded
                        if (!repository.isQuestionRewarded(user, q.getId())) {
                            repository.awardParivaarPoints(user, 50, "Correct Quiz Answer: " + q.getQuestion());
                            repository.markQuestionRewarded(user, q.getId());
                            rewarded = true;
                        }
                    }
                    
                    // TEST 20-21: Green/red color feedback on selected option
                    if (correct) {
                        optContainer.setStyle("-fx-background-color: #4CAF50; -fx-background-radius: 8;");
                        lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                        txt.setStyle("-fx-text-fill: white;");
                    } else {
                        optContainer.setStyle("-fx-background-color: #f44336; -fx-background-radius: 8;");
                        lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                        txt.setStyle("-fx-text-fill: white;");
                        // Also highlight the correct answer in green
                        int correctIdx = q.getCorrectOption();
                        if (correctIdx >= 0 && correctIdx < optionsList.getChildren().size()) {
                            HBox correctContainer = (HBox) optionsList.getChildren().get(correctIdx);
                            correctContainer.setStyle("-fx-background-color: #4CAF50; -fx-background-radius: 8;");
                            for (Node child : correctContainer.getChildren()) {
                                child.setStyle("-fx-text-fill: white;");
                            }
                        }
                    }
                    
                    // Show feedback with specific messages per test 20-21
                    feedbackBox.setVisible(true);
                    if (correct) {
                        feedbackTitle.setText("Correct \u2713, 50 ParivaarCoins earned");
                        feedbackTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");
                        feedbackSub.setText("Great job! Keep going.");
                    } else {
                        String correctAnswerText = q.getOptions().get(q.getCorrectOption());
                        feedbackTitle.setText("Incorrect \u2717, the answer was: " + correctAnswerText);
                        feedbackTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #c62828;");
                        feedbackSub.setText("Review the material and try again next time.");
                    }
                    
                    nextBtn.setDisable(false);
                });
                
                optionsList.getChildren().add(optContainer);
            }
        };

        updateQuestion.run();

        nextBtn.setOnAction(e -> {
            if (currentIndex[0] < questions.size() - 1) {
                currentIndex[0]++;
                javafx.application.Platform.runLater(updateQuestion);
            } else {
                dialog.close();
                processQuizResults(lesson, quiz, questions, user, responses, correctAnswers[0]);
            }
        });

        dialog.showAndWait();
    }

    private void processQuizResults(Lesson lesson,
                                     QuizDefinition quiz,
                                     List<QuizQuestion> questions,
                                     User user,
                                     List<Integer> responses,
                                     int correctCount) {
        int score = correctCount * 50; // Each correct answer = 50 coins as per UI
        int maxScore = questions.size() * 50;
        int totalMarks = Math.max(maxScore, 1);
        
        // Simple passing logic
        boolean passed = correctCount >= (questions.size() * 0.6); 
        
        QuizAttempt attempt = repository.recordQuizAttempt(
                user,
                quiz,
                score,
                totalMarks,
                passed,
                new ArrayList<>(responses));
        
        if (passed && attempt != null) {
            repository.saveLessonCompletion(user, lesson, attempt);
        }
        
        String pointMsg = passed ? " Lesson completed!" : " Review the course material and try again.";
        String message = String.format("You scored %d / %d. %s%s",
                score,
                totalMarks,
                passed ? "Module marked complete" : "Review the course material and try again",
                passed ? pointMsg : "");
        showFeedback(passed ? "Quiz passed" : "Quiz submitted", message);
        
        JsonObject activityData = new JsonObject();
        activityData.addProperty("lesson_id", lesson.getId());
        activityData.addProperty("quiz_id", quiz.getId());
        activityData.addProperty("score", score);
        activityData.addProperty("max_score", totalMarks);
        activityData.addProperty("passed", passed);
        repository.logStudentActivity(user, "quiz_complete", activityData);
        
        // Tests 38-39: Check and log alerts for educator after quiz completion
        java.util.concurrent.CompletableFuture.runAsync(() -> repository.checkAndLogStudentAlerts(user));
        
        refreshContent();
    }

    private void showFeedback(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.show();
    }
}
