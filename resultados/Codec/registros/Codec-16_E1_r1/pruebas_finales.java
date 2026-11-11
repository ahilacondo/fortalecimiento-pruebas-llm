package org.apache.commons.codec.binary;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.junit.Test;

public class Base32LLMTest {

    private static byte[] bytes(final String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static String text(final byte[] data) {
        return new String(data, StandardCharsets.UTF_8);
    }

    private static byte octet(final char c) {
        return (byte) c;
    }

    // ------------------------------------------------------------------
    // Codificación estándar (base32)
    // ------------------------------------------------------------------

    @Test
    public void testEncodeEmptyArrayReturnsEmpty() {
        final Base32 codec = new Base32();
        assertEquals(0, codec.encode(new byte[0]).length);
        assertEquals("", codec.encodeAsString(new byte[0]));
    }

    @Test
    public void testEncodeNullReturnsNull() {
        final Base32 codec = new Base32();
        assertNull(codec.encode((byte[]) null));
    }

    @Test
    public void testEncodeSingleZeroByte() {
        assertEquals("AA======", new Base32().encodeAsString(new byte[] {0x00}));
    }

    @Test
    public void testEncodeSingleFfByte() {
        assertEquals("74======", new Base32().encodeAsString(new byte[] {(byte) 0xFF}));
    }

    @Test
    public void testEncodeFiveFfBytesProducesEightSevens() {
        final byte[] data = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        assertEquals("77777777", new Base32().encodeAsString(data));
    }

    @Test
    public void testEncodeHelloWithoutPadding() {
        // "Hello" = 5 bytes exactly, so no padding is expected.
        assertEquals("JBSWY3DP", new Base32().encodeAsString(bytes("Hello")));
    }

    @Test
    public void testEncodeOutputIsMultipleOfEightWithoutChunking() {
        final Base32 codec = new Base32();
        final Random random = new Random(1234L);
        for (int len = 0; len <= 40; len++) {
            final byte[] data = new byte[len];
            random.nextBytes(data);
            final byte[] encoded = codec.encode(data);
            assertEquals("length for input size " + len, 0, encoded.length % 8);
            for (final byte b : encoded) {
                assertTrue("unexpected byte in output", codec.isInAlphabet(b) || b == octet('='));
            }
        }
    }

    @Test
    public void testEncodeIsDeterministicAcrossInstances() {
        final byte[] data = bytes("deterministic input");
        assertEquals(new Base32().encodeAsString(data), new Base32().encodeAsString(data));
    }

    @Test
    public void testEncodeWithCustomPaddingByte() {
        final Base32 codec = new Base32((byte) '*');
        assertEquals("MY******", codec.encodeAsString(bytes("f")));
        assertEquals("MZXW6YTBOI******", codec.encodeAsString(bytes("foobar")));
    }

    // ------------------------------------------------------------------
    // Codificación Base32 Hex
    // ------------------------------------------------------------------

    @Test
    public void testHexEncodeFoobar() {
        assertEquals("CPNMUOJ1E8======", new Base32(true).encodeAsString(bytes("foobar")));
    }

    @Test
    public void testHexEncodeSingleZeroByte() {
        assertEquals("00======", new Base32(true).encodeAsString(new byte[] {0x00}));
    }

    @Test
    public void testHexDecodeFoobar() {
        assertEquals("foobar", text(new Base32(true).decode(bytes("CPNMUOJ1E8======"))));
    }

    @Test
    public void testHexIsInAlphabetRange() {
        final Base32 codec = new Base32(true);
        assertTrue(codec.isInAlphabet(octet('0')));
        assertTrue(codec.isInAlphabet(octet('9')));
        assertTrue(codec.isInAlphabet(octet('A')));
        assertTrue(codec.isInAlphabet(octet('V')));
        assertFalse(codec.isInAlphabet(octet('W')));
        assertFalse(codec.isInAlphabet(octet('Z')));
    }

    // ------------------------------------------------------------------
    // Decodificación
    // ------------------------------------------------------------------

    @Test
    public void testDecodeEmptyArrayReturnsEmpty() {
        assertEquals(0, new Base32().decode(new byte[0]).length);
    }

    @Test
    public void testDecodeNullReturnsNull() {
        assertNull(new Base32().decode((byte[]) null));
    }

    @Test
    public void testDecodeHello() {
        assertEquals("Hello", text(new Base32().decode(bytes("JBSWY3DP"))));
    }

    @Test
    public void testDecodeWithPaddingRfcVector() {
        assertEquals("foobar", text(new Base32().decode(bytes("MZXW6YTBOI======"))));
        assertEquals("f", text(new Base32().decode(bytes("MY======"))));
        assertEquals("fo", text(new Base32().decode(bytes("MZXQ===="))));
    }

    @Test
    public void testDecodeWithoutPaddingRfcVector() {
        assertEquals("fo", text(new Base32().decode(bytes("MZXQ"))));
        assertEquals("foobar", text(new Base32().decode(bytes("MZXW6YTBOI"))));
    }

    @Test
    public void testDecodeIgnoresWhitespaceAndCrLf() {
        assertEquals("foobar", text(new Base32().decode(bytes("MZXW\r\n6YTB OI======"))));
    }

    @Test
    public void testDecodeIgnoresNonAlphabetCharacters() {
        // '!' and '1' are not in the Base32 alphabet and must be skipped.
        assertEquals("foobar", text(new Base32().decode(bytes("MZ!XW6Y1TBOI======"))));
    }

    @Test
    public void testDecodeChunkedDataWithSeparators() {
        assertEquals("foobar", text(new Base32().decode(bytes("MZXW6YTB\r\nOI======\r\n"))));
    }

    @Test
    public void testDecodeWithCustomPaddingByte() {
        assertEquals("foobar", text(new Base32((byte) '*').decode(bytes("MZXW6YTBOI******"))));
    }

    // ------------------------------------------------------------------
    // Codificación con fragmentación (chunking)
    // ------------------------------------------------------------------

    @Test
    public void testChunkedEncodeAddsCrLfAfterFullLine() {
        // lineLength 8 -> one block of 8 chars followed by CRLF.
        assertEquals("MZXW6YTB\r\n", new Base32(8).encodeAsString(bytes("fooba")));
    }

    @Test
    public void testChunkedEncodeMultipleLines() {
        // "foobar": first 5 bytes form a full line, "r" is flushed at EOF with its own line.
        assertEquals("MZXW6YTB\r\nOI======\r\n", new Base32(8).encodeAsString(bytes("foobar")));
    }

    @Test
    public void testChunkedEncodeWithLineLength20() {
        assertEquals("MZXW6YTBOI======\r\n", new Base32(20).encodeAsString(bytes("foobar")));
    }

    @Test
    public void testChunkedEncodeOutputNeverStartsWithSeparatorForEmptyInput() {
        assertEquals("", new Base32(8).encodeAsString(new byte[0]));
    }

    @Test
    public void testGetEncodedLengthNonChunked() {
        final Base32 codec = new Base32();
        assertEquals(0, codec.getEncodedLength(new byte[0]));
        assertEquals(8, codec.getEncodedLength(new byte[1]));
        assertEquals(8, codec.getEncodedLength(new byte[5]));
        assertEquals(16, codec.getEncodedLength(new byte[6]));
    }

    @Test
    public void testGetEncodedLengthChunkedIncludesSeparator() {
        final Base32 codec = new Base32(8);
        // 5 bytes -> 8 chars + CRLF = 10
        assertEquals(10, codec.getEncodedLength(new byte[5]));
        assertEquals(10, codec.encode(new byte[5]).length);
    }

    // ------------------------------------------------------------------
    // Ida y vuelta (round trip)
    // ------------------------------------------------------------------

    @Test
    public void testRoundTripRandomDataStandard() {
        final Base32 codec = new Base32();
        final Random random = new Random(42L);
        for (int len = 0; len <= 64; len++) {
            final byte[] data = new byte[len];
            random.nextBytes(data);
            assertArrayEquals("size " + len, data, codec.decode(codec.encode(data)));
        }
    }

    @Test
    public void testRoundTripRandomDataHex() {
        final Base32 codec = new Base32(true);
        final Random random = new Random(7L);
        for (int len = 0; len <= 64; len++) {
            final byte[] data = new byte[len];
            random.nextBytes(data);
            assertArrayEquals("size " + len, data, codec.decode(codec.encode(data)));
        }
    }

    @Test
    public void testRoundTripRandomDataChunked() {
        final Base32 codec = new Base32(16);
        final Random random = new Random(99L);
        for (int len = 0; len <= 64; len++) {
            final byte[] data = new byte[len];
            random.nextBytes(data);
            assertArrayEquals("size " + len, data, new Base32().decode(codec.encode(data)));
        }
    }

    @Test
    public void testRoundTripAllByteValues() {
        final Base32 codec = new Base32();
        final byte[] data = new byte[256];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        assertArrayEquals(data, codec.decode(codec.encode(data)));
    }

    // ------------------------------------------------------------------
    // isInAlphabet
    // ------------------------------------------------------------------

    @Test
    public void testIsInAlphabetStandard() {
        final Base32 codec = new Base32();
        assertTrue(codec.isInAlphabet(octet('A')));
        assertTrue(codec.isInAlphabet(octet('Z')));
        assertTrue(codec.isInAlphabet(octet('2')));
        assertTrue(codec.isInAlphabet(octet('7')));
        assertFalse(codec.isInAlphabet(octet('0')));
        assertFalse(codec.isInAlphabet(octet('1')));
        assertFalse(codec.isInAlphabet(octet('8')));
        assertFalse(codec.isInAlphabet(octet('a')));
        assertFalse(codec.isInAlphabet(octet('=')));
        assertFalse(codec.isInAlphabet(octet(' ')));
        assertFalse(codec.isInAlphabet((byte) -1));
    }

    // ------------------------------------------------------------------
    // Constructores y validaciones
    // ------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsAlphabetCharacterAsPad() {
        new Base32(octet('A'));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsWhitespacePad() {
        new Base32(octet(' '));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsSeparatorWithAlphabetCharacters() {
        new Base32(10, bytes("AB"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNullSeparatorWhenChunking() {
        new Base32(10, (byte[]) null);
    }

    @Test
    public void testConstructorAcceptsNullSeparatorWithoutChunking() {
        final Base32 codec = new Base32(0, null);
        assertEquals("MZXW6YTBOI======", codec.encodeAsString(bytes("foobar")));
    }
}
