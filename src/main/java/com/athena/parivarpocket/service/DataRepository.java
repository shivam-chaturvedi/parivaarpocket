package com.athena.parivarpocket.service;

import com.athena.parivarpocket.model.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private volatile List<QuizDefinition> quizDefinitionCache;
    private volatile List<QuizQuestion> quizQuestionCache;
    private volatile List<QuizAttempt> quizAttemptCache;
    private volatile List<LessonCompletion> lessonCompletionCache;
    private volatile List<JobOpportunity> jobCache;
    private volatile List<NotificationItem> notificationsCache;
    private volatile List<StudentProgress> studentProgressCache;
    private volatile List<WalletEntry> walletCache;
    private volatile Map<String, BudgetGoal> budgetGoalCache = Map.of();
    private User currentUser;

    public DataRepository(LocalStoreService storeService) {
        this.storeService = storeService;
    }

    public synchronized void prefetchAll(User user) {
        this.currentUser = user;
        walletEntries.clear();
        lessonsCache = mapTable("lessons", null, this::toLesson);
        quizCache = mapTable("quiz_results", null, this::toQuizResult);
        quizDefinitionCache = mapTable("quizzes", null, this::toQuizDefinition);
        quizQuestionCache = mapTable("quiz_questions", null, this::toQuizQuestion);
        jobCache = mapTable("job_opportunities", null, this::toJobOpportunity);
        notificationsCache = mapTable("notifications", null, this::toNotification);
        studentProgressCache = mapTable("student_progress", null, this::toStudentProgress);
        walletCache = fetchWalletFromSupabase(user);
        lessonCompletionCache = user != null ? fetchLessonCompletions(user) : List.of();
        quizAttemptCache = user != null ? fetchQuizAttempts(user) : List.of();
        if (notificationsCache.isEmpty()) {
            notificationsCache = List.of(new NotificationItem(
                    "Welcome",
                    "We are syncing notifications from Supabase—nothing new yet.",
                    "info",
                    LocalDate.now()));
        }
        List<BudgetGoal> goals = mapTable("budget_goals", null, this::toBudgetGoal);
        budgetGoalCache = goals.stream()
                .collect(Collectors.toMap(goal -> goal.getUserEmail().toLowerCase(Locale.ROOT),
                        Function.identity(),
                        (first, second) -> second));
    }

    public User getCurrentUser() {
        return currentUser;
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

    public List<QuizDefinition> getQuizDefinitions() {
        if (quizDefinitionCache != null) {
            return quizDefinitionCache;
        }
        return mapTable("quizzes", null, this::toQuizDefinition);
    }

    public List<QuizDefinition> getQuizzesForLesson(String lessonId) {
        if (lessonId == null || lessonId.isBlank()) {
            return List.of();
        }
        return getQuizDefinitions().stream()
                .filter(q -> lessonId.equals(q.getLessonId()))
                .collect(Collectors.toList());
    }

    public List<QuizQuestion> getQuestionsForQuiz(String quizId) {
        if (quizId == null || quizId.isBlank()) {
            return List.of();
        }
        List<QuizQuestion> cache = quizQuestionCache != null
                ? quizQuestionCache
                : mapTable("quiz_questions", null, this::toQuizQuestion);
        return cache.stream()
                .filter(q -> quizId.equals(q.getQuizId()))
                .collect(Collectors.toList());
    }

    public List<LessonCompletion> getLessonCompletions() {
        return lessonCompletionCache != null ? lessonCompletionCache : List.of();
    }

    public boolean isLessonCompleted(String lessonId) {
        if (lessonId == null || lessonCompletionCache == null) {
            return false;
        }
        return lessonCompletionCache.stream().anyMatch(c -> lessonId.equals(c.getLessonId()));
    }

    public boolean createLessonWithQuiz(String lessonTitle,
                                        String difficulty,
                                        String description,
                                        String courseUrl,
                                        String quizTitle,
                                        int passingMarks,
                                        int totalMarks,
                                        String questionText,
                                        List<String> options,
                                        int correctOption,
                                        int points) {
        User user = currentUser;
        if (user == null) {
            return false;
        }
        Lesson lesson = insertLesson(lessonTitle, difficulty, description, courseUrl, user);
        if (lesson == null) {
            return false;
        }
        QuizDefinition quiz = insertQuiz(lesson.getId(), quizTitle, difficulty, passingMarks, totalMarks, user);
        if (quiz == null) {
            return false;
        }
        QuizQuestion question = insertQuizQuestion(quiz.getId(), questionText, options, correctOption, points, user);
        return question != null;
    }

    public QuizAttempt recordQuizAttempt(User user,
                                         QuizDefinition quiz,
                                         int score,
                                         int maxScore,
                                         boolean passed,
                                         List<Integer> responses) {
        if (user == null || quiz == null) {
            return null;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("quiz_id", quiz.getId());
        payload.addProperty("user_email", user.getEmail().toLowerCase(Locale.ROOT));
        payload.addProperty("score", score);
        payload.addProperty("max_score", maxScore);
        payload.addProperty("passed", passed);
        JsonArray responseArray = new JsonArray();
        if (responses != null) {
            responses.forEach(responseArray::add);
        }
        payload.add("responses", responseArray);
        JsonArray inserted = supabaseClient.insertRecord("quiz_attempts", null, payload, user.getAccessToken());
        if (inserted != null && !inserted.isEmpty()) {
            QuizAttempt attempt = toQuizAttempt(inserted.get(0).getAsJsonObject());
            cacheQuizAttempt(attempt);
            return attempt;
        }
        return null;
    }

    public LessonCompletion saveLessonCompletion(User user, Lesson lesson, QuizAttempt attempt) {
        if (user == null || lesson == null || attempt == null) {
            return null;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("lesson_id", lesson.getId());
        payload.addProperty("user_email", user.getEmail().toLowerCase(Locale.ROOT));
        payload.addProperty("quiz_attempt_id", attempt.getId());
        JsonArray inserted = supabaseClient.insertRecord("lesson_completions", null, payload, user.getAccessToken());
        if (inserted != null && !inserted.isEmpty()) {
            LessonCompletion completion = toLessonCompletion(inserted.get(0).getAsJsonObject());
            cacheLessonCompletion(completion);
            return completion;
        }
        return null;
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

    public BudgetGoal getBudgetGoal(User user) {
        if (user == null || user.getEmail() == null) {
            return null;
        }
        return budgetGoalCache.get(user.getEmail().toLowerCase(Locale.ROOT));
    }

    public BudgetGoal upsertBudgetGoal(User user, double currentBudget, double targetSavings) {
        if (user == null || user.getEmail() == null) {
            return null;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("user_email", user.getEmail().toLowerCase(Locale.ROOT));
        payload.addProperty("current_budget", currentBudget);
        payload.addProperty("target_savings", targetSavings);
        JsonArray inserted = supabaseClient.insertRecord("budget_goals", "on_conflict=user_email", payload, user.getAccessToken());
        if (inserted != null && !inserted.isEmpty()) {
            BudgetGoal goal = toBudgetGoal(inserted.get(0).getAsJsonObject());
            cacheBudgetGoal(goal);
            return goal;
        }
        return null;
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

    private Lesson insertLesson(String title,
                                String difficulty,
                                String description,
                                String courseUrl,
                                User user) {
        JsonObject payload = new JsonObject();
        payload.addProperty("title", title);
        payload.addProperty("difficulty", difficulty);
        payload.addProperty("description", description);
        payload.addProperty("course_url", courseUrl);
        JsonArray inserted = supabaseClient.insertRecord("lessons", null, payload, user.getAccessToken());
        if (inserted != null && !inserted.isEmpty()) {
            Lesson created = toLesson(inserted.get(0).getAsJsonObject());
            cacheLesson(created);
            return created;
        }
        return null;
    }

    private QuizDefinition insertQuiz(String lessonId,
                                      String title,
                                      String difficulty,
                                      int passingMarks,
                                      int totalMarks,
                                      User user) {
        JsonObject payload = new JsonObject();
        payload.addProperty("lesson_id", lessonId);
        payload.addProperty("title", title);
        payload.addProperty("difficulty", difficulty);
        payload.addProperty("passing_marks", passingMarks);
        payload.addProperty("total_marks", totalMarks);
        JsonArray inserted = supabaseClient.insertRecord("quizzes", null, payload, user.getAccessToken());
        if (inserted != null && !inserted.isEmpty()) {
            QuizDefinition quiz = toQuizDefinition(inserted.get(0).getAsJsonObject());
            cacheQuizDefinition(quiz);
            return quiz;
        }
        return null;
    }

    private QuizQuestion insertQuizQuestion(String quizId,
                                            String question,
                                            List<String> options,
                                            int correctOption,
                                            int points,
                                            User user) {
        JsonObject payload = new JsonObject();
        payload.addProperty("quiz_id", quizId);
        payload.addProperty("question", question);
        payload.add("options", toJsonArray(options));
        payload.addProperty("correct_option", correctOption);
        payload.addProperty("points", points);
        JsonArray inserted = supabaseClient.insertRecord("quiz_questions", null, payload, user.getAccessToken());
        if (inserted != null && !inserted.isEmpty()) {
            QuizQuestion quizQuestion = toQuizQuestion(inserted.get(0).getAsJsonObject());
            cacheQuizQuestion(quizQuestion);
            return quizQuestion;
        }
        return null;
    }

    private JsonArray toJsonArray(List<String> values) {
        JsonArray array = new JsonArray();
        if (values != null) {
            for (String value : values) {
                array.add(value);
            }
        }
        return array;
    }

    private void cacheLesson(Lesson lesson) {
        if (lesson == null) {
            return;
        }
        List<Lesson> updated = new ArrayList<>(lessonsCache != null ? lessonsCache : List.of());
        updated.add(lesson);
        lessonsCache = List.copyOf(updated);
    }

    private void cacheQuizDefinition(QuizDefinition quiz) {
        if (quiz == null) {
            return;
        }
        List<QuizDefinition> updated = new ArrayList<>(quizDefinitionCache != null ? quizDefinitionCache : List.of());
        updated.add(quiz);
        quizDefinitionCache = List.copyOf(updated);
    }

    private void cacheQuizQuestion(QuizQuestion question) {
        if (question == null) {
            return;
        }
        List<QuizQuestion> updated = new ArrayList<>(quizQuestionCache != null ? quizQuestionCache : List.of());
        updated.add(question);
        quizQuestionCache = List.copyOf(updated);
    }

    private Lesson toLesson(JsonObject json) {
        return new Lesson(
                safeString(json, "id", ""),
                safeString(json, "title", "Untitled lesson"),
                safeString(json, "difficulty", "Beginner"),
                safeString(json, "description", "Details coming soon."),
                safeInt(json, "progress_percent", 0),
                safeInt(json, "quizzes_completed", 0),
                safeInt(json, "quizzes_total", 0),
                safeString(json, "course_url", "")
        );
    }

    private QuizDefinition toQuizDefinition(JsonObject json) {
        return new QuizDefinition(
                safeString(json, "id", ""),
                safeString(json, "lesson_id", ""),
                safeString(json, "title", "Quiz"),
                safeString(json, "difficulty", "Beginner"),
                safeInt(json, "passing_marks", 0),
                safeInt(json, "total_marks", 0)
        );
    }

    private QuizQuestion toQuizQuestion(JsonObject json) {
        return new QuizQuestion(
                safeString(json, "id", ""),
                safeString(json, "quiz_id", ""),
                safeString(json, "question", ""),
                safeStringList(json, "options"),
                safeInt(json, "correct_option", 0),
                safeInt(json, "points", 1)
        );
    }

    private QuizAttempt toQuizAttempt(JsonObject json) {
        return new QuizAttempt(
                safeString(json, "id", ""),
                safeString(json, "quiz_id", ""),
                safeString(json, "user_email", ""),
                safeInt(json, "score", 0),
                safeInt(json, "max_score", 0),
                safeBoolean(json, "passed", false),
                safeIntList(json, "responses"),
                safeDateTime(json, "created_at", LocalDateTime.now())
        );
    }

    private LessonCompletion toLessonCompletion(JsonObject json) {
        return new LessonCompletion(
                safeString(json, "id", ""),
                safeString(json, "lesson_id", ""),
                safeString(json, "user_email", ""),
                safeString(json, "quiz_attempt_id", ""),
                safeDateTime(json, "completed_at", LocalDateTime.now())
        );
    }

    private boolean safeBoolean(JsonObject json, String key, boolean fallback) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            try {
                return json.get(key).getAsBoolean();
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    private List<Integer> safeIntList(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull() && json.get(key).isJsonArray()) {
            List<Integer> list = new ArrayList<>();
            JsonArray array = json.get(key).getAsJsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonNull()) {
                    try {
                        list.add(element.getAsInt());
                    } catch (Exception ignored) {
                    }
                }
            }
            return List.copyOf(list);
        }
        return List.of();
    }

    private LocalDateTime safeDateTime(JsonObject json, String key, LocalDateTime fallback) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            try {
                return LocalDateTime.parse(json.get(key).getAsString());
            } catch (Exception ignored) {
            }
        }
        return fallback;
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
                safeString(json, "job_url", ""),
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

    private List<LessonCompletion> fetchLessonCompletions(User user) {
        if (user == null || user.getEmail() == null) {
            return List.of();
        }
        String encoded = URLEncoder.encode(user.getEmail().toLowerCase(Locale.ROOT), StandardCharsets.UTF_8);
        String query = "user_email=eq." + encoded + "&order=completed_at.desc";
        return mapTable("lesson_completions", query, this::toLessonCompletion, user.getAccessToken());
    }

    private List<QuizAttempt> fetchQuizAttempts(User user) {
        if (user == null || user.getEmail() == null) {
            return List.of();
        }
        String encoded = URLEncoder.encode(user.getEmail().toLowerCase(Locale.ROOT), StandardCharsets.UTF_8);
        String query = "user_email=eq." + encoded + "&order=created_at.desc";
        return mapTable("quiz_attempts", query, this::toQuizAttempt, user.getAccessToken());
    }

    private void cacheLessonCompletion(LessonCompletion completion) {
        if (completion == null) {
            return;
        }
        List<LessonCompletion> updated = new ArrayList<>(lessonCompletionCache != null ? lessonCompletionCache : List.of());
        updated.add(completion);
        lessonCompletionCache = List.copyOf(updated);
    }

    private void cacheQuizAttempt(QuizAttempt attempt) {
        if (attempt == null) {
            return;
        }
        List<QuizAttempt> updated = new ArrayList<>(quizAttemptCache != null ? quizAttemptCache : List.of());
        updated.add(attempt);
        quizAttemptCache = List.copyOf(updated);
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

    private BudgetGoal toBudgetGoal(JsonObject json) {
        return new BudgetGoal(
                safeString(json, "id", ""),
                safeString(json, "user_email", "").toLowerCase(Locale.ROOT),
                safeDouble(json, "current_budget", 0),
                safeDouble(json, "target_savings", 0),
                safeDateTime(json, "updated_at", LocalDateTime.now())
        );
    }

    private void cacheBudgetGoal(BudgetGoal goal) {
        if (goal == null) {
            return;
        }
        Map<String, BudgetGoal> updated = new HashMap<>(budgetGoalCache != null ? budgetGoalCache : Map.of());
        updated.put(goal.getUserEmail().toLowerCase(Locale.ROOT), goal);
        budgetGoalCache = Map.copyOf(updated);
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
