package org.apache.commons.lang.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.TimeZone;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link DurationFormatUtils} que cubren comportamientos
 * no verificados por {@code DurationFormatUtilsTest}. Solo usan la API pública.
 */
public class DurationFormatUtilsLLMTest {

    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long MILLIS_PER_MINUTE = 60L * MILLIS_PER_SECOND;
    private static final long MILLIS_PER_HOUR = 60L * MILLIS_PER_MINUTE;
    private static final long MILLIS_PER_DAY = 24L * MILLIS_PER_HOUR;

    private static TimeZone utc() {
        return TimeZone.getTimeZone("UTC");
    }

    /** Construye un instante en milisegundos en UTC. */
    private static long utcMillis(int year, int month, int day) {
        Calendar c = Calendar.getInstance(utc());
        c.clear();
        c.set(year, month, day, 0, 0, 0);
        return c.getTimeInMillis();
    }

    // -----------------------------------------------------------------------
    // 1. formatDuration(long, String, boolean) con padWithZeros = false

    @Test
    public void testFormatDurationWithoutPaddingOmitsLeadingZeros() {
        // 1 minuto con "mm:ss" sin relleno: minutos "1", segundos "0"
        assertEquals("1:0", DurationFormatUtils.formatDuration(MILLIS_PER_MINUTE, "mm:ss", false));
        // Con relleno el mismo patrón produce "01:00"
        assertEquals("01:00", DurationFormatUtils.formatDuration(MILLIS_PER_MINUTE, "mm:ss", true));
    }

    @Test
    public void testFormatDurationWithoutPaddingHMSPattern() {
        // 1 h, 2 min, 3 s, 4 ms
        long duration = 3723004L;
        assertEquals("1:2:3.004", DurationFormatUtils.formatDuration(duration, "H:mm:ss.SSS", false));
        assertEquals("01:02:03.004", DurationFormatUtils.formatDuration(duration, "HH:mm:ss.SSS", true));
    }

    @Test
    public void testFormatDurationWithoutPaddingMilliseconds() {
        assertEquals("5", DurationFormatUtils.formatDuration(5L, "SSS", false));
        assertEquals("005", DurationFormatUtils.formatDuration(5L, "SSS", true));
    }

    @Test
    public void testFormatDurationWithoutPaddingZeroDuration() {
        assertEquals("0", DurationFormatUtils.formatDuration(0L, "d", false));
        assertEquals("0", DurationFormatUtils.formatDuration(0L, "H", false));
    }

    // -----------------------------------------------------------------------
    // 2. Patrón sin token d: las horas no se reducen a módulo 24

    @Test
    public void testHoursAreNotReducedModuloDayWhenDayTokenAbsent() {
        long twentyFiveHours = 25L * MILLIS_PER_HOUR;
        assertEquals("25", DurationFormatUtils.formatDuration(twentyFiveHours, "H"));
        assertEquals("25:30",
                DurationFormatUtils.formatDuration(twentyFiveHours + 30L * MILLIS_PER_MINUTE, "H:mm"));
    }

    @Test
    public void testHoursAreReducedModuloDayWhenDayTokenPresent() {
        // Con token d, 25 h = 1 día + 1 hora
        long twentyFiveHours = 25L * MILLIS_PER_HOUR;
        assertEquals("1 1", DurationFormatUtils.formatDuration(twentyFiveHours, "d H"));
    }

    // -----------------------------------------------------------------------
    // 3. Duraciones negativas (characterización del comportamiento actual)

    @Test
    public void testFormatDurationNegativeMinutesCurrentBehavior() {
        // Comportamiento observado: la división entera produce -1 minuto.
        assertEquals("-1", DurationFormatUtils.formatDuration(-MILLIS_PER_MINUTE, "m"));
    }

    @Test
    public void testFormatDurationNegativeSecondsCurrentBehavior() {
        assertEquals("-1", DurationFormatUtils.formatDuration(-MILLIS_PER_SECOND, "s"));
    }

    @Test
    public void testFormatPeriodWithEndBeforeStartCurrentBehavior() {
        // endMillis < startMillis: se delega en formatDuration con diferencia negativa.
        long start = utcMillis(2000, Calendar.JANUARY, 1);
        long end = start - MILLIS_PER_MINUTE;
        assertEquals("-1", DurationFormatUtils.formatPeriod(start, end, "m", true, utc()));
    }

    // -----------------------------------------------------------------------
    // 4. Sobrecarga formatPeriod(start, end, format) con zona horaria por defecto

    @Test
    public void testFormatPeriodDefaultOverloadShortDuration() {
        long start = 0L;
        long end = start + MILLIS_PER_MINUTE;
        assertEquals("1", DurationFormatUtils.formatPeriod(start, end, "m"));
        assertEquals("01:00", DurationFormatUtils.formatPeriod(start, end, "mm:ss"));
    }

