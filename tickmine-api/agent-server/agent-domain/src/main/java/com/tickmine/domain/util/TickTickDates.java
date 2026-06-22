package com.tickmine.domain.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TickTickDates {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter TICKTICK_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    private static final Pattern COMPACT_DATE =
            Pattern.compile("(\\d{4})[-]?(\\d{2})[-]?(\\d{2})");

    private static final Pattern TIME_OF_DAY = Pattern.compile("(\\d{1,2}):(\\d{2})");

    private TickTickDates() {}

    public static String normalize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text.isBlank() ? null : text.trim();
        }
        return String.valueOf(value);
    }

    public static ZoneId zone(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return DEFAULT_ZONE;
        }
        try {
            return ZoneId.of(timeZone);
        } catch (RuntimeException ignored) {
            return DEFAULT_ZONE;
        }
    }

    public static LocalDate parseDate(String raw) {
        return parseDate(raw, null);
    }

    public static LocalDate parseDate(String raw, String timeZone) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        ZoneId zone = zone(timeZone);
        String normalized = normalizeOffset(raw);
        try {
            return OffsetDateTime.parse(normalized, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .atZoneSameInstant(zone)
                    .toLocalDate();
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return LocalDate.parse(normalized.substring(0, Math.min(10, normalized.length())));
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        Matcher matcher = COMPACT_DATE.matcher(normalized);
        if (matcher.find()) {
            return LocalDate.of(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
        }
        return null;
    }

    static String normalizeOffset(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.endsWith("Z")) {
            return text.substring(0, text.length() - 1) + "+00:00";
        }
        if (text.matches(".*[+-]\\d{4}$")) {
            return text.substring(0, text.length() - 2) + ":" + text.substring(text.length() - 2);
        }
        return text;
    }

    /** @deprecated prefer {@link #sortKey(String, String, String)} for ordering */
    @Deprecated
    public static LocalDate effectiveDate(String dueDate, String startDate) {
        return sortKey(dueDate, startDate, null);
    }

    public static LocalDate[] dateRange(String startDate, String dueDate, String timeZone) {
        LocalDate start = parseDate(startDate, timeZone);
        LocalDate due = parseDate(dueDate, timeZone);
        if (start == null && due == null) {
            return null;
        }
        if (start == null) {
            start = due;
        }
        if (due == null) {
            due = start;
        }
        if (start.isAfter(due)) {
            LocalDate tmp = start;
            start = due;
            due = tmp;
        }
        return new LocalDate[] {start, due};
    }

    public static boolean isUndated(String startDate, String dueDate, String timeZone) {
        return dateRange(startDate, dueDate, timeZone) == null;
    }

    public static LocalDate sortKey(String startDate, String dueDate, String timeZone) {
        LocalDate[] range = dateRange(startDate, dueDate, timeZone);
        return range != null ? range[1] : null;
    }

    /** Includes overdue tasks, tasks due/active on {@code day}, and in-progress date ranges. */
    public static boolean matchesDay(String startDate, String dueDate, String timeZone, LocalDate day) {
        LocalDate[] range = dateRange(startDate, dueDate, timeZone);
        if (range == null) {
            return false;
        }
        if (range[1].isBefore(day)) {
            return true;
        }
        return !range[0].isAfter(day) && !range[1].isBefore(day);
    }

    public static boolean overlapsRange(
            String startDate, String dueDate, String timeZone, LocalDate rangeStart, LocalDate rangeEnd) {
        LocalDate[] range = dateRange(startDate, dueDate, timeZone);
        if (range == null) {
            return false;
        }
        return !range[1].isBefore(rangeStart) && !range[0].isAfter(rangeEnd);
    }

    public static String toTickTickInstant(LocalDate date, LocalTime time, ZoneId zone) {
        var zoned = time != null ? date.atTime(time).atZone(zone) : date.atStartOfDay(zone);
        return zoned.toInstant().atOffset(ZoneOffset.UTC).format(TICKTICK_FORMAT);
    }

    public static LocalTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim();
        try {
            return LocalTime.parse(text);
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        Matcher matcher = TIME_OF_DAY.matcher(text);
        if (matcher.find()) {
            return LocalTime.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
        }
        return null;
    }
}
