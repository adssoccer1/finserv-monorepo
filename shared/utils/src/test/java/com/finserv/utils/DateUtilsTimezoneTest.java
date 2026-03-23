package com.finserv.utils;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DateUtils.toLocalDate() — fixes Issue #3.
 */
class DateUtilsTimezoneTest {

    // Happy path: UTC midnight converts to same date
    @Test
    void utcMidnight_returnsCorrectDate() {
        ZonedDateTime utcMidnight = LocalDate.of(2026, 3, 15)
            .atStartOfDay(ZoneId.of("UTC"));
        Date date = Date.from(utcMidnight.toInstant());

        LocalDate result = DateUtils.toLocalDate(date);
        assertEquals(LocalDate.of(2026, 3, 15), result);
    }

    // Rejection/failure case: late evening UTC should NOT roll to next day
    @Test
    void lateEveningUtc_returnsSameDay() {
        ZonedDateTime lateUtc = ZonedDateTime.of(2026, 3, 15, 23, 59, 59, 0,
            ZoneId.of("UTC"));
        Date date = Date.from(lateUtc.toInstant());

        LocalDate result = DateUtils.toLocalDate(date);
        assertEquals(LocalDate.of(2026, 3, 15), result);
    }

    // Edge case: timestamp that would be "yesterday" in US/Eastern but "today" in UTC
    // At 7 PM ET (00:00 UTC next day), toLocalDate should return the UTC date
    @Test
    void boundaryTimestamp_usesUtcNotSystemTimezone() {
        // 2026-03-16 00:30 UTC = 2026-03-15 19:30 US/Eastern
        ZonedDateTime justAfterMidnightUtc = ZonedDateTime.of(2026, 3, 16, 0, 30, 0, 0,
            ZoneId.of("UTC"));
        Date date = Date.from(justAfterMidnightUtc.toInstant());

        LocalDate result = DateUtils.toLocalDate(date);
        // Must return March 16 (UTC date), not March 15 (Eastern date)
        assertEquals(LocalDate.of(2026, 3, 16), result);
    }
}
