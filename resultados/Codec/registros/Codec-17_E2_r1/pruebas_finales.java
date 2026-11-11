/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.codec.binary;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

/**
 * Pruebas adicionales para {@link StringUtils} que cubren ramas y líneas no ejecutadas
 * por la suite existente. Solo utilizan la API pública de la clase.
 */
public class StringUtilsLLMTest {

    private static final byte[] ABC_BYTES = {'a', 'b', 'c'};

    // ---------------------------------------------------------------------
    // equals(CharSequence, CharSequence)
    // ---------------------------------------------------------------------

    @Test
    public void testEqualsBothNullReturnsTrue() {
        Assert.assertTrue(StringUtils.equals(null, null));
    }

    @Test
    public void testEqualsFirstNullReturnsFalse() {
        Assert.assertFalse(StringUtils.equals(null, "abc"));
    }

    @Test
    public void testEqualsSecondNullReturnsFalse() {
        Assert.assertFalse(StringUtils.equals("abc", null));
    }

    @Test
    public void testEqualsNullAndStringBuilderReturnsFalse() {
        Assert.assertFalse(StringUtils.equals(null, new StringBuilder("abc")));
        Assert.assertFalse(StringUtils.equals(new StringBuilder("abc"), null));
    }

    @Test
    public void testEqualsSameReferenceReturnsTrue() {
        final String s = "abc";
        Assert.assertTrue(StringUtils.equals(s, s));
    }

    @Test
    public void testEqualsSameStringContentDifferentInstancesReturnsTrue() {
        Assert.assertTrue(StringUtils.equals(new String("abc"), new String("abc")));
    }

    @Test
    public void testEqualsIsCaseSensitive() {
        Assert.assertFalse(StringUtils.equals("abc", "ABC"));
    }

    @Test
    public void testEqualsDifferentStringsSameLengthReturnsFalse() {
        Assert.assertFalse(StringUtils.equals("abc", "abd"));
    }

    @Test
    public void testEqualsPrefixStringReturnsFalse() {
        Assert.assertFalse(StringUtils.equals("abc", "ab"));
        Assert.assertFalse(StringUtils.equals("ab", "abc"));
    }

    @Test
    public void testEqualsEmptyStrings() {
        Assert.assertTrue(StringUtils.equals("", ""));
        Assert.assertFalse(StringUtils.equals("", "a"));
    }

    @Test
    public void testEqualsStringAndStringBuilderSameContentReturnsTrue() {
        Assert.assertTrue(StringUtils.equals("abc", new StringBuilder("abc")));
    }

    @Test
    public void testEqualsStringBuilderAndStringSameContentReturnsTrue() {
        Assert.assertTrue(StringUtils.equals(new StringBuilder("abc"), "abc"));
    }

    @Test
    public void testEqualsTwoStringBuildersSameContentReturnsTrue() {
        Assert.assertTrue(StringUtils.equals(new StringBuilder("xyz"), new StringBuilder("xyz")));
    }

    @Test
    public void testEqualsStringBuildersDifferentContentReturnsFalse() {
        Assert.assertFalse(StringUtils.equals(new StringBuilder("abc"), new StringBuilder("abd")));
    }

    @Test
    public void testEqualsStringBuilderShorterThanStringReturnsFalse() {
        Assert.assertFalse(StringUtils.equals(new StringBuilder("ab"), "abc"));
        Assert.assertFalse(StringUtils.equals("abc", new StringBuilder("ab")));
    }

    @Test
    public void testEqualsStringBuilderDifferentCaseReturnsFalse() {
        Assert.assertFalse(StringUtils.equals(new StringBuilder("abc"), "ABC"));
    }

    @Test
    public void testEqualsIsSymmetricForMixedTypes() {
        final CharSequence sb = new StringBuilder("hola");
        final CharSequence str = "hola";
        Assert.assertEquals(StringUtils.equals(sb, str), StringUtils.equals(str, sb));
        Assert.assertTrue(StringUtils.equals(sb, str));
    }

    @Test
    public void testEqualsEmptyStringBuilderAndEmptyString() {
        Assert.assertTrue(StringUtils.equals(new StringBuilder(), ""));
        Assert.assertFalse(StringUtils.equals(new StringBuilder(), "x"));
    }

    // ---------------------------------------------------------------------
    // getBytes* con entrada null
    // ---------------------------------------------------------------------

    @Test
    public void testGetBytesIso8859_1NullReturnsNull() {
        Assert.assertNull(StringUtils.getBytesIso8859_1(null));
    }

    @Test
    public void testGetBytesUsAsciiNullReturnsNull() {
        Assert.assertNull(StringUtils.getBytesUsAscii(null));
    }

    @Test
    public void testGetBytesUtf16NullReturnsNull() {
        Assert.assertNull(StringUtils.getBytesUtf16(null));
    }

    @Test
    public void testGetBytesUtf16BeNullReturnsNull() {
        Assert.assertNull(StringUtils.getBytesUtf16Be(null));
    }

