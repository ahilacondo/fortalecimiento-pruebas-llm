package org.apache.commons.codec.binary;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.junit.Test;

/**
 * Pruebas adicionales para {@link Base64} que usan únicamente la API pública de la clase.
 */
public class Base64LLMTest {

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    private static String str(byte[] b) {
        return new String(b, StandardCharsets.US_ASCII);
    }

    private static byte[] zeros(int n) {
        return new byte[n];
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static byte[] sampleData(int n) {
        byte[] d = new byte[n];
        for (int i = 0; i < n; i++) {
            d[i] = (byte) (i * 31 + 7);
        }
        return d;
    }

    // ---------------------------------------------------------------- codificación estándar

    @Test
    public void testEncodeKnownValueStandard() {
        byte[] encoded = Base64.encodeBase64(ascii("Hello World"));
        assertEquals("SGVsbG8gV29ybGQ=", str(encoded));
    }

    @Test
    public void testEncodeNullReturnsNull() {
        assertNull(Base64.encodeBase64((byte[]) null));
    }

    @Test
    public void testEncodeEmptyReturnsEmpty() {
        byte[] encoded = Base64.encodeBase64(new byte[0]);
        assertEquals(0, encoded.length);
    }

    @Test
    public void testEncodeBase64StringAppendsCrlf() {
        assertEquals("SGVsbG8gV29ybGQ=\r\n", Base64.encodeBase64String(ascii("Hello World")));
    }

    @Test
    public void testEncodeStandardUsesPlusAndSlash() {
        byte[] input = {(byte) 0xFB, (byte) 0xFF};
        assertEquals("+/8=", str(Base64.encodeBase64(input, false)));
    }

    @Test
    public void testEncodeToStringWithInstance() {
        Base64 b64 = new Base64();
        assertEquals("SGVsbG8=", b64.encodeToString(ascii("Hello")));
    }

    @Test
    public void testEncodeInstanceWithZeroLineLengthDoesNotChunk() {
        Base64 b64 = new Base64(0);
        assertEquals("SGVsbG8gV29ybGQ=", str(b64.encode(ascii("Hello World"))));
    }

    // ---------------------------------------------------------------- chunking

    @Test
    public void testEncodeChunkedSplitsAt76Characters() {
        // 60 bytes a cero -> 80 caracteres base64 -> 76 + CRLF + 4 + CRLF
        String expected = repeat('A', 76) + "\r\n" + "AAAA\r\n";
        assertEquals(expected, str(Base64.encodeBase64Chunked(zeros(60))));
    }

    @Test
    public void testEncodeChunkedSingleByteEndsWithCrlf() {
        byte[] encoded = Base64.encodeBase64Chunked(zeros(1));
        assertEquals("AA==\r\n", str(encoded));
        assertEquals(13, encoded[encoded.length - 2]);
        assertEquals(10, encoded[encoded.length - 1]);
    }

    @Test
    public void testCustomLineLengthAndSeparator() {
        // 9 bytes a cero -> 12 caracteres; línea de 8 con separador '\n'
        Base64 b64 = new Base64(8, new byte[]{'\n'});
        assertEquals("AAAAAAAA\nAAAA\n", str(b64.encode(zeros(9))));
    }

    @Test
    public void testNullSeparatorDisablesChunking() {
        Base64 b64 = new Base64(8, null);
        assertEquals("AAAAAAAAAAAA", str(b64.encode(zeros(9))));
    }

    @Test
    public void testConstructorRejectsSeparatorWithBase64Characters() {
        try {
            new Base64(64, new byte[]{'A'});
            fail("Se esperaba IllegalArgumentException para 'A'");
        } catch (IllegalArgumentException expected) {
            // esperado
        }
        try {
            new Base64(64, new byte[]{'$', '='});
            fail("Se esperaba IllegalArgumentException para '='");
        } catch (IllegalArgumentException expected) {
            // esperado
        }
    }

    // ---------------------------------------------------------------- URL-safe

    @Test
    public void testUrlSafeUsesDashAndUnderscoreWithoutPadding() {
        byte[] input = {(byte) 0xFB, (byte) 0xFF};
        assertEquals("-_8", Base64.encodeBase64URLSafeString(input));
        assertEquals("-_8", str(Base64.encodeBase64URLSafe(input)));
    }

    @Test
    public void testUrlSafeOmitsPadding() {
        assertEquals("Zg", Base64.encodeBase64URLSafeString(new byte[]{102}));
        assertEquals("Zg==", Base64.encodeBase64String(new byte[]{102}).replace("\r\n", ""));
    }

    @Test
    public void testUrlSafeFlagViaConstructor() {
        assertTrue(new Base64(true).isUrlSafe());
        assertFalse(new Base64(false).isUrlSafe());
        assertFalse(new Base64().isUrlSafe());
        byte[] input = {(byte) 0xFB, (byte) 0xFF};
        assertEquals("-_8", new Base64(true).encodeToString(input));
        assertEquals("+/8=", new Base64(false).encodeToString(input));
    }

    // ---------------------------------------------------------------- decodificación

    @Test
    public void testDecodeKnownValue() {
        assertEquals("Hello World", str(Base64.decodeBase64("SGVsbG8gV29ybGQ=")));
    }

    @Test
    public void testDecodeWithoutPadding() {
        assertEquals("Hello", str(Base64.decodeBase64("SGVsbG8")));
    }

    @Test
    public void testDecodeIgnoresLineBreaksAndSpaces() {
        assertEquals("Hello World", str(Base64.decodeBase64("SGVs\r\nbG8g\r\nV29y bGQ=")));
    }

    @Test
    public void testDecodeIgnoresNonAlphabetCharacters() {
        assertEquals("ABC", str(Base64.decodeBase64("$$$QUJD")));
    }

    @Test
    public void testDecodeStandardAndUrlSafeAreEquivalent() {
        byte[] expected = {(byte) 0xFB, (byte) 0xFF};
        assertArrayEquals(expected, Base64.decodeBase64("+/8="));
        assertArrayEquals(expected, Base64.decodeBase64("-_8="));
        assertArrayEquals(expected, Base64.decodeBase64("-_8"));
    }

    @Test
    public void testDecodeNullAndEmpty() {
        assertNull(Base64.decodeBase64((byte[]) null));
        assertEquals(0, Base64.decodeBase64(new byte[0]).length);
        assertEquals(0, Base64.decodeBase64("").length);
    }

    @Test
    public void testDecodeInstanceIsReusable() {
        Base64 b64 = new Base64();
        assertEquals("A", str(b64.decode(ascii("QQ=="))));
        assertEquals("B", str(b64.decode(ascii("Qg=="))));
        assertEquals("A", str(b64.decode("QQ==")));
    }

    @Test
    public void testDecodeObjectWithString() throws Exception {
        Object result = new Base64().decode((Object) "SGVsbG8=");
        assertTrue(result instanceof byte[]);
        assertEquals("Hello", str((byte[]) result));
    }

    @Test
    public void testDecodeObjectWithByteArray() throws Exception {
        Object result = new Base64().decode((Object) ascii("SGVsbG8="));
        assertEquals("Hello", str((byte[]) result));
    }

    @Test
    public void testDecodeObjectRejectsUnsupportedType() {
        try {
            new Base64().decode((Object) Integer.valueOf(5));
            fail("Se esperaba DecoderException");
        } catch (DecoderException expected) {
            // esperado
        }
    }

    @Test
    public void testEncodeObjectWithByteArray() throws Exception {
        Object result = new Base64(0).encode((Object) ascii("Hello"));
        assertTrue(result instanceof byte[]);
        assertEquals("SGVsbG8=", str((byte[]) result));
    }

    @Test
    public void testEncodeObjectRejectsString() {
        try {
            new Base64().encode((Object) "Hello");
            fail("Se esperaba EncoderException");
        } catch (EncoderException expected) {
            // esperado
        }
    }

    // ---------------------------------------------------------------- idas y vueltas

    @Test
    public void testRoundTripStandardAndUrlSafeForSmallSizes() {
        for (int n = 0; n <= 40; n++) {
            byte[] data = sampleData(n);

            byte[] standard = Base64.encodeBase64(data);
            assertArrayEquals("estándar n=" + n, data, Base64.decodeBase64(standard));

            byte[] urlSafe = Base64.encodeBase64URLSafe(data);
            assertArrayEquals("url-safe n=" + n, data, Base64.decodeBase64(urlSafe));
            assertFalse("sin '+' n=" + n, str(urlSafe).contains("+"));
            assertFalse("sin '/' n=" + n, str(urlSafe).contains("/"));
            assertFalse("sin '=' n=" + n, str(urlSafe).contains("="));
        }
    }

    @Test
    public void testRoundTripChunked() {
        byte[] data = sampleData(200);
        byte[] chunked = Base64.encodeBase64Chunked(data);
        assertTrue(str(chunked).contains("\r\n"));
        assertArrayEquals(data, Base64.decodeBase64(chunked));
    }

    @Test
    public void testRoundTripAllByteValues() {
        byte[] data = new byte[256];
        for (int i = 0; i < 256; i++) {
            data[i] = (byte) i;
        }
        assertArrayEquals(data, Base64.decodeBase64(Base64.encodeBase64(data)));
        assertArrayEquals(data, Base64.decodeBase64(Base64.encodeBase64URLSafe(data)));
    }

    // ---------------------------------------------------------------- validación de alfabeto

    @Test
    public void testIsBase64AcceptsAlphabetCharacters() {
        assertTrue(Base64.isBase64((byte) 'A'));
        assertTrue(Base64.isBase64((byte) 'Z'));
        assertTrue(Base64.isBase64((byte) 'a'));
        assertTrue(Base64.isBase64((byte) 'z'));
        assertTrue(Base64.isBase64((byte) '0'));
        assertTrue(Base64.isBase64((byte) '9'));
        assertTrue(Base64.isBase64((byte) '+'));
        assertTrue(Base64.isBase64((byte) '/'));
        assertTrue(Base64.isBase64((byte) '='));
    }

    @Test
    public void testIsBase64AcceptsUrlSafeCharacters() {
        assertTrue(Base64.isBase64((byte) '-'));
        assertTrue(Base64.isBase64((byte) '_'));
    }

    @Test
    public void testIsBase64RejectsNonAlphabetCharacters() {
        assertFalse(Base64.isBase64((byte) ' '));
        assertFalse(Base64.isBase64((byte) '\n'));
        assertFalse(Base64.isBase64((byte) '$'));
        assertFalse(Base64.isBase64((byte) '*'));
        assertFalse(Base64.isBase64((byte) 0x80));
        assertFalse(Base64.isBase64((byte) -1));
    }

    @Test
    public void testIsArrayByteBase64EmptyAndWhitespace() {
        assertTrue(Base64.isArrayByteBase64(new byte[0]));
        assertTrue(Base64.isArrayByteBase64(ascii("QUJD\r\n")));
        assertTrue(Base64.isArrayByteBase64(ascii("-_8=")));
        assertFalse(Base64.isArrayByteBase64(ascii("QU$D")));
    }

    @Test
    public void testEncodedOutputContainsOnlyAlphabetCharacters() {
        for (int n = 0; n <= 40; n++) {
            byte[] data = sampleData(n);
            byte[] encoded = Base64.encodeBase64(data);
            for (byte b : encoded) {
                assertTrue("carácter fuera de alfabeto n=" + n, Base64.isBase64(b));
            }
            assertTrue("isArrayByteBase64 n=" + n, Base64.isArrayByteBase64(encoded));
        }
    }
}
