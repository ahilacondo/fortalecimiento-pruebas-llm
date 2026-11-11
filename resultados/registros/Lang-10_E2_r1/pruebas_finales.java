package org.apache.commons.lang3.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

/**
 * Pruebas adicionales para {@link FastDateParser} que cubren comportamientos
 * no ejercitados por la suite existente. Solo usan la API pública de la clase
 * (el constructor protegido es accesible desde el mismo paquete).
 */
public class FastDateParserLLMTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");
    private static final Locale US = Locale.US;
    private static final Locale JAPANESE_IMPERIAL = new Locale("ja", "JP", "JP");

    /** Construye una fecha en UTC; el mes es base 0 (0 = enero). */
    private static Date utc(int year, int month, int day, int hour, int minute, int second, int millis) {
        Calendar cal = Calendar.getInstance(UTC, US);
        cal.clear();
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal.getTime();
    }

    private static Date utc(int year, int month, int day) {
        return utc(year, month, day, 0, 0, 0, 0);
    }

    @Test
    public void testParseSimpleDateUsesParserTimeZone() throws ParseException {
        FastDateParser parser = new FastDateParser("yyyy-MM-dd", UTC, US);
        assertEquals(utc(2003, 1, 10), parser.parse("2003-02-10"));
    }

    @Test
    public void testParseMillisecondsAreReadAsDigits() throws ParseException {
        FastDateParser parser = new FastDateParser("yyyy-MM-dd HH:mm:ss.SSS", UTC, US);
        assertEquals(utc(2003, 1, 10, 15, 33, 20, 7), parser.parse("2003-02-10 15:33:20.007"));
        assertEquals(utc(2003, 1, 10, 15, 33, 20, 989), parser.parse("2003-02-10 15:33:20.989"));
    }

    @Test
    public void testParseHourOfDayModuloTwentyFour() throws ParseException {
        FastDateParser parser = new FastDateParser("yyyy-MM-dd HH:mm", UTC, US);
        // HH se normaliza con modulo 24: "24" equivale a 00 del mismo día
        assertEquals(utc(2003, 1, 10, 0, 0, 0, 0), parser.parse("2003-02-10 24:00"));
    }

    @Test
    public void testLiteralTextMismatchThrowsParseException() {
        FastDateParser parser = new FastDateParser("yyyy-MM-dd", UTC, US);
        try {
            parser.parse("2003/02/10");
            fail("Se esperaba ParseException para un separador literal distinto");
        } catch (ParseException e) {
            assertTrue("Mensaje inesperado: " + e.getMessage(),
                    e.getMessage().contains("Unparseable date: \"2003/02/10\""));
            assertEquals(0, e.getErrorOffset());
        }
    }

    @Test
    public void testParseFailureMessageIncludesGeneratedRegex() {
        FastDateParser parser = new FastDateParser("yyyy-MM-dd", UTC, US);
        try {
            parser.parse("no es una fecha");
            fail("Se esperaba ParseException");
        } catch (ParseException e) {
            assertTrue(e.getMessage().contains("does not match"));
            assertTrue(e.getMessage().contains(parser.getParsePattern().pattern()));
        }
    }

    @Test
    public void testJapaneseImperialFailureMentionsDateRange() {
        FastDateParser parser = new FastDateParser("yyyy", UTC, JAPANESE_IMPERIAL);
        try {
            parser.parse("abc");
            fail("Se esperaba ParseException para el locale ja_JP_JP");
        } catch (ParseException e) {
            assertTrue("Mensaje inesperado: " + e.getMessage(),
                    e.getMessage().contains("does not support dates before 1868 AD"));
        }
    }

    @Test
    public void testParseWithPositionReturnsNullAndKeepsIndexOnMismatch() {
        FastDateParser parser = new FastDateParser("yyyy-MM-dd", UTC, US);
        ParsePosition pos = new ParsePosition(0);
        assertNull(parser.parse("abc", pos));
        assertEquals(0, pos.getIndex());
    }

    @Test
    public void testParseWithPositionReturnsNullOnNonDigitInNumericField() {
        FastDateParser parser = new FastDateParser("yyyy-MM-dd", UTC, US);
        ParsePosition pos = new ParsePosition(0);
        assertNull(parser.parse("20a3-02-10", pos));
        assertEquals(0, pos.getIndex());
    }

    @Test
    public void testParseWithPositionStartsAtOffsetAndAdvancesIndex() {
        FastDateParser parser = new FastDateParser("yyyy-MM-dd", UTC, US);
        ParsePosition pos = new ParsePosition(2);
        Date result = parser.parse("xx2003-02-10yy", pos);
        assertEquals(utc(2003, 1, 10), result);
        assertEquals(12, pos.getIndex());
    }

    @Test
    public void testParsePartialInputLeavesTrailingTextUnconsumed() {
        FastDateParser parser = new FastDateParser("yyyy-MM-dd", UTC, US);
        ParsePosition pos = new ParsePosition(0);
        Date result = parser.parse("2003-02-10 trailing", pos);
        assertEquals(utc(2003, 1, 10), result);
        assertEquals(10, pos.getIndex());
    }

    @Test
    public void testParseObjectDelegatesToParse() throws ParseException {
        FastDateParser parser = new FastDateParser("yyyy-MM-dd", UTC, US);
        Object obj = parser.parseObject("2003-02-10");
        assertTrue(obj instanceof Date);
        assertEquals(parser.parse("2003-02-10"), obj);
    }

    @Test
    public void testParseObjectWithPositionDelegatesAndUpdatesIndex() {
        FastDateParser parser = new FastDateParser("yyyy-MM-dd", UTC, US);
        ParsePosition pos = new ParsePosition(1);
        Object obj = parser.parseObject("#2003-02-10", pos);
        assertEquals(utc(2003, 1, 10), obj);
        assertEquals(11, pos.getIndex());
    }

    @Test
    public void testLenientDayOverflowRollsIntoNextMonth() throws ParseException {
        FastDateParser parser = new FastDateParser("yyyy/MM/dd", UTC, US);
        // 2003 no es bisiesto: 31 de febrero -> 3 de marzo
        assertEquals(utc(2003, 2, 3), parser.parse("2003/02/31"));
    }

    @Test
    public void testFourDigitYearIsNotWindowAdjusted() throws ParseException {
        FastDateParser parser = new FastDateParser("yyyy-MM-dd", UTC, US);
        assertEquals(utc(1899, 11, 31), parser.parse("1899-12-31"));
    }

    @Test
    public void testTwoDigitYearOfCurrentYearMapsToCurrentYear() throws ParseException {
        int thisYear = Calendar.getInstance(UTC, US).get(Calendar.YEAR);
        String yy = String.format("%02d", thisYear % 100);
        FastDateParser parser = new FastDateParser("yy-MM-dd", UTC, US);
        Calendar parsed = Calendar.getInstance(UTC, US);
        parsed.setTime(parser.parse(yy + "-01-01"));
        assertEquals(thisYear, parsed.get(Calendar.YEAR));
    }

    @Test
    public void testTwoDigitYearFallsWithinEightyTwentyWindow() throws ParseException {
        int thisYear = Calendar.getInstance(UTC, US).get(Calendar.YEAR);
        FastDateParser parser = new FastDateParser("yy-MM-dd", UTC, US);
        Calendar parsed = Calendar.getInstance(UTC, US);
        parsed.setTime(parser.parse("03-02-10"));
        int year = parsed.get(Calendar.YEAR);
        assertEquals(3, year % 100);
        assertTrue("año fuera de la ventana: " + year, year >= thisYear - 80 && year < thisYear + 20);
    }

    @Test
    public void testDayOfYearPattern() throws ParseException {
        FastDateParser parser = new FastDateParser("yyyy DDD", UTC, US);
        // día 41 del año 2003 = 10 de febrero
        assertEquals(utc(2003, 1, 10), parser.parse("2003 041"));
    }

    @Test
    public void testAdjacentNumericFieldsUseFixedWidth() throws ParseException {
        FastDateParser parser = new FastDateParser("ddMMyyyy", UTC, US);
        assertEquals(utc(2003, 1, 10), parser.parse("10022003"));
    }

    @Test
    public void testWhitespaceInPatternMatchesVariableSpacing() throws ParseException {
        FastDateParser parser = new FastDateParser("yyyy MM dd", UTC, US);
        assertEquals(utc(2003, 1, 10), parser.parse("2003     02   10"));
    }

    @Test
    public void testShortMonthNameParsing() throws ParseException {
        FastDateParser parser = new FastDateParser("dd MMM yyyy", UTC, US);
        assertEquals(utc(2003, 1, 10), parser.parse("10 Feb 2003"));
    }

    @Test
    public void testLongMonthNameParsing() throws ParseException {
        FastDateParser parser = new FastDateParser("dd MMMM yyyy", UTC, US);
        assertEquals(utc(2003, 1, 10), parser.parse("10 February 2003"));
    }

    @Test
    public void testNumericOffsetTimeZoneIsAppliedToParsedTime() throws ParseException {
        FastDateParser parser = new FastDateParser("yyyy-MM-dd'T'HH:mm:ssZ", UTC, US);
        // 10:00 en +0100 equivale a 09:00 UTC
        assertEquals(utc(2003, 1, 10, 9, 0, 0, 0), parser.parse("2003-02-10T10:00:00+0100"));
        // 04:00 en -0500 equivale a 09:00 UTC
        assertEquals(utc(2003, 1, 10, 9, 0, 0, 0), parser.parse("2003-02-10T04:00:00-0500"));
    }

    @Test
    public void testZoneInInputOverridesParserTimeZone() throws ParseException {
        FastDateParser parser = new FastDateParser("yyyy-MM-dd HH:mm Z", NEW_YORK, US);
        assertEquals(utc(2003, 1, 10, 10, 0, 0, 0), parser.parse("2003-02-10 10:00 +0000"));
    }

    @Test
    public void testEqualsIsReflexiveAndSymmetricForEqualParsers() {
        FastDateParser a = new FastDateParser("yyyy-MM-dd", UTC, US);
        FastDateParser b = new FastDateParser("yyyy-MM-dd", UTC, US);
        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
    }

    @Test
    public void testEqualsReturnsFalseForNull() {
        FastDateParser a = new FastDateParser("yyyy-MM-dd", UTC, US);
        assertFalse(a.equals(null));
    }

    @Test
    public void testEqualsReturnsFalseForDifferentPattern() {
        FastDateParser a = new FastDateParser("yyyy-MM-dd", UTC, US);
        FastDateParser b = new FastDateParser("dd-MM-yyyy", UTC, US);
        assertFalse(a.equals(b));
    }

    @Test
    public void testEqualsReturnsFalseForDifferentTimeZone() {
        FastDateParser a = new FastDateParser("yyyy-MM-dd", UTC, US);
        FastDateParser b = new FastDateParser("yyyy-MM-dd", NEW_YORK, US);
        assertFalse(a.equals(b));
    }

    @Test
    public void testEqualsReturnsFalseForDifferentLocale() {
        FastDateParser a = new FastDateParser("yyyy-MM-dd", UTC, US);
        FastDateParser b = new FastDateParser("yyyy-MM-dd", UTC, Locale.FRANCE);
        assertFalse(a.equals(b));
    }

    @Test
    public void testHashCodeConsistentWithEquals() {
        FastDateParser a = new FastDateParser("yyyy-MM-dd", UTC, US);
        FastDateParser b = new FastDateParser("yyyy-MM-dd", UTC, US);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testToStringHasExactFormat() {
        FastDateParser parser = new FastDateParser("yyyy-MM-dd", UTC, US);
        assertEquals("FastDateParser[yyyy-MM-dd,en_US,UTC]", parser.toString());
    }

    @Test
    public void testSerializationCloneKeepsEqualityAndBehavior() throws ParseException {
        FastDateParser parser = new FastDateParser("yyyy-MM-dd HH:mm:ss.SSS", UTC, US);
        FastDateParser copy = SerializationUtils.clone(parser);
        assertNotNull(copy);
        assertEquals(parser, copy);
        assertEquals(parser.hashCode(), copy.hashCode());
        assertEquals(utc(2003, 1, 10, 15, 33, 20, 989), copy.parse("2003-02-10 15:33:20.989"));
    }

    @Test
    public void testInvalidPatternStartingWithUnknownLetterThrows() {
        try {
            new FastDateParser("x", UTC, US);
            fail("Se esperaba IllegalArgumentException para un patrón inválido");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid pattern", e.getMessage());
        }
    }

    @Test
    public void testEmptyPatternThrows() {
        try {
            new FastDateParser("", UTC, US);
            fail("Se esperaba IllegalArgumentException para un patrón vacío");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid pattern", e.getMessage());
        }
    }

    @Test
    public void testQuotedLiteralTIsRequiredInParsedInput() throws ParseException {
        FastDateParser parser = new FastDateParser("yyyy-MM-dd'T'HH", UTC, US);
        assertEquals(utc(2003, 1, 10, 7, 0, 0, 0), parser.parse("2003-02-10T07"));
        try {
            parser.parse("2003-02-10 07");
            fail("Se esperaba ParseException: el literal 'T' no coincide");
        } catch (ParseException expected) {
            assertTrue(expected.getMessage().contains("Unparseable date"));
        }
    }
}