    @Test
    public void testGetBytesUtf16LeNullReturnsNull() {
        Assert.assertNull(StringUtils.getBytesUtf16Le(null));
    }

    @Test
    public void testGetBytesUtf8NullReturnsNull() {
        Assert.assertNull(StringUtils.getBytesUtf8(null));
    }

    @Test
    public void testGetBytesUncheckedNullWithValidCharsetReturnsNull() {
        Assert.assertNull(StringUtils.getBytesUnchecked(null, "UTF-8"));
    }

    @Test
    public void testGetBytesUncheckedValidCharsetReturnsEncodedBytes() {
        Assert.assertArrayEquals(ABC_BYTES, StringUtils.getBytesUnchecked("abc", "US-ASCII"));
    }

    @Test
    public void testGetBytesUncheckedBadNameMessageContainsCharsetName() {
        try {
            StringUtils.getBytesUnchecked("abc", "NO-SUCH-CHARSET");
            Assert.fail("Se esperaba IllegalStateException");
        } catch (final IllegalStateException e) {
            Assert.assertTrue(e.getMessage().contains("NO-SUCH-CHARSET"));
        }
    }

    // ---------------------------------------------------------------------
    // getBytes* con contenido concreto
    // ---------------------------------------------------------------------

    @Test
    public void testGetBytesIso8859_1UnmappableCharacterBecomesQuestionMark() {
        // El símbolo euro no existe en ISO-8859-1: se sustituye por '?'
        Assert.assertArrayEquals(new byte[] {'a', '?'}, StringUtils.getBytesIso8859_1("a\u20AC"));
    }

    @Test
    public void testGetBytesIso8859_1LatinCharacterEncodedAsSingleByte() {
        Assert.assertArrayEquals(new byte[] {(byte) 0xE9}, StringUtils.getBytesIso8859_1("\u00E9"));
    }

    @Test
    public void testGetBytesUsAsciiNonAsciiBecomesQuestionMark() {
        Assert.assertArrayEquals(new byte[] {'a', '?', 'c'}, StringUtils.getBytesUsAscii("a\u00E9c"));
    }

    @Test
    public void testGetBytesUtf8MultiByteCharacter() {
        Assert.assertArrayEquals(new byte[] {(byte) 0xC3, (byte) 0xA9}, StringUtils.getBytesUtf8("\u00E9"));
    }

    @Test
    public void testGetBytesUtf8EmptyString() {
        Assert.assertArrayEquals(new byte[0], StringUtils.getBytesUtf8(""));
    }

    @Test
    public void testGetBytesUtf16BeCharacterLayout() {
        Assert.assertArrayEquals(new byte[] {0, 'A'}, StringUtils.getBytesUtf16Be("A"));
    }

    @Test
    public void testGetBytesUtf16LeCharacterLayout() {
        Assert.assertArrayEquals(new byte[] {'A', 0}, StringUtils.getBytesUtf16Le("A"));
    }

    @Test
    public void testGetBytesUtf16StartsWithBigEndianBomAndContent() {
        final byte[] actual = StringUtils.getBytesUtf16("A");
        Assert.assertArrayEquals(new byte[] {(byte) 0xFE, (byte) 0xFF, 0, 'A'}, actual);
    }

    @Test
    public void testGetBytesUtf16EmptyStringHasOnlyBom() {
        final byte[] actual = StringUtils.getBytesUtf16("");
        Assert.assertArrayEquals(new byte[] {(byte) 0xFE, (byte) 0xFF}, actual);
    }

    // ---------------------------------------------------------------------
    // getByteBufferUtf8
    // ---------------------------------------------------------------------

    @Test
    public void testGetByteBufferUtf8NullReturnsNull() {
        Assert.assertNull(StringUtils.getByteBufferUtf8(null));
    }

    @Test
    public void testGetByteBufferUtf8ContainsEncodedBytes() {
        final ByteBuffer buffer = StringUtils.getByteBufferUtf8("abc");
        Assert.assertNotNull(buffer);
        Assert.assertEquals(3, buffer.remaining());
        Assert.assertArrayEquals(ABC_BYTES, buffer.array());
    }

    @Test
    public void testGetByteBufferUtf8MultiByteCharacterLength() {
        final ByteBuffer buffer = StringUtils.getByteBufferUtf8("\u00E9");
        Assert.assertNotNull(buffer);
        Assert.assertEquals(2, buffer.remaining());
        Assert.assertArrayEquals(new byte[] {(byte) 0xC3, (byte) 0xA9}, buffer.array());
    }

    @Test
    public void testGetByteBufferUtf8EmptyStringHasNoRemaining() {
        final ByteBuffer buffer = StringUtils.getByteBufferUtf8("");
        Assert.assertNotNull(buffer);
        Assert.assertEquals(0, buffer.remaining());
    }

