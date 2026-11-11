package org.apache.commons.lang3;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Locale;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link StringUtils} que usan solo la API pública.
 */
public class StringUtilsLLMTest {

    // ---------------------------------------------------------------------
    // Constantes
    // ---------------------------------------------------------------------

    @Test
    public void testConstantesPublicas() {
        assertEquals("", StringUtils.EMPTY);
        assertEquals(-1, StringUtils.INDEX_NOT_FOUND);
    }

    // ---------------------------------------------------------------------
    // Comprobaciones de vacío y en blanco
    // ---------------------------------------------------------------------

    @Test
    public void testIsEmptyYIsNotEmptyConCharSequence() {
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
        assertTrue(StringUtils.isEmpty(new StringBuilder()));
        assertFalse(StringUtils.isEmpty(" "));
        assertFalse(StringUtils.isEmpty(new StringBuilder("x")));

        assertFalse(StringUtils.isNotEmpty(null));
        assertFalse(StringUtils.isNotEmpty(""));
        assertTrue(StringUtils.isNotEmpty(" "));
        assertTrue(StringUtils.isNotEmpty("a"));
    }

    @Test
    public void testIsBlankYIsNotBlank() {
        assertTrue(StringUtils.isBlank(null));
        assertTrue(StringUtils.isBlank(""));
        assertTrue(StringUtils.isBlank(" \t\n"));
        assertFalse(StringUtils.isBlank(" a "));
        assertFalse(StringUtils.isBlank("a"));

        assertFalse(StringUtils.isNotBlank(null));
        assertFalse(StringUtils.isNotBlank(" \t"));
        assertTrue(StringUtils.isNotBlank("  bob  "));
    }

    // ---------------------------------------------------------------------
    // Trim y strip
    // ---------------------------------------------------------------------

    @Test
    public void testTrimEliminaCaracteresMenoresOIgualesA32() {
        assertEquals("x", StringUtils.trim("\u0001x"));
        assertEquals("abc", StringUtils.trim("\t abc \n"));
        assertNull(StringUtils.trim(null));
    }

    @Test
    public void testStripNoEliminaControlesQueNoSonWhitespace() {
        // \u0001 no es whitespace para Character.isWhitespace
        assertEquals("\u0001x", StringUtils.strip("\u0001x"));
        assertEquals("abc", StringUtils.strip("\t abc \n"));
    }

    @Test
    public void testTrimToNullYTrimToEmpty() {
        assertNull(StringUtils.trimToNull("   "));
        assertEquals("a", StringUtils.trimToNull(" a "));
        assertNull(StringUtils.trimToNull(null));
        assertEquals("", StringUtils.trimToEmpty(null));
        assertEquals("", StringUtils.trimToEmpty("     "));
        assertEquals("abc", StringUtils.trimToEmpty("  abc "));
    }

    @Test
    public void testStripToNullYStripToEmpty() {
        assertNull(StringUtils.stripToNull("\t \n"));
        assertEquals("ab c", StringUtils.stripToNull(" ab c "));
        assertNull(StringUtils.stripToNull(null));
        assertEquals("", StringUtils.stripToEmpty(null));
        assertEquals("abc", StringUtils.stripToEmpty(" abc "));
    }

    @Test
    public void testStripConCaracteresExplicitos() {
        assertEquals("abc", StringUtils.strip("xxabcxx", "x"));
        assertEquals("  abc", StringUtils.strip("  abcyx", "xyz"));
        assertEquals("abc  ", StringUtils.stripStart("yxabc  ", "xyz"));
        assertEquals("  abc", StringUtils.stripEnd("  abcyx", "xyz"));
        assertEquals("abc", StringUtils.strip("abc", ""));
        assertNull(StringUtils.strip(null, "x"));
    }

    @Test
    public void testStripAllTrataNullsYArreglosVacios() {
        assertNull(StringUtils.stripAll((String[]) null));

        String[] vacio = new String[0];
        assertSame(vacio, StringUtils.stripAll(vacio));

        String[] resultado = StringUtils.stripAll(new String[] {" a ", null, "b "});
        assertArrayEquals(new String[] {"a", null, "b"}, resultado);
    }

    // ---------------------------------------------------------------------
    // Igualdad
    // ---------------------------------------------------------------------

    @Test
    public void testEqualsYEqualsIgnoreCase() {
        assertTrue(StringUtils.equals(null, null));
        assertFalse(StringUtils.equals("a", null));
        assertTrue(StringUtils.equals("abc", "abc"));
        assertFalse(StringUtils.equals("abc", "ABC"));

        assertTrue(StringUtils.equalsIgnoreCase("abc", "ABC"));
        assertTrue(StringUtils.equalsIgnoreCase(null, null));
        assertFalse(StringUtils.equalsIgnoreCase(null, ""));
    }

