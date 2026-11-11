package org.apache.commons.lang3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link StringUtils} que cubren los requisitos de la
 * documentación no verificados por la suite existente.
 */
public class StringUtilsLLMTest {

    // Requisito 1: isEmpty / isNotEmpty

    @Test
    public void testIsEmpty_NullEmptyAndSpacesWithoutTrimming() {
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
        assertFalse(StringUtils.isEmpty(" "));
        assertFalse(StringUtils.isEmpty("bob"));
        assertFalse(StringUtils.isEmpty("  bob  "));
    }

    @Test
    public void testIsEmpty_DoesNotTrimText() {
        // El texto con espacios no debe recortarse para decidir si esta vacio
        assertFalse(StringUtils.isEmpty("\t"));
        assertFalse(StringUtils.isEmpty("\n"));
    }

    @Test
    public void testIsNotEmpty_ReturnsOppositeOfIsEmpty() {
        assertFalse(StringUtils.isNotEmpty(null));
        assertFalse(StringUtils.isNotEmpty(""));
        assertTrue(StringUtils.isNotEmpty(" "));
        assertTrue(StringUtils.isNotEmpty("bob"));
        assertTrue(StringUtils.isNotEmpty("  bob  "));
    }

    // Requisito 2: isBlank / isNotBlank

    @Test
    public void testIsBlank_NullEmptyAndWhitespaceOnly() {
        assertTrue(StringUtils.isBlank(null));
        assertTrue(StringUtils.isBlank(""));
        assertTrue(StringUtils.isBlank(" "));
        assertTrue(StringUtils.isBlank(" \t\n\r"));
        assertTrue(StringUtils.isBlank("\u000B\u000C\u001C"));
        assertTrue(StringUtils.isBlank("\u2003"));
    }

    @Test
    public void testIsBlank_NonBreakingSpaceIsNotWhitespace() {
        // Character.isWhitespace('\u00A0') es false, por tanto no es blanco
        assertFalse(StringUtils.isBlank("\u00A0"));
    }

    @Test
    public void testIsBlank_TextIsNotBlank() {
        assertFalse(StringUtils.isBlank("bob"));
        assertFalse(StringUtils.isBlank("  bob  "));
        assertFalse(StringUtils.isBlank(" a "));
    }

    @Test
    public void testIsNotBlank_ReturnsOppositeOfIsBlank() {
        assertFalse(StringUtils.isNotBlank(null));
        assertFalse(StringUtils.isNotBlank(""));
        assertFalse(StringUtils.isNotBlank(" "));
        assertFalse(StringUtils.isNotBlank(" \t\n"));
        assertTrue(StringUtils.isNotBlank("bob"));
        assertTrue(StringUtils.isNotBlank("  bob  "));
        assertTrue(StringUtils.isNotBlank("\u00A0"));
    }

    // Requisito 3: trim, trimToNull, trimToEmpty

    @Test
    public void testTrim_RemovesControlCharsUpTo32AndKeepsNull() {
        assertNull(StringUtils.trim(null));
        assertEquals("", StringUtils.trim(""));
        assertEquals("", StringUtils.trim("     "));
        assertEquals("abc", StringUtils.trim("\u0001abc\u0002"));
        assertEquals("abc", StringUtils.trim("    abc    "));
        assertEquals("abc", StringUtils.trim("\t abc \n"));
        assertEquals("a b", StringUtils.trim("  a b  "));
    }

    @Test
    public void testTrimToNull_ReturnsNullForNullEmptyAndControlOnly() {
        assertNull(StringUtils.trimToNull(null));
        assertNull(StringUtils.trimToNull(""));
        assertNull(StringUtils.trimToNull("     "));
        assertNull(StringUtils.trimToNull("\u0000\u0001 \u001F"));
        assertEquals("abc", StringUtils.trimToNull("abc"));
        assertEquals("abc", StringUtils.trimToNull("    abc    "));
    }

    @Test
    public void testTrimToEmpty_ReturnsEmptyForNullAndControlOnly() {
        assertEquals("", StringUtils.trimToEmpty(null));
        assertEquals("", StringUtils.trimToEmpty(""));
        assertEquals("", StringUtils.trimToEmpty("     "));
        assertEquals("", StringUtils.trimToEmpty("\u0000\u0001"));
        assertEquals("abc", StringUtils.trimToEmpty("abc"));
        assertEquals("abc", StringUtils.trimToEmpty("    abc    "));
    }