    @Test
    public void testFormatPeriodDefaultOverloadMatchesExplicitDefaultTimeZone() {
        Calendar start = Calendar.getInstance();
        start.clear();
        start.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
        Calendar end = Calendar.getInstance();
        end.clear();
        end.set(2000, Calendar.MARCH, 1, 0, 0, 0);

        long s = start.getTimeInMillis();
        long e = end.getTimeInMillis();

        // 60 días entre 1 de enero y 1 de marzo de 2000 (año bisiesto)
        String viaDefaultOverload = DurationFormatUtils.formatPeriod(s, e, "d");
        String viaExplicit = DurationFormatUtils.formatPeriod(s, e, "d", true, TimeZone.getDefault());
        assertEquals(viaExplicit, viaDefaultOverload);
        assertEquals("60", viaDefaultOverload);
    }

    @Test
    public void testFormatPeriodDefaultOverloadMonths() {
        Calendar start = Calendar.getInstance();
        start.clear();
        start.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
        Calendar end = Calendar.getInstance();
        end.clear();
        end.set(2000, Calendar.MARCH, 1, 0, 0, 0);

        assertEquals("2", DurationFormatUtils.formatPeriod(start.getTimeInMillis(), end.getTimeInMillis(), "M"));
    }

    // -----------------------------------------------------------------------
    // 5. Frontera de 28 días en formatPeriod

    @Test
    public void testFormatPeriodJustBelow28DaysDelegatesToFormatDuration() {
        long start = 0L;
        long end = start + 28L * MILLIS_PER_DAY - 1L;
        // Delegación en formatDuration: 27 días completos
        assertEquals("27", DurationFormatUtils.formatPeriod(start, end, "d", true, utc()));
        assertEquals(DurationFormatUtils.formatDuration(end - start, "d"),
                DurationFormatUtils.formatPeriod(start, end, "d", true, utc()));
    }

    @Test
    public void testFormatPeriodExactly28DaysUsesCalendarPath() {
        long start = 0L;
        long end = start + 28L * MILLIS_PER_DAY;
        // Con 28 días exactos se usa el cálculo calendario (no formatDuration, que daría "28").
        // Resultado observado: 28 (diferencia de día del mes) + 28 (diferencia de día del año).
        assertEquals("56", DurationFormatUtils.formatPeriod(start, end, "d", true, utc()));
        assertFalse("En la frontera no se debe delegar en formatDuration",
                "28".equals(DurationFormatUtils.formatPeriod(start, end, "d", true, utc())));
    }

    // -----------------------------------------------------------------------
    // 6. Rama de días negativos en formatPeriod (sumar 31 días y restar un mes)

    @Test
    public void testFormatPeriodNegativeDayAdjustmentBorrowsMonth() {
        // 15 de enero a 10 de marzo de 2000: 1 mes y 24 días (contando mes primero)
        long start = utcMillis(2000, Calendar.JANUARY, 15);
        long end = utcMillis(2000, Calendar.MARCH, 10);
        assertEquals("1 months 24 days",
                DurationFormatUtils.formatPeriod(start, end, "M' months 'd' days'", true, utc()));
    }

    @Test
    public void testFormatPeriodNegativeDayAdjustmentWithoutPadding() {
        long start = utcMillis(2000, Calendar.JANUARY, 15);
        long end = utcMillis(2000, Calendar.MARCH, 10);
        assertEquals("1:24", DurationFormatUtils.formatPeriod(start, end, "M:d", false, utc()));
    }

    // -----------------------------------------------------------------------
    // 7. formatDurationWords con duración 0

    @Test
    public void testFormatDurationWordsZeroWithLeadingSuppressionIsNotEmpty() {
        // Comportamiento observado: la supresión de ceros iniciales deja "0 seconds".
        assertEquals("0 seconds", DurationFormatUtils.formatDurationWords(0L, true, false));
        assertEquals("0 seconds", DurationFormatUtils.formatDurationWords(0L, true, true));
    }

    @Test
    public void testFormatDurationWordsZeroWithoutSuppression() {
        assertEquals("0 days 0 hours 0 minutes 0 seconds",
                DurationFormatUtils.formatDurationWords(0L, false, false));
    }

    // -----------------------------------------------------------------------
    // 8. Escape de comillas simples dobles ('') en el patrón

    @Test
    public void testDoubleQuoteInsideLiteralIsNotEscapedAsApostrophe() {
        // "a''b": '' se trata como literal vacío, no como apóstrofo.
        String result = DurationFormatUtils.formatDuration(0L, "a''b");
        assertEquals("ab", result);
        assertFalse("No debe aparecer un apóstrofo", result.contains("'"));
    }
}
