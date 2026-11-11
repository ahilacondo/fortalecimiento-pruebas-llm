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
 * Pruebas adicionales para {@link StringUtils} que cubren los comportamientos
 * no verificados por {@code StringUtilsTest}.
 */
public class StringUtilsLLMTest {

    // Cadenas no ASCII expresadas con escapes Unicode para no depender de la codificación del fichero fuente
    private static final String N_TILDE = "\u00F1";          // ñ
    private static final String EURO = "\u20AC";             // €
    private static final String GRINNING = "\uD83D\uDE00";   // 😀 (par sustituto)

    // ---------------------------------------------------------------------
    // 1. getByteBufferUtf8: codificación y contenido
    // ---------------------------------------------------------------------

    @Test
    public void testGetByteBufferUtf8EncodesAsciiContent() {
        final ByteBuffer buffer = StringUtils.getByteBufferUtf8("abc");
        Assert.assertNotNull(buffer);
        Assert.assertArrayEquals(new byte[] {'a', 'b', 'c'}, toBytes(buffer));
    }

    @Test
    public void testGetByteBufferUtf8EncodesAsUtf8() {
        final ByteBuffer buffer = StringUtils.getByteBufferUtf8(N_TILDE + EURO);
        Assert.assertNotNull(buffer);
        Assert.assertArrayEquals((N_TILDE + EURO).getBytes(StandardCharsets.UTF_8), toBytes(buffer));
        // ñ = C3 B1, € = E2 82 AC
        Assert.assertArrayEquals(
                new byte[] {(byte) 0xC3, (byte) 0xB1, (byte) 0xE2, (byte) 0x82, (byte) 0xAC},
                toBytes(buffer));
    }

    @Test
    public void testGetByteBufferUtf8ReturnsBufferAtStartWithFullLimit() {
        final ByteBuffer buffer = StringUtils.getByteBufferUtf8("abc");
        Assert.assertEquals(0, buffer.position());
        Assert.assertEquals(3, buffer.limit());
        Assert.assertEquals(3, buffer.remaining());
    }

    @Test
    public void testGetByteBufferUtf8EmptyStringHasEmptyBuffer() {
        final ByteBuffer buffer = StringUtils.getByteBufferUtf8("");
        Assert.assertNotNull(buffer);
        Assert.assertEquals(0, buffer.position());
        Assert.assertEquals(0, buffer.limit());
        Assert.assertEquals(0, buffer.remaining());
    }

    // ---------------------------------------------------------------------
    // 2. getByteBufferUtf8(null)
    // ---------------------------------------------------------------------

    @Test
    public void testGetByteBufferUtf8NullInputReturnsNull() {
        Assert.assertNull(StringUtils.getByteBufferUtf8(null));
    }

    // ---------------------------------------------------------------------
    // 3. Entrada null en los métodos getBytes* (salvo getBytesUnchecked)
    // ---------------------------------------------------------------------

    @Test
    public void testGetBytesMethodsNullInputReturnNull() {
        Assert.assertNull(StringUtils.getBytesIso8859_1(null));
        Assert.assertNull(StringUtils.getBytesUsAscii(null));
        Assert.assertNull(StringUtils.getBytesUtf16(null));
        Assert.assertNull(StringUtils.getBytesUtf16Be(null));
        Assert.assertNull(StringUtils.getBytesUtf16Le(null));
        Assert.assertNull(StringUtils.getBytesUtf8(null));
    }

    // ---------------------------------------------------------------------
    // 4. Cadena vacía y array de bytes vacío
    // ---------------------------------------------------------------------

    @Test
    public void testGetBytesEmptyStringReturnsEmptyArray() {
        Assert.assertArrayEquals(new byte[0], StringUtils.getBytesIso8859_1(""));
        Assert.assertArrayEquals(new byte[0], StringUtils.getBytesUsAscii(""));
        Assert.assertArrayEquals(new byte[0], StringUtils.getBytesUtf16Be(""));
        Assert.assertArrayEquals(new byte[0], StringUtils.getBytesUtf16Le(""));
        Assert.assertArrayEquals(new byte[0], StringUtils.getBytesUtf8(""));
    }

