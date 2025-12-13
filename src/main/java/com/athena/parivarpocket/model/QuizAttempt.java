package com.athena.parivarpocket.model;

import java.time.LocalDateTime;
import java.util.List;

public class QuizAttempt {
    private final String id;
    private final String quizId;
    private final String userEmail;
    private final int score;
    private final int maxScore;
    private final boolean passed;
    private final List<Integer> responses;
    private final LocalDateTime createdAt;

    public QuizAttempt(String id,
                       String quizId,
                       String userEmail,
                       int score,
                       int maxScore,
                       boolean passed,
                       List<Integer> responses,
                       LocalDateTime createdAt) {
        this.id = id;
        this.quizId = quizId;
        this.userEmail = userEmail;
        this.score = score;
        this.maxScore = maxScore;
        this.passed = passed;
        this.responses = responses;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getQuizId() {
        return quizId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public int getScore() {
        return score;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public boolean isPassed() {
        return passed;
    }

    public List<Integer> getResponses() {
        return responses;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
