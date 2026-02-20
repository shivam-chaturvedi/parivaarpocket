package com.aditya.parivarpocket.service;

import com.aditya.parivarpocket.model.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
    private volatile List<StudentProfile> profileCache;
    private volatile Map<String, BudgetGoal> budgetGoalCache = Map.of();
    private volatile List<String> favoriteIdsCache = List.of();
    private volatile List<JobOpportunity> favoriteJobsCache = List.of();
    private final Set<String> rewardedQuestionsCache = Collections.synchronizedSet(new HashSet<>());
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
        jobCache = mapTable("jobs", null, this::toJobOpportunity);
        notificationsCache = mapTable("notifications", null, this::toNotification);
        studentProgressCache = mapTable("student_progress", null, this::toStudentProgress);
        profileCache = mapTable("profiles", null, this::toStudentProfile);
        walletCache = fetchWalletFromSupabase(user);
        lessonCompletionCache = user != null ? fetchLessonCompletions(user) : List.of();
        quizAttemptCache = user != null ? fetchQuizAttempts(user) : List.of();
        
        // Eagerly pre-fetch favorites for instant tab transitions
        if (user != null) {
            this.favoriteIdsCache = fetchFavoriteJobIdsFromSupabase(user);
            this.favoriteJobsCache = fetchFavoriteJobsFromSupabase(user);
            fetchQuizRewardsFromSupabase(user);
        }

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
        jobCache = mapTable("jobs", null, this::toJobOpportunity);
        return jobCache;
    }

    public LocalDateTime getLatestJobSyncTime() {
        try {
            // Fetch only the created_at of the most recent job
            String query = "select=created_at&order=created_at.desc&limit=1";
            JsonArray data = supabaseClient.fetchTable("jobs", query, null);
            if (data != null && !data.isEmpty()) {
                JsonObject obj = data.get(0).getAsJsonObject();
                if (obj.has("created_at") && !obj.get("created_at").isJsonNull()) {
                    return LocalDateTime.parse(obj.get("created_at").getAsString().replace("Z", ""));
                }
            }
        } catch (Exception e) {
            System.err.println("[DataRepository] Unable to fetch latest job sync time: " + e.getMessage());
        }
        return null;
    }

    public List<JobOpportunity> syncJobs(List<JobOpportunity> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return getJobOpportunities();
        }
        JsonArray payload = new JsonArray();
        for (JobOpportunity job : jobs) {
            if (job == null) {
                continue;
            }
            payload.add(jobToPayload(job));
        }
        JsonArray inserted = safeInsertRecord("jobs", "on_conflict=id", payload, null);
        List<JobOpportunity> allJobsFromDb = mapTable("jobs", null, this::toJobOpportunity);
        if (!allJobsFromDb.isEmpty()) {
            jobCache = allJobsFromDb;
            return jobCache;
        }
        List<JobOpportunity> persisted = parseJobArray(inserted);
        if (!persisted.isEmpty()) {
            jobCache = persisted;
            return persisted;
        }
        jobCache = List.copyOf(jobs);
        return jobCache;
    }

    public List<StudentProfile> getStudentProfiles() {
        if (profileCache != null) {
            return profileCache;
        }
        return mapTable("profiles", null, this::toStudentProfile);
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

    public Map<String, Double> getQuizStats(User user) {
        List<QuizAttempt> attempts = user != null ? fetchQuizAttempts(user) : List.of();
        if (attempts.isEmpty()) {
            return Map.of("max", 0.0, "min", 0.0, "median", 0.0);
        }
        List<Double> scores = attempts.stream()
                .map(a -> (double) a.getScore() / Math.max(a.getMaxScore(), 1) * 100)
                .sorted()
                .toList();
        
        double max = scores.get(scores.size() - 1);
        double min = scores.get(0);
        double median;
        if (scores.size() % 2 == 0) {
            median = (scores.get(scores.size() / 2 - 1) + scores.get(scores.size() / 2)) / 2;
        } else {
            median = scores.get(scores.size() / 2);
        }
        
        Map<String, Double> stats = new HashMap<>();
        stats.put("max", max);
        stats.put("min", min);
        stats.put("median", median);
        return stats;
    }

    public void awardParivaarPoints(User user, int amount, String reason) {
        if (user == null || amount <= 0) return;
        
        StudentProgress progress = getStudentProgress(user.getEmail());
        if (progress == null) {
            // Create a default progress object if none exists
            progress = new StudentProgress(user.getName(), user.getEmail(), 0, getLessons().size(), 0, 0, 0, 0, 0, 0, 0, 0);
        }
        
        int newPoints = progress.getParivaarPoints() + amount;
        StudentProgress updated = new StudentProgress(
                progress.getStudentName(),
                progress.getUserEmail(),
                progress.getModulesCompleted(),
                progress.getTotalModules(),
                progress.getQuizzesTaken(),
                progress.getAverageScore(),
                progress.getWalletHealthScore(),
                newPoints,
                progress.getEmploymentApplications(),
                progress.getJobSaves(),
                progress.getWalletSavings(),
                progress.getAlerts()
        );
        
        // Optimistically update local cache so UI is "live"
        cacheStudentProgress(updated);
        
        if (updateStudentProgress(updated)) {
            // Add wallet entry as income
            addWalletEntry(user, new WalletEntry(
                WalletEntryType.INCOME,
                "Education",
                (double) amount,
                reason != null ? reason : "Lesson completion reward",
                LocalDate.now()
            ));

            // Refresh cache from server to stay in sync if possible
            try {
                List<StudentProgress> refreshed = mapTable("student_progress", null, this::toStudentProgress);
                if (refreshed != null && !refreshed.isEmpty()) {
                    studentProgressCache = refreshed;
                }
            } catch (Exception e) {
                System.err.println("[DataRepository] Failed to refresh student progress cache: " + e.getMessage());
            }
        }
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
        JsonArray inserted = safeInsertRecord("quiz_attempts", null, payload, user.getAccessToken());
        if (inserted != null && !inserted.isEmpty()) {
            QuizAttempt attempt = toQuizAttempt(inserted.get(0).getAsJsonObject());
            cacheQuizAttempt(attempt);
            syncStudentProgress(user); // Force sync aggregate stats
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
        JsonArray inserted = safeInsertRecord("lesson_completions", null, payload, user.getAccessToken());
        if (inserted != null && !inserted.isEmpty()) {
            LessonCompletion completion = toLessonCompletion(inserted.get(0).getAsJsonObject());
            cacheLessonCompletion(completion);
            syncStudentProgress(user); // Force sync aggregate stats
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
        JsonArray inserted = safeInsertRecord("budget_goals", "on_conflict=user_email", payload, user.getAccessToken());
        if (inserted != null && !inserted.isEmpty()) {
            BudgetGoal goal = toBudgetGoal(inserted.get(0).getAsJsonObject());
            cacheBudgetGoal(goal);

            // Log budget goal update activity
            JsonObject activityData = new JsonObject();
            activityData.addProperty("current_budget", currentBudget);
            activityData.addProperty("target_savings", targetSavings);
            logStudentActivity(user, "budget_goal_update", activityData);

            return goal;
        }
        return null;
    }

    public boolean isQuestionRewarded(User user, String questionId) {
        if (user == null || questionId == null) return true; // Fail safe to avoid double rewards
        return rewardedQuestionsCache.contains(questionId);
    }

    public void markQuestionRewarded(User user, String questionId) {
        if (user == null || questionId == null) return;
        
        JsonObject payload = new JsonObject();
        payload.addProperty("user_email", user.getEmail().toLowerCase(Locale.ROOT));
        payload.addProperty("question_id", questionId);
        
        JsonArray inserted = safeInsertRecord("quiz_rewards", null, payload, user.getAccessToken());
        if (inserted != null && !inserted.isEmpty()) {
            rewardedQuestionsCache.add(questionId);
        }
    }

    private void fetchQuizRewardsFromSupabase(User user) {
        if (user == null) return;
        try {
            String query = "select=question_id&user_email=eq." + user.getEmail().toLowerCase(Locale.ROOT);
            JsonArray data = supabaseClient.fetchTable("quiz_rewards", query, user.getAccessToken());
            rewardedQuestionsCache.clear();
            if (data != null) {
                for (JsonElement e : data) {
                    if (e.isJsonObject() && e.getAsJsonObject().has("question_id")) {
                        rewardedQuestionsCache.add(e.getAsJsonObject().get("question_id").getAsString());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DataRepository] Failed to fetch quiz rewards: " + e.getMessage());
        }
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
        } else {
            storeService.saveWalletEntries(user, entries);
        }
        walletEntries.put(key, entries);
        return entries;
    }

    public void addWalletEntry(User user, WalletEntry entry) {
        if (user == null || entry == null) {
            return;
        }
        // Persist to Supabase
        insertWalletEntry(user, entry);

        // Update local memory cache
        List<WalletEntry> entries = new ArrayList<>(loadWallet(user));
        // Add to top if sorting by date descending
        entries.add(0, entry); 
        walletEntries.put(keyForUser(user), entries);
        storeService.saveWalletEntries(user, entries);

        // Log wallet transaction activity
        JsonObject activityData = new JsonObject();
        activityData.addProperty("type", entry.getType().toString());
        activityData.addProperty("category", entry.getCategory());
        activityData.addProperty("amount", entry.getAmount());
        activityData.addProperty("note", entry.getNote());
        logStudentActivity(user, "wallet_transaction", activityData);
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

    public double calculateTotalSavings(List<WalletEntry> entries) {
        return entries.stream()
                .filter(e -> e.getType() == WalletEntryType.SAVINGS)
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
        String token = user != null ? user.getAccessToken() : getSafeToken();
        JsonArray inserted = safeInsertRecord("lessons", null, payload, token);
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
        String token = user != null ? user.getAccessToken() : getSafeToken();
        JsonArray inserted = safeInsertRecord("quizzes", null, payload, token);
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
        String token = user != null ? user.getAccessToken() : getSafeToken();
        JsonArray inserted = safeInsertRecord("quiz_questions", null, payload, token);
        if (inserted != null && !inserted.isEmpty()) {
            QuizQuestion quizQuestion = toQuizQuestion(inserted.get(0).getAsJsonObject());
            cacheQuizQuestion(quizQuestion);
            return quizQuestion;
        }
        return null;
    }

    private void insertWalletEntry(User user, WalletEntry entry) {
        JsonObject payload = new JsonObject();
        payload.addProperty("owner_email", user.getEmail().toLowerCase(Locale.ROOT));
        payload.addProperty("entry_type", entry.getType().toString().toLowerCase());
        payload.addProperty("category", entry.getCategory());
        payload.addProperty("amount", entry.getAmount());
        payload.addProperty("note", entry.getNote());
        payload.addProperty("entry_date", entry.getDate().toString());
        String token = user != null ? user.getAccessToken() : getSafeToken();
        safeInsertRecord("wallet_entries", null, payload, token);
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
                safeString(json, "id", ""),
                safeString(json, "title", "Job Opportunity"),
                safeString(json, "company_name", "Partner Organisation"),
                safeString(json, "location", "Unknown"),
                safeString(json, "locality", "Unknown"),
                safeString(json, "job_link", ""),
                safeLong(json, "pub_date_ts_milli", 0L),
                safeString(json, "formatted_relative_time", ""),
                safeNullableDouble(json, "salary_min"),
                safeNullableDouble(json, "salary_max"),
                safeString(json, "salary_type", ""),
                safeString(json, "category", "General"),
                safeStringList(json, "required_skills"),
                safeString(json, "working_hours", "Full-time"),
                safeString(json, "safety_guidance", "Always verify employer identity."),
                safeString(json, "contact_info", "Apply via Indeed.")
        );
    }

    private JsonObject jobToPayload(JobOpportunity job) {
        JsonObject payload = new JsonObject();
        String id = job.getId();
        if (id == null || id.isBlank()) {
            id = java.util.UUID.randomUUID().toString();
        }
        payload.addProperty("id", id);
        payload.addProperty("title", defaultIfBlank(job.getTitle(), "Job Opportunity"));
        payload.addProperty("company_name", defaultIfBlank(job.getCompany(), "Partner Organisation"));
        payload.addProperty("location", defaultIfBlank(job.getLocation(), "Unknown"));
        payload.addProperty("locality", defaultIfBlank(job.getLocality(), "Unknown"));
        payload.addProperty("job_link", defaultIfBlank(job.getJobLink(), ""));
        long pubDate = job.getPubDateTsMilli();
        if (pubDate > 0) {
            payload.addProperty("pub_date_ts_milli", pubDate);
        } else {
            payload.add("pub_date_ts_milli", JsonNull.INSTANCE);
        }
        payload.addProperty("formatted_relative_time", defaultIfBlank(job.getFormattedRelativeTime(), ""));
        addNullable(payload, "salary_min", job.getSalaryMin());
        addNullable(payload, "salary_max", job.getSalaryMax());
        addNullableString(payload, "salary_type", job.getSalaryType());
        payload.addProperty("category", job.getCategory());
        payload.add("required_skills", toJsonArray(job.getRequiredSkills()));
        payload.addProperty("working_hours", job.getWorkingHours());
        payload.addProperty("safety_guidance", job.getSafetyGuidance());
        payload.addProperty("contact_info", job.getContactInfo());
        return payload;
    }

    public void toggleFavorite(User user, String jobId) {
        if (user == null || jobId == null) return;
        try {
            // Check if the job is already favorited by fetching the record
            String query = "user_email=eq." + user.getEmail() + "&job_id=eq." + jobId;
            JsonArray existing = supabaseClient.fetchTable("job_favorites", query, null);

            if (existing != null && !existing.isEmpty()) {
                String idToRemove = existing.get(0).getAsJsonObject().get("id").getAsString();
                supabaseClient.deleteRecord("job_favorites", "id=eq." + idToRemove, null);
                
                // Update caches
                List<String> updatedIds = new ArrayList<>(favoriteIdsCache);
                updatedIds.remove(jobId);
                this.favoriteIdsCache = List.copyOf(updatedIds);
                
                List<JobOpportunity> updatedJobs = new ArrayList<>(favoriteJobsCache);
                updatedJobs.removeIf(j -> j.getId().equals(jobId));
                this.favoriteJobsCache = List.copyOf(updatedJobs);
            } else {
                JsonObject fav = new JsonObject();
                fav.addProperty("user_email", user.getEmail());
                fav.addProperty("job_id", jobId);
                JsonArray result = safeInsertRecord("job_favorites", null, fav, null);
                
                // Update caches
                List<String> updatedIds = new ArrayList<>(favoriteIdsCache);
                updatedIds.add(jobId);
                this.favoriteIdsCache = List.copyOf(updatedIds);
                
                // If we have the job in cache, add to favorite jobs cache
                if (jobCache != null) { // Assuming jobCache is a List<JobOpportunity> containing all jobs
                    jobCache.stream()
                            .filter(j -> j.getId().equals(jobId))
                            .findFirst()
                            .ifPresent(j -> {
                                List<JobOpportunity> updatedJobs = new ArrayList<>(favoriteJobsCache);
                                updatedJobs.add(j);
                                this.favoriteJobsCache = List.copyOf(updatedJobs);
                            });
                }
            }
        } catch (Exception e) {
            System.err.println("[DataRepository] Favorite toggle failed: " + e.getMessage());
        }
    }

    public boolean isFavorite(User user, String jobId) {
        if (user == null || jobId == null) return false;
        return favoriteIdsCache.contains(jobId);
    }

    public List<String> fetchFavoriteJobIds(User user) {
        return favoriteIdsCache;
    }

    private List<String> fetchFavoriteJobIdsFromSupabase(User user) {
        if (user == null) return Collections.emptyList();
        try {
            String query = "select=job_id&user_email=eq." + user.getEmail();
            JsonArray data = supabaseClient.fetchTable("job_favorites", query, null);
            List<String> ids = new ArrayList<>();
            for (JsonElement e : data) {
                ids.add(e.getAsJsonObject().get("job_id").getAsString());
            }
            return ids;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<JobOpportunity> fetchFavoriteJobs(User user) {
        return favoriteJobsCache;
    }

    public void removeFavoriteJob(String userId, String jobId) {
        User user = getCurrentUser();
        if (user == null || jobId == null) return;
        try {
            // Check if the job is favorited and remove it
            String query = "user_email=eq." + user.getEmail() + "&job_id=eq." + jobId;
            JsonArray existing = supabaseClient.fetchTable("job_favorites", query, null);

            if (existing != null && !existing.isEmpty()) {
                String idToRemove = existing.get(0).getAsJsonObject().get("id").getAsString();
                supabaseClient.deleteRecord("job_favorites", "id=eq." + idToRemove, null);
                
                // Update caches
                List<String> updatedIds = new ArrayList<>(favoriteIdsCache);
                updatedIds.remove(jobId);
                this.favoriteIdsCache = List.copyOf(updatedIds);
                
                List<JobOpportunity> updatedJobs = new ArrayList<>(favoriteJobsCache);
                updatedJobs.removeIf(j -> j.getId().equals(jobId));
                this.favoriteJobsCache = List.copyOf(updatedJobs);
            }
        } catch (Exception e) {
            System.err.println("[DataRepository] Remove favorite failed: " + e.getMessage());
        }
    }

    private List<JobOpportunity> fetchFavoriteJobsFromSupabase(User user) {
        if (user == null) return Collections.emptyList();
        List<String> jobIds = fetchFavoriteJobIdsFromSupabase(user);
        if (jobIds.isEmpty()) return Collections.emptyList();

        try {
            // Join IDs into a comma-separated list for the 'in' filter
            String idsFilter = String.join(",", jobIds);
            String query = "id=in.(" + idsFilter + ")";
            JsonArray data = supabaseClient.fetchTable("jobs", query, null);
            return parseJobArray(data);
        } catch (Exception e) {
            System.err.println("[DataRepository] Failed to fetch favorite job details: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private static void addNullable(JsonObject payload, String key, Number value) {
        if (value == null) {
            payload.add(key, JsonNull.INSTANCE);
        } else {
            payload.addProperty(key, value);
        }
    }

    private static void addNullableString(JsonObject payload, String key, String value) {
        if (value == null || value.isBlank()) {
            payload.add(key, JsonNull.INSTANCE);
        } else {
            payload.addProperty(key, value);
        }
    }

    private JsonArray toJsonArray(List<String> values) {
        JsonArray array = new JsonArray();
        if (values != null) {
            for (String value : values) {
                if (value != null) {
                    array.add(value);
                }
            }
        }
        return array;
    }

    private List<JobOpportunity> parseJobArray(JsonArray data) {
        if (data == null || data.isEmpty()) {
            return List.of();
        }
        List<JobOpportunity> list = new ArrayList<>();
        for (JsonElement element : data) {
            if (!element.isJsonObject()) {
                continue;
            }
            JobOpportunity job = toJobOpportunity(element.getAsJsonObject());
            if (job != null) {
                list.add(job);
            }
        }
        return List.copyOf(list);
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private NotificationItem toNotification(JsonObject json) {
        return new NotificationItem(
                safeString(json, "title", "New update"),
                safeString(json, "description", ""),
                safeString(json, "severity", "info"),
                safeDate(json, "notify_date", LocalDate.now())
        );
    }

    private StudentProfile toStudentProfile(JsonObject json) {
        return new StudentProfile(
                safeString(json, "id", ""),
                safeString(json, "email", ""),
                safeString(json, "role", "student"),
                safeDateTime(json, "created_at", null)
        );
    }

    public List<WalletEntry> fetchWalletByEmail(String userEmail) {
        if (userEmail == null || userEmail.isEmpty()) return Collections.emptyList();
        String encoded = URLEncoder.encode(userEmail.toLowerCase(Locale.ROOT), StandardCharsets.UTF_8);
        // Correct column name is owner_email according to migration 010
        String query = "owner_email=eq." + encoded + "&order=created_at.desc";
        return mapTable("wallet_entries", query, this::toWalletEntry, getSafeToken());
    }

    public List<WalletEntry> fetchWalletEntries(User user) {
        if (user == null || user.getEmail() == null) return Collections.emptyList();
        String encoded = URLEncoder.encode(user.getEmail().toLowerCase(Locale.ROOT), StandardCharsets.UTF_8);
        String query = "owner_email=eq." + encoded + "&order=entry_date.desc";
        String token = user.getAccessToken();
        return mapTable("wallet_entries", query, this::toWalletEntry, token);
    }

    private String getSafeToken() {
        return currentUser != null ? currentUser.getAccessToken() : null;
    }

    private StudentProgress toStudentProgress(JsonObject json) {
        return new StudentProgress(
                safeString(json, "student_name", "Unknown Student"),
                safeString(json, "user_email", ""),
                safeInt(json, "modules_completed", 0),
                safeInt(json, "total_modules", 0),
                safeInt(json, "quizzes_taken", 0),
                safeDouble(json, "average_score", 0.0),
                safeDouble(json, "wallet_health_score", 0.0),
                safeInt(json, "parivaar_points", 0),
                safeInt(json, "employment_applications", 0),
                safeInt(json, "job_saves", 0),
                safeInt(json, "wallet_savings", 0),
                safeInt(json, "alerts", 0)
        );
    }

    public StudentProgress getProgressForEmail(String email) {
        return getStudentProgress(email);
    }

    public StudentProgress getStudentProgress(String email) {
        if (email == null) return null;
        
        String normalized = email.toLowerCase(Locale.ROOT);
        
        // Fetch existing record to get the name (if available)
        StudentProgress existing = fetchStudentProgressRecord(normalized);
        String studentName = (existing != null && existing.getStudentName() != null) ? existing.getStudentName() : "Student";
        
        // 1. Fetch Lessons Count (Live)
        List<LessonCompletion> completions = fetchLessonCompletionsByEmail(normalized);
        int modulesCompleted = completions.size();

        // 2. Fetch Quiz Stats (Live)
        List<QuizAttempt> attempts = fetchQuizAttemptsByEmail(normalized);
        int quizzesTaken = attempts.size();
        
        double avgScore = attempts.stream()
                .mapToDouble(a -> (double) a.getScore() / Math.max(a.getMaxScore(), 1) * 100)
                .average()
                .orElse(0.0);

        // 3. Fetch Wallet Stats (Live)
        // We need total savings.
        List<WalletEntry> wallet = fetchWalletByEmail(normalized);
        int totalSavings = (int) calculateTotalSavings(wallet);
        
        // Calculate "Parivaar Points" or "Total Coins" - typically distinct from savings? 
        // Existing logic used Income of type Education?
        int totalCoins = (int) wallet.stream()
                .filter(e -> e.getType() == WalletEntryType.INCOME && "Education".equalsIgnoreCase(e.getCategory()))
                .mapToDouble(WalletEntry::getAmount)
                .sum();
        
        // 4. Fetch Job Activity (Live)
        List<JobApplication> apps = fetchJobApplications(normalized);
        int employmentApplications = apps.size();
        
        int jobSaves = fetchJobSavesCount(normalized);

        // 5. Fetch Alerts (Live)
        List<Alert> alertsList = fetchAlerts(normalized);
        int alertsCount = alertsList.size();
        
        double walletHealthScore = calculateWalletHealthScore(totalSavings); // Helper method logic

        // Create the progress object with live data
        StudentProgress progress = new StudentProgress(
                studentName,
                normalized,
                modulesCompleted,
                getLessons().size(),
                quizzesTaken,
                avgScore,
                walletHealthScore,
                totalCoins,
                employmentApplications,
                jobSaves,
                totalSavings,
                alertsCount
        );
        
        // Cache it
        cacheStudentProgress(progress);
        
        // Optionally update the persistent record asynchronously so it stays relatively fresh
        // but we return the calculated one immediately.
        CompletableFuture.runAsync(() -> updateStudentProgress(progress));
        
        return progress;
    }

    private int fetchJobSavesCount(String email) {
        if (email == null) return 0;
        try {
            // We can just count the rows
            String query = "user_email=eq." + URLEncoder.encode(email, StandardCharsets.UTF_8) + "&select=count";
            // Supabase client might not support count easily with fetchTable without 'count=exact' header
            // Fallback: fetch IDs
             String q = "select=id&user_email=eq." + URLEncoder.encode(email, StandardCharsets.UTF_8);
             JsonArray data = supabaseClient.fetchTable("job_favorites", q, getSafeToken());
             return data != null ? data.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private double calculateWalletHealthScore(int savings) {
        // Simple logic: Target is 2000 (from existing code in EducatorDashboardView chart).
        // 5000 is "Good", 8000 is "Excellent" in Dashboard logic.
        // Let's normalize to a 0-100 score.
        // If 5000 is 50/100? No, dashboard says >= 50 is Good.
        // Let's say 10000 savings = 100 score.
        return Math.min(100.0, (double) savings / 100.0); 
    }

    private StudentProgress fetchStudentProgressRecord(String email) {
        String encoded = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String query = "user_email=eq." + encoded + "&limit=1";
        List<StudentProgress> matches = mapTable("student_progress", query, this::toStudentProgress);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private List<LessonCompletion> fetchLessonCompletions(User user) {
        if (user == null || user.getEmail() == null) {
            return List.of();
        }
        String encoded = URLEncoder.encode(user.getEmail().toLowerCase(Locale.ROOT), StandardCharsets.UTF_8);
        String query = "user_email=eq." + encoded + "&order=completed_at.desc";
        String token = user.getAccessToken();
        return mapTable("lesson_completions", query, this::toLessonCompletion, token);
    }

    public List<LessonCompletion> fetchLessonCompletionsByEmail(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return List.of();
        }
        String encoded = URLEncoder.encode(userEmail.toLowerCase(Locale.ROOT), StandardCharsets.UTF_8);
        String query = "user_email=eq." + encoded + "&order=completed_at.desc";
        return mapTable("lesson_completions", query, this::toLessonCompletion, getSafeToken());
    }


    private List<QuizAttempt> fetchQuizAttempts(User user) {
        if (user == null || user.getEmail() == null) {
            return List.of();
        }
        String encoded = URLEncoder.encode(user.getEmail().toLowerCase(Locale.ROOT), StandardCharsets.UTF_8);
        String query = "user_email=eq." + encoded + "&order=created_at.desc";
        String token = user.getAccessToken();
        return mapTable("quiz_attempts", query, this::toQuizAttempt, token);
    }

    public List<QuizAttempt> fetchQuizAttemptsByEmail(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return List.of();
        }
        String encoded = URLEncoder.encode(userEmail.toLowerCase(Locale.ROOT), StandardCharsets.UTF_8);
        String query = "user_email=eq." + encoded + "&order=created_at.desc";
        return mapTable("quiz_attempts", query, this::toQuizAttempt, getSafeToken());
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

    private Double safeNullableDouble(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            try {
                return json.get(key).getAsDouble();
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private long safeLong(JsonObject json, String key, long fallback) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            try {
                return json.get(key).getAsLong();
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    private String stringifyElement(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        if (element.isJsonPrimitive()) {
            return element.getAsJsonPrimitive().getAsString();
        }
        return element.toString();
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


    public void recordJobApplication(User user, JobOpportunity job) {
        if (user == null || job == null) {
            return;
        }
        
        // 1. Record the application activity in the consolidated logs
        JsonObject activityData = new JsonObject();
        activityData.addProperty("job_id", job.getId());
        activityData.addProperty("job_title", job.getTitle());
        activityData.addProperty("company", job.getCompany());
        activityData.addProperty("action", "application_submitted");
        logStudentActivity(user, "job_application_event", activityData);

        // 2. Increment employment applications in StudentProgress
        StudentProgress progress = getStudentProgress(user.getEmail());
        if (progress != null) {
            StudentProgress updated = new StudentProgress(
                    progress.getStudentName(),
                    progress.getUserEmail(),
                    progress.getModulesCompleted(),
                    progress.getTotalModules(),
                    progress.getQuizzesTaken(),
                    progress.getAverageScore(),
                    progress.getWalletHealthScore(),
                    progress.getParivaarPoints(),
                    progress.getEmploymentApplications() + 1,
                    progress.getJobSaves(),
                    progress.getWalletSavings(),
                    progress.getAlerts()
            );
            updateStudentProgress(updated);
        }

        // 3. Award coins as incentive
        awardParivaarPoints(user, 50, "Job Application: " + job.getTitle());

        // Tests 38-39: Check and log alerts for educator
        CompletableFuture.runAsync(() -> checkAndLogStudentAlerts(user));
    }

    public void logStudentActivity(User user, String activityType, JsonObject activityData) {
        if (user == null || activityType == null || activityType.isBlank()) {
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("user_email", user.getEmail().toLowerCase(Locale.ROOT));
        payload.addProperty("activity_type", activityType);
        payload.add("activity_data", activityData != null ? activityData : new JsonObject());
        try {
            // Using a simple insert; if we want it to be "instant" we could run it on the same thread 
            // but the user wants it to be recorded *before* the external link opens.
            safeInsertRecord("student_activity_logs", null, payload, user.getAccessToken());
        } catch (Exception e) {
            System.err.println("[DataRepository] Unable to log student activity: " + e.getMessage());
        }
    }

    /**
     * Tests 38-39: Check student metrics and log alerts for educator notifications.
     * Called after job applications and quiz completions.
     */
    public void checkAndLogStudentAlerts(User user) {
        if (user == null || user.getEmail() == null) return;
        try {
            String email = user.getEmail().toLowerCase(java.util.Locale.ROOT);

            // Test 38: Low job activity alert
            List<JobApplication> apps = fetchJobApplications(email);
            List<String> savedJobIds = fetchFavoriteJobIds(user);
            if (apps.size() < 2 && savedJobIds.size() < 2) {
                JsonObject meta = new JsonObject();
                meta.addProperty("applications", apps.size());
                meta.addProperty("bookmarks", savedJobIds.size());
                logAlert(user, "Low Job Activity",
                    "Student has few job applications (" + apps.size() + ") and bookmarks (" + savedJobIds.size() + "). Encourage exploration.",
                    "info", meta);
            }

            // Test 39: Low quiz scores or missing modules alert
            List<QuizAttempt> attempts = fetchQuizAttemptsByEmail(email);
            List<Lesson> lessons = getLessons();
            List<LessonCompletion> completions = fetchLessonCompletionsByEmail(email);

            double avgScore = attempts.stream()
                    .mapToDouble(a -> (double) a.getScore() / Math.max(a.getMaxScore(), 1) * 100)
                    .average().orElse(100.0); // Default 100 if no attempts yet

            double completionRate = lessons.isEmpty() ? 1.0 : (double) completions.size() / lessons.size();

            if (avgScore < 50.0 && !attempts.isEmpty()) {
                JsonObject meta = new JsonObject();
                meta.addProperty("average_score", Math.round(avgScore));
                meta.addProperty("quizzes_taken", attempts.size());
                logAlert(user, "Low Quiz Performance",
                    "Student's average quiz score is " + Math.round(avgScore) + "%. Additional support may be needed.",
                    "warning", meta);
            }

            if (completionRate < 0.30 && !lessons.isEmpty()) {
                JsonObject meta = new JsonObject();
                meta.addProperty("modules_completed", completions.size());
                meta.addProperty("total_modules", lessons.size());
                logAlert(user, "Missing Modules",
                    "Student has completed only " + completions.size() + " of " + lessons.size() + " modules. Encourage engagement.",
                    "warning", meta);
            }
        } catch (Exception e) {
            System.err.println("[DataRepository] checkAndLogStudentAlerts failed: " + e.getMessage());
        }
    }

    public void logAlert(User user, String category, String message, String severity, JsonObject metadata) {
        if (user == null || category == null || category.isBlank() || message == null || message.isBlank()) {
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("user_email", user.getEmail().toLowerCase(Locale.ROOT));
        payload.addProperty("category", category);
        payload.addProperty("severity", severity != null && !severity.isBlank() ? severity : "info");
        payload.addProperty("message", message);
        payload.add("metadata", metadata != null ? metadata : new JsonObject());
        try {
            safeInsertRecord("alerts", null, payload, user.getAccessToken());
        } catch (Exception e) {
            System.err.println("[DataRepository] Unable to log alert: " + e.getMessage());
        }
    }

    public List<JobApplication> fetchJobApplications(String userEmail) {
        String encoded = URLEncoder.encode(userEmail.toLowerCase(Locale.ROOT), StandardCharsets.UTF_8);
        String query = "user_email=eq." + encoded + "&activity_type=eq.job_application_event&order=created_at.desc";
        return mapTable("student_activity_logs", query, this::toJobApplicationFromLog, getSafeToken());
    }

    public List<StudentActivity> fetchStudentActivities(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return Collections.emptyList();
        }
        String encoded = URLEncoder.encode(userEmail.toLowerCase(Locale.ROOT), StandardCharsets.UTF_8);
        String query = "user_email=eq." + encoded + "&order=created_at.desc&limit=50";
        return mapTable("student_activity_logs", query, this::toStudentActivity, getSafeToken());
    }

    public List<Alert> fetchAlerts(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return Collections.emptyList();
        }
        String encoded = URLEncoder.encode(userEmail.toLowerCase(Locale.ROOT), StandardCharsets.UTF_8);
        String query = "user_email=eq." + encoded + "&order=created_at.desc";
        return mapTable("alerts", query, this::toAlert, getSafeToken());
    }

    public List<Alert> fetchAllAlerts() {
        // Fetch all alerts
        List<Alert> allAlerts = mapTable("alerts", "order=created_at.desc&limit=100", this::toAlert, getSafeToken());
        
        // Filter out the ones read by this specific educator
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getRole() == UserRole.EDUCATOR) {
            String encodedEmail = URLEncoder.encode(currentUser.getEmail().toLowerCase(Locale.ROOT), StandardCharsets.UTF_8);
            String readQuery = "select=alert_id&educator_email=eq." + encodedEmail;
            
            try {
                JsonArray readData = supabaseClient.fetchTable("educator_alert_reads", readQuery, getSafeToken());
                if (readData != null && !readData.isEmpty()) {
                    List<String> readIds = new ArrayList<>();
                    for (JsonElement e : readData) {
                        readIds.add(e.getAsJsonObject().get("alert_id").getAsString());
                    }
                    
                    return allAlerts.stream()
                        .filter(a -> !readIds.contains(a.getId()))
                        .collect(Collectors.toList());
                }
            } catch (Exception e) {
                System.err.println("[DataRepository] Failed to filter read alerts: " + e.getMessage());
            }
        }
        
        return allAlerts;
    }

    private JobApplication toJobApplicationFromLog(JsonObject json) {
        JsonObject data = json.has("activity_data") ? json.getAsJsonObject("activity_data") : new JsonObject();
        return new JobApplication(
                safeString(json, "id", ""),
                safeString(data, "job_id", ""),
                "Submitted", // Activity logs represent point-in-time events
                safeDateTime(json, "created_at", LocalDateTime.now())
        );
    }

    private StudentActivity toStudentActivity(JsonObject json) {
        JsonObject data = json.has("activity_data") && json.get("activity_data").isJsonObject()
                ? json.getAsJsonObject("activity_data")
                : new JsonObject();
        Map<String, String> details = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
            details.put(entry.getKey(), stringifyElement(entry.getValue()));
        }
        return new StudentActivity(
                safeString(json, "id", ""),
                safeString(json, "activity_type", ""),
                details,
                safeDateTime(json, "created_at", LocalDateTime.now())
        );
    }

    private Alert toAlert(JsonObject json) {
        JsonObject data = json.has("metadata") && json.get("metadata").isJsonObject()
                ? json.getAsJsonObject("metadata")
                : new JsonObject();
        Map<String, String> details = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
            details.put(entry.getKey(), stringifyElement(entry.getValue()));
        }
        return new Alert(
                safeString(json, "id", ""),
                safeString(json, "user_email", ""),
                safeString(json, "category", ""),
                safeString(json, "severity", "info"),
                safeString(json, "message", ""),
                details,
                safeDateTime(json, "created_at", LocalDateTime.now()),
                safeInt(json, "read", 0) == 1
        );
    }

    public void markAlertAsReadForEducator(User educator, String alertId) {
        if (educator == null || alertId == null || alertId.isEmpty()) return;

        JsonObject payload = new JsonObject();
        payload.addProperty("educator_email", educator.getEmail().toLowerCase(Locale.ROOT));
        payload.addProperty("alert_id", alertId);

        try {
            safeInsertRecord("educator_alert_reads", null, payload, getSafeToken());
        } catch (Exception e) {
            System.err.println("[DataRepository] Failed to mark alert as read for educator: " + e.getMessage());
        }
    }

    public void markAlertAsRead(String alertId) {
        if (alertId == null || alertId.isEmpty()) return;
        
        JsonObject payload = new JsonObject();
        // payload.addProperty("id", alertId); // ID not needed in payload for update
        payload.addProperty("read", true);
        
        try {
            // Use update instead of upsert to avoid constraint violations on missing rows
            safeUpdateRecord("alerts", "id=eq." + alertId, payload, getSafeToken());
        } catch (Exception e) {
            System.err.println("[DataRepository] Failed to mark alert as read: " + e.getMessage());
        }
    }
    public boolean updateStudentProgress(StudentProgress progress) {
        if (progress == null || progress.getUserEmail() == null) {
            return false;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("user_email", progress.getUserEmail().toLowerCase(Locale.ROOT));
        payload.addProperty("student_name", progress.getStudentName());
        payload.addProperty("modules_completed", progress.getModulesCompleted());
        payload.addProperty("total_modules", progress.getTotalModules());
        payload.addProperty("quizzes_taken", progress.getQuizzesTaken());
        payload.addProperty("average_score", progress.getAverageScore());
        payload.addProperty("wallet_health_score", progress.getWalletHealthScore());
        payload.addProperty("parivaar_points", progress.getParivaarPoints());
        payload.addProperty("employment_applications", progress.getEmploymentApplications());
        payload.addProperty("job_saves", progress.getJobSaves());
        payload.addProperty("wallet_savings", progress.getWalletSavings());
        payload.addProperty("alerts", progress.getAlerts());

        // Upsert based on user_email
        JsonArray inserted = safeInsertRecord("student_progress", "on_conflict=user_email", payload, getSafeToken());
        return inserted != null && !inserted.isEmpty();
    }
    public void syncStudentProgress(User user) {
        if (user == null || user.getEmail() == null) return;
        
        List<QuizAttempt> attempts = fetchQuizAttempts(user);
        List<LessonCompletion> completions = fetchLessonCompletions(user);
        StudentProgress current = getStudentProgress(user.getEmail());
        
        if (current == null) {
            current = new StudentProgress(user.getName(), user.getEmail(), 0, getLessons().size(), 0, 0, 0, 0, 0, 0, 0, 0);
        }
        
        int quizzesTaken = attempts.size();
        double avgScore = attempts.stream()
                .mapToDouble(a -> (double) a.getScore() / Math.max(a.getMaxScore(), 1) * 100)
                .average()
                .orElse(0.0);
        
        int modulesCompleted = completions.size();
        
        StudentProgress updated = new StudentProgress(
                current.getStudentName(),
                current.getUserEmail(),
                modulesCompleted,
                getLessons().size(),
                quizzesTaken,
                avgScore,
                current.getWalletHealthScore(),
                current.getParivaarPoints(),
                current.getEmploymentApplications(),
                current.getJobSaves(),
                current.getWalletSavings(),
                current.getAlerts()
        );
        
        // Optimistically update local cache
        cacheStudentProgress(updated);
        
        if (updateStudentProgress(updated)) {
            // Refresh cache from server safely
            try {
                List<StudentProgress> refreshed = mapTable("student_progress", null, this::toStudentProgress);
                if (refreshed != null && !refreshed.isEmpty()) {
                    studentProgressCache = refreshed;
                }
            } catch (Exception ignored) {}
        }
    }

    private void cacheStudentProgress(StudentProgress progress) {
        if (progress == null || progress.getUserEmail() == null) return;
        List<StudentProgress> list = new ArrayList<>(studentProgressCache != null ? studentProgressCache : List.of());
        String email = progress.getUserEmail().toLowerCase(Locale.ROOT);
        list.removeIf(p -> email.equals(p.getUserEmail().toLowerCase(Locale.ROOT)));
        list.add(progress);
        studentProgressCache = List.copyOf(list);
    }

    private JsonArray safeInsertRecord(String table, String queryParams, JsonElement payload, String token) {
        try {
            return supabaseClient.insertRecord(table, queryParams, payload, token);
        } catch (Exception e) {
            System.err.println("[DataRepository] Error inserting into " + table + ": " + e.getMessage());
            return null;
        }
    }

    private JsonArray safeUpdateRecord(String table, String queryParams, JsonElement payload, String token) {
        try {
            return supabaseClient.updateRecord(table, queryParams, payload, token);
        } catch (Exception e) {
            System.err.println("[DataRepository] Error updating " + table + ": " + e.getMessage());
            return null;
        }
    }
}
