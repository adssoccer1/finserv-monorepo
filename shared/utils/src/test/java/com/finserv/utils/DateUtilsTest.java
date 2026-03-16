package com.finserv.utils;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DateUtils.toLocalDate() — verifies that UTC is used
 * explicitly instead of system default timezone (fix for Issue #3).
 */
class DateUtilsTest {

    // Happy path: a midday UTC timestamp returns the correct UTC date
    @Test
    void toLocalDate_middayUTC_returnsCorrectDate() {
        ZonedDateTime utcNoon = ZonedDateTime.of(2024, 3, 15, 12, 0, 0, 0, ZoneId.of("UTC"));
        Date date = Date.from(utcNoon.toInstant());
        assertEquals(LocalDate.of(2024, 3, 15), DateUtils.toLocalDate(date));
    }

    // Regression: 23:45 UTC must still be March 15, not March 14
    // (this was the exact scenario reported in the bug)
    @Test
    void toLocalDate_lateNightUTC_returnsUTCDate() {
        ZonedDateTime lateUtc = ZonedDateTime.of(2024, 3, 15, 23, 45, 0, 0, ZoneId.of("UTC"));
        Date date = Date.from(lateUtc.toInstant());
        assertEquals(LocalDate.of(2024, 3, 15), DateUtils.toLocalDate(date));
    }

    // Edge case: midnight UTC boundary — 00:00 of March 16 is March 16
    @Test
    void toLocalDate_midnightUTC_returnsSameDay() {
        ZonedDateTime midnight = ZonedDateTime.of(2024, 3, 16, 0, 0, 0, 0, ZoneId.of("UTC"));
        Date date = Date.from(midnight.toInstant());
        assertEquals(LocalDate.of(2024, 3, 16), DateUtils.toLocalDate(date));
    }

    // Edge case: just before midnight UTC — 23:59:59 is still same day
    @Test
    void toLocalDate_justBeforeMidnightUTC_returnsSameDay() {
        ZonedDateTime justBefore = ZonedDateTime.of(2024, 3, 15, 23, 59, 59, 0, ZoneId.of("UTC"));
        Date date = Date.from(justBefore.toInstant());
        assertEquals(LocalDate.of(2024, 3, 15), DateUtils.toLocalDate(date));
    }
}