    // ---------------------------------------------------------------------
    // Búsqueda: indexOf / lastIndexOf / contains
    // ---------------------------------------------------------------------

    @Test
    public void testIndexOfChar() {
        assertEquals(2, StringUtils.indexOf("abc", 'c'));
        assertEquals(-1, StringUtils.indexOf(null, 'a'));
        assertEquals(-1, StringUtils.indexOf("", 'a'));
        assertEquals(5, StringUtils.indexOf("aabaabaa", 'b', 3));
        assertEquals(-1, StringUtils.indexOf("aabaabaa", 'b', 9));
    }

    @Test
    public void testIndexOfString() {
        assertEquals(1, StringUtils.indexOf("aabaabaa", "ab"));
        assertEquals(0, StringUtils.indexOf("abc", ""));
        assertEquals(0, StringUtils.indexOf("", ""));
        assertEquals(-1, StringUtils.indexOf(null, "a"));
        assertEquals(-1, StringUtils.indexOf("abc", (String) null));
        assertEquals(2, StringUtils.indexOf("aabaabaa", "b", -1));
        assertEquals(3, StringUtils.indexOf("abc", "", 9));
    }

    @Test
    public void testOrdinalIndexOf() {
        assertEquals(1, StringUtils.ordinalIndexOf("aabaabaa", "a", 2));
        assertEquals(5, StringUtils.ordinalIndexOf("aabaabaa", "b", 2));
        assertEquals(4, StringUtils.ordinalIndexOf("aabaabaa", "ab", 2));
        assertEquals(-1, StringUtils.ordinalIndexOf("aabaabaa", "a", 0));
        assertEquals(-1, StringUtils.ordinalIndexOf(null, "a", 1));
    }

    @Test
    public void testLastIndexOfChar() {
        assertEquals(7, StringUtils.lastIndexOf("aabaabaa", 'a'));
        assertEquals(2, StringUtils.lastIndexOf("aabaabaa", 'b', 4));
        assertEquals(-1, StringUtils.lastIndexOf("aabaabaa", 'b', -1));
        assertEquals(-1, StringUtils.lastIndexOf("", 'a'));
    }

    @Test
    public void testLastIndexOfString() {
        assertEquals(8, StringUtils.lastIndexOf("aabaabaa", ""));
        assertEquals(1, StringUtils.lastIndexOf("aabaabaa", "ab"));
        assertEquals(-1, StringUtils.lastIndexOf(null, "a"));
    }

    @Test
    public void testLastOrdinalIndexOf() {
        assertEquals(7, StringUtils.lastOrdinalIndexOf("aabaabaa", "a", 1));
        assertEquals(2, StringUtils.lastOrdinalIndexOf("aabaabaa", "b", 2));
        assertEquals(1, StringUtils.lastOrdinalIndexOf("aabaabaa", "ab", 2));
    }

    @Test
    public void testIndexOfIgnoreCaseYLastIndexOfIgnoreCase() {
        assertEquals(1, StringUtils.indexOfIgnoreCase("aabaabaa", "AB"));
        assertEquals(5, StringUtils.indexOfIgnoreCase("aabaabaa", "B", 3));
        assertEquals(-1, StringUtils.indexOfIgnoreCase(null, "a"));

        assertEquals(4, StringUtils.lastIndexOfIgnoreCase("aabaabaa", "AB"));
        assertEquals(-1, StringUtils.lastIndexOfIgnoreCase("aabaabaa", "B", 0));
    }

    @Test
    public void testContainsVariantes() {
        assertTrue(StringUtils.contains("abc", 'b'));
        assertFalse(StringUtils.contains("", 'a'));
        assertTrue(StringUtils.contains("abc", ""));
        assertFalse(StringUtils.contains("abc", (String) null));

        assertTrue(StringUtils.containsIgnoreCase("ABC", "bc"));
        assertTrue(StringUtils.containsIgnoreCase("abc", ""));
        assertFalse(StringUtils.containsIgnoreCase("ab", "abc"));
        assertFalse(StringUtils.containsIgnoreCase(null, "a"));
    }

    // ---------------------------------------------------------------------
    // indexOfAny / containsAny / containsNone / containsOnly
    // ---------------------------------------------------------------------

    @Test
    public void testIndexOfAnyConChars() {
        assertEquals(3, StringUtils.indexOfAny("zzabyycdxx", new char[] {'b', 'y'}));
        assertEquals(-1, StringUtils.indexOfAny("aba", new char[] {'z'}));
        assertEquals(-1, StringUtils.indexOfAny("abc", (char[]) null));
        assertEquals(-1, StringUtils.indexOfAny("", new char[] {'a'}));
    }

