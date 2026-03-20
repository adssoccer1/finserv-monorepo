package com.finserv.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    void toLocalDate_usesUtcNotSystemDefault() {
        // 2024-03-15 23:45 UTC — on US/Eastern this would be 2024-03-15 19:45 ET
        // but toLocalDate should return 2024-03-15 (UTC), not 2024-03-14
        ZonedDateTime utcTime = ZonedDateTime.of(2024, 3, 15, 23, 45, 0, 0, ZoneId.of("UTC"));
        Date date = Date.from(utcTime.toInstant());

        // Even if system TZ were US/Eastern, the result should be March 15
        LocalDate result = DateUtils.toLocalDate(date);
        assertEquals(LocalDate.of(2024, 3, 15), result);
    }

    @Test
    void toLocalDate_midnightUtcBoundary() {
        // Exactly midnight UTC should be on the new day
        ZonedDateTime midnight = ZonedDateTime.of(2024, 3, 16, 0, 0, 0, 0, ZoneId.of("UTC"));
        Date date = Date.from(midnight.toInstant());

        LocalDate result = DateUtils.toLocalDate(date);
        assertEquals(LocalDate.of(2024, 3, 16), result);
    }

    @Test
    void toLocalDate_justBeforeMidnightUtc() {
        // 23:59:59 UTC should still be on the same day
        ZonedDateTime justBefore = ZonedDateTime.of(2024, 3, 15, 23, 59, 59, 0, ZoneId.of("UTC"));
        Date date = Date.from(justBefore.toInstant());

        LocalDate result = DateUtils.toLocalDate(date);
        assertEquals(LocalDate.of(2024, 3, 15), result);
    }
}