    @Test
    public void testNewStringEmptyArrayReturnsEmptyString() {
        Assert.assertEquals("", StringUtils.newStringIso8859_1(new byte[0]));
        Assert.assertEquals("", StringUtils.newStringUsAscii(new byte[0]));
        Assert.assertEquals("", StringUtils.newStringUtf16Be(new byte[0]));
        Assert.assertEquals("", StringUtils.newStringUtf16Le(new byte[0]));
        Assert.assertEquals("", StringUtils.newStringUtf8(new byte[0]));
        Assert.assertEquals("", StringUtils.newString(new byte[0], "UTF-8"));
    }

    // ---------------------------------------------------------------------
    // 5. Caracteres no ASCII: codificación, decodificación e ida y vuelta
    // ---------------------------------------------------------------------

    @Test
    public void testGetBytesUtf8NonAsciiCharacters() {
        Assert.assertArrayEquals(
                new byte[] {(byte) 0xC3, (byte) 0xB1},
                StringUtils.getBytesUtf8(N_TILDE));
        Assert.assertArrayEquals(
                new byte[] {(byte) 0xE2, (byte) 0x82, (byte) 0xAC},
                StringUtils.getBytesUtf8(EURO));
    }

    @Test
    public void testGetBytesIso8859_1NonAsciiCharacter() {
        Assert.assertArrayEquals(new byte[] {(byte) 0xF1}, StringUtils.getBytesIso8859_1(N_TILDE));
    }

    @Test
    public void testGetBytesUtf16BeNonAsciiCharacter() {
        Assert.assertArrayEquals(
                new byte[] {(byte) 0x20, (byte) 0xAC},
                StringUtils.getBytesUtf16Be(EURO));
    }

    @Test
    public void testGetBytesUtf16LeNonAsciiCharacter() {
        Assert.assertArrayEquals(
                new byte[] {(byte) 0xAC, (byte) 0x20},
                StringUtils.getBytesUtf16Le(EURO));
    }

    @Test
    public void testGetBytesUtf16BeSurrogatePair() {
        Assert.assertArrayEquals(
                new byte[] {(byte) 0xD8, (byte) 0x3D, (byte) 0xDE, (byte) 0x00},
                StringUtils.getBytesUtf16Be(GRINNING));
    }

    @Test
    public void testGetBytesUtf16LeSurrogatePair() {
        Assert.assertArrayEquals(
                new byte[] {(byte) 0x3D, (byte) 0xD8, (byte) 0x00, (byte) 0xDE},
                StringUtils.getBytesUtf16Le(GRINNING));
    }

    @Test
    public void testNewStringUtf8DecodesNonAsciiCharacters() {
        final byte[] bytes = {(byte) 0xC3, (byte) 0xB1, (byte) 0xE2, (byte) 0x82, (byte) 0xAC};
        Assert.assertEquals(N_TILDE + EURO, StringUtils.newStringUtf8(bytes));
    }

    @Test
    public void testNewStringIso8859_1DecodesNonAsciiCharacter() {
        Assert.assertEquals(N_TILDE, StringUtils.newStringIso8859_1(new byte[] {(byte) 0xF1}));
    }

    @Test
    public void testNewStringUtf16BeDecodesSurrogatePair() {
        final byte[] bytes = {(byte) 0xD8, (byte) 0x3D, (byte) 0xDE, (byte) 0x00};
        Assert.assertEquals(GRINNING, StringUtils.newStringUtf16Be(bytes));
    }

    @Test
    public void testNewStringUtf16LeDecodesSurrogatePair() {
        final byte[] bytes = {(byte) 0x3D, (byte) 0xD8, (byte) 0x00, (byte) 0xDE};
        Assert.assertEquals(GRINNING, StringUtils.newStringUtf16Le(bytes));
    }

    @Test
    public void testUtf8RoundTripNonAscii() {
        final String text = "a" + N_TILDE + EURO + GRINNING;
        Assert.assertEquals(text, StringUtils.newStringUtf8(StringUtils.getBytesUtf8(text)));
    }

    @Test
    public void testUtf16BeRoundTripNonAscii() {
        final String text = N_TILDE + EURO + GRINNING;
        Assert.assertEquals(text, StringUtils.newStringUtf16Be(StringUtils.getBytesUtf16Be(text)));
    }

    @Test
    public void testUtf16LeRoundTripNonAscii() {
        final String text = N_TILDE + EURO + GRINNING;
        Assert.assertEquals(text, StringUtils.newStringUtf16Le(StringUtils.getBytesUtf16Le(text)));
    }

