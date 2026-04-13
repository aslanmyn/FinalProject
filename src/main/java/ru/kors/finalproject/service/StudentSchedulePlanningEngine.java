package ru.kors.finalproject.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class StudentSchedulePlanningEngine {
    private static final LocalTime NOON = LocalTime.of(12, 0);
    private static final LocalTime THREE_PM = LocalTime.of(15, 0);
    private static final LocalTime SIX_PM = LocalTime.of(18, 0);
    private static final Pattern TIME_AFTER_PATTERN = Pattern.compile("(?:after|после)\\s*(\\d{1,2})(?::(\\d{2}))?");
    private static final Pattern TIME_BEFORE_PATTERN = Pattern.compile("(?:before|до)\\s*(\\d{1,2})(?::(\\d{2}))?");
    private static final List<String> DAY_ORDER = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY");

    private StudentSchedulePlanningEngine() {
    }

    public static PlanningResult plan(String message, List<CourseOption> availableSections) {
        PreferenceProfile preferences = parsePreferences(message);
        List<CourseGroup> courseGroups = groupSectionsByCourse(availableSections);
        if (courseGroups.isEmpty()) {
            return new PlanningResult(
                    false,
                    true,
                    "Я не нашел доступных секций для следующего семестра.",
                    "Нет доступных секций для построения расписания.",
                    List.of(),
                    List.of("Нет доступных секций"),
                    List.of(),
                    List.of(),
                    List.of("Следующий семестр еще не заполнен секциями.")
            );
        }

        SearchState state = new SearchState();
        search(0, courseGroups, new ArrayList<>(), new ArrayList<>(), preferences, state);
        if (state.best == null || state.best.selectedSections().isEmpty()) {
            return new PlanningResult(
                    false,
                    true,
                    "Мне не удалось собрать даже частичное расписание без конфликтов из доступных секций.",
                    "Не удалось построить расписание без конфликтов.",
                    List.of(),
                    List.of("Не удалось собрать расписание без конфликтов"),
                    courseGroups.stream().map(CourseGroup::courseCode).toList(),
                    List.of(),
                    List.of("Доступные времена секций конфликтуют между собой.")
            );
        }

        FitEvaluation fit = evaluatePreferences(state.best.selectedSections(), preferences);
        List<String> blockingCourses = new ArrayList<>(fit.blockingCourses());
        blockingCourses.addAll(state.best.omittedCourseCodes());
        blockingCourses = blockingCourses.stream().distinct().toList();

        List<String> warnings = new ArrayList<>(fit.warnings());
        if (!state.best.omittedCourseCodes().isEmpty()) {
            warnings.add("Не удалось включить все предметы без конфликтов: " + String.join(", ", state.best.omittedCourseCodes()));
        }
        if (preferences.rawPreferences().isEmpty()) {
            warnings.add("Я не нашел четких временных предпочтений, поэтому выбрал самый аккуратный вариант без конфликтов.");
        }

        boolean feasible = state.best.omittedCourseCodes().isEmpty();
        boolean partial = !feasible;

        return new PlanningResult(
                feasible,
                partial,
                buildAnswer(feasible, partial, fit, state.best.omittedCourseCodes()),
                buildSummary(feasible, partial, fit, state.best.omittedCourseCodes()),
                fit.satisfiedPreferences(),
                fit.unsatisfiedPreferences(),
                blockingCourses,
                state.best.selectedSections().stream()
                        .map(CourseOption::sectionId)
                        .filter(Objects::nonNull)
                        .toList(),
                warnings
        );
    }

    private static List<CourseGroup> groupSectionsByCourse(List<CourseOption> availableSections) {
        Map<String, List<CourseOption>> grouped = availableSections.stream()
                .filter(option -> option.courseCode() != null)
                .collect(Collectors.groupingBy(
                        CourseOption::courseCode,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return grouped.entrySet().stream()
                .map(entry -> new CourseGroup(
                        entry.getKey(),
                        entry.getValue().stream().map(CourseOption::courseName).filter(Objects::nonNull).findFirst().orElse(entry.getKey()),
                        entry.getValue().stream()
                                .sorted(Comparator.comparing(CourseOption::sectionId, Comparator.nullsLast(Long::compareTo)))
                                .toList()
                ))
                .sorted(Comparator.comparingInt((CourseGroup group) -> group.options().size())
                        .thenComparing(CourseGroup::courseCode))
                .toList();
    }

    private static void search(
            int index,
            List<CourseGroup> groups,
            List<CourseOption> selected,
            List<String> omitted,
            PreferenceProfile preferences,
            SearchState state
    ) {
        if (index >= groups.size()) {
            CandidatePlan candidate = evaluateCandidate(selected, omitted, preferences);
            if (state.best == null || candidate.score() > state.best.score()) {
                state.best = candidate;
            }
            return;
        }

        CourseGroup group = groups.get(index);
        for (CourseOption option : group.options()) {
            if (conflictsWithAny(selected, option)) {
                continue;
            }
            selected.add(option);
            search(index + 1, groups, selected, omitted, preferences, state);
            selected.remove(selected.size() - 1);
        }

        omitted.add(group.courseCode());
        search(index + 1, groups, selected, omitted, preferences, state);
        omitted.remove(omitted.size() - 1);
    }

    private static CandidatePlan evaluateCandidate(
            List<CourseOption> selected,
            List<String> omitted,
            PreferenceProfile preferences
    ) {
        FitEvaluation fit = evaluatePreferences(selected, preferences);
        int score = selected.size() * 100000;
        score -= omitted.size() * 5000;
        score += fit.score();
        score += compactnessScore(selected, preferences);
        return new CandidatePlan(List.copyOf(selected), List.copyOf(omitted), score);
    }

    private static FitEvaluation evaluatePreferences(List<CourseOption> selected, PreferenceProfile preferences) {
        Map<String, List<SlotOccurrence>> slotsByDay = buildSlotsByDay(selected);
        List<String> satisfied = new ArrayList<>();
        List<String> unsatisfied = new ArrayList<>();
        List<String> blockingCourses = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int score = 0;

        for (String day : DAY_ORDER) {
            DayPreference dayPreference = preferences.explicitByDay().get(day);
            if (dayPreference == null) {
                continue;
            }
            DayEvaluation evaluation = evaluateDay(day, dayPreference, slotsByDay.getOrDefault(day, List.of()));
            score += evaluation.score();
            if (evaluation.satisfied()) {
                satisfied.add(dayPreference.label());
            } else {
                unsatisfied.add(dayPreference.label());
                blockingCourses.addAll(evaluation.blockingCourses());
            }
        }

        if (preferences.defaultForOtherDays() != null) {
            Set<String> explicitDays = preferences.explicitByDay().keySet();
            List<SlotOccurrence> remainingSlots = slotsByDay.entrySet().stream()
                    .filter(entry -> !explicitDays.contains(entry.getKey()))
                    .flatMap(entry -> entry.getValue().stream())
                    .toList();
            DayEvaluation evaluation = evaluateAggregate(preferences.defaultForOtherDays(), remainingSlots);
            score += evaluation.score();
            if (evaluation.satisfied()) {
                satisfied.add(preferences.defaultForOtherDays().label());
            } else {
                unsatisfied.add(preferences.defaultForOtherDays().label());
                blockingCourses.addAll(evaluation.blockingCourses());
            }
        }

        if (preferences.preferCompactSchedule()) {
            int usedDays = (int) slotsByDay.values().stream().filter(items -> !items.isEmpty()).count();
            int totalGapMinutes = totalGapMinutes(slotsByDay);
            if (usedDays <= 4 && totalGapMinutes <= 120) {
                score += 80;
                satisfied.add("Компактное расписание");
            } else {
                score -= 80;
                unsatisfied.add("Компактное расписание");
                warnings.add("В расписании остаются лишние окна или слишком много учебных дней.");
            }
        }

        return new FitEvaluation(
                score,
                satisfied.stream().distinct().toList(),
                unsatisfied.stream().distinct().toList(),
                blockingCourses.stream().distinct().toList(),
                warnings.stream().distinct().toList()
        );
    }

    private static DayEvaluation evaluateDay(String day, DayPreference preference, List<SlotOccurrence> slots) {
        if (preference.avoidDay()) {
            if (slots.isEmpty()) {
                return new DayEvaluation(true, 180, List.of());
            }
            return new DayEvaluation(false, -260, slots.stream().map(SlotOccurrence::courseCode).distinct().toList());
        }
        if (slots.isEmpty()) {
            return new DayEvaluation(true, 120, List.of());
        }

        List<String> violatingCourses = slots.stream()
                .filter(slot -> !matchesPreference(slot, preference))
                .map(SlotOccurrence::courseCode)
                .distinct()
                .toList();
        if (violatingCourses.isEmpty()) {
            return new DayEvaluation(true, 180 + slotAlignmentBonus(slots, preference), List.of());
        }
        return new DayEvaluation(false, -200 + slotAlignmentBonus(slots, preference), violatingCourses);
    }

    private static DayEvaluation evaluateAggregate(DayPreference preference, List<SlotOccurrence> slots) {
        if (slots.isEmpty()) {
            return new DayEvaluation(true, 80, List.of());
        }
        List<String> violatingCourses = slots.stream()
                .filter(slot -> !matchesPreference(slot, preference))
                .map(SlotOccurrence::courseCode)
                .distinct()
                .toList();
        if (violatingCourses.isEmpty()) {
            return new DayEvaluation(true, 120 + slotAlignmentBonus(slots, preference), List.of());
        }
        return new DayEvaluation(false, -140 + slotAlignmentBonus(slots, preference), violatingCourses);
    }

    private static int slotAlignmentBonus(List<SlotOccurrence> slots, DayPreference preference) {
        int bonus = 0;
        for (SlotOccurrence slot : slots) {
            if (preference.preferredStartFrom() != null) {
                if (!slot.startTime().isBefore(preference.preferredStartFrom())) {
                    bonus += 20;
                } else {
                    bonus -= 25 + minutesBetween(slot.startTime(), preference.preferredStartFrom()) / 10;
                }
            }
            if (preference.preferredStartTo() != null) {
                if (!slot.startTime().isAfter(preference.preferredStartTo())) {
                    bonus += 15;
                } else {
                    bonus -= 20 + minutesBetween(preference.preferredStartTo(), slot.startTime()) / 10;
                }
            }
        }
        return bonus;
    }

    private static boolean matchesPreference(SlotOccurrence slot, DayPreference preference) {
        if (preference.avoidDay()) {
            return false;
        }
        if (preference.preferredStartFrom() != null && slot.startTime().isBefore(preference.preferredStartFrom())) {
            return false;
        }
        if (preference.preferredStartTo() != null && slot.startTime().isAfter(preference.preferredStartTo())) {
            return false;
        }
        return true;
    }

    private static int compactnessScore(List<CourseOption> selected, PreferenceProfile preferences) {
        Map<String, List<SlotOccurrence>> slotsByDay = buildSlotsByDay(selected);
        int usedDays = (int) slotsByDay.values().stream().filter(items -> !items.isEmpty()).count();
        int score = -usedDays * 12;
        int totalGapMinutes = totalGapMinutes(slotsByDay);
        score -= totalGapMinutes / (preferences.preferCompactSchedule() ? 4 : 10);
        return score;
    }

    private static int totalGapMinutes(Map<String, List<SlotOccurrence>> slotsByDay) {
        int totalGapMinutes = 0;
        for (List<SlotOccurrence> daySlots : slotsByDay.values()) {
            if (daySlots.size() < 2) {
                continue;
            }
            List<SlotOccurrence> sorted = daySlots.stream()
                    .sorted(Comparator.comparing(SlotOccurrence::startTime))
                    .toList();
            for (int index = 1; index < sorted.size(); index++) {
                LocalTime previousEnd = sorted.get(index - 1).endTime();
                LocalTime currentStart = sorted.get(index).startTime();
                if (currentStart.isAfter(previousEnd)) {
                    totalGapMinutes += minutesBetween(previousEnd, currentStart);
                }
            }
        }
        return totalGapMinutes;
    }

    private static Map<String, List<SlotOccurrence>> buildSlotsByDay(List<CourseOption> selected) {
        Map<String, List<SlotOccurrence>> slotsByDay = new LinkedHashMap<>();
        for (String day : DAY_ORDER) {
            slotsByDay.put(day, new ArrayList<>());
        }
        for (CourseOption option : selected) {
            for (MeetingOption slot : option.meetingTimes()) {
                if (slot.dayOfWeek() == null || slot.startTime() == null || slot.endTime() == null) {
                    continue;
                }
                slotsByDay.computeIfAbsent(slot.dayOfWeek(), key -> new ArrayList<>())
                        .add(new SlotOccurrence(
                                option.courseCode(),
                                option.courseName(),
                                LocalTime.parse(slot.startTime()),
                                LocalTime.parse(slot.endTime())
                        ));
            }
        }
        return slotsByDay;
    }

    private static boolean conflictsWithAny(List<CourseOption> selected, CourseOption candidate) {
        return selected.stream().anyMatch(existing -> hasConflict(existing, candidate));
    }

    private static boolean hasConflict(CourseOption left, CourseOption right) {
        for (MeetingOption leftSlot : left.meetingTimes()) {
            for (MeetingOption rightSlot : right.meetingTimes()) {
                if (leftSlot.dayOfWeek() == null || !leftSlot.dayOfWeek().equals(rightSlot.dayOfWeek())) {
                    continue;
                }
                if (leftSlot.startTime() == null || leftSlot.endTime() == null || rightSlot.startTime() == null || rightSlot.endTime() == null) {
                    continue;
                }
                LocalTime leftStart = LocalTime.parse(leftSlot.startTime());
                LocalTime leftEnd = LocalTime.parse(leftSlot.endTime());
                LocalTime rightStart = LocalTime.parse(rightSlot.startTime());
                LocalTime rightEnd = LocalTime.parse(rightSlot.endTime());
                if (leftStart.isBefore(rightEnd) && rightStart.isBefore(leftEnd)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static PreferenceProfile parsePreferences(String message) {
        String normalized = normalize(message);
        Map<String, DayPreference> explicit = new LinkedHashMap<>();
        DayPreference defaultForOtherDays = null;

        for (String clause : splitClauses(normalized)) {
            Set<String> days = detectDays(clause);
            DayPreference preference = parseDayPreference(clause);
            boolean forOtherDays = clause.contains("остальн") || clause.contains("other day") || clause.contains("remaining day");
            if (preference == null) {
                continue;
            }
            if (forOtherDays) {
                defaultForOtherDays = preference.withLabel(buildOtherDaysLabel(preference));
                continue;
            }
            if (!days.isEmpty()) {
                for (String day : days) {
                    explicit.put(day, preference.withLabel(buildDayLabel(day, preference)));
                }
            }
        }

        if (explicit.isEmpty() && defaultForOtherDays == null) {
            DayPreference fallback = parseDayPreference(normalized);
            if (fallback != null) {
                defaultForOtherDays = fallback.withLabel(buildOtherDaysLabel(fallback));
            }
        }

        boolean preferCompact = containsAny(normalized, "без окон", "без больших окон", "compact", "компакт", "минимум окон");
        List<String> rawPreferences = new ArrayList<>();
        explicit.values().forEach(preference -> rawPreferences.add(preference.label()));
        if (defaultForOtherDays != null) {
            rawPreferences.add(defaultForOtherDays.label());
        }
        if (preferCompact) {
            rawPreferences.add("Компактное расписание");
        }

        return new PreferenceProfile(explicit, defaultForOtherDays, preferCompact, rawPreferences);
    }

    private static List<String> splitClauses(String normalized) {
        String prepared = normalized
                .replace(";", ",")
                .replace(".", ",")
                .replace(" но ", ",")
                .replace(" but ", ",")
                .replace(" а ", ",");
        List<String> clauses = new ArrayList<>();
        for (String part : prepared.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isBlank()) {
                clauses.add(trimmed);
            }
        }
        return clauses.isEmpty() ? List.of(normalized) : clauses;
    }

    private static Set<String> detectDays(String clause) {
        Map<String, DayTokens> tokens = dayTokenRules();
        List<String> words = tokenizeWords(clause);
        Set<String> days = new LinkedHashSet<>();
        for (Map.Entry<String, DayTokens> entry : tokens.entrySet()) {
            boolean matched = words.stream().anyMatch(word ->
                    entry.getValue().exact().contains(word)
                            || entry.getValue().prefixes().stream().anyMatch(word::startsWith));
            if (matched) {
                days.add(entry.getKey());
            }
        }
        return days;
    }

    private static Map<String, List<String>> dayTokens() {
        Map<String, List<String>> tokens = new LinkedHashMap<>();
        tokens.put("MONDAY", List.of("monday", "mon", "понед", "пн"));
        tokens.put("TUESDAY", List.of("tuesday", "tue", "втор", "вт"));
        tokens.put("WEDNESDAY", List.of("wednesday", "wed", "сред", "ср"));
        tokens.put("THURSDAY", List.of("thursday", "thu", "четв", "чт"));
        tokens.put("FRIDAY", List.of("friday", "fri", "пят", "пт"));
        tokens.put("SATURDAY", List.of("saturday", "sat", "суб", "сб"));
        return tokens;
    }

    private static List<String> tokenizeWords(String clause) {
        return Arrays.stream(clause.split("[^\\p{L}\\p{N}]+"))
                .filter(word -> !word.isBlank())
                .toList();
    }

    private static Map<String, DayTokens> dayTokenRules() {
        Map<String, DayTokens> tokens = new LinkedHashMap<>();
        tokens.put("MONDAY", new DayTokens(
                Set.of("monday", "mon", "\u043f\u043d"),
                Set.of("\u043f\u043e\u043d\u0435\u0434")
        ));
        tokens.put("TUESDAY", new DayTokens(
                Set.of("tuesday", "tue", "\u0432\u0442"),
                Set.of("\u0432\u0442\u043e\u0440")
        ));
        tokens.put("WEDNESDAY", new DayTokens(
                Set.of("wednesday", "wed", "\u0441\u0440"),
                Set.of("\u0441\u0440\u0435\u0434")
        ));
        tokens.put("THURSDAY", new DayTokens(
                Set.of("thursday", "thu", "\u0447\u0442"),
                Set.of("\u0447\u0435\u0442\u0432\u0435\u0440")
        ));
        tokens.put("FRIDAY", new DayTokens(
                Set.of("friday", "fri", "\u043f\u0442"),
                Set.of("\u043f\u044f\u0442")
        ));
        tokens.put("SATURDAY", new DayTokens(
                Set.of("saturday", "sat", "\u0441\u0431"),
                Set.of("\u0441\u0443\u0431")
        ));
        return tokens;
    }

    private static DayPreference parseDayPreference(String clause) {
        Set<String> days = detectDays(clause);
        if (!days.isEmpty() && containsAny(clause, "без ", "avoid", "no classes", "свободн")) {
            return new DayPreference(null, null, true, "Избежать дня");
        }

        Matcher afterMatcher = TIME_AFTER_PATTERN.matcher(clause);
        if (afterMatcher.find()) {
            return new DayPreference(parseTime(afterMatcher.group(1), afterMatcher.group(2)), null, false, "После времени");
        }

        Matcher beforeMatcher = TIME_BEFORE_PATTERN.matcher(clause);
        if (beforeMatcher.find()) {
            return new DayPreference(null, parseTime(beforeMatcher.group(1), beforeMatcher.group(2)), false, "До времени");
        }

        if (containsAny(clause, "утром", "с утра", "morning", "early")) {
            return new DayPreference(null, NOON, false, "Лучше с утра");
        }
        if (containsAny(clause, "после обеда", "afternoon")) {
            return new DayPreference(NOON, null, false, "После обеда");
        }
        if (containsAny(clause, "вечером", "evening", "после 15")) {
            return new DayPreference(THREE_PM, null, false, "После 15:00");
        }
        if (containsAny(clause, "после шести", "after 18")) {
            return new DayPreference(SIX_PM, null, false, "После 18:00");
        }
        return null;
    }

    private static LocalTime parseTime(String hours, String minutes) {
        int hour = Integer.parseInt(hours);
        int minute = minutes == null ? 0 : Integer.parseInt(minutes);
        return LocalTime.of(hour, minute);
    }

    private static String buildAnswer(boolean feasible, boolean partial, FitEvaluation fit, List<String> omittedCourseCodes) {
        StringBuilder answer = new StringBuilder();
        if (partial) {
            answer.append("Я собрал лучший возможный частичный вариант на следующий семестр.");
        } else {
            answer.append("Я собрал расписание на следующий семестр без конфликтов.");
        }

        if (!fit.satisfiedPreferences().isEmpty()) {
            answer.append("\n\nУчтено:");
            fit.satisfiedPreferences().forEach(item -> answer.append("\n- ").append(item));
        }
        if (!fit.unsatisfiedPreferences().isEmpty()) {
            answer.append("\n\nНе удалось полностью выполнить:");
            fit.unsatisfiedPreferences().forEach(item -> answer.append("\n- ").append(item));
        }
        if (!omittedCourseCodes.isEmpty()) {
            answer.append("\n\nНе удалось добавить предметы:");
            omittedCourseCodes.forEach(item -> answer.append("\n- ").append(item));
        }
        return answer.toString().trim();
    }

    private static String buildSummary(boolean feasible, boolean partial, FitEvaluation fit, List<String> omittedCourseCodes) {
        if (partial) {
            return "Частичное расписание без конфликтов: часть предметов пришлось пропустить.";
        }
        if (fit.unsatisfiedPreferences().isEmpty()) {
            return "Расписание собрано без конфликтов и с учетом пожеланий.";
        }
        return "Расписание собрано без конфликтов, но не все пожелания удалось соблюсти.";
    }

    private static String buildDayLabel(String day, DayPreference preference) {
        return switch (describePreference(preference)) {
            case "morning" -> dayDisplayName(day) + ": лучше с утра";
            case "after" -> dayDisplayName(day) + ": после " + preference.preferredStartFrom();
            case "before" -> dayDisplayName(day) + ": до " + preference.preferredStartTo();
            case "avoid" -> "Без занятий в " + dayDisplayName(day).toLowerCase(Locale.ROOT);
            default -> dayDisplayName(day);
        };
    }

    private static String buildOtherDaysLabel(DayPreference preference) {
        return switch (describePreference(preference)) {
            case "morning" -> "Остальные дни: лучше с утра";
            case "after" -> "Остальные дни: после " + preference.preferredStartFrom();
            case "before" -> "Остальные дни: до " + preference.preferredStartTo();
            case "avoid" -> "Остальные дни: без занятий";
            default -> "Остальные дни";
        };
    }

    private static String describePreference(DayPreference preference) {
        if (preference.avoidDay()) {
            return "avoid";
        }
        if (preference.preferredStartFrom() != null) {
            return "after";
        }
        if (preference.preferredStartTo() != null && NOON.equals(preference.preferredStartTo())) {
            return "morning";
        }
        if (preference.preferredStartTo() != null) {
            return "before";
        }
        return "generic";
    }

    private static String dayDisplayName(String day) {
        return switch (day) {
            case "MONDAY" -> "Понедельник";
            case "TUESDAY" -> "Вторник";
            case "WEDNESDAY" -> "Среда";
            case "THURSDAY" -> "Четверг";
            case "FRIDAY" -> "Пятница";
            case "SATURDAY" -> "Суббота";
            default -> day;
        };
    }

    private static int minutesBetween(LocalTime from, LocalTime to) {
        return Math.abs((to.getHour() * 60 + to.getMinute()) - (from.getHour() * 60 + from.getMinute()));
    }

    private static boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String message) {
        return message == null
                ? ""
                : message.toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
    }

    public record CourseOption(
            Long sectionId,
            String courseCode,
            String courseName,
            String teacherName,
            String lessonType,
            int capacity,
            List<MeetingOption> meetingTimes
    ) {
    }

    public record MeetingOption(String dayOfWeek, String startTime, String endTime, String room) {
    }

    public record PlanningResult(
            boolean feasible,
            boolean partial,
            String chatResponse,
            String summary,
            List<String> satisfiedPreferences,
            List<String> unsatisfiedPreferences,
            List<String> blockingCourses,
            List<Long> selectedSectionIds,
            List<String> warnings
    ) {
    }

    private record CourseGroup(String courseCode, String courseName, List<CourseOption> options) {
    }

    private record SlotOccurrence(String courseCode, String courseName, LocalTime startTime, LocalTime endTime) {
    }

    private record DayPreference(
            LocalTime preferredStartFrom,
            LocalTime preferredStartTo,
            boolean avoidDay,
            String label
    ) {
        private DayPreference withLabel(String replacement) {
            return new DayPreference(preferredStartFrom, preferredStartTo, avoidDay, replacement);
        }
    }

    private record PreferenceProfile(
            Map<String, DayPreference> explicitByDay,
            DayPreference defaultForOtherDays,
            boolean preferCompactSchedule,
            List<String> rawPreferences
    ) {
    }

    private record DayTokens(Set<String> exact, Set<String> prefixes) {
    }

    private record DayEvaluation(boolean satisfied, int score, List<String> blockingCourses) {
    }

    private record FitEvaluation(
            int score,
            List<String> satisfiedPreferences,
            List<String> unsatisfiedPreferences,
            List<String> blockingCourses,
            List<String> warnings
    ) {
    }

    private record CandidatePlan(
            List<CourseOption> selectedSections,
            List<String> omittedCourseCodes,
            int score
    ) {
    }

    private static final class SearchState {
        private CandidatePlan best;
    }
}
