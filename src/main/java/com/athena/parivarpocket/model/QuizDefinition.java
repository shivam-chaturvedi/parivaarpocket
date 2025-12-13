package com.athena.parivarpocket.model;

public class QuizDefinition {
    private final String id;
    private final String lessonId;
    private final String title;
    private final String difficulty;
    private final int passingMarks;
    private final int totalMarks;

    public QuizDefinition(String id,
                          String lessonId,
                          String title,
                          String difficulty,
                          int passingMarks,
                          int totalMarks) {
        this.id = id;
        this.lessonId = lessonId;
        this.title = title;
        this.difficulty = difficulty;
        this.passingMarks = passingMarks;
        this.totalMarks = totalMarks;
    }

    public String getId() {
        return id;
    }

    public String getLessonId() {
        return lessonId;
    }

    public String getTitle() {
        return title;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public int getPassingMarks() {
        return passingMarks;
    }

    public int getTotalMarks() {
        return totalMarks;
    }
}
