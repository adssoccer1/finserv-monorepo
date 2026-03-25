package com.finserv.utils;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    void toLocalDate_shouldUseUtcTimezone() {
        // 2024-01-15 23:30:00 UTC — in US/Eastern this would be Jan 15 at 6:30 PM,
        // but in UTC+5 it would be Jan 16. We want UTC date = Jan 15.
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2024, Calendar.JANUARY, 15, 23, 30, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        LocalDate result = DateUtils.toLocalDate(date);
        assertEquals(LocalDate.of(2024, 1, 15), result);
    }

    @Test
    void toLocalDate_shouldNotUseSystemDefault() {
        // Midnight UTC on Jan 16 — in a negative-offset timezone like US/Pacific
        // this would be Jan 15 if system default were used.
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2024, Calendar.JANUARY, 16, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        LocalDate result = DateUtils.toLocalDate(date);
        assertEquals(LocalDate.of(2024, 1, 16), result);
    }

    @Test
    void toLocalDate_edgeCase_exactMidnightUtc() {
        // Exactly midnight UTC should resolve to that day, not the previous day
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2024, Calendar.MARCH, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        LocalDate result = DateUtils.toLocalDate(date);
        assertEquals(LocalDate.of(2024, 3, 1), result);
    }
}