    // Requisito 4: strip, stripToNull, stripToEmpty, strip(str, stripChars)

    @Test
    public void testStrip_RemovesWhitespaceAtBothEnds() {
        assertNull(StringUtils.strip(null));
        assertEquals("", StringUtils.strip(""));
        assertEquals("", StringUtils.strip("   "));
        assertEquals("abc", StringUtils.strip("abc"));
        assertEquals("abc", StringUtils.strip("  abc"));
        assertEquals("abc", StringUtils.strip("abc  "));
        assertEquals("abc", StringUtils.strip(" \t abc \n"));
    }

    @Test
    public void testStrip_DoesNotRemoveInnerWhitespace() {
        assertEquals("ab c", StringUtils.strip(" ab c "));
        assertEquals("a\tb", StringUtils.strip("\ta\tb\t"));
    }

    @Test
    public void testStripToNull_ReturnsNullForNullEmptyAndWhitespaceOnly() {
        assertNull(StringUtils.stripToNull(null));
        assertNull(StringUtils.stripToNull(""));
        assertNull(StringUtils.stripToNull("   "));
        assertNull(StringUtils.stripToNull(" \t\n "));
        assertEquals("abc", StringUtils.stripToNull("abc"));
        assertEquals("abc", StringUtils.stripToNull("  abc"));
        assertEquals("ab c", StringUtils.stripToNull(" ab c "));
    }

    @Test
    public void testStripToEmpty_ReturnsEmptyForNullAndWhitespaceOnly() {
        assertEquals("", StringUtils.stripToEmpty(null));
        assertEquals("", StringUtils.stripToEmpty(""));
        assertEquals("", StringUtils.stripToEmpty("   "));
        assertEquals("abc", StringUtils.stripToEmpty("abc"));
        assertEquals("abc", StringUtils.stripToEmpty("  abc  "));
        assertEquals("ab c", StringUtils.stripToEmpty(" ab c "));
    }

    @Test
    public void testStripWithChars_RemovesOnlyGivenCharacters() {
        assertEquals("abc", StringUtils.strip("xxabcxx", "xyz"));
        assertEquals("  abc", StringUtils.strip("  abcyx", "xyz"));
        assertEquals("abc  ", StringUtils.strip("yxabc  ", "xyz"));
        // Los espacios no pertenecen al conjunto, asi que no se eliminan
        assertEquals("  abc  ", StringUtils.strip("  abc  ", "xyz"));
    }

    @Test
    public void testStripWithChars_NullCharsTreatedAsWhitespace() {
        assertEquals("abc", StringUtils.strip("  abc  ", null));
        assertEquals("abc", StringUtils.strip(" \t abc \n", null));
        assertEquals("abc", StringUtils.strip("\u0020abc\u0020", null));
    }

    @Test
    public void testStripWithChars_EmptyCharsLeavesStringUnchanged() {
        assertEquals("xyzabc", StringUtils.strip("xyzabc", ""));
        assertEquals("  abc  ", StringUtils.strip("  abc  ", ""));
    }

    @Test
    public void testStripWithChars_NullInputReturnsNullAndEmptyReturnsEmpty() {
        assertNull(StringUtils.strip(null, "x"));
        assertNull(StringUtils.strip(null, null));
        assertEquals("", StringUtils.strip("", null));
        assertEquals("", StringUtils.strip("", "xyz"));
    }

    // Requisito 5: equals / equalsIgnoreCase

    @Test
    public void testEquals_NullSafe() {
        assertTrue(StringUtils.equals(null, null));
        assertFalse(StringUtils.equals(null, "abc"));
        assertFalse(StringUtils.equals("abc", null));
        assertTrue(StringUtils.equals("abc", "abc"));
        assertTrue(StringUtils.equals("", ""));
    }

    @Test
    public void testEquals_IsCaseSensitive() {
        assertFalse(StringUtils.equals("abc", "ABC"));
        assertFalse(StringUtils.equals("abc", "abc "));
    }

