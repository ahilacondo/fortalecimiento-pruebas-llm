package org.apache.commons.codec.binary;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.junit.Test;

/**
 * Pruebas adicionales para {@link Base64} que usan únicamente su API pública.
 */
public class Base64LLMTest {

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    private static String str(byte[] b) {
        return new String(b, StandardCharsets.US_ASCII);
    }

    /** Equivalente a String.repeat (compatible con Java 8). */
    private static String rep(String s, int n) {
        StringBuilder sb = new StringBuilder(s.length() * n);
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    /** Bytes 0xFB 0xFF: en base64 estándar "+/8=", en URL-safe "-_8". */
    private static byte[] specialBytes() {
        return new byte[] {(byte) 0xfb, (byte) 0xff};
    }

    // ---------------------------------------------------------------
    // Vectores RFC 4648 (codificación y decodificación)
    // ---------------------------------------------------------------

    @Test
    public void testEncodeBase64StringMatchesRfc4648Vectors() {
        assertEquals("", Base64.encodeBase64String(ascii("")));
        assertEquals("Zg==", Base64.encodeBase64String(ascii("f")));
        assertEquals("Zm8=", Base64.encodeBase64String(ascii("fo")));
        assertEquals("Zm9v", Base64.encodeBase64String(ascii("foo")));
        assertEquals("Zm9vYg==", Base64.encodeBase64String(ascii("foob")));
        assertEquals("Zm9vYmE=", Base64.encodeBase64String(ascii("fooba")));
        assertEquals("Zm9vYmFy", Base64.encodeBase64String(ascii("foobar")));
    }

    @Test
    public void testDecodeBase64StringMatchesRfc4648Vectors() {
        assertEquals("", str(Base64.decodeBase64("")));
        assertEquals("f", str(Base64.decodeBase64("Zg==")));
        assertEquals("fo", str(Base64.decodeBase64("Zm8=")));
        assertEquals("foo", str(Base64.decodeBase64("Zm9v")));
        assertEquals("foob", str(Base64.decodeBase64("Zm9vYg==")));
        assertEquals("fooba", str(Base64.decodeBase64("Zm9vYmE=")));
        assertEquals("foobar", str(Base64.decodeBase64("Zm9vYmFy")));
    }

    // ---------------------------------------------------------------
    // Nulos y vacíos
    // ---------------------------------------------------------------

    @Test
    public void testEncodeNullReturnsNullAndEmptyReturnsEmpty() {
        assertNull(Base64.encodeBase64((byte[]) null));
        assertNull(Base64.encodeBase64String(null));
        assertEquals(0, Base64.encodeBase64(new byte[0]).length);
        assertEquals("", Base64.encodeBase64String(new byte[0]));
    }

    @Test
    public void testDecodeNullReturnsNullAndEmptyReturnsEmpty() {
        assertNull(Base64.decodeBase64((byte[]) null));
        assertEquals(0, Base64.decodeBase64(new byte[0]).length);
        assertEquals(0, Base64.decodeBase64("").length);
    }

    // ---------------------------------------------------------------
    // URL-safe y alfabeto estándar
    // ---------------------------------------------------------------

    @Test
    public void testStandardEncodingUsesPlusSlashAndPadding() {
        assertEquals("+/8=", Base64.encodeBase64String(specialBytes()));
        assertEquals("+/8=", new Base64().encodeToString(specialBytes()).replace("\r\n", ""));
    }

    @Test
    public void testUrlSafeStringUsesDashUnderscoreAndNoPadding() {
        assertEquals("-_8", Base64.encodeBase64URLSafeString(specialBytes()));
    }

    @Test
    public void testUrlSafeBytesUsesDashUnderscoreAndNoPadding() {
        assertEquals("-_8", str(Base64.encodeBase64URLSafe(specialBytes())));
    }

    @Test
    public void testInstanceUrlSafeWithoutChunkingEncodesCorrectly() {
        Base64 b64 = new Base64(0, null, true);
        assertTrue("isUrlSafe debe ser true", b64.isUrlSafe());
        assertEquals("-_8", b64.encodeToString(specialBytes()));
    }

    @Test
    public void testInstanceUrlSafeWithChunkingAddsTrailingSeparator() {
        // Con longitud de línea > 0 el codificador añade el separador al final.
        Base64 b64 = new Base64(true);
        assertEquals("-_8\r\n", b64.encodeToString(specialBytes()));
    }

    @Test
    public void testIsUrlSafeFlagReflectsConstructor() {
        assertFalse("Base64() no es URL-safe", new Base64().isUrlSafe());
        assertFalse("Base64(false) no es URL-safe", new Base64(false).isUrlSafe());
        assertTrue("Base64(true) es URL-safe", new Base64(true).isUrlSafe());
    }

    @Test
    public void testDecodeAcceptsBothAlphabets() {
        assertArrayEquals(specialBytes(), Base64.decodeBase64("-_8"));
        assertArrayEquals(specialBytes(), Base64.decodeBase64("+/8="));
        assertArrayEquals(specialBytes(), Base64.decodeBase64("-/8="));
        assertArrayEquals(specialBytes(), Base64.decodeBase64("+_8"));
    }

    // ---------------------------------------------------------------
    // Fraccionamiento (chunking)
    // ---------------------------------------------------------------

    @Test
    public void testChunkedExactly57BytesProducesSingleLineOf76Characters() {
        byte[] input = new byte[57]; // todos ceros -> 'A'
        String result = str(Base64.encodeBase64Chunked(input));
        assertEquals("Longitud total esperada 76 + CRLF", 78, result.length());
        assertEquals("Primeros 76 caracteres deben ser 'A'", rep("A", 76), result.substring(0, 76));
        assertTrue("Debe terminar en CRLF", result.endsWith("\r\n"));
    }

    @Test
    public void testChunked58BytesProducesSecondLineWithPadding() {
        byte[] input = new byte[58];
        String result = str(Base64.encodeBase64Chunked(input));
        assertEquals("76 'A' + CRLF + 'AA==' + CRLF", rep("A", 76) + "\r\nAA==\r\n", result);
    }

    @Test
    public void testChunkedLinesNeverExceed76Characters() {
        byte[] input = new byte[300];
        for (int i = 0; i < input.length; i++) {
            input[i] = (byte) (i * 7);
        }
        String encoded = str(Base64.encodeBase64Chunked(input));
        String[] lines = encoded.split("\r\n", -1);
        assertTrue("Debe haber más de una línea", lines.length > 2);
        for (String line : lines) {
            assertTrue("Línea demasiado larga: " + line.length(), line.length() <= 76);
        }
        assertArrayEquals("Round-trip de datos fraccionados", input, Base64.decodeBase64(encoded));
    }

    @Test
    public void testNonChunkedEncodingHasNoLineBreaks() {
        byte[] input = new byte[200];
        String encoded = Base64.encodeBase64String(input);
        assertEquals("No debe contener saltos de línea", -1, encoded.indexOf('\n'));
        assertEquals("No debe contener CR", -1, encoded.indexOf('\r'));
    }

    @Test
    public void testInstanceLineLengthIsRoundedDownToMultipleOfFour() {
        // 7 se redondea a 4: cada línea tiene 4 caracteres.
        Base64 b64 = new Base64(7);
        assertEquals("AAAA\r\nAAAA\r\n", b64.encodeToString(new byte[6]));
    }

    @Test
    public void testCustomSeparatorIsUsedBetweenLines() {
        Base64 b64 = new Base64(4, new byte[] {'$'});
        assertEquals("AAAA$AAAA$", b64.encodeToString(new byte[6]));
        assertArrayEquals(new byte[6], Base64.decodeBase64("AAAA$AAAA$"));
    }

    @Test
    public void testNullSeparatorDisablesChunking() {
        Base64 b64 = new Base64(4, null);
        assertEquals("AAAAAAAA", b64.encodeToString(new byte[6]));
    }

    // ---------------------------------------------------------------
    // Validación de alfabeto
    // ---------------------------------------------------------------

    @Test
    public void testIsBase64ByteClassification() {
        assertTrue(Base64.isBase64((byte) 'A'));
        assertTrue(Base64.isBase64((byte) 'z'));
        assertTrue(Base64.isBase64((byte) '0'));
        assertTrue(Base64.isBase64((byte) '+'));
        assertTrue(Base64.isBase64((byte) '/'));
        assertTrue(Base64.isBase64((byte) '='));
        assertFalse(Base64.isBase64((byte) '-'));
        assertFalse(Base64.isBase64((byte) '_'));
        assertFalse(Base64.isBase64((byte) '*'));
        assertFalse(Base64.isBase64((byte) ' '));
        assertFalse(Base64.isBase64((byte) 0x80));
    }

    @Test
    public void testIsBase64StringTreatsWhitespaceAsValid() {
        assertTrue(Base64.isBase64("SGVs bG8=\r\n"));
        assertTrue(Base64.isBase64(""));
        assertFalse(Base64.isBase64("SGVs*bG8="));
    }

    @Test
    public void testIsBase64ByteArrayRejectsNonAsciiAndAcceptsWhitespace() {
        assertTrue(Base64.isBase64(new byte[] {'A', '\r', '\n', '\t', ' '}));
        assertTrue(Base64.isBase64(new byte[0]));
        assertFalse(Base64.isBase64(new byte[] {'A', (byte) 0xC3}));
    }

    @Test
    public void testEncodedOutputIsValidBase64() {
        byte[] encoded = Base64.encodeBase64(new byte[] {1, 2, 3, 4, 5, (byte) 0xff});
        assertTrue("La salida codificada debe ser Base64 válido", Base64.isBase64(encoded));
    }

    // ---------------------------------------------------------------
    // Constructores
    // ---------------------------------------------------------------

    @Test
    public void testConstructorRejectsSeparatorContainingBase64Character() {
        try {
            new Base64(64, new byte[] {'+'});
            fail("Debía rechazar '+' como separador de línea");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    public void testConstructorsAcceptValidArguments() {
        assertNotNull(new Base64());
        assertNotNull(new Base64(-5));
        assertNotNull(new Base64(64, new byte[] {' ', '\t', '\r', '\n'}));
        assertNotNull(new Base64(64, new byte[] {'$'}, true));
    }

    // ---------------------------------------------------------------
    // Decodificación tolerante
    // ---------------------------------------------------------------

    @Test
    public void testDecodeIgnoresCrLfAndInvalidCharacters() {
        assertEquals("foobar", str(Base64.decodeBase64("Zm9v\r\nYmFy")));
        assertEquals("foobar", str(Base64.decodeBase64("Zm9v!!YmFy")));
    }

    @Test
    public void testDecodeWithoutPaddingIsAccepted() {
        assertEquals("foob", str(Base64.decodeBase64("Zm9vYg")));
        assertEquals("fooba", str(Base64.decodeBase64("Zm9vYmE")));
    }

    @Test
    public void testDecodeStopsAtFirstPaddingCharacter() {
        assertEquals("foob", str(Base64.decodeBase64("Zm9vYg==Zm9v")));
    }

    @Test
    public void testInstanceDecodeStringMatchesStaticDecode() {
        assertEquals("foobar", str(new Base64().decode("Zm9vYmFy")));
    }

    // ---------------------------------------------------------------
    // Contrato de Encoder / Decoder (Object)
    // ---------------------------------------------------------------

    @Test
    public void testEncodeObjectWithByteArrayReturnsBase64Bytes() throws Exception {
        Base64 b64 = new Base64();
        Object result = b64.encode((Object) ascii("foobar"));
        assertEquals("Zm9vYmFy", str((byte[]) result));
    }

    @Test(expected = EncoderException.class)
    public void testEncodeObjectWithStringThrowsEncoderException() throws Exception {
        new Base64().encode((Object) "foobar");
    }

    @Test
    public void testDecodeObjectWithStringAndByteArray() throws Exception {
        Base64 b64 = new Base64();
        assertEquals("foobar", str((byte[]) b64.decode((Object) "Zm9vYmFy")));
        assertEquals("foo", str((byte[]) b64.decode((Object) ascii("Zm9v"))));
    }

    @Test(expected = DecoderException.class)
    public void testDecodeObjectWithIntegerThrowsDecoderException() throws Exception {
        new Base64().decode((Object) Integer.valueOf(5));
    }

    // ---------------------------------------------------------------
    // Reutilización de instancias
    // ---------------------------------------------------------------

    @Test
    public void testInstanceEncodeIsReusableAcrossCalls() {
        Base64 b64 = new Base64();
        byte[] first = b64.encode(ascii("foo"));
        b64.encode(ascii("completely different input"));
        byte[] second = b64.encode(ascii("foo"));
        assertArrayEquals("Zm9v", ascii("Zm9v"), first);
        assertArrayEquals(first, second);
    }

    @Test
    public void testInstanceDecodeIsReusableAcrossCalls() {
        Base64 b64 = new Base64();
        byte[] first = b64.decode("Zm9vYmFy");
        b64.decode("SGVsbG8=");
        byte[] second = b64.decode("Zm9vYmFy");
        assertEquals("foobar", str(first));
        assertArrayEquals(first, second);
    }

    // ---------------------------------------------------------------
    // Round-trips
    // ---------------------------------------------------------------

    @Test
    public void testAllByteValuesRoundTripInBothModes() {
        byte[] all = new byte[256];
        for (int i = 0; i < 256; i++) {
            all[i] = (byte) i;
        }
        assertArrayEquals(all, Base64.decodeBase64(Base64.encodeBase64String(all)));
        assertArrayEquals(all, Base64.decodeBase64(Base64.encodeBase64URLSafeString(all)));
    }

    @Test
    public void testUrlSafeOutputContainsNoStandardOnlyCharacters() {
        byte[] data = new byte[500];
        new Random(42).nextBytes(data);
        String encoded = Base64.encodeBase64URLSafeString(data);
        assertEquals(-1, encoded.indexOf('+'));
        assertEquals(-1, encoded.indexOf('/'));
        assertEquals(-1, encoded.indexOf('='));
        assertArrayEquals(data, Base64.decodeBase64(encoded));
    }

    @Test
    public void testRandomRoundTripForLengths0To50() {
        Random random = new Random(1234L);
        for (int len = 0; len <= 50; len++) {
            byte[] data = new byte[len];
            random.nextBytes(data);
            byte[] encoded = Base64.encodeBase64(data);
            assertArrayEquals("longitud " + len, data, Base64.decodeBase64(encoded));
        }
    }

    // ---------------------------------------------------------------
    // Límite de tamaño máximo
    // ---------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeWithZeroMaxResultSizeThrows() {
        Base64.encodeBase64(new byte[3], false, false, 0);
    }

    @Test
    public void testEncodeWithDefaultMaxResultSizeSucceeds() {
        assertEquals("AAAA", str(Base64.encodeBase64(new byte[3], false, false, Integer.MAX_VALUE)));
    }

    // ---------------------------------------------------------------
    // Enteros BigInteger (uso criptográfico)
    // ---------------------------------------------------------------

    @Test
    public void testEncodeIntegerKnownValue255() {
        assertEquals("/w==", str(Base64.encodeInteger(BigInteger.valueOf(255))));
    }

    @Test
    public void testEncodeIntegerKnownValue1() {
        assertEquals("AQ==", str(Base64.encodeInteger(BigInteger.valueOf(1))));
    }

    @Test
    public void testDecodeIntegerKnownValue() {
        assertEquals(BigInteger.valueOf(255), Base64.decodeInteger(ascii("/w==")));
    }

    @Test
    public void testIntegerRoundTripWithHighBitSet() {
        byte[] magnitude = new byte[32];
        magnitude[0] = (byte) 0x80;
        BigInteger value = new BigInteger(1, magnitude);
        assertEquals(value, Base64.decodeInteger(Base64.encodeInteger(value)));
    }

    @Test(expected = NullPointerException.class)
    public void testEncodeIntegerNullThrowsNullPointerException() {
        Base64.encodeInteger(null);
    }

    // ---------------------------------------------------------------
    // Utilidades de clasificación estática
    // ---------------------------------------------------------------

    @Test
    public void testChunkSizeConstantsAreExposed() {
        assertEquals(76, Base64.MIME_CHUNK_SIZE);
        assertEquals(64, Base64.PEM_CHUNK_SIZE);
    }
}
