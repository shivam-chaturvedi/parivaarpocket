package com.athena.parivarpocket.model;

public class QuizResult {
    private final String title;
    private final int score;
    private final String difficulty;
    private final int coinsAwarded;

    public QuizResult(String title, int score, String difficulty, int coinsAwarded) {
        this.title = title;
        this.score = score;
        this.difficulty = difficulty;
        this.coinsAwarded = coinsAwarded;
    }

    public String getTitle() {
        return title;
    }

    public int getScore() {
        return score;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public int getCoinsAwarded() {
        return coinsAwarded;
    }
}
