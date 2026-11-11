package org.apache.commons.lang3.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link FastDateFormat} que utilizan únicamente la API pública.
 * Todas las pruebas fijan la zona horaria y el locale explícitamente para ser deterministas
 * y no modifican los valores por defecto de la JVM.
 */
public class FastDateFormatLLMTest {

    /** 1970-01-01T00:00:00Z */
    private static final long EPOCH = 0L;

    /** 2001-09-09T01:46:40Z (domingo) */
    private static final long SAMPLE = 1000000000000L;

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone TOKYO = TimeZone.getTimeZone("Asia/Tokyo");
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

    // ------------------------------------------------------------------
    // Patrón y propiedades básicas
    // ------------------------------------------------------------------

    @Test
    public void getPatternReturnsPatternUsedToCreateInstance() {
        FastDateFormat f = FastDateFormat.getInstance("yyyy-MM-dd", UTC, Locale.US);
        assertEquals("yyyy-MM-dd", f.getPattern());
    }

    @Test
    public void getTimeZoneReturnsForcedTimeZone() {
        FastDateFormat f = FastDateFormat.getInstance("yyyy", TOKYO, Locale.US);
        assertEquals(TOKYO, f.getTimeZone());
        assertTrue(f.getTimeZoneOverridesCalendar());
    }

    @Test
    public void getTimeZoneOverridesCalendarIsFalseWithoutExplicitTimeZone() {
        FastDateFormat f = FastDateFormat.getInstance("yyyy", Locale.US);
        assertFalse(f.getTimeZoneOverridesCalendar());
    }

    @Test
    public void getLocaleReturnsLocaleUsedToCreateInstance() {
        FastDateFormat f = FastDateFormat.getInstance("yyyy", Locale.GERMANY);
        assertEquals(Locale.GERMANY, f.getLocale());
    }

    @Test
    public void toStringContainsPattern() {
        FastDateFormat f = FastDateFormat.getInstance("yyyy-MM-dd", UTC, Locale.US);
        assertEquals("FastDateFormat[yyyy-MM-dd]", f.toString());
    }

    @Test
    public void getMaxLengthEstimateIsAtLeastActualFormattedLength() {
        FastDateFormat f = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSS Z", UTC, Locale.US);
        String out = f.format(SAMPLE);
        assertTrue("estimate=" + f.getMaxLengthEstimate() + " length=" + out.length(),
                f.getMaxLengthEstimate() >= out.length());
        assertTrue(f.getMaxLengthEstimate() > 0);
    }

    // ------------------------------------------------------------------
    // Caché y igualdad
    // ------------------------------------------------------------------

    @Test
    public void getInstanceReturnsCachedInstanceForSameArguments() {
        FastDateFormat f1 = FastDateFormat.getInstance("yyyy-MM-dd", UTC, Locale.US);
        FastDateFormat f2 = FastDateFormat.getInstance("yyyy-MM-dd", UTC, Locale.US);
        assertSame(f1, f2);
    }

    @Test
    public void equalsAndHashCodeAreConsistentForSameArguments() {
        FastDateFormat f1 = FastDateFormat.getInstance("yyyy-MM-dd", UTC, Locale.US);
        FastDateFormat f2 = FastDateFormat.getInstance("yyyy-MM-dd", UTC, Locale.US);
        assertTrue(f1.equals(f2));
        assertEquals(f1.hashCode(), f2.hashCode());
    }

    @Test
    public void equalsReturnsFalseForDifferentPattern() {
        FastDateFormat f1 = FastDateFormat.getInstance("yyyy-MM-dd", UTC, Locale.US);
        FastDateFormat f2 = FastDateFormat.getInstance("dd/MM/yyyy", UTC, Locale.US);
        assertFalse(f1.equals(f2));
    }

    @Test
    public void equalsReturnsFalseForDifferentLocale() {
        FastDateFormat f1 = FastDateFormat.getInstance("yyyy", UTC, Locale.US);
        FastDateFormat f2 = FastDateFormat.getInstance("yyyy", UTC, Locale.GERMANY);
        assertFalse(f1.equals(f2));
    }

    @Test
    public void equalsReturnsFalseForNonFormatObject() {
        FastDateFormat f = FastDateFormat.getInstance("yyyy", UTC, Locale.US);
        assertFalse(f.equals("yyyy"));
        assertFalse(f.equals(null));
    }

    // ------------------------------------------------------------------
    // Formato de long, Date y Calendar
    // ------------------------------------------------------------------

    @Test
    public void formatLongEpochInUtc() {
        FastDateFormat f = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss", UTC, Locale.US);
        assertEquals("1970-01-01 00:00:00", f.format(EPOCH));
    }

