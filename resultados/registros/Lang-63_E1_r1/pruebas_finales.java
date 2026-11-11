package org.apache.commons.lang.time;

import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Pruebas adicionales para {@link DurationFormatUtils} que usan solo su API pública.
 */
public class DurationFormatUtilsLLMTest {

    private static final long ONE_SECOND = 1000L;
    private static final long ONE_MINUTE = 60L * ONE_SECOND;
    private static final long ONE_HOUR = 60L * ONE_MINUTE;
    private static final long ONE_DAY = 24L * ONE_HOUR;

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /** Construye un instante en milisegundos en UTC, sin depender de la zona por defecto. */
    private static long utcMillis(int year, int month, int day, int hour, int minute, int second, int millis) {
        GregorianCalendar gc = new GregorianCalendar(UTC);
        gc.clear();
        gc.set(year, month, day, hour, minute, second);
        gc.set(GregorianCalendar.MILLISECOND, millis);
        return gc.getTimeInMillis();
    }

    // ------------------------------------------------------------------
    // Constructor y constantes

    @Test
    public void testConstructorCreatesInstance() {
        assertNotNull(new DurationFormatUtils());
    }

    @Test
    public void testIsoExtendedFormatPatternConstantValue() {
        assertEquals("'P'yyyy'Y'M'M'd'DT'H'H'm'M's.S'S'", DurationFormatUtils.ISO_EXTENDED_FORMAT_PATTERN);
    }

    // ------------------------------------------------------------------
    // formatDurationHMS

    @Test
    public void testFormatDurationHMSHoursExceedOneDay() {
        // Sin token d, las horas no se reducen a días.
        assertEquals("25:00:00.000", DurationFormatUtils.formatDurationHMS(25 * ONE_HOUR));
    }

    @Test
    public void testFormatDurationHMSMillisecondsOnly() {
        assertEquals("0:00:01.999", DurationFormatUtils.formatDurationHMS(1999L));
    }

    // ------------------------------------------------------------------
    // formatDurationISO

    @Test
    public void testFormatDurationISOOneDay() {
        assertEquals("P0Y0M1DT0H0M0.000S", DurationFormatUtils.formatDurationISO(ONE_DAY));
    }

    @Test
    public void testFormatDurationISOHoursMinutesSecondsMillis() {
        // 1 h 30 min 5 s 250 ms
        long duration = ONE_HOUR + 30 * ONE_MINUTE + 5 * ONE_SECOND + 250L;
        assertEquals("P0Y0M0DT1H30M5.250S", DurationFormatUtils.formatDurationISO(duration));
    }

    // ------------------------------------------------------------------
    // formatDuration

    @Test
    public void testFormatDurationPaddedMinutesAndSeconds() {
        // 2 min 3.456 s
        assertEquals("02:03", DurationFormatUtils.formatDuration(123456L, "mm:ss"));
    }

    @Test
    public void testFormatDurationUnpaddedMinutesAndSeconds() {
        // 1 min 5 s sin relleno
        assertEquals("1:5", DurationFormatUtils.formatDuration(65 * ONE_SECOND, "m:ss", false));
    }

    @Test
    public void testFormatDurationMillisecondsCarryIntoSeconds() {
        // 1999 ms -> 1 s y 999 ms
        assertEquals("1.999", DurationFormatUtils.formatDuration(1999L, "s.SSS"));
    }

    @Test
    public void testFormatDurationDaysHoursMinutesSecondsMillis() {
        // 1 día, 1 h, 1 min, 1 s, 1 ms
        long duration = ONE_DAY + ONE_HOUR + ONE_MINUTE + ONE_SECOND + 1L;
        assertEquals("1 days 1:01:01.001", DurationFormatUtils.formatDuration(duration, "d' days 'H:mm:ss.SSS"));
    }

