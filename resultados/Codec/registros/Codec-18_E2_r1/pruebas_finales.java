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
 * Pruebas adicionales para {@link StringUtils}, centradas en la ruta de valores nulos,
 * en {@link StringUtils#getByteBufferUtf8(String)} y en las ramas de
 * {@link StringUtils#equals(CharSequence, CharSequence)} con CharSequence que no son String.
 */
public class StringUtilsLLMTest {

    // ---------------------------------------------------------------------
    // equals(CharSequence, CharSequence)
    // ---------------------------------------------------------------------

    @Test
    public void testEqualsSameCharSequenceInstanceIsTrue() {
        final StringBuilder sb = new StringBuilder("abc");
        Assert.assertTrue(StringUtils.equals(sb, sb));
    }

    @Test
    public void testEqualsStringBuilderAndStringWithSameContentIsTrue() {
        Assert.assertTrue(StringUtils.equals(new StringBuilder("abc"), "abc"));
        Assert.assertTrue(StringUtils.equals("abc", new StringBuilder("abc")));
    }

    @Test
    public void testEqualsTwoStringBuildersWithSameContentIsTrue() {
        Assert.assertTrue(StringUtils.equals(new StringBuilder("hola"), new StringBuilder("hola")));
    }

    @Test
    public void testEqualsTwoStringBuildersWithDifferentContentIsFalse() {
        Assert.assertFalse(StringUtils.equals(new StringBuilder("abc"), new StringBuilder("abd")));
    }

    @Test
    public void testEqualsStringBuilderIsCaseSensitive() {
        Assert.assertFalse(StringUtils.equals(new StringBuilder("abc"), "ABC"));
        Assert.assertFalse(StringUtils.equals("ABC", new StringBuilder("abc")));
    }

    @Test
    public void testEqualsStringBuilderWithDifferentLengthIsFalse() {
        Assert.assertFalse(StringUtils.equals(new StringBuilder("abc"), new StringBuilder("abcd")));
        Assert.assertFalse(StringUtils.equals(new StringBuilder("abcd"), new StringBuilder("abc")));
        Assert.assertFalse(StringUtils.equals("abc", new StringBuilder("abcd")));
    }

    @Test
    public void testEqualsEmptyStringBuilderAndEmptyStringIsTrue() {
        Assert.assertTrue(StringUtils.equals(new StringBuilder(), ""));
        Assert.assertTrue(StringUtils.equals("", new StringBuilder()));
    }

    @Test
    public void testEqualsNullAndStringBuilderIsFalse() {
        Assert.assertFalse(StringUtils.equals(null, new StringBuilder("abc")));
        Assert.assertFalse(StringUtils.equals(new StringBuilder("abc"), null));
    }

    // ---------------------------------------------------------------------
    // getByteBufferUtf8(String)
    // ---------------------------------------------------------------------

    @Test
    public void testGetByteBufferUtf8NullInputReturnsNull() {
        Assert.assertNull(StringUtils.getByteBufferUtf8(null));
    }

    @Test
    public void testGetByteBufferUtf8AsciiContent() {
        final ByteBuffer buffer = StringUtils.getByteBufferUtf8("abc");
        Assert.assertNotNull(buffer);
        Assert.assertEquals(3, buffer.remaining());
        final byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        Assert.assertArrayEquals(new byte[] {'a', 'b', 'c'}, actual);
    }

    @Test
    public void testGetByteBufferUtf8MultiByteContent() {
        // "\u00E9" (e acute) occupies two bytes in UTF-8: C3 A9
        final ByteBuffer buffer = StringUtils.getByteBufferUtf8("\u00E9");
        Assert.assertNotNull(buffer);
        Assert.assertEquals(2, buffer.remaining());
        final byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        Assert.assertArrayEquals(new byte[] {(byte) 0xC3, (byte) 0xA9}, actual);
    }

    @Test
    public void testGetByteBufferUtf8EmptyStringHasNoRemainingBytes() {
        final ByteBuffer buffer = StringUtils.getByteBufferUtf8("");
        Assert.assertNotNull(buffer);
        Assert.assertEquals(0, buffer.remaining());
    }

    @Test
    public void testGetByteBufferUtf8MatchesGetBytesUtf8() {
        final String text = "Ma\u00F1ana \u20AC";
        final ByteBuffer buffer = StringUtils.getByteBufferUtf8(text);
        final byte[] fromBuffer = new byte[buffer.remaining()];
        buffer.get(fromBuffer);
        Assert.assertArrayEquals(StringUtils.getBytesUtf8(text), fromBuffer);
        Assert.assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), fromBuffer);
    }

    // ---------------------------------------------------------------------
    // Entradas nulas en los métodos getBytes*
    // ---------------------------------------------------------------------

    @Test
    public void testGetBytesIso8859_1NullInputReturnsNull() {
        Assert.assertNull(StringUtils.getBytesIso8859_1(null));
    }

    @Test
    public void testGetBytesUsAsciiNullInputReturnsNull() {
        Assert.assertNull(StringUtils.getBytesUsAscii(null));
    }

    @Test
    public void testGetBytesUtf16NullInputReturnsNull() {
        Assert.assertNull(StringUtils.getBytesUtf16(null));
    }

    @Test
    public void testGetBytesUtf16BeNullInputReturnsNull() {
        Assert.assertNull(StringUtils.getBytesUtf16Be(null));
    }

    @Test
    public void testGetBytesUtf16LeNullInputReturnsNull() {
        Assert.assertNull(StringUtils.getBytesUtf16Le(null));
    }

    @Test
    public void testGetBytesUtf8NullInputReturnsNull() {
        Assert.assertNull(StringUtils.getBytesUtf8(null));
    }

    @Test
    public void testGetBytesUncheckedNullInputWithValidCharsetReturnsNull() {
        Assert.assertNull(StringUtils.getBytesUnchecked(null, "UTF-8"));
    }

    // ---------------------------------------------------------------------
    // Codificación con charsets concretos
    // ---------------------------------------------------------------------

    @Test
    public void testGetBytesIso8859_1EncodesLatin1Character() {
        // U+00E9 in ISO-8859-1 is the single byte 0xE9
        Assert.assertArrayEquals(new byte[] {(byte) 0xE9}, StringUtils.getBytesIso8859_1("\u00E9"));
    }

    @Test
    public void testGetBytesUsAsciiReplacesNonAsciiWithQuestionMark() {
        Assert.assertArrayEquals(new byte[] {'a', '?', 'b'}, StringUtils.getBytesUsAscii("a\u00E9b"));
    }

    @Test
    public void testGetBytesUtf8EncodesMultiByteCharacter() {
        Assert.assertArrayEquals(new byte[] {(byte) 0xE2, (byte) 0x82, (byte) 0xAC},
                StringUtils.getBytesUtf8("\u20AC"));
    }

    @Test
    public void testGetBytesUtf16BeEncodesBigEndianWithoutBom() {
        Assert.assertArrayEquals(new byte[] {0, 'A', 0, 'B'}, StringUtils.getBytesUtf16Be("AB"));
    }

    @Test
    public void testGetBytesUtf16LeEncodesLittleEndianWithoutBom() {
        Assert.assertArrayEquals(new byte[] {'A', 0, 'B', 0}, StringUtils.getBytesUtf16Le("AB"));
    }

    @Test
    public void testGetBytesUtf16WritesBigEndianBomThenData() {
        // La codificación UTF-16 de Java escribe BOM FE FF seguido de big-endian.
        Assert.assertArrayEquals(new byte[] {(byte) 0xFE, (byte) 0xFF, 0, 'A'},
                StringUtils.getBytesUtf16("A"));
    }

    @Test
    public void testGetBytesUncheckedEncodesWithNamedCharset() {
        Assert.assertArrayEquals("\u00E9".getBytes(StandardCharsets.UTF_16BE),
                StringUtils.getBytesUnchecked("\u00E9", "UTF-16BE"));
    }

    @Test
    public void testGetBytesUncheckedEmptyStringReturnsEmptyArray() {
        Assert.assertArrayEquals(new byte[0], StringUtils.getBytesUnchecked("", "UTF-8"));
    }

    // ---------------------------------------------------------------------
    // Decodificación con charsets concretos
    // ---------------------------------------------------------------------

    @Test
    public void testNewStringIso8859_1DecodesLatin1Byte() {
        Assert.assertEquals("\u00E9", StringUtils.newStringIso8859_1(new byte[] {(byte) 0xE9}));
    }

    @Test
    public void testNewStringIso8859_1NonNullEmptyArrayReturnsEmptyString() {
        Assert.assertEquals("", StringUtils.newStringIso8859_1(new byte[0]));
    }

    @Test
    public void testNewStringUsAsciiReplacesMalformedBytes() {
        // 0x80 no es válido en US-ASCII; el decodificador lo sustituye por U+FFFD
        Assert.assertEquals("a\uFFFD", StringUtils.newStringUsAscii(new byte[] {'a', (byte) 0x80}));
    }

    @Test
    public void testNewStringUsAsciiDecodesAsciiBytes() {
        Assert.assertEquals("ABC", StringUtils.newStringUsAscii(new byte[] {'A', 'B', 'C'}));
    }

    @Test
    public void testNewStringUtf8DecodesMultiByteCharacter() {
        Assert.assertEquals("\u20AC", StringUtils.newStringUtf8(new byte[] {(byte) 0xE2, (byte) 0x82, (byte) 0xAC}));
    }

    @Test
    public void testNewStringUtf8RoundTripWithGetBytesUtf8() {
        final String text = "Ma\u00F1ana \u20AC \uD83D\uDE00";
        Assert.assertEquals(text, StringUtils.newStringUtf8(StringUtils.getBytesUtf8(text)));
    }

    @Test
    public void testNewStringUtf16DecodesBomPrefixedBytes() {
        Assert.assertEquals("A",
                StringUtils.newStringUtf16(new byte[] {(byte) 0xFE, (byte) 0xFF, 0, 'A'}));
    }

    @Test
    public void testNewStringUtf16RoundTripWithGetBytesUtf16() {
        final String text = "Hola \u00E9";
        Assert.assertEquals(text, StringUtils.newStringUtf16(StringUtils.getBytesUtf16(text)));
    }

    @Test
    public void testNewStringUtf16BeDecodesBigEndianBytes() {
        Assert.assertEquals("AB", StringUtils.newStringUtf16Be(new byte[] {0, 'A', 0, 'B'}));
    }

    @Test
    public void testNewStringUtf16LeDecodesLittleEndianBytes() {
        Assert.assertEquals("AB", StringUtils.newStringUtf16Le(new byte[] {'A', 0, 'B', 0}));
    }

    @Test
    public void testNewStringUtf16BeRoundTrip() {
        final String text = "\u00E9t\u00E9";
        Assert.assertEquals(text, StringUtils.newStringUtf16Be(StringUtils.getBytesUtf16Be(text)));
    }

    @Test
    public void testNewStringUtf16LeRoundTrip() {
        final String text = "\u00E9t\u00E9";
        Assert.assertEquals(text, StringUtils.newStringUtf16Le(StringUtils.getBytesUtf16Le(text)));
    }

    @Test
    public void testNewStringUtf8EmptyArrayReturnsEmptyString() {
        Assert.assertEquals("", StringUtils.newStringUtf8(new byte[0]));
    }

    @Test
    public void testNewStringWithNamedCharsetDecodesUtf8() {
        Assert.assertEquals("\u00E9", StringUtils.newString(new byte[] {(byte) 0xC3, (byte) 0xA9}, "UTF-8"));
    }

    @Test
    public void testNewStringWithNamedCharsetRoundTripWithGetBytesUnchecked() {
        final String text = "Prueba \u00E1";
        Assert.assertEquals(text,
                StringUtils.newString(StringUtils.getBytesUnchecked(text, "UTF-16LE"), "UTF-16LE"));
    }

    @Test
    public void testNewStringWithNamedCharsetNullInputReturnsNullForValidCharset() {
        Assert.assertNull(StringUtils.newString(null, "UTF-8"));
    }

    @Test
    public void testNewStringNullInputForAllCharsetVariantsWithNonNullEmptyArrays() {
        Assert.assertEquals("", StringUtils.newStringIso8859_1(new byte[0]));
        Assert.assertEquals("", StringUtils.newStringUsAscii(new byte[0]));
        Assert.assertEquals("", StringUtils.newStringUtf8(new byte[0]));
        Assert.assertEquals("", StringUtils.newStringUtf16Be(new byte[0]));
        Assert.assertEquals("", StringUtils.newStringUtf16Le(new byte[0]));
    }
}
