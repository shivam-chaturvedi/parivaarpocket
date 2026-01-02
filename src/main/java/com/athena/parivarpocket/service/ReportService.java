package com.athena.parivarpocket.service;

import com.athena.parivarpocket.model.StudentProgress;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReportService {
    private final Path reportsDir;

    public ReportService() {
        this.reportsDir = Path.of(System.getProperty("user.home"), ".parivaarpocket", "reports");
        try {
            Files.createDirectories(reportsDir);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create reports folder", e);
        }
    }

    public Path exportStudentReport(StudentProgress progress) {
        String filename = progress.getStudentName().replaceAll("\\s+", "_") + "_report.txt";
        Path reportFile = reportsDir.resolve(filename);
        String content = buildReport(progress);
        try {
            Files.writeString(reportFile, content, StandardCharsets.UTF_8);
            return reportFile;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write report", e);
        }
    }

    private String buildReport(StudentProgress progress) {
        return """
                ParivaarPocket Progress Report
                Generated: %s

                Student: %s
                Modules: %d / %d
                Quizzes: %d
                Average Score: %.1f%%
                Wallet Health: %.1f%%
                ParivaarCoins: %d
                Employment commitments: %d
                Jobs saved for follow-up: %d
                Wallet savings: ₹%d
                Active alerts: %d

                Guidance:
                - Encourage consistent quiz practice to raise confidence.
                - Review wallet habits weekly and keep receipts for accuracy.
                - Pair with an educator mentor for job application prep.
                """
                .formatted(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")),
                progress.getStudentName(),
                progress.getModulesCompleted(),
                progress.getTotalModules(),
                progress.getQuizzesTaken(),
                progress.getAverageScore(),
                progress.getWalletHealthScore(),
                progress.getParivaarPoints(),
                progress.getEmploymentApplications(),
                progress.getJobSaves(),
                progress.getWalletSavings(),
                progress.getAlerts());
    }
}