    @Test
    public void testIndexOfAnyConStrings() {
        assertEquals(2, StringUtils.indexOfAny("zzabyycdxx", new String[] {"cd", "ab"}));
        assertEquals(1, StringUtils.indexOfAny("zzabyycdxx", new String[] {"zab", "aby"}));
        assertEquals(0, StringUtils.indexOfAny("zzabyycdxx", new String[] {""}));
        assertEquals(-1, StringUtils.indexOfAny("zzabyycdxx", new String[] {"mn", "op"}));
        assertEquals(-1, StringUtils.indexOfAny(null, new String[] {"a"}));
    }

    @Test
    public void testLastIndexOfAnyConStrings() {
        assertEquals(6, StringUtils.lastIndexOfAny("zzabyycdxx", new String[] {"ab", "cd"}));
        assertEquals(10, StringUtils.lastIndexOfAny("zzabyycdxx", new String[] {"mn", ""}));
        assertEquals(-1, StringUtils.lastIndexOfAny("zzabyycdxx", new String[] {"mn", "op"}));
    }

    @Test
    public void testContainsAnyConChars() {
        assertTrue(StringUtils.containsAny("zzabyycdxx", new char[] {'z', 'a'}));
        assertFalse(StringUtils.containsAny("aba", new char[] {'z'}));
        assertFalse(StringUtils.containsAny("", new char[] {'a'}));
        assertFalse(StringUtils.containsAny("abc", (char[]) null));
    }

    @Test
    public void testContainsAnyConString() {
        assertTrue(StringUtils.containsAny("abc", "xyzc"));
        assertFalse(StringUtils.containsAny("abc", "xyz"));
        assertFalse(StringUtils.containsAny("abc", (String) null));
        assertFalse(StringUtils.containsAny(null, "abc"));
    }

    @Test
    public void testIndexOfAnyButChars() {
        assertEquals(3, StringUtils.indexOfAnyBut("zzabyycdxx", "za"));
        assertEquals(-1, StringUtils.indexOfAnyBut("aba", "ab"));
        assertEquals(-1, StringUtils.indexOfAnyBut(null, "ab"));
    }

    @Test
    public void testContainsOnly() {
        assertTrue(StringUtils.containsOnly("abab", "abc"));
        assertFalse(StringUtils.containsOnly("ab1", "abc"));
        assertTrue(StringUtils.containsOnly("", "abc"));
        assertFalse(StringUtils.containsOnly("ab", ""));
        assertFalse(StringUtils.containsOnly(null, "abc"));
        assertFalse(StringUtils.containsOnly("abc", (char[]) null));
    }

    @Test
    public void testContainsNone() {
        assertFalse(StringUtils.containsNone("abz", "xyz"));
        assertTrue(StringUtils.containsNone("ab1", "xyz"));
        assertTrue(StringUtils.containsNone(null, "xyz"));
        assertTrue(StringUtils.containsNone("abc", (String) null));
    }

    // ---------------------------------------------------------------------
    // Substring, left, right, mid
    // ---------------------------------------------------------------------

    @Test
    public void testSubstringConUnIndice() {
        assertEquals("bc", StringUtils.substring("abc", -2));
        assertEquals("abc", StringUtils.substring("abc", -4));
        assertEquals("", StringUtils.substring("abc", 4));
        assertNull(StringUtils.substring(null, 1));
    }

    @Test
    public void testSubstringConInicioYFin() {
        assertEquals("c", StringUtils.substring("abc", 2, 4));
        assertEquals("", StringUtils.substring("abc", 2, 0));
        assertEquals("b", StringUtils.substring("abc", -2, -1));
        assertEquals("ab", StringUtils.substring("abc", -4, 2));
    }

    @Test
    public void testLeftRightMid() {
        assertEquals("ab", StringUtils.left("abc", 2));
        assertEquals("", StringUtils.left("abc", -1));
        assertEquals("abc", StringUtils.left("abc", 4));
        assertEquals("bc", StringUtils.right("abc", 2));
        assertEquals("ab", StringUtils.mid("abc", 0, 2));
        assertEquals("", StringUtils.mid("abc", 4, 2));
        assertEquals("ab", StringUtils.mid("abc", -2, 2));
        assertEquals("", StringUtils.mid("abc", 0, -1));
        assertNull(StringUtils.mid(null, 0, 1));
    }

    // ---------------------------------------------------------------------
    // Subcadenas relativas a separadores
    // ---------------------------------------------------------------------

