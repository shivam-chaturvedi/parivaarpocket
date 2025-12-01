package com.athena.parivarpocket.model;

public class StudentProgress {
    private final String studentName;
    private final int modulesCompleted;
    private final int totalModules;
    private final int quizzesTaken;
    private final double averageScore;
    private final double walletHealthScore;
    private final int parivaarPoints;
    private final int employmentApplications;

    public StudentProgress(String studentName, int modulesCompleted, int totalModules, int quizzesTaken,
                           double averageScore, double walletHealthScore, int parivaarPoints, int employmentApplications) {
        this.studentName = studentName;
        this.modulesCompleted = modulesCompleted;
        this.totalModules = totalModules;
        this.quizzesTaken = quizzesTaken;
        this.averageScore = averageScore;
        this.walletHealthScore = walletHealthScore;
        this.parivaarPoints = parivaarPoints;
        this.employmentApplications = employmentApplications;
    }

    public String getStudentName() {
        return studentName;
    }

    public int getModulesCompleted() {
        return modulesCompleted;
    }

    public int getTotalModules() {
        return totalModules;
    }

    public int getQuizzesTaken() {
        return quizzesTaken;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public double getWalletHealthScore() {
        return walletHealthScore;
    }

    public int getParivaarPoints() {
        return parivaarPoints;
    }

    public int getEmploymentApplications() {
        return employmentApplications;
    }
}
