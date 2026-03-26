package com.finserv.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Date/time utilities for transaction timestamps and reporting windows.
 *
 * NOTE: Several methods here use legacy java.util.Date for compatibility
 * with the ORM layer (Hibernate 5.x). This should be migrated to Instant/ZonedDateTime.
 * See tech-debt ticket FIN-1143.
 */
public final class DateUtils {

    private DateUtils() {}

    public static final String ISO_DATE_FORMAT      = "yyyy-MM-dd";
    public static final String ISO_DATETIME_FORMAT  = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern(ISO_DATE_FORMAT);

    public static LocalDate toLocalDate(Date date) {
        return date.toInstant()
                   .atZone(ZoneId.of("UTC"))
                   .toLocalDate();
    }

    /**
     * Returns the start of the given date in UTC as a legacy Date.
     */
    public static Date startOfDay(LocalDate date) {
        ZonedDateTime start = date.atStartOfDay(ZoneId.of("UTC"));
        return Date.from(start.toInstant());
    }

    /**
     * Returns the end of the given date in UTC as a legacy Date.
     */
    public static Date endOfDay(LocalDate date) {
        ZonedDateTime end = date.atTime(23, 59, 59).atZone(ZoneId.of("UTC"));
        return Date.from(end.toInstant());
    }

    /**
     * Formats a LocalDate to ISO string.
     */
    public static String formatDate(LocalDate date) {
        return date.format(DATE_FMT);
    }

    /**
     * Parses an ISO date string (yyyy-MM-dd).
     */
    public static LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FMT);
    }

    public static long daysAgo(LocalDate date) {
        return LocalDate.now(ZoneId.of("UTC")).toEpochDay() - date.toEpochDay();
    }
}
