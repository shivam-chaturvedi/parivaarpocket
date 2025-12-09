package com.athena.parivarpocket.service;

import com.athena.parivarpocket.model.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DataRepository {
    private final LocalStoreService storeService;
    private final Map<String, List<WalletEntry>> walletEntries = new HashMap<>();
    private final SupabaseClient supabaseClient = new SupabaseClient();
    private volatile List<Lesson> lessonsCache;
    private volatile List<QuizResult> quizCache;
    private volatile List<JobOpportunity> jobCache;
    private volatile List<NotificationItem> notificationsCache;
    private volatile List<StudentProgress> studentProgressCache;
    private volatile List<WalletEntry> walletCache;
    private User currentUser;

    public DataRepository(LocalStoreService storeService) {
        this.storeService = storeService;
    }

    public synchronized void prefetchAll(User user) {
        this.currentUser = user;
        walletEntries.clear();
        lessonsCache = mapTable("lessons", null, this::toLesson);
        quizCache = mapTable("quiz_results", null, this::toQuizResult);
        jobCache = mapTable("job_opportunities", null, this::toJobOpportunity);
        notificationsCache = mapTable("notifications", null, this::toNotification);
        studentProgressCache = mapTable("student_progress", null, this::toStudentProgress);
        walletCache = fetchWalletFromSupabase(user);
        if (notificationsCache.isEmpty()) {
            notificationsCache = List.of(new NotificationItem(
                    "Welcome",
                    "We are syncing notifications from Supabase—nothing new yet.",
                    "info",
                    LocalDate.now()));
        }
    }

    public List<Lesson> getLessons() {
        if (lessonsCache != null) {
            return lessonsCache;
        }
        return mapTable("lessons", null, this::toLesson);
    }

    public List<QuizResult> getQuizResults() {
        if (quizCache != null) {
            return quizCache;
        }
        return mapTable("quiz_results", null, this::toQuizResult);
    }

    public List<JobOpportunity> getJobOpportunities() {
        if (jobCache != null) {
            return jobCache;
        }
        return mapTable("job_opportunities", null, this::toJobOpportunity);
    }

    public List<NotificationItem> getNotifications(User user) {
        List<NotificationItem> base = notificationsCache != null ? notificationsCache : mapTable("notifications", null, this::toNotification);
        List<NotificationItem> result = new ArrayList<>(base);
        String noteText = user.getRole() == UserRole.STUDENT
                ? "Your mentor flagged missing receipts for last week."
                : "3 students crossed expense limits.";
        result.add(new NotificationItem("Educator Note", noteText, "info", LocalDate.now().minusDays(3)));
        return result;
    }

    public List<StudentProgress> getStudentsProgress() {
        if (studentProgressCache != null) {
            return studentProgressCache;
        }
        return mapTable("student_progress", null, this::toStudentProgress);
    }

    public List<WalletEntry> loadWallet(User user) {
        String key = keyForUser(user);
        if (walletEntries.containsKey(key)) {
            return walletEntries.get(key);
        }
        List<WalletEntry> entries = walletCache != null && currentUser != null && keyForUser(currentUser).equals(key)
                ? walletCache
                : fetchWalletFromSupabase(user);
        if (entries.isEmpty()) {
            entries = new ArrayList<>(storeService.loadWalletEntries(user));
            if (entries.isEmpty()) {
                entries.addAll(defaultWallet());
            }
        } else {
            storeService.saveWalletEntries(user, entries);
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

    private <T> List<T> mapTable(String table, String query, Function<JsonObject, T> mapper) {
        return mapTable(table, query, mapper, null);
    }

    private <T> List<T> mapTable(String table, String query, Function<JsonObject, T> mapper, String bearerToken) {
        try {
            JsonArray data = supabaseClient.fetchTable(table, query, bearerToken);
            List<T> records = new ArrayList<>();
            if (data != null) {
                for (JsonElement element : data) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    T mapped = mapper.apply(element.getAsJsonObject());
                    if (mapped != null) {
                        records.add(mapped);
                    }
                }
            }
            return List.copyOf(records);
        } catch (Exception e) {
            return List.of();
        }
    }

    private Lesson toLesson(JsonObject json) {
        return new Lesson(
                safeString(json, "title", "Untitled lesson"),
                safeString(json, "difficulty", "Beginner"),
                safeString(json, "description", "Details coming soon."),
                safeInt(json, "progress_percent", 0),
                safeInt(json, "quizzes_completed", 0),
                safeInt(json, "quizzes_total", 0)
        );
    }

    private QuizResult toQuizResult(JsonObject json) {
        return new QuizResult(
                safeString(json, "title", "Quiz"),
                safeInt(json, "score", 0),
                safeString(json, "difficulty", "Beginner"),
                safeInt(json, "coins_awarded", 0)
        );
    }

    private JobOpportunity toJobOpportunity(JsonObject json) {
        return new JobOpportunity(
                safeString(json, "title", "Job Opportunity"),
                safeString(json, "company", "Partner Organisation"),
                safeString(json, "location", "Unknown"),
                safeString(json, "category", "General"),
                safeString(json, "hours", "Flexible hours"),
                safeString(json, "pay_range", "Negotiable"),
                safeStringList(json, "required_skills"),
                safeString(json, "safety_notes", ""),
                safeString(json, "contact", ""),
                safeInt(json, "suitability_score", 0)
        );
    }

    private NotificationItem toNotification(JsonObject json) {
        return new NotificationItem(
                safeString(json, "title", "New update"),
                safeString(json, "description", ""),
                safeString(json, "severity", "info"),
                safeDate(json, "notify_date", LocalDate.now())
        );
    }

    private StudentProgress toStudentProgress(JsonObject json) {
        return new StudentProgress(
                safeString(json, "student_name", "Student"),
                safeInt(json, "modules_completed", 0),
                safeInt(json, "total_modules", 1),
                safeInt(json, "quizzes_taken", 0),
                safeDouble(json, "average_score", 0),
                safeDouble(json, "wallet_health_score", 0),
                safeInt(json, "parivaar_points", 0),
                safeInt(json, "employment_applications", 0),
                safeInt(json, "job_saves", 0),
                safeInt(json, "wallet_savings", 0),
                safeInt(json, "alerts", 0)
        );
    }

    private List<WalletEntry> fetchWalletFromSupabase(User user) {
        if (user == null || user.getEmail() == null) {
            return List.of();
        }
        String encodedEmail = URLEncoder.encode(user.getEmail().toLowerCase(Locale.ROOT), StandardCharsets.UTF_8);
        String query = "owner_email=eq." + encodedEmail + "&order=entry_date.desc";
        return mapTable("wallet_entries", query, this::toWalletEntry, user.getAccessToken());
    }

    private WalletEntry toWalletEntry(JsonObject json) {
        WalletEntryType type = WalletEntryType.fromString(safeString(json, "entry_type", "expense"));
        return new WalletEntry(
                type,
                safeString(json, "category", "General"),
                safeDouble(json, "amount", 0),
                safeString(json, "note", ""),
                safeDate(json, "entry_date", LocalDate.now())
        );
    }

    private String safeString(JsonObject json, String key, String fallback) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return fallback;
    }

    private int safeInt(JsonObject json, String key, int fallback) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            try {
                return json.get(key).getAsInt();
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    private double safeDouble(JsonObject json, String key, double fallback) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            try {
                return json.get(key).getAsDouble();
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    private List<String> safeStringList(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            JsonElement element = json.get(key);
            if (element.isJsonArray()) {
                List<String> values = new ArrayList<>();
                for (JsonElement entry : element.getAsJsonArray()) {
                    if (!entry.isJsonNull()) {
                        values.add(entry.getAsString());
                    }
                }
                return values;
            }
            String raw = element.getAsString();
            if (!raw.isBlank()) {
                return List.of(raw.split(","));
            }
        }
        return List.of();
    }

    private LocalDate safeDate(JsonObject json, String key, LocalDate fallback) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            String raw = json.get(key).getAsString();
            if (!raw.isBlank()) {
                try {
                    return LocalDate.parse(raw);
                } catch (DateTimeParseException ignored) {
                }
            }
        }
        return fallback;
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
