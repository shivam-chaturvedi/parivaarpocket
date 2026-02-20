package com.aditya.parivarpocket.model;

public class Lesson {
    private final String id;
    private final String title;
    private final String difficulty;
    private final String description;
    private final int progressPercent;
    private final int quizzesCompleted;
    private final int quizzesTotal;
    private final String courseUrl;

    public Lesson(String id,
                  String title,
                  String difficulty,
                  String description,
                  int progressPercent,
                  int quizzesCompleted,
                  int quizzesTotal,
                  String courseUrl) {
        this.id = id;
        this.title = title;
        this.difficulty = difficulty;
        this.description = description;
        this.progressPercent = progressPercent;
        this.quizzesCompleted = quizzesCompleted;
        this.quizzesTotal = quizzesTotal;
        this.courseUrl = courseUrl;
    }

    public String getId() {
        return id;
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

    public String getCourseUrl() {
        return courseUrl;
    }

}
