package com.athena.parivarpocket.service;

import com.athena.parivarpocket.model.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class DataRepository {
    private final LocalStoreService storeService;
    private final Map<String, List<WalletEntry>> walletEntries = new HashMap<>();

    public DataRepository(LocalStoreService storeService) {
        this.storeService = storeService;
    }

    public List<Lesson> getLessons() {
        return List.of(
                new Lesson("Budgeting Basics", "Beginner", "Understand income, expenses, and how to build a safe budget.", 70, 3, 5),
                new Lesson("Smart Saving", "Intermediate", "Set savings goals and track them with alerts and reminders.", 40, 2, 4),
                new Lesson("Responsible Spending", "Intermediate", "Plan needs vs wants and avoid unnecessary costs.", 55, 2, 4),
                new Lesson("Earning & Employability", "Advanced", "Explore local jobs, safety checks, and application skills.", 25, 1, 3)
        );
    }

    public List<QuizResult> getQuizResults() {
        return List.of(
                new QuizResult("Budget Planner Quiz", 92, "Intermediate", 120),
                new QuizResult("Savings Safety", 78, "Beginner", 80),
                new QuizResult("Job Readiness", 84, "Advanced", 140)
        );
    }

    public List<JobOpportunity> getJobOpportunities() {
        return List.of(
                new JobOpportunity("Mathematics Tutor", "Learn & Grow Academy", "Kolkata", "Tutoring",
                        "Part-time (10-15 hrs/week)", "₹3,000 - ₹5,000/month",
                        List.of("Grade 8-10 maths", "Communication"), "Work only with school references", "call: 90000-00001", 86),
                new JobOpportunity("Delivery Associate", "QuickDeliver Services", "Howrah", "Delivery",
                        "Full-time (40 hrs/week)", "₹8,000 - ₹12,000/month",
                        List.of("Road safety", "Time management"), "Helmet + reflective jacket required", "call: 90000-00002", 74),
                new JobOpportunity("Retail Sales Assistant", "StyleMart Fashion", "Park Street, Kolkata", "Retail",
                        "Full-time (48 hrs/week)", "₹6,000 - ₹9,000/month",
                        List.of("Cataloging", "Customer help"), "Work in pairs; CCTV monitored", "email: hello@local.org", 65),
                new JobOpportunity("Data Entry Intern", "TechSolutions Pvt Ltd", "Salt Lake, Kolkata", "Internship",
                        "Full-time (6 months)", "₹5,000 - ₹7,000/month",
                        List.of("Basic Excel", "Accuracy"), "Secure office; ID required on entry", "call: 90000-00003", 90),
                new JobOpportunity("STEM Workshop Assistant", "Park Circus Learners", "Park Circus", "Tutoring",
                        "Sundays • 4 hrs/week", "₹3,500 - ₹5,000/month",
                        List.of("STEM kits", "Team support"), "Teacher present at all times", "email: safejobs@ngo.org", 82)
        );
    }

    public List<NotificationItem> getNotifications(User user) {
        return List.of(
                new NotificationItem("Wallet Alert", "Spending on transport exceeded the weekly budget.", "warning", LocalDate.now().minusDays(1)),
                new NotificationItem("Quiz Reminder", "Practice \"Savings Safety\" quiz to unlock ParivaarCoins.", "info", LocalDate.now().minusDays(2)),
                new NotificationItem("New Job", "Verified \"STEM Workshop Assistant\" opportunity added in Park Circus.", "success", LocalDate.now()),
                new NotificationItem("Educator Note", user.getRole() == UserRole.STUDENT ? "Your mentor flagged missing receipts for last week." : "3 students crossed expense limits.", "info", LocalDate.now().minusDays(3))
        );
    }

    public List<StudentProgress> getStudentsProgress() {
        return List.of(
                new StudentProgress("Rajesh Kumar", 8, 12, 15, 82, 78, 1250, 3),
                new StudentProgress("Priya Singh", 10, 12, 18, 89, 85, 1620, 5),
                new StudentProgress("Amit Dutta", 6, 12, 9, 72, 64, 950, 2),
                new StudentProgress("Sunita Shaw", 9, 12, 16, 91, 88, 1710, 4)
        );
    }

    public List<WalletEntry> loadWallet(User user) {
        String key = keyForUser(user);
        if (walletEntries.containsKey(key)) {
            return walletEntries.get(key);
        }
        List<WalletEntry> entries = new ArrayList<>(storeService.loadWalletEntries(user));
        if (entries.isEmpty()) {
            entries.addAll(defaultWallet());
        }
        walletEntries.put(key, entries);
        return entries;
    }

    public void addWalletEntry(User user, WalletEntry entry) {
        List<WalletEntry> entries = new ArrayList<>(loadWallet(user));
        entries.add(entry);
        walletEntries.put(keyForUser(user), entries);
        storeService.saveWalletEntries(user, entries);
    }

    public void saveWallet(User user, List<WalletEntry> entries) {
        walletEntries.put(keyForUser(user), entries);
        storeService.saveWalletEntries(user, entries);
    }

    public double calculateIncome(List<WalletEntry> entries) {
        return entries.stream()
                .filter(e -> e.getType() == WalletEntryType.INCOME)
                .mapToDouble(WalletEntry::getAmount)
                .sum();
    }

    public double calculateExpenses(List<WalletEntry> entries) {
        return entries.stream()
                .filter(e -> e.getType() == WalletEntryType.EXPENSE)
                .mapToDouble(WalletEntry::getAmount)
                .sum();
    }

    public Map<String, Double> categoryBreakdown(List<WalletEntry> entries) {
        return entries.stream()
                .filter(e -> e.getType() == WalletEntryType.EXPENSE)
                .collect(Collectors.groupingBy(WalletEntry::getCategory, Collectors.summingDouble(WalletEntry::getAmount)));
    }

    private String keyForUser(User user) {
        return user.getEmail() + "|" + user.getRole().name();
    }

    private List<WalletEntry> defaultWallet() {
        return List.of(
                new WalletEntry(WalletEntryType.INCOME, "Scholarship", 4500, "Monthly stipend", LocalDate.now().minusDays(12)),
                new WalletEntry(WalletEntryType.INCOME, "Tutoring", 2200, "Math tutoring Grade 9", LocalDate.now().minusDays(5)),
                new WalletEntry(WalletEntryType.EXPENSE, "Food", 1800, "Groceries and snacks", LocalDate.now().minusDays(3)),
                new WalletEntry(WalletEntryType.EXPENSE, "Transport", 600, "Bus to school", LocalDate.now().minusDays(2)),
                new WalletEntry(WalletEntryType.SAVINGS, "Savings", 3200, "Emergency fund", LocalDate.now().minusDays(7))
        );
    }
}
