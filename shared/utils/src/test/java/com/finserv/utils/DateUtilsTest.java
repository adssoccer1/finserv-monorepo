package com.finserv.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    // === Issue #3: toLocalDate must use UTC, not system default ===

    @Test
    @DisplayName("#3 happy path: UTC midnight date converts correctly")
    void toLocalDate_utcMidnight_returnsCorrectDate() {
        // 2024-03-15 00:00:00 UTC
        Instant instant = LocalDate.of(2024, 3, 15)
                .atStartOfDay(ZoneId.of("UTC")).toInstant();
        Date date = Date.from(instant);
        assertEquals(LocalDate.of(2024, 3, 15), DateUtils.toLocalDate(date));
    }

    @Test
    @DisplayName("#3 rejection: late-night UTC timestamp must NOT be bucketed to previous day")
    void toLocalDate_lateNightUtc_returnsCorrectUtcDate() {
        // 2024-03-15 23:30:00 UTC — on a US/Eastern server this would be 2024-03-15 7:30 PM
        // Without the fix, a server in US/Eastern would return 2024-03-15 correctly,
        // but a 00:30 UTC timestamp would incorrectly return 2024-03-14
        Instant instant = Instant.parse("2024-03-15T00:30:00Z");
        Date date = Date.from(instant);
        // This should be March 15 in UTC regardless of system timezone
        assertEquals(LocalDate.of(2024, 3, 15), DateUtils.toLocalDate(date));
    }

    @Test
    @DisplayName("#3 edge: date at exactly midnight UTC boundary")
    void toLocalDate_exactMidnightUtc_returnsCorrectDate() {
        Instant instant = Instant.parse("2024-03-15T00:00:00Z");
        Date date = Date.from(instant);
        assertEquals(LocalDate.of(2024, 3, 15), DateUtils.toLocalDate(date));
    }
}
