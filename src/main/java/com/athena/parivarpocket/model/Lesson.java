package com.athena.parivarpocket.model;

public class Lesson {
    private final String title;
    private final String difficulty;
    private final String description;
    private final int progressPercent;
    private final int quizzesCompleted;
    private final int quizzesTotal;

    public Lesson(String title, String difficulty, String description, int progressPercent, int quizzesCompleted, int quizzesTotal) {
        this.title = title;
        this.difficulty = difficulty;
        this.description = description;
        this.progressPercent = progressPercent;
        this.quizzesCompleted = quizzesCompleted;
        this.quizzesTotal = quizzesTotal;
    }

    public String getTitle() {
        return title;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getDescription() {
        return description;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public int getQuizzesCompleted() {
        return quizzesCompleted;
    }

    public int getQuizzesTotal() {
        return quizzesTotal;
    }
}