    @Test
    public void testEqualsIgnoreCase_NullSafeAndCaseInsensitive() {
        assertTrue(StringUtils.equalsIgnoreCase(null, null));
        assertFalse(StringUtils.equalsIgnoreCase(null, "abc"));
        assertFalse(StringUtils.equalsIgnoreCase("abc", null));
        assertTrue(StringUtils.equalsIgnoreCase("abc", "abc"));
        assertTrue(StringUtils.equalsIgnoreCase("abc", "ABC"));
        assertTrue(StringUtils.equalsIgnoreCase("", ""));
        assertFalse(StringUtils.equalsIgnoreCase("abc", "abcd"));
    }

    // Requisito 6: startsWith, endsWith y variantes IgnoreCase

    @Test
    public void testStartsWith_NullSafeAndCaseSensitive() {
        assertTrue(StringUtils.startsWith(null, null));
        assertFalse(StringUtils.startsWith(null, "abc"));
        assertFalse(StringUtils.startsWith("abcdef", null));
        assertTrue(StringUtils.startsWith("abcdef", "abc"));
        assertFalse(StringUtils.startsWith("ABCDEF", "abc"));
        assertTrue(StringUtils.startsWith("abc", ""));
        assertTrue(StringUtils.startsWith("", ""));
        assertFalse(StringUtils.startsWith("ab", "abc"));
    }

    @Test
    public void testStartsWithIgnoreCase_NullSafeAndCaseInsensitive() {
        assertTrue(StringUtils.startsWithIgnoreCase(null, null));
        assertFalse(StringUtils.startsWithIgnoreCase(null, "abc"));
        assertFalse(StringUtils.startsWithIgnoreCase("abcdef", null));
        assertTrue(StringUtils.startsWithIgnoreCase("abcdef", "abc"));
        assertTrue(StringUtils.startsWithIgnoreCase("ABCDEF", "abc"));
        assertFalse(StringUtils.startsWithIgnoreCase("ABCDEF", "bcd"));
        assertFalse(StringUtils.startsWithIgnoreCase("ab", "abc"));
    }

    @Test
    public void testEndsWith_NullSafeAndCaseSensitive() {
        assertTrue(StringUtils.endsWith(null, null));
        assertFalse(StringUtils.endsWith(null, "def"));
        assertFalse(StringUtils.endsWith("abcdef", null));
        assertTrue(StringUtils.endsWith("abcdef", "def"));
        assertFalse(StringUtils.endsWith("ABCDEF", "def"));
        assertFalse(StringUtils.endsWith("abcdef", "cde"));
        assertTrue(StringUtils.endsWith("abc", ""));
        assertTrue(StringUtils.endsWith("", ""));
        assertFalse(StringUtils.endsWith("def", "abcdef"));
    }

    @Test
    public void testEndsWithIgnoreCase_NullSafeAndCaseInsensitive() {
        assertTrue(StringUtils.endsWithIgnoreCase(null, null));
        assertFalse(StringUtils.endsWithIgnoreCase(null, "def"));
        assertFalse(StringUtils.endsWithIgnoreCase("abcdef", null));
        assertTrue(StringUtils.endsWithIgnoreCase("abcdef", "def"));
        assertTrue(StringUtils.endsWithIgnoreCase("ABCDEF", "def"));
        assertFalse(StringUtils.endsWithIgnoreCase("ABCDEF", "cde"));
        assertFalse(StringUtils.endsWithIgnoreCase("ab", "abc"));
    }

    // Requisito 7: indexOf, lastIndexOf, contains, indexOfAny, containsAny,
    // containsOnly, containsNone con null o vacio

    @Test
    public void testIndexOfChar_NullOrEmptyReturnsMinusOne() {
        assertEquals(-1, StringUtils.indexOf(null, 'a'));
        assertEquals(-1, StringUtils.indexOf("", 'a'));
        assertEquals(-1, StringUtils.indexOf("abc", 'z'));
    }

    @Test
    public void testIndexOfString_NullAndEmptyInputs() {
        assertEquals(-1, StringUtils.indexOf(null, "a"));
        assertEquals(-1, StringUtils.indexOf("abc", (String) null));
        assertEquals(-1, StringUtils.indexOf(null, (String) null));
        assertEquals(-1, StringUtils.indexOf("", "a"));
        assertEquals(0, StringUtils.indexOf("", ""));
    }

