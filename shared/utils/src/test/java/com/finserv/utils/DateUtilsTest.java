package com.finserv.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    void toLocalDate_returnsCorrectDateForUTCTimestamp() {
        // 2024-03-15 23:45 UTC should be March 15, not March 14
        Instant instant = Instant.parse("2024-03-15T23:45:00Z");
        Date date = Date.from(instant);
        LocalDate result = DateUtils.toLocalDate(date);
        assertEquals(LocalDate.of(2024, 3, 15), result);
    }

    @Test
    void toLocalDate_handlesMiddleOfDay() {
        Instant instant = Instant.parse("2024-03-15T12:00:00Z");
        Date date = Date.from(instant);
        LocalDate result = DateUtils.toLocalDate(date);
        assertEquals(LocalDate.of(2024, 3, 15), result);
    }

    @Test
    void toLocalDate_handlesMidnightBoundary() {
        // Exactly midnight UTC should be that day
        Instant instant = Instant.parse("2024-03-15T00:00:00Z");
        Date date = Date.from(instant);
        LocalDate result = DateUtils.toLocalDate(date);
        assertEquals(LocalDate.of(2024, 3, 15), result);
    }

    @Test
    void startOfDay_returnsStartInUTC() {
        LocalDate date = LocalDate.of(2024, 3, 15);
        Date result = DateUtils.startOfDay(date);
        Instant expected = Instant.parse("2024-03-15T00:00:00Z");
        assertEquals(expected, result.toInstant());
    }

    @Test
    void endOfDay_returnsEndInUTC() {
        LocalDate date = LocalDate.of(2024, 3, 15);
        Date result = DateUtils.endOfDay(date);
        Instant expected = Instant.parse("2024-03-15T23:59:59Z");
        assertEquals(expected, result.toInstant());
    }

    @Test
    void formatDate_producesISOFormat() {
        LocalDate date = LocalDate.of(2024, 3, 15);
        assertEquals("2024-03-15", DateUtils.formatDate(date));
    }

    @Test
    void parseDate_parsesISOFormat() {
        LocalDate result = DateUtils.parseDate("2024-03-15");
        assertEquals(LocalDate.of(2024, 3, 15), result);
    }
}