    @Test
    public void formatLongSampleInUtc() {
        FastDateFormat f = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss'Z'", UTC, Locale.US);
        assertEquals("2001-09-09T01:46:40Z", f.format(SAMPLE));
    }

    @Test
    public void formatDateMatchesFormatLong() {
        FastDateFormat f = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss", UTC, Locale.US);
        assertEquals(f.format(SAMPLE), f.format(new Date(SAMPLE)));
    }

    @Test
    public void formatCalendarWithForcedTimeZoneConvertsToFormatterZone() {
        Calendar cal = new GregorianCalendar(UTC);
        cal.setTimeInMillis(SAMPLE);
        FastDateFormat f = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss", TOKYO, Locale.US);
        // Tokyo es UTC+9 sin horario de verano
        assertEquals("2001-09-09 10:46:40", f.format(cal));
    }

    @Test
    public void formatCalendarDoesNotModifyCallerCalendarTimeZone() {
        Calendar cal = new GregorianCalendar(UTC);
        cal.setTimeInMillis(SAMPLE);
        FastDateFormat f = FastDateFormat.getInstance("HH:mm", TOKYO, Locale.US);
        f.format(cal);
        assertEquals("UTC", cal.getTimeZone().getID());
    }

    @Test
    public void formatCalendarWithoutForcedTimeZoneUsesCalendarTimeZone() {
        Calendar cal = new GregorianCalendar(TOKYO);
        cal.setTimeInMillis(SAMPLE);
        FastDateFormat f = FastDateFormat.getInstance("HH:mm", Locale.US);
        assertFalse(f.getTimeZoneOverridesCalendar());
        assertEquals("10:46", f.format(cal));
    }

    @Test
    public void formatObjectSupportsLongDateAndCalendar() {
        FastDateFormat f = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss", UTC, Locale.US);
        Calendar cal = new GregorianCalendar(UTC);
        cal.setTimeInMillis(SAMPLE);

        assertEquals("2001-09-09 01:46:40", f.format((Object) Long.valueOf(SAMPLE)));
        assertEquals("2001-09-09 01:46:40", f.format((Object) new Date(SAMPLE)));
        assertEquals("2001-09-09 01:46:40", f.format((Object) cal));
    }

    @Test(expected = IllegalArgumentException.class)
    public void formatObjectRejectsUnknownType() {
        FastDateFormat f = FastDateFormat.getInstance("yyyy", UTC, Locale.US);
        f.format((Object) "no soy una fecha");
    }

    @Test
    public void formatLongAppendsToExistingBufferAndReturnsSameBuffer() {
        FastDateFormat f = FastDateFormat.getInstance("yyyy", UTC, Locale.US);
        StringBuffer sb = new StringBuffer("X:");
        StringBuffer result = f.format(EPOCH, sb);
        assertSame(sb, result);
        assertEquals("X:1970", sb.toString());
    }

    @Test
    public void formatNegativeOffsetTimeZone() {
        // 1970-01-01T00:00Z en Nueva York es 1969-12-31 19:00 (EST, UTC-5)
        FastDateFormat f = FastDateFormat.getInstance("yyyy-MM-dd HH:mm", NEW_YORK, Locale.US);
        assertEquals("1969-12-31 19:00", f.format(EPOCH));
    }

    // ------------------------------------------------------------------
    // Literales y patrones
    // ------------------------------------------------------------------

    @Test
    public void literalTextInsideQuotesIsCopied() {
        FastDateFormat f = FastDateFormat.getInstance("'Year' yyyy", UTC, Locale.US);
        assertEquals("Year 1970", f.format(EPOCH));
    }

    @Test
    public void nonLetterCharactersAreCopiedVerbatim() {
        FastDateFormat f = FastDateFormat.getInstance("yyyy/MM/dd", UTC, Locale.US);
        assertEquals("1970/01/01", f.format(EPOCH));
    }

    @Test
    public void escapedSingleQuoteIsRenderedAsQuote() {
        FastDateFormat f = FastDateFormat.getInstance("yyyy''MM", UTC, Locale.US);
        assertEquals("1970'01", f.format(EPOCH));
    }