    @Test
    public void testLastIndexOf_NullAndEmptyInputs() {
        assertEquals(-1, StringUtils.lastIndexOf(null, 'a'));
        assertEquals(-1, StringUtils.lastIndexOf("", 'a'));
        assertEquals(-1, StringUtils.lastIndexOf(null, "a"));
        assertEquals(-1, StringUtils.lastIndexOf("abc", (String) null));
        assertEquals(-1, StringUtils.lastIndexOf("", "a"));
        assertEquals(0, StringUtils.lastIndexOf("", ""));
    }

    @Test
    public void testContains_NullAndEmptyInputs() {
        assertFalse(StringUtils.contains(null, 'a'));
        assertFalse(StringUtils.contains("", 'a'));
        assertFalse(StringUtils.contains(null, "a"));
        assertFalse(StringUtils.contains("abc", (String) null));
        assertFalse(StringUtils.contains("", "a"));
        assertTrue(StringUtils.contains("", ""));
    }

    @Test
    public void testIndexOfAny_NullOrEmptyInputs() {
        assertEquals(-1, StringUtils.indexOfAny(null, new char[] {'a'}));
        assertEquals(-1, StringUtils.indexOfAny("", new char[] {'a'}));
        assertEquals(-1, StringUtils.indexOfAny("abc", (char[]) null));
        assertEquals(-1, StringUtils.indexOfAny("abc", new char[0]));
        assertEquals(-1, StringUtils.indexOfAny(null, "a"));
        assertEquals(-1, StringUtils.indexOfAny("abc", (String) null));
        assertEquals(-1, StringUtils.indexOfAny("abc", ""));
        assertEquals(-1, StringUtils.indexOfAny("", "a"));
        assertEquals(-1, StringUtils.indexOfAny("abc", (String[]) null));
        assertEquals(-1, StringUtils.indexOfAny("abc", new String[0]));
    }

    @Test
    public void testContainsAny_NullOrEmptyInputs() {
        assertFalse(StringUtils.containsAny(null, new char[] {'a'}));
        assertFalse(StringUtils.containsAny("", new char[] {'a'}));
        assertFalse(StringUtils.containsAny("abc", (char[]) null));
        assertFalse(StringUtils.containsAny("abc", new char[0]));
        assertFalse(StringUtils.containsAny(null, "a"));
        assertFalse(StringUtils.containsAny("", "a"));
        assertFalse(StringUtils.containsAny("abc", (String) null));
        assertFalse(StringUtils.containsAny("abc", ""));
    }

    @Test
    public void testContainsOnly_NullAndEmptyInputs() {
        assertFalse(StringUtils.containsOnly(null, "abc"));
        assertFalse(StringUtils.containsOnly("abc", (String) null));
        assertFalse(StringUtils.containsOnly(null, new char[] {'a'}));
        assertFalse(StringUtils.containsOnly("abc", (char[]) null));
        assertFalse(StringUtils.containsOnly("ab", ""));
        assertFalse(StringUtils.containsOnly("abc", new char[0]));
        // Cadena vacia siempre devuelve true segun el contrato
        assertTrue(StringUtils.containsOnly("", "abc"));
        assertTrue(StringUtils.containsOnly("", new char[] {'a'}));
    }

    @Test
    public void testContainsNone_NullAndEmptyInputs() {
        assertTrue(StringUtils.containsNone(null, "abc"));
        assertTrue(StringUtils.containsNone("abc", (String) null));
        assertTrue(StringUtils.containsNone(null, new char[] {'a'}));
        assertTrue(StringUtils.containsNone("abc", (char[]) null));
        assertTrue(StringUtils.containsNone("", "abc"));
        assertTrue(StringUtils.containsNone("", new char[] {'a'}));
        assertTrue(StringUtils.containsNone("ab", ""));
        assertFalse(StringUtils.containsNone("abz", "xyz"));
    }

    // Requisito 8: substring, left, right, mid, substringBefore, substringAfter,
    // substringBetween con null, vacio y posiciones fuera de rango

