package org.apache.commons.lang3.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link FastDateFormat} que usan solo la API pública.
 */
public class FastDateFormatLLMTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");
    private static final TimeZone KOLKATA = TimeZone.getTimeZone("Asia/Kolkata");

    private static long millis(int year, int month, int day, int hour, int minute, int second, int millisecond) {
        Calendar cal = new GregorianCalendar(UTC);
        cal.clear();
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millisecond);
        return cal.getTimeInMillis();
    }

    /** 10 de enero de 2003 (viernes) a la hora indicada en UTC. */
    private static long jan10(int hour, int minute, int second, int millisecond) {
        return millis(2003, Calendar.JANUARY, 10, hour, minute, second, millisecond);
    }

    private static FastDateFormat fmt(String pattern) {
        return FastDateFormat.getInstance(pattern, UTC, Locale.US);
    }

    // ------------------------------------------------------------------
    // Creación, caché y accesores
    // ------------------------------------------------------------------

    @Test
    public void testGetPatternReturnsPatternUsedToCreate() {
        assertEquals("yyyy-MM-dd", fmt("yyyy-MM-dd").getPattern());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstanceWithNullPatternThrows() {
        FastDateFormat.getInstance((String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstanceWithUnsupportedLetterThrows() {
        FastDateFormat.getInstance("Q", UTC, Locale.US);
    }

    @Test
    public void testGetInstanceReturnsCachedInstanceForSameArguments() {
        FastDateFormat a = FastDateFormat.getInstance("yyyy-MM-dd", UTC, Locale.US);
        FastDateFormat b = FastDateFormat.getInstance("yyyy-MM-dd", UTC, Locale.US);
        assertSame(a, b);
    }

    @Test
    public void testGetInstanceWithDifferentTimeZoneYieldsDifferentInstance() {
        FastDateFormat utc = FastDateFormat.getInstance("yyyy-MM-dd", UTC, Locale.US);
        FastDateFormat ny = FastDateFormat.getInstance("yyyy-MM-dd", NEW_YORK, Locale.US);
        assertNotSame(utc, ny);
        assertFalse(utc.equals(ny));
    }

    @Test
    public void testForcedTimeZoneAccessors() {
        FastDateFormat f = FastDateFormat.getInstance("yyyy", NEW_YORK, Locale.US);
        assertEquals(NEW_YORK, f.getTimeZone());
        assertTrue(f.getTimeZoneOverridesCalendar());
    }

    @Test
    public void testNotForcedTimeZoneUsesDefaultZone() {
        FastDateFormat f = FastDateFormat.getInstance("yyyy-MM-dd", (TimeZone) null, Locale.US);
        assertEquals(TimeZone.getDefault(), f.getTimeZone());
        assertFalse(f.getTimeZoneOverridesCalendar());
    }

    @Test
    public void testGetLocaleReturnsRequestedLocale() {
        assertEquals(Locale.US, fmt("yyyy").getLocale());
        assertEquals(Locale.GERMANY,
                FastDateFormat.getInstance("yyyy", UTC, Locale.GERMANY).getLocale());
    }

    @Test
    public void testNullLocaleUsesDefaultLocale() {
        FastDateFormat f = FastDateFormat.getInstance("yyyy", UTC, (Locale) null);
        assertEquals(Locale.getDefault(), f.getLocale());
    }

    // ------------------------------------------------------------------
    // Formateo: métodos public de formato
    // ------------------------------------------------------------------

    @Test
    public void testFormatLongMillisWithQuotedLiteral() {
        assertEquals("2003-01-10T15:33:20",
                fmt("yyyy-MM-dd'T'HH:mm:ss").format(jan10(15, 33, 20, 0)));
    }

    @Test
    public void testFormatDateCalendarAndMillisAgree() {
        long ms = jan10(15, 33, 20, 0);
        FastDateFormat f = fmt("yyyy-MM-dd HH:mm:ss");
        Calendar cal = new GregorianCalendar(UTC);
        cal.setTimeInMillis(ms);

        assertEquals("2003-01-10 15:33:20", f.format(new Date(ms)));
        assertEquals("2003-01-10 15:33:20", f.format(cal));
        assertEquals("2003-01-10 15:33:20", f.format(ms));
    }

    @Test
    public void testFormatObjectDispatchesByRuntimeType() {
        long ms = jan10(15, 33, 20, 0);
        FastDateFormat f = fmt("yyyy-MM-dd");
        Calendar cal = new GregorianCalendar(UTC);
        cal.setTimeInMillis(ms);

        assertEquals("2003-01-10", f.format((Object) new Date(ms)));
        assertEquals("2003-01-10", f.format((Object) cal));
        assertEquals("2003-01-10", f.format((Object) Long.valueOf(ms)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFormatObjectWithUnsupportedTypeThrows() {
        fmt("yyyy").format((Object) "2003");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFormatObjectNullThrows() {
        fmt("yyyy").format((Object) null);
    }

    @Test
    public void testFormatAppendsToExistingBuffer() {
        long ms = jan10(15, 33, 20, 0);
        FastDateFormat f = fmt("yyyy-MM-dd");
        StringBuffer sb = new StringBuffer("x=");

        StringBuffer returned = f.format(new Date(ms), sb);
        assertSame(sb, returned);
        assertEquals("x=2003-01-10", sb.toString());

        sb.setLength(0);
        sb.append("y=");
        returned = f.format(ms, sb);
        assertSame(sb, returned);
        assertEquals("y=2003-01-10", sb.toString());
    }

    @Test
    public void testFormatCalendarWithForcedZoneConvertsWithoutAlteringCalendar() {
        Calendar cal = new GregorianCalendar(UTC);
        cal.setTimeInMillis(jan10(15, 33, 20, 0));

        String result = FastDateFormat.getInstance("HH:mm:ss", NEW_YORK, Locale.US).format(cal);

        assertEquals("10:33:20", result);
        assertEquals(UTC, cal.getTimeZone());
    }

    @Test
    public void testFormatCalendarWithoutForcedZoneUsesCalendarZone() {
        Calendar cal = new GregorianCalendar(NEW_YORK);
        cal.clear();
        cal.set(2003, Calendar.JANUARY, 10, 10, 33, 20);

        String result = FastDateFormat.getInstance("HH:mm:ss", (TimeZone) null, Locale.US).format(cal);

        assertEquals("10:33:20", result);
    }
}
