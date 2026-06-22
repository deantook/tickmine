package com.tickmine.domain.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class TickTickDatesTest {

    @Test
    void normalizeOffset_supportsTickTickFormat() {
        assertEquals("2026-06-21T16:00:00+00:00", TickTickDates.normalizeOffset("2026-06-21T16:00:00+0000"));
        assertEquals("2026-06-22T07:00:00+00:00", TickTickDates.normalizeOffset("2026-06-22T07:00:00Z"));
    }

    @Test
    void parseDate_isoOffsetDateTime_usesTaskTimezone() {
        assertEquals(
                LocalDate.of(2026, 6, 22),
                TickTickDates.parseDate("2026-06-21T16:00:00+0000", "Asia/Shanghai"));
    }

    @Test
    void parseDate_isoOffsetDateTime_defaultsToShanghai() {
        assertEquals(LocalDate.of(2026, 6, 22), TickTickDates.parseDate("2026-06-22T00:00:00+0000"));
    }

    @Test
    void parseDate_compactFormat() {
        assertEquals(LocalDate.of(2026, 6, 22), TickTickDates.parseDate("20260622T070000000Z"));
    }

    @Test
    void toTickTickInstant_allDay_usesLocalMidnight() {
        assertEquals(
                "2026-06-21T16:00:00+0000",
                TickTickDates.toTickTickInstant(
                        LocalDate.of(2026, 6, 22), null, TickTickDates.DEFAULT_ZONE));
    }

    @Test
    void toTickTickInstant_timedTask() {
        assertEquals(
                "2026-06-22T07:00:00+0000",
                TickTickDates.toTickTickInstant(
                        LocalDate.of(2026, 6, 22), LocalTime.of(15, 0), TickTickDates.DEFAULT_ZONE));
    }

    @Test
    void matchesDay_includesOverdueAndActiveRange() {
        LocalDate today = LocalDate.of(2026, 6, 22);
        assertTrue(TickTickDates.matchesDay(
                "2026-06-20T00:00:00+0000", "2026-06-20T00:00:00+0000", "Asia/Shanghai", today));
        assertTrue(TickTickDates.matchesDay(
                "2026-06-21T16:00:00+0000", "2026-06-21T16:00:00+0000", "Asia/Shanghai", today));
        assertFalse(TickTickDates.matchesDay(
                "2026-06-23T00:00:00+0000", "2026-06-25T00:00:00+0000", "Asia/Shanghai", today));
        assertTrue(TickTickDates.matchesDay(
                "2026-06-23T00:00:00+0000", "2026-06-25T00:00:00+0000", "Asia/Shanghai", LocalDate.of(2026, 6, 24)));
    }

    @Test
    void overlapsRange_weekScope() {
        LocalDate today = LocalDate.of(2026, 6, 22);
        assertTrue(TickTickDates.overlapsRange(
                "2026-06-23T00:00:00+0000", "2026-06-25T00:00:00+0000", "Asia/Shanghai", today, today.plusDays(6)));
        assertFalse(TickTickDates.overlapsRange(
                "2026-07-01T00:00:00+0000", "2026-07-02T00:00:00+0000", "Asia/Shanghai", today, today.plusDays(6)));
    }

    @Test
    void parseTime_acceptsHourMinute() {
        assertEquals(LocalTime.of(15, 0), TickTickDates.parseTime("15:00"));
    }

    @Test
    void normalize_nonStringValue() {
        assertEquals("20260622", TickTickDates.normalize(20260622L));
    }

    @Test
    void normalize_blankString() {
        assertNull(TickTickDates.normalize("  "));
    }
}
