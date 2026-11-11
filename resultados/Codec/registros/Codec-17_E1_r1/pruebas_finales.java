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

import org.junit.Assert;
import org.junit.Test;

/**
 * Pruebas adicionales para {@link StringUtils}, usando solo su API pública.
 */
public class StringUtilsLLMTest {

    private static final String E_ACUTE = "\u00E9";      // é
    private static final String EURO = "\u20AC";         // €

    private static byte[] bytes(final int... values) {
        final byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }
        return result;
    }

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
    public void testEqualsSameInstanceReturnsTrue() {
        final String s = "abc";
        Assert.assertTrue(StringUtils.equals(s, s));
    }

    @Test
    public void testEqualsEqualStringsReturnsTrue() {
        Assert.assertTrue(StringUtils.equals("abc", new String("abc")));
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
    public void testEqualsDifferentLengthStringsReturnsFalse() {
        Assert.assertFalse(StringUtils.equals("abc", "ab"));
        Assert.assertFalse(StringUtils.equals("ab", "abc"));
    }

    @Test
    public void testEqualsEmptyStringsReturnsTrue() {
        Assert.assertTrue(StringUtils.equals("", ""));
    }

    @Test
    public void testEqualsEmptyAndNonEmptyReturnsFalse() {
        Assert.assertFalse(StringUtils.equals("", "a"));
    }

    @Test
    public void testEqualsStringBuilderAndStringWithSameContentReturnsTrue() {
        Assert.assertTrue(StringUtils.equals(new StringBuilder("abc"), "abc"));
        Assert.assertTrue(StringUtils.equals("abc", new StringBuilder("abc")));
    }

    @Test
    public void testEqualsStringBuildersWithDifferentContentReturnsFalse() {
        Assert.assertFalse(StringUtils.equals(new StringBuilder("abc"), new StringBuilder("abd")));
    }

    @Test
    public void testEqualsStringBuilderAndStringDifferentCaseReturnsFalse() {
        Assert.assertFalse(StringUtils.equals(new StringBuilder("abc"), "ABC"));
    }

    // ---------------------------------------------------------------------
    // getBytes* (codificación)
    // ---------------------------------------------------------------------

    @Test
    public void testGetBytesUtf8EmptyStringReturnsEmptyArray() {
        final byte[] actual = StringUtils.getBytesUtf8("");
        Assert.assertNotNull(actual);
        Assert.assertEquals(0, actual.length);
    }

    @Test
    public void testGetBytesUtf8NonAsciiUsesTwoBytes() {
        Assert.assertArrayEquals(bytes(0xC3, 0xA9), StringUtils.getBytesUtf8(E_ACUTE));
    }

    @Test
    public void testGetBytesUtf8EuroSignUsesThreeBytes() {
        Assert.assertArrayEquals(bytes(0xE2, 0x82, 0xAC), StringUtils.getBytesUtf8(EURO));
    }

    @Test
    public void testGetBytesIso8859_1SingleByteForLatin1Character() {
        Assert.assertArrayEquals(bytes(0xE9), StringUtils.getBytesIso8859_1(E_ACUTE));
    }

    @Test
    public void testGetBytesUsAsciiReplacesUnmappableCharacter() {
        // Los caracteres no ASCII se sustituyen por '?' (0x3F)
        Assert.assertArrayEquals(bytes(0x41, 0x3F), StringUtils.getBytesUsAscii("A" + E_ACUTE));
    }

    @Test
    public void testGetBytesUtf16IncludesBigEndianBom() {
        Assert.assertArrayEquals(bytes(0xFE, 0xFF, 0x00, 0x41), StringUtils.getBytesUtf16("A"));
    }

    @Test
    public void testGetBytesUtf16BeHasNoBomAndBigEndianOrder() {
        Assert.assertArrayEquals(bytes(0x00, 0x41), StringUtils.getBytesUtf16Be("A"));
    }

    @Test
    public void testGetBytesUtf16LeHasNoBomAndLittleEndianOrder() {
        Assert.assertArrayEquals(bytes(0x41, 0x00), StringUtils.getBytesUtf16Le("A"));
    }

    @Test
    public void testGetBytesMethodsReturnNullForNullInput() {
        Assert.assertNull(StringUtils.getBytesIso8859_1(null));
        Assert.assertNull(StringUtils.getBytesUsAscii(null));
        Assert.assertNull(StringUtils.getBytesUtf16(null));
        Assert.assertNull(StringUtils.getBytesUtf16Be(null));
        Assert.assertNull(StringUtils.getBytesUtf16Le(null));
        Assert.assertNull(StringUtils.getBytesUtf8(null));
        Assert.assertNull(StringUtils.getBytesUnchecked(null, "UTF-8"));
        Assert.assertNull(StringUtils.getByteBufferUtf8(null));
    }

    @Test
    public void testGetBytesUncheckedWithUtf8MatchesGetBytesUtf8() {
        Assert.assertArrayEquals(
                StringUtils.getBytesUtf8("h" + E_ACUTE + EURO),
                StringUtils.getBytesUnchecked("h" + E_ACUTE + EURO, "UTF-8"));
    }

    @Test
    public void testGetBytesUncheckedUnknownCharsetThrowsWithCharsetNameInMessage() {
        try {
            StringUtils.getBytesUnchecked("abc", "UNKNOWN");
            Assert.fail("Se esperaba IllegalStateException");
        } catch (final IllegalStateException e) {
            Assert.assertTrue(e.getMessage().startsWith("UNKNOWN"));
        }
    }

    // ---------------------------------------------------------------------
    // getByteBufferUtf8
    // ---------------------------------------------------------------------

    @Test
    public void testGetByteBufferUtf8ContainsEncodedBytes() {
        final ByteBuffer buffer = StringUtils.getByteBufferUtf8("abc");
        Assert.assertNotNull(buffer);
        Assert.assertEquals(3, buffer.remaining());
        final byte[] content = new byte[buffer.remaining()];
        buffer.get(content);
        Assert.assertArrayEquals(bytes('a', 'b', 'c'), content);
    }

    @Test
    public void testGetByteBufferUtf8NonAsciiUsesUtf8Length() {
        final ByteBuffer buffer = StringUtils.getByteBufferUtf8(E_ACUTE);
        Assert.assertNotNull(buffer);
        Assert.assertEquals(2, buffer.remaining());
    }

    // ---------------------------------------------------------------------
    // newString* (decodificación)
    // ---------------------------------------------------------------------

    @Test
    public void testNewStringUtf8RoundTrip() {
        final String original = "h" + E_ACUTE + "llo " + EURO;
        Assert.assertEquals(original, StringUtils.newStringUtf8(StringUtils.getBytesUtf8(original)));
    }

    @Test
    public void testNewStringUtf8EmptyArrayReturnsEmptyString() {
        Assert.assertEquals("", StringUtils.newStringUtf8(new byte[0]));
    }

    @Test
    public void testNewStringUtf8InvalidSequenceIsReplaced() {
        Assert.assertEquals("\uFFFD", StringUtils.newStringUtf8(bytes(0xFF)));
    }

    @Test
    public void testNewStringUtf8NullReturnsNull() {
        Assert.assertNull(StringUtils.newStringUtf8(null));
    }

    @Test
    public void testNewStringIso8859_1DecodesLatin1Byte() {
        Assert.assertEquals(E_ACUTE, StringUtils.newStringIso8859_1(bytes(0xE9)));
    }

    @Test
    public void testNewStringIso8859_1RoundTrip() {
        final String original = "caf" + E_ACUTE;
        Assert.assertEquals(original, StringUtils.newStringIso8859_1(StringUtils.getBytesIso8859_1(original)));
    }

    @Test
    public void testNewStringUsAsciiReplacesHighByteWithReplacementCharacter() {
        Assert.assertEquals("A\uFFFD", StringUtils.newStringUsAscii(bytes(0x41, 0xE9)));
    }

    @Test
    public void testNewStringUsAsciiNullReturnsNull() {
        Assert.assertNull(StringUtils.newStringUsAscii(null));
    }

    @Test
    public void testNewStringUtf16RoundTrip() {
        final String original = "Hola";
        Assert.assertEquals(original, StringUtils.newStringUtf16(StringUtils.getBytesUtf16(original)));
    }

    @Test
    public void testNewStringUtf16NullReturnsNull() {
        Assert.assertNull(StringUtils.newStringUtf16(null));
    }

    @Test
    public void testNewStringUtf16BeDecodesBigEndianBytes() {
        Assert.assertEquals("AB", StringUtils.newStringUtf16Be(bytes(0x00, 0x41, 0x00, 0x42)));
    }

    @Test
    public void testNewStringUtf16BeRoundTrip() {
        final String original = E_ACUTE + EURO;
        Assert.assertEquals(original, StringUtils.newStringUtf16Be(StringUtils.getBytesUtf16Be(original)));
    }

    @Test
    public void testNewStringUtf16BeNullReturnsNull() {
        Assert.assertNull(StringUtils.newStringUtf16Be(null));
    }

    @Test
    public void testNewStringUtf16LeDecodesLittleEndianBytes() {
        Assert.assertEquals("AB", StringUtils.newStringUtf16Le(bytes(0x41, 0x00, 0x42, 0x00)));
    }

    @Test
    public void testNewStringUtf16LeRoundTrip() {
        final String original = E_ACUTE + EURO;
        Assert.assertEquals(original, StringUtils.newStringUtf16Le(StringUtils.getBytesUtf16Le(original)));
    }

    @Test
    public void testNewStringUtf16LeNullReturnsNull() {
        Assert.assertNull(StringUtils.newStringUtf16Le(null));
    }

    @Test
    public void testNewStringWithValidCharsetNullBytesReturnsNull() {
        Assert.assertNull(StringUtils.newString(null, "UTF-8"));
        Assert.assertNull(StringUtils.newString(null, "ISO-8859-1"));
    }

    @Test
    public void testNewStringWithValidCharsetDecodesBytes() {
        Assert.assertEquals("abc", StringUtils.newString(bytes('a', 'b', 'c'), "US-ASCII"));
    }

    @Test
    public void testNewStringUnknownCharsetThrowsIllegalStateException() {
        try {
            StringUtils.newString(bytes('a'), "UNKNOWN");
            Assert.fail("Se esperaba IllegalStateException");
        } catch (final IllegalStateException e) {
            Assert.assertTrue(e.getMessage().startsWith("UNKNOWN"));
        }
    }

    // ---------------------------------------------------------------------
    // Round trips de codificación/decodificación
    // ---------------------------------------------------------------------

    @Test
    public void testRoundTripUtf8WithEmptyString() {
        Assert.assertEquals("", StringUtils.newStringUtf8(StringUtils.getBytesUtf8("")));
    }

    @Test
    public void testRoundTripUsingGenericNewStringAndGetBytesUnchecked() {
        final String original = "texto de prueba";
        final byte[] encoded = StringUtils.getBytesUnchecked(original, "UTF-8");
        Assert.assertEquals(original, StringUtils.newString(encoded, "UTF-8"));
    }
}