    @Test
    public void isoLikePatternWithQuotedTAndZ() {
        FastDateFormat f = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss'Z'", UTC, Locale.US);
        assertEquals("2001-09-09T01:46:40Z", f.format(SAMPLE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidPatternLetterIsRejected() {
        FastDateFormat.getInstance("yyyy-Q", UTC, Locale.US);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullPatternIsRejected() {
        FastDateFormat.getInstance((String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullPatternWithTimeZoneAndLocaleIsRejected() {
        FastDateFormat.getInstance((String) null, UTC, Locale.US);
    }

    // ------------------------------------------------------------------
    // Campos numéricos: relleno y casos especiales
    // ------------------------------------------------------------------

    @Test
    public void unpaddedAndPaddedDayOfMonth() {
        // 2001-09-09: día 9
        FastDateFormat unpadded = FastDateFormat.getInstance("d", UTC, Locale.US);
        FastDateFormat padded = FastDateFormat.getInstance("dd", UTC, Locale.US);
        assertEquals("9", unpadded.format(SAMPLE));
        assertEquals("09", padded.format(SAMPLE));
    }

    @Test
    public void tripleDLetterPadsToThreeDigits() {
        FastDateFormat f = FastDateFormat.getInstance("ddd", UTC, Locale.US);
        assertEquals("001", f.format(EPOCH));
    }

    @Test
    public void twoDigitYearField() {
        FastDateFormat f = FastDateFormat.getInstance("yy", UTC, Locale.US);
        assertEquals("01", f.format(SAMPLE));
    }

    @Test
    public void fourDigitYearField() {
        FastDateFormat f = FastDateFormat.getInstance("yyyy", UTC, Locale.US);
        assertEquals("2001", f.format(SAMPLE));
    }

    @Test
    public void millisecondFieldWithSssPadding() {
        // 1000000000123 -> milisegundo 123 dentro del segundo
        FastDateFormat f = FastDateFormat.getInstance("SSS", UTC, Locale.US);
        assertEquals("123", f.format(SAMPLE + 123L));
    }

    @Test
    public void twelveHourClockAtMidnightShowsTwelve() {
        FastDateFormat f = FastDateFormat.getInstance("h:mm a", UTC, Locale.US);
        assertEquals("12:00 AM", f.format(EPOCH));
    }

    @Test
    public void twelveHourClockInAfternoonOrMorning() {
        // 01:46:40 -> "1:46 AM"
        FastDateFormat f = FastDateFormat.getInstance("h:mm a", UTC, Locale.US);
        assertEquals("1:46 AM", f.format(SAMPLE));
    }

    @Test
    public void zeroBasedTwelveHourFieldAtMidnightShowsZero() {
        FastDateFormat f = FastDateFormat.getInstance("K a", UTC, Locale.US);
        assertEquals("0 AM", f.format(EPOCH));
    }

    @Test
    public void oneToTwentyFourHourFieldAtMidnightShowsTwentyFour() {
        FastDateFormat f = FastDateFormat.getInstance("k", UTC, Locale.US);
        assertEquals("24", f.format(EPOCH));
    }

    @Test
    public void twentyFourHourFieldAtSampleTime() {
        FastDateFormat f = FastDateFormat.getInstance("HH", UTC, Locale.US);
        assertEquals("01", f.format(SAMPLE));
    }

    // ------------------------------------------------------------------
    // Campos de texto: meses, días de la semana, marcador AM/PM, era
    // ------------------------------------------------------------------

    @Test
    public void fullMonthNameInEnglish() {
        FastDateFormat f = FastDateFormat.getInstance("MMMM", UTC, Locale.US);
        assertEquals("September", f.format(SAMPLE));
    }

    @Test
    public void shortMonthNameInEnglish() {
        FastDateFormat f = FastDateFormat.getInstance("MMM", UTC, Locale.US);
        assertEquals("Sep", f.format(SAMPLE));
    }

    @Test
    public void fullMonthNameInGerman() {
        FastDateFormat f = FastDateFormat.getInstance("MMMM", UTC, Locale.GERMANY);
        assertEquals("Januar", f.format(EPOCH));
    }

    @Test
    public void fullWeekdayNameInEnglish() {
        // 1970-01-01 fue jueves
        FastDateFormat f = FastDateFormat.getInstance("EEEE", UTC, Locale.US);
        assertEquals("Thursday", f.format(EPOCH));
    }

    @Test
    public void shortWeekdayNameInEnglish() {
        // 2001-09-09 fue domingo
        FastDateFormat f = FastDateFormat.getInstance("EEE", UTC, Locale.US);
        assertEquals("Sun", f.format(SAMPLE));
    }

    @Test
    public void amPmMarkerForSampleTime() {
        // 01:46:40 UTC -> AM
        FastDateFormat f = FastDateFormat.getInstance("a", UTC, Locale.US);
        assertEquals("AM", f.format(SAMPLE));
    }

    @Test
    public void eraFieldInEnglish() {
        FastDateFormat f = FastDateFormat.getInstance("G", UTC, Locale.US);
        assertEquals("AD", f.format(SAMPLE));
    }
}
