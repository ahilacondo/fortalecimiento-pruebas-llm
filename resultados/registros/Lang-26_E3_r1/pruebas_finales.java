package org.apache.commons.lang3.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link FastDateFormat} que cubren comportamientos
 * no verificados por {@link FastDateFormatTest}. Solo usan la API pública.
 */
public class FastDateFormatLLMTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /** Milisegundos para una fecha/hora UTC. El mes es 1-based. */
    private static long utcMillis(int year, int month, int day, int hour, int minute, int second, int millis) {
        GregorianCalendar c = new GregorianCalendar(UTC);
        c.clear();
        c.set(year, month - 1, day, hour, minute, second);
        c.set(Calendar.MILLISECOND, millis);
        return c.getTimeInMillis();
    }

    /** Calendario con la zona indicada apuntando al instante dado. */
    private static Calendar calendarAt(long millis, TimeZone tz) {
        GregorianCalendar c = new GregorianCalendar(tz);
        c.setTimeInMillis(millis);
        return c;
    }

    /** Salida de referencia de SimpleDateFormat. */
    private static String referenceFormat(String pattern, TimeZone tz, Locale locale, long millis) {
        SimpleDateFormat f = new SimpleDateFormat(pattern, locale);
        f.setTimeZone(tz);
        return f.format(new Date(millis));
    }

    // ------------------------------------------------------------------
    // 1. Salida de format(Date), format(long) y format(Calendar)
    // ------------------------------------------------------------------

    @Test
    public void formatDateLongAndCalendarMatchSimpleDateFormat() {
        String[] patterns = {
            "yyyy-MM-dd HH:mm:ss.SSS",
            "EEE, d MMM yyyy HH:mm:ss",
            "EEEE, MMMM d, yyyy h:mm a",
            "dd/MM/yy HH:mm",
            "D w W F"
        };
        long millis = utcMillis(2003, 1, 10, 15, 33, 20, 123);

        for (String pattern : patterns) {
            FastDateFormat f = FastDateFormat.getInstance(pattern, UTC, Locale.US);
            String expected = referenceFormat(pattern, UTC, Locale.US, millis);
            assertEquals("format(Date) con patrón " + pattern, expected, f.format(new Date(millis)));
            assertEquals("format(long) con patrón " + pattern, expected, f.format(millis));
            assertEquals("format(Calendar) con patrón " + pattern,
                    expected, f.format(calendarAt(millis, UTC)));
        }
    }

    @Test
    public void formatKnownValueWithMilliseconds() {
        long millis = utcMillis(2003, 1, 10, 15, 33, 20, 123);
        FastDateFormat f = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSS", UTC, Locale.US);
        assertEquals("2003-01-10 15:33:20.123", f.format(millis));
    }

    @Test
    public void formatPadsMillisecondsToThreeDigits() {
        long millis = utcMillis(2003, 1, 10, 0, 0, 0, 7);
        FastDateFormat f = FastDateFormat.getInstance("SSS", UTC, Locale.US);
        assertEquals("007", f.format(millis));
    }

    @Test
    public void formatIntoBufferAppendsToExistingContent() {
        long millis = utcMillis(2003, 1, 10, 15, 33, 20, 0);
        FastDateFormat f = FastDateFormat.getInstance("yyyy-MM-dd", UTC, Locale.US);
        StringBuffer sb = new StringBuffer("X=");
        StringBuffer result = f.format(millis, sb);
        assertTrue("debe devolver el mismo buffer", result == sb);
        assertEquals("X=2003-01-10", sb.toString());
    }

    // ------------------------------------------------------------------
    // 2. Tokens Z y ZZ con offsets positivos y negativos
    // ------------------------------------------------------------------

    @Test
    public void numericZoneOffsetPositiveOffsets() {
        long millis = utcMillis(2003, 1, 10, 15, 33, 20, 0);
        TimeZone plus8 = TimeZone.getTimeZone("GMT+08:00");
        TimeZone plus0530 = TimeZone.getTimeZone("GMT+05:30");

        assertEquals("+0800", FastDateFormat.getInstance("Z", plus8, Locale.US).format(millis));
        assertEquals("+08:00", FastDateFormat.getInstance("ZZ", plus8, Locale.US).format(millis));
        assertEquals("+0530", FastDateFormat.getInstance("Z", plus0530, Locale.US).format(millis));
        assertEquals("+05:30", FastDateFormat.getInstance("ZZ", plus0530, Locale.US).format(millis));
    }

    @Test
    public void numericZoneOffsetNegativeOffsets() {
        long millis = utcMillis(2003, 1, 10, 15, 33, 20, 0);
        TimeZone minus5 = TimeZone.getTimeZone("GMT-05:00");

        assertEquals("-0500", FastDateFormat.getInstance("Z", minus5, Locale.US).format(millis));
        assertEquals("-05:00", FastDateFormat.getInstance("ZZ", minus5, Locale.US).format(millis));
    }

    @Test
    public void numericZoneOffsetZeroOffset() {
        long millis = utcMillis(2003, 1, 10, 15, 33, 20, 0);
        assertEquals("+0000", FastDateFormat.getInstance("Z", UTC, Locale.US).format(millis));
        assertEquals("+00:00", FastDateFormat.getInstance("ZZ", UTC, Locale.US).format(millis));
    }

    @Test
    public void numericZoneOffsetFollowsDaylightSavingTime() {
        TimeZone ny = TimeZone.getTimeZone("America/New_York");
        long winter = utcMillis(2003, 1, 10, 20, 0, 0, 0);
        long summer = utcMillis(2003, 7, 10, 16, 0, 0, 0);
        FastDateFormat z = FastDateFormat.getInstance("Z", ny, Locale.US);
        assertEquals("-0500", z.format(winter));
        assertEquals("-0400", z.format(summer));
    }

    @Test
    public void numericZoneOffsetWithoutForcedZoneUsesCalendarZone() {
        TimeZone minus5 = TimeZone.getTimeZone("GMT-05:00");
        long millis = utcMillis(2003, 1, 10, 15, 33, 20, 0);
        FastDateFormat f = FastDateFormat.getInstance("ZZ", (TimeZone) null, Locale.US);
        assertFalse(f.getTimeZoneOverridesCalendar());
        assertEquals("-05:00", f.format(calendarAt(millis, minus5)));
    }

    // ------------------------------------------------------------------
    // 3. Tokens z y zzzz (nombre de zona horaria)
    // ------------------------------------------------------------------

    @Test
    public void zoneNameForcedMatchesSimpleDateFormatWinterAndSummer() {
        TimeZone ny = TimeZone.getTimeZone("America/New_York");
        long winter = utcMillis(2003, 1, 10, 20, 0, 0, 0);
        long summer = utcMillis(2003, 7, 10, 16, 0, 0, 0);

        for (String pattern : new String[] {"z", "zzzz"}) {
            FastDateFormat f = FastDateFormat.getInstance(pattern, ny, Locale.US);
            assertTrue(f.getTimeZoneOverridesCalendar());

            String winterOut = f.format(winter);
            String summerOut = f.format(summer);
            assertEquals("invierno, patrón " + pattern,
                    referenceFormat(pattern, ny, Locale.US, winter), winterOut);
            assertEquals("verano, patrón " + pattern,
                    referenceFormat(pattern, ny, Locale.US, summer), summerOut);
            assertFalse("el nombre debe cambiar entre invierno y verano, patrón " + pattern,
                    winterOut.equals(summerOut));
        }
    }

    @Test
    public void zoneNameForcedUsesFormatterLocale() {
        TimeZone ny = TimeZone.getTimeZone("America/New_York");
        long summer = utcMillis(2003, 7, 10, 16, 0, 0, 0);
        FastDateFormat f = FastDateFormat.getInstance("zzzz", ny, Locale.GERMANY);
        assertEquals(referenceFormat("zzzz", ny, Locale.GERMANY, summer), f.format(summer));
    }

    @Test
    public void zoneNameNotForcedUsesCalendarZone() {
        TimeZone ny = TimeZone.getTimeZone("America/New_York");
        long winter = utcMillis(2003, 1, 10, 20, 0, 0, 0);
        FastDateFormat f = FastDateFormat.getInstance("zzzz", (TimeZone) null, Locale.US);
        assertFalse(f.getTimeZoneOverridesCalendar());
        assertEquals(referenceFormat("zzzz", ny, Locale.US, winter),
                f.format(calendarAt(winter, ny)));
    }

    // ------------------------------------------------------------------
    // 4. Zona forzada: format(Calendar) no modifica el Calendar original
    // ------------------------------------------------------------------

    @Test
    public void forcedZoneFormatCalendarDoesNotMutateOriginalCalendar() {
        TimeZone tokyo = TimeZone.getTimeZone("Asia/Tokyo");
        long millis = utcMillis(2003, 1, 10, 6, 33, 20, 0); // 15:33:20 en Tokyo
        Calendar cal = calendarAt(millis, tokyo);
        assertEquals(15, cal.get(Calendar.HOUR_OF_DAY));

        FastDateFormat f = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss", UTC, Locale.US);
        assertTrue(f.getTimeZoneOverridesCalendar());

        // La salida usa la zona forzada (UTC)...
        assertEquals("2003-01-10 06:33:20", f.format(cal));

        // ...pero el Calendar original conserva su zona y su hora local.
        assertEquals(tokyo.getID(), cal.getTimeZone().getID());
        assertEquals(15, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(millis, cal.getTimeInMillis());
    }
}