    @Test
    public void testFormatDurationWithoutDayTokenKeepsAccumulatedHours() {
        long duration = ONE_DAY + ONE_HOUR + ONE_MINUTE + ONE_SECOND + 1L;
        assertEquals("25:01:01.001", DurationFormatUtils.formatDuration(duration, "H:mm:ss.SSS"));
    }

    @Test
    public void testFormatDurationQuotedLiteralText() {
        assertEquals("Total: 0", DurationFormatUtils.formatDuration(0L, "'Total: 'm"));
    }

    @Test
    public void testFormatDurationUnquotedNonTokenCharactersAreKept() {
        assertEquals("a-b", DurationFormatUtils.formatDuration(0L, "a-b"));
    }

    @Test
    public void testFormatDurationMillisecondPaddingWidth() {
        assertEquals("00007", DurationFormatUtils.formatDuration(7L, "SSSSS"));
        assertEquals("7", DurationFormatUtils.formatDuration(7L, "SSSSS", false));
    }

    @Test
    public void testFormatDurationDaysBeyondMonthLength() {
        assertEquals("40", DurationFormatUtils.formatDuration(40 * ONE_DAY, "d"));
    }

    // ------------------------------------------------------------------
    // formatDurationWords

    @Test
    public void testFormatDurationWordsSingularSecondWithSuppression() {
        assertEquals("1 second", DurationFormatUtils.formatDurationWords(ONE_SECOND, true, true));
    }

    @Test
    public void testFormatDurationWordsDaysAndHoursWithTrailingSuppression() {
        long duration = 2 * ONE_DAY + 3 * ONE_HOUR;
        assertEquals("2 days 3 hours", DurationFormatUtils.formatDurationWords(duration, true, true));
    }

    // ------------------------------------------------------------------
    // formatPeriod

    @Test
    public void testFormatPeriodShortRangeDelegatesToDurationFormatting() {
        // Rango menor de 28 días: se usa formatDuration.
        assertEquals("1 days 1:01:01.001",
                DurationFormatUtils.formatPeriod(0L, ONE_DAY + ONE_HOUR + ONE_MINUTE + ONE_SECOND + 1L,
                        "d' days 'H:mm:ss.SSS", true, UTC));
    }

    @Test
    public void testFormatPeriodAcrossMonthWithTimeFields() {
        // 1970-01-01 00:00:00.000 -> 1970-02-01 12:30:45.250 (UTC)
        long start = utcMillis(1970, 0, 1, 0, 0, 0, 0);
        long end = utcMillis(1970, 1, 1, 12, 30, 45, 250);
        assertEquals("31 days 12:30:45.250",
                DurationFormatUtils.formatPeriod(start, end, "d' days 'H:mm:ss.SSS", true, UTC));
    }

    @Test
    public void testFormatPeriodMonthsAndDaysAcrossFebruary2000() {
        // 2000-01-15 -> 2000-03-10: 1 mes y 24 días (conteo mes primero)
        long start = utcMillis(2000, 0, 15, 0, 0, 0, 0);
        long end = utcMillis(2000, 2, 10, 0, 0, 0, 0);
        assertEquals("1 months 24 days",
                DurationFormatUtils.formatPeriod(start, end, "M' months 'd' days'", true, UTC));
    }

    @Test
    public void testFormatPeriodYearsZeroPadded() {
        long start = utcMillis(1970, 0, 1, 0, 0, 0, 0);
        long end = utcMillis(1974, 0, 1, 0, 0, 0, 0);
        assertEquals("0004", DurationFormatUtils.formatPeriod(start, end, "yyyy", true, UTC));
    }

    @Test
    public void testFormatPeriodYearsUnpadded() {
        long start = utcMillis(1970, 0, 1, 0, 0, 0, 0);
        long end = utcMillis(1974, 0, 1, 0, 0, 0, 0);
        assertEquals("4", DurationFormatUtils.formatPeriod(start, end, "yyyy", false, UTC));
    }
}