    @Test
    public void testSubstring_NullAndOutOfRange() {
        assertNull(StringUtils.substring(null, 0));
        assertNull(StringUtils.substring(null, -3));
        assertEquals("", StringUtils.substring("", 0));
        assertEquals("", StringUtils.substring("", 3));
        assertEquals("", StringUtils.substring("abc", 4));
        assertEquals("", StringUtils.substring("abc", 10));
        assertEquals("abc", StringUtils.substring("abc", -4));
        assertEquals("abc", StringUtils.substring("abc", -10));
    }

    @Test
    public void testSubstringStartEnd_NullAndOutOfRange() {
        assertNull(StringUtils.substring(null, 0, 2));
        assertNull(StringUtils.substring(null, -1, -2));
        assertEquals("", StringUtils.substring("", 0, 2));
        assertEquals("", StringUtils.substring("abc", 2, 0));
        assertEquals("", StringUtils.substring("abc", 5, 10));
        assertEquals("", StringUtils.substring("abc", -10, -5));
        assertEquals("bc", StringUtils.substring("abc", 1, 100));
        assertEquals("ab", StringUtils.substring("abc", -4, 2));
    }

    @Test
    public void testLeft_NullAndOutOfRange() {
        assertNull(StringUtils.left(null, 2));
        assertEquals("", StringUtils.left("", 2));
        assertEquals("", StringUtils.left("abc", -1));
        assertEquals("", StringUtils.left("abc", 0));
        assertEquals("abc", StringUtils.left("abc", 10));
    }

    @Test
    public void testRight_NullAndOutOfRange() {
        assertNull(StringUtils.right(null, 2));
        assertEquals("", StringUtils.right("", 2));
        assertEquals("", StringUtils.right("abc", -1));
        assertEquals("", StringUtils.right("abc", 0));
        assertEquals("abc", StringUtils.right("abc", 10));
    }

    @Test
    public void testMid_NullAndOutOfRange() {
        assertNull(StringUtils.mid(null, 0, 2));
        assertEquals("", StringUtils.mid("", 0, 2));
        assertEquals("", StringUtils.mid("abc", 0, -1));
        assertEquals("", StringUtils.mid("abc", 4, 2));
        assertEquals("", StringUtils.mid("abc", 3, 2));
        assertEquals("ab", StringUtils.mid("abc", -5, 2));
        assertEquals("bc", StringUtils.mid("abc", 1, 10));
    }

    @Test
    public void testSubstringBefore_NullEmptyAndNullSeparator() {
        assertNull(StringUtils.substringBefore(null, "a"));
        assertEquals("", StringUtils.substringBefore("", "a"));
        assertEquals("", StringUtils.substringBefore("", ""));
        assertEquals("", StringUtils.substringBefore("abc", ""));
        assertEquals("abc", StringUtils.substringBefore("abc", null));
        assertEquals("abc", StringUtils.substringBefore("abc", "d"));
        assertEquals("a", StringUtils.substringBefore("abcba", "b"));
    }

    @Test
    public void testSubstringAfter_NullEmptyAndNullSeparator() {
        assertNull(StringUtils.substringAfter(null, "a"));
        assertNull(StringUtils.substringAfter(null, null));
        assertEquals("", StringUtils.substringAfter("", "a"));
        assertEquals("", StringUtils.substringAfter("abc", null));
        assertEquals("abc", StringUtils.substringAfter("abc", ""));
        assertEquals("", StringUtils.substringAfter("abc", "d"));
        assertEquals("", StringUtils.substringAfter("abc", "c"));
        assertEquals("cba", StringUtils.substringAfter("abcba", "b"));
    }

    @Test
    public void testSubstringBetween_NullAndEmptyTags() {
        assertNull(StringUtils.substringBetween(null, "[", "]"));
        assertNull(StringUtils.substringBetween("wx[b]yz", null, "]"));
        assertNull(StringUtils.substringBetween("wx[b]yz", "[", null));
        assertNull(StringUtils.substringBetween(null, null));
        assertEquals("", StringUtils.substringBetween("", "", ""));
        assertNull(StringUtils.substringBetween("", "[", "]"));
        assertNull(StringUtils.substringBetween("", "", "]"));
        assertEquals("", StringUtils.substringBetween("tagabctag", ""));
        assertEquals("", StringUtils.substringBetween("yabcz", "", ""));
        assertEquals("abc", StringUtils.substringBetween("tagabctag", "tag"));
        assertNull(StringUtils.substringBetween("abc", "[", "]"));
    }
}