    @Test
    public void testSubstringBeforeYAfter() {
        assertEquals("a", StringUtils.substringBefore("abcba", "b"));
        assertEquals("abc", StringUtils.substringBefore("abc", "d"));
        assertEquals("abc", StringUtils.substringBefore("abc", null));
        assertEquals("", StringUtils.substringBefore("abc", ""));

        assertEquals("cba", StringUtils.substringAfter("abcba", "b"));
        assertEquals("", StringUtils.substringAfter("abc", "d"));
        assertEquals("abc", StringUtils.substringAfter("abc", null));
        assertEquals("abc", StringUtils.substringAfter("abc", ""));
    }

    @Test
    public void testSubstringBeforeLastYAfterLast() {
        assertEquals("abc", StringUtils.substringBeforeLast("abcba", "b"));
        assertEquals("abc", StringUtils.substringBeforeLast("abc", "d"));
        assertEquals("a", StringUtils.substringAfterLast("abcba", "b"));
        assertEquals("", StringUtils.substringAfterLast("abc", "d"));
        assertEquals("", StringUtils.substringAfterLast("abc", ""));
    }

    @Test
    public void testSubstringBetween() {
        assertEquals("abc", StringUtils.substringBetween("yabcz", "y", "z"));
        assertNull(StringUtils.substringBetween("yabcz", "y", "x"));
        assertNull(StringUtils.substringBetween(null, "y", "z"));
    }

    // ---------------------------------------------------------------------
    // Transformaciones básicas
    // ---------------------------------------------------------------------

    @Test
    public void testRepeatReverseCapitalize() {
        assertEquals("ababab", StringUtils.repeat("ab", 3));
        assertEquals("", StringUtils.repeat("ab", 0));
        assertNull(StringUtils.repeat(null, 2));

        assertEquals("cba", StringUtils.reverse("abc"));
        assertNull(StringUtils.reverse(null));

        assertEquals("Abc", StringUtils.capitalize("abc"));
        assertEquals("", StringUtils.capitalize(""));
        assertEquals("aBC", StringUtils.uncapitalize("ABC"));
    }

    @Test
    public void testDefaultStringYChompChop() {
        assertEquals("", StringUtils.defaultString(null));
        assertEquals("x", StringUtils.defaultString(null, "x"));
        assertEquals("abc", StringUtils.chomp("abc\n"));
        assertEquals("ab", StringUtils.chop("abc"));
    }

    @Test
    public void testPadding() {
        assertEquals("001", StringUtils.leftPad("1", 3, '0'));
        assertEquals("a  ", StringUtils.rightPad("a", 3));
        assertEquals("1", StringUtils.leftPad("1", 0, '0'));
    }

    // ---------------------------------------------------------------------
    // Predicados de contenido y prefijos
    // ---------------------------------------------------------------------

    @Test
    public void testStartsEndsWith() {
        assertTrue(StringUtils.startsWith("abc", "ab"));
        assertTrue(StringUtils.startsWithIgnoreCase("ABC", "ab"));
        assertTrue(StringUtils.endsWith("abc", "bc"));
        assertFalse(StringUtils.endsWith("abc", null));
    }

    @Test
    public void testIsAlphaIsNumericIsAlphanumeric() {
        assertTrue(StringUtils.isAlpha("abc"));
        assertFalse(StringUtils.isAlpha("a1"));
        assertTrue(StringUtils.isNumeric("123"));
        assertFalse(StringUtils.isNumeric("12.3"));
        assertTrue(StringUtils.isAlphanumeric("a1"));
        assertFalse(StringUtils.isAlphanumeric("a-1"));
    }

    @Test
    public void testCountMatches() {
        assertEquals(2, StringUtils.countMatches("abba", "b"));
        assertEquals(0, StringUtils.countMatches(null, "b"));
    }

    // ---------------------------------------------------------------------
    // Join y split
    // ---------------------------------------------------------------------

    @Test
    public void testJoinYSplit() {
        assertEquals("a,b", StringUtils.join(new String[] {"a", "b"}, ","));
        assertNull(StringUtils.join((Object[]) null, ","));
        assertArrayEquals(new String[] {"a", "b", "c"}, StringUtils.split("a b  c"));
        assertNull(StringUtils.split(null));
        assertEquals(0, StringUtils.split("").length);
    }

    @Test
    public void testLocaleInvariantesParaCasos() {
        // Comprobación de que los métodos de caso son consistentes con equalsIgnoreCase
        assertTrue(StringUtils.equalsIgnoreCase("straße".toLowerCase(Locale.ROOT),
                "STRAßE".toLowerCase(Locale.ROOT)));
        assertNotNull(Arrays.asList("x"));
    }
}