    // ---------------------------------------------------------------------
    // 6. Caracteres no representables y bytes UTF-8 inválidos
    // ---------------------------------------------------------------------

    @Test
    public void testGetBytesUsAsciiReplacesUnmappableWithQuestionMark() {
        Assert.assertArrayEquals(new byte[] {'?'}, StringUtils.getBytesUsAscii(N_TILDE));
        Assert.assertArrayEquals(new byte[] {'a', '?', 'b'}, StringUtils.getBytesUsAscii("a" + EURO + "b"));
    }

    @Test
    public void testGetBytesIso8859_1ReplacesUnmappableWithQuestionMark() {
        Assert.assertArrayEquals(new byte[] {'?'}, StringUtils.getBytesIso8859_1(EURO));
    }

    @Test
    public void testNewStringUtf8InvalidBytesReplacedWithReplacementCharacter() {
        Assert.assertEquals("\uFFFD", StringUtils.newStringUtf8(new byte[] {(byte) 0xFF}));
    }

    @Test
    public void testNewStringUsAsciiInvalidBytesReplacedWithReplacementCharacter() {
        Assert.assertEquals("\uFFFD", StringUtils.newStringUsAscii(new byte[] {(byte) 0xC3}));
    }

    // ---------------------------------------------------------------------
    // 7. equals con CharSequence que no son String
    // ---------------------------------------------------------------------

    @Test
    public void testEqualsStringBuilderWithString() {
        Assert.assertTrue(StringUtils.equals(new StringBuilder("abc"), "abc"));
        Assert.assertTrue(StringUtils.equals("abc", new StringBuilder("abc")));
    }

    @Test
    public void testEqualsStringBuilderWithStringBuilder() {
        Assert.assertTrue(StringUtils.equals(new StringBuilder("abc"), new StringBuilder("abc")));
        Assert.assertFalse(StringUtils.equals(new StringBuilder("abc"), new StringBuilder("abd")));
    }

    @Test
    public void testEqualsStringBuilderDifferentLengthsAreNotEqual() {
        Assert.assertFalse(StringUtils.equals(new StringBuilder("abc"), new StringBuilder("ab")));
        Assert.assertFalse(StringUtils.equals(new StringBuilder("ab"), new StringBuilder("abc")));
        Assert.assertFalse(StringUtils.equals(new StringBuilder("abc"), "ab"));
        Assert.assertFalse(StringUtils.equals("ab", new StringBuilder("abc")));
    }

    @Test
    public void testEqualsStringBuilderIsCaseSensitive() {
        Assert.assertFalse(StringUtils.equals(new StringBuilder("abc"), "ABC"));
        Assert.assertFalse(StringUtils.equals("ABC", new StringBuilder("abc")));
    }

    @Test
    public void testEqualsEmptyStringBuilderWithEmptyString() {
        Assert.assertTrue(StringUtils.equals(new StringBuilder(), ""));
        Assert.assertTrue(StringUtils.equals("", new StringBuilder()));
    }

    // ---------------------------------------------------------------------
    // 8. equals: identidad y null frente a CharSequence que no es String
    // ---------------------------------------------------------------------

    @Test
    public void testEqualsSameNonNullStringBuilderReference() {
        final StringBuilder sb = new StringBuilder("abc");
        Assert.assertTrue(StringUtils.equals(sb, sb));
    }

    @Test
    public void testEqualsSameNonNullStringBuilderReferenceEmpty() {
        final StringBuilder sb = new StringBuilder();
        Assert.assertTrue(StringUtils.equals(sb, sb));
    }

    @Test
    public void testEqualsNullWithStringBuilderIsFalse() {
        Assert.assertFalse(StringUtils.equals(null, new StringBuilder("abc")));
        Assert.assertFalse(StringUtils.equals(new StringBuilder("abc"), null));
    }

    @Test
    public void testEqualsNullWithEmptyStringBuilderIsFalse() {
        Assert.assertFalse(StringUtils.equals(null, new StringBuilder()));
        Assert.assertFalse(StringUtils.equals(new StringBuilder(), null));
    }

    // ---------------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------------

    /**
     * Copia los bytes restantes de un buffer sin modificar su posición.
     */
    private static byte[] toBytes(final ByteBuffer buffer) {
        final ByteBuffer copy = buffer.duplicate();
        final byte[] result = new byte[copy.remaining()];
        copy.get(result);
        return result;
    }
}
