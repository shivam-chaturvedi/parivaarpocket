package com.athena.parivarpocket.model;

import java.time.LocalDateTime;

public class LessonCompletion {
    private final String id;
    private final String lessonId;
    private final String userEmail;
    private final String quizAttemptId;
    private final LocalDateTime completedAt;

    public LessonCompletion(String id,
                            String lessonId,
                            String userEmail,
                            String quizAttemptId,
                            LocalDateTime completedAt) {
        this.id = id;
        this.lessonId = lessonId;
        this.userEmail = userEmail;
        this.quizAttemptId = quizAttemptId;
        this.completedAt = completedAt;
    }

    public String getId() {
        return id;
    }

    public String getLessonId() {
        return lessonId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getQuizAttemptId() {
        return quizAttemptId;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
