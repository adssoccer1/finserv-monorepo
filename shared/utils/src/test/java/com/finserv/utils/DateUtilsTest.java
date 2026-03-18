package com.finserv.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    // --- Happy path: UTC date conversion ---
    @Test
    void toLocalDate_utcMidnight_returnsCorrectDate() {
        // 2024-06-15 00:00:00 UTC
        Instant instant = LocalDate.of(2024, 6, 15)
                .atStartOfDay(ZoneId.of("UTC")).toInstant();
        Date date = Date.from(instant);
        assertEquals(LocalDate.of(2024, 6, 15), DateUtils.toLocalDate(date));
    }

    // --- Rejection: system timezone would produce wrong date ---
    @Test
    void toLocalDate_lateUtcTime_returnsUtcDateRegardlessOfSystemTimezone() {
        // 2024-06-15 23:30:00 UTC — in US/Eastern this is 2024-06-15 19:30 (same day)
        // but in Pacific/Auckland (+12) this would be 2024-06-16 11:30 (next day)
        TimeZone originalTz = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"));
            Instant instant = LocalDate.of(2024, 6, 15)
                    .atTime(23, 30).atZone(ZoneId.of("UTC")).toInstant();
            Date date = Date.from(instant);
            // Must return 2024-06-15 (UTC date), NOT 2024-06-16 (Auckland date)
            assertEquals(LocalDate.of(2024, 6, 15), DateUtils.toLocalDate(date));
        } finally {
            TimeZone.setDefault(originalTz);
        }
    }

    // --- Edge case: exactly midnight UTC boundary ---
    @Test
    void toLocalDate_exactlyMidnightUtc_returnsNewDay() {
        // 2024-01-01 00:00:00 UTC should be 2024-01-01, not 2023-12-31
        Instant midnight = LocalDate.of(2024, 1, 1)
                .atStartOfDay(ZoneId.of("UTC")).toInstant();
        Date date = Date.from(midnight);
        assertEquals(LocalDate.of(2024, 1, 1), DateUtils.toLocalDate(date));
    }
}
