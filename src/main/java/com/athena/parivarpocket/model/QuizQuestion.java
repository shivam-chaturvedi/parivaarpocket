package com.athena.parivarpocket.model;

import java.util.List;

public class QuizQuestion {
    private final String id;
    private final String quizId;
    private final String question;
    private final List<String> options;
    private final int correctOption;
    private final int points;

    public QuizQuestion(String id,
                        String quizId,
                        String question,
                        List<String> options,
                        int correctOption,
                        int points) {
        this.id = id;
        this.quizId = quizId;
        this.question = question;
        this.options = options;
        this.correctOption = correctOption;
        this.points = points;
    }

    public String getId() {
        return id;
    }

    public String getQuizId() {
        return quizId;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getOptions() {
        return options;
    }

    public int getCorrectOption() {
        return correctOption;
    }

    public int getPoints() {
        return points;
    }
}