    // ---------------------------------------------------------------------
    // newString* con entrada null
    // ---------------------------------------------------------------------

    @Test
    public void testNewStringNullWithValidCharsetReturnsNull() {
        Assert.assertNull(StringUtils.newString(null, "UTF-8"));
    }

    @Test
    public void testNewStringUsAsciiNullReturnsNull() {
        Assert.assertNull(StringUtils.newStringUsAscii(null));
    }

    @Test
    public void testNewStringUtf16NullReturnsNull() {
        Assert.assertNull(StringUtils.newStringUtf16(null));
    }

    @Test
    public void testNewStringUtf16BeNullReturnsNull() {
        Assert.assertNull(StringUtils.newStringUtf16Be(null));
    }

    @Test
    public void testNewStringUtf16LeNullReturnsNull() {
        Assert.assertNull(StringUtils.newStringUtf16Le(null));
    }

    @Test
    public void testNewStringUtf8NullReturnsNull() {
        Assert.assertNull(StringUtils.newStringUtf8(null));
    }

    // ---------------------------------------------------------------------
    // newString* con contenido concreto
    // ---------------------------------------------------------------------

    @Test
    public void testNewStringUtf8DecodesMultiByteCharacter() {
        final byte[] bytes = {(byte) 0xC3, (byte) 0xA9};
        Assert.assertEquals("\u00E9", StringUtils.newStringUtf8(bytes));
    }

    @Test
    public void testNewStringUtf8EmptyArrayReturnsEmptyString() {
        Assert.assertEquals("", StringUtils.newStringUtf8(new byte[0]));
    }

    @Test
    public void testNewStringIso8859_1DecodesLatinCharacter() {
        Assert.assertEquals("\u00E9", StringUtils.newStringIso8859_1(new byte[] {(byte) 0xE9}));
    }

    @Test
    public void testNewStringUsAsciiNonAsciiByteBecomesReplacementCharacter() {
        Assert.assertEquals("a\uFFFDc", StringUtils.newStringUsAscii(new byte[] {'a', (byte) 0xE9, 'c'}));
    }

    @Test
    public void testNewStringUtf16DecodesWithBom() {
        final byte[] bytes = {(byte) 0xFE, (byte) 0xFF, 0, 'A'};
        Assert.assertEquals("A", StringUtils.newStringUtf16(bytes));
    }

    @Test
    public void testNewStringUtf16BeDecodesCharacters() {
        Assert.assertEquals("A", StringUtils.newStringUtf16Be(new byte[] {0, 'A'}));
    }

    @Test
    public void testNewStringUtf16LeDecodesCharacters() {
        Assert.assertEquals("A", StringUtils.newStringUtf16Le(new byte[] {'A', 0}));
    }

    // ---------------------------------------------------------------------
    // Ida y vuelta (round trip) entre bytes y cadenas
    // ---------------------------------------------------------------------

    @Test
    public void testRoundTripUtf8() {
        final String original = "Grüße, 世界";
        Assert.assertEquals(original, StringUtils.newStringUtf8(StringUtils.getBytesUtf8(original)));
    }

    @Test
    public void testRoundTripUtf16() {
        final String original = "abc";
        Assert.assertEquals(original, StringUtils.newStringUtf16(StringUtils.getBytesUtf16(original)));
    }

    @Test
    public void testRoundTripUtf16BeAndLe() {
        final String original = "xyz";
        Assert.assertEquals(original, StringUtils.newStringUtf16Be(StringUtils.getBytesUtf16Be(original)));
        Assert.assertEquals(original, StringUtils.newStringUtf16Le(StringUtils.getBytesUtf16Le(original)));
    }

    @Test
    public void testRoundTripIso8859_1() {
        final String original = "caf\u00E9";
        Assert.assertEquals(original, StringUtils.newStringIso8859_1(StringUtils.getBytesIso8859_1(original)));
    }

    @Test
    public void testRoundTripUsAscii() {
        final String original = "ABC123";
        Assert.assertEquals(original, StringUtils.newStringUsAscii(StringUtils.getBytesUsAscii(original)));
    }

    @Test
    public void testRoundTripUncheckedWithUtf8Name() {
        final String original = "\u00E9t\u00E9";
        Assert.assertEquals(original,
                StringUtils.newString(StringUtils.getBytesUnchecked(original, "UTF-8"), "UTF-8"));
    }

    @Test
    public void testNewStringAndNewStringUtf8AgreeOnSameBytes() {
        final byte[] bytes = {(byte) 0xC3, (byte) 0xA9, 'x'};
        Assert.assertEquals(StringUtils.newString(bytes, "UTF-8"), StringUtils.newStringUtf8(bytes));
    }

    @Test
    public void testGetBytesUtf8MatchesStandardCharsetsEncoding() {
        final String original = "Ñandú";
        Assert.assertArrayEquals(original.getBytes(StandardCharsets.UTF_8), StringUtils.getBytesUtf8(original));
    }
}
