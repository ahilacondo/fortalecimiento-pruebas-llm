package org.apache.commons.codec.binary;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link Base64} que cubren comportamientos no verificados por la suite existente.
 */
public class Base64LLMTest {

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] raw(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }
        return result;
    }

    // ------------------------------------------------------------------
    // 1. La decodificación se detiene en el primer '=' e ignora lo posterior
    // ------------------------------------------------------------------

    @Test
    public void testDecodeStopsAtFirstPadAndIgnoresTrailingData() {
        // "QQ==" -> "A"; lo que sigue al '=' se ignora por completo.
        assertArrayEquals(bytes("A"), Base64.decodeBase64(bytes("QQ==QUFB")));
    }

    @Test
    public void testDecodeStopsAtFirstPadInMiddleOfStream() {
        // "QUFBQQ==" -> "AAAA"; el bloque "QUFB" posterior al '=' no debe producir salida.
        assertArrayEquals(bytes("AAAA"), Base64.decodeBase64(bytes("QUFBQQ==QUFB")));
    }

    @Test
    public void testDecodeStringStopsAtFirstPad() {
        assertArrayEquals(bytes("A"), new Base64().decode("QQ==QUFB"));
    }

    // ------------------------------------------------------------------
    // 2. Símbolos sobrantes sin completar un cuarteto
    // ------------------------------------------------------------------

    @Test
    public void testSingleLeftoverSymbolIsDiscarded() {
        // "QUFB" -> "AAA"; la 'Q' sobrante no produce ningún byte.
        assertArrayEquals(bytes("AAA"), Base64.decodeBase64(bytes("QUFBQ")));
    }

    @Test
    public void testOnlyOneSymbolProducesEmptyResult() {
        assertEquals(0, Base64.decodeBase64(bytes("Q")).length);
    }

    @Test
    public void testTwoLeftoverSymbolsProduceOneByte() {
        // "QQ" (sin padding) -> "A"
        assertArrayEquals(bytes("A"), Base64.decodeBase64(bytes("QQ")));
        // "QUFBQQ" -> "AAA" + "A"
        assertArrayEquals(bytes("AAAA"), Base64.decodeBase64(bytes("QUFBQQ")));
    }

    @Test
    public void testThreeLeftoverSymbolsProduceTwoBytes() {
        // "QUE" -> "AA"; "QUFB" + "QUE" -> "AAAAA"
        assertArrayEquals(bytes("AA"), Base64.decodeBase64(bytes("QUE")));
        assertArrayEquals(bytes("AAAAA"), Base64.decodeBase64(bytes("QUFBQUE")));
    }

    // ------------------------------------------------------------------
    // 3. maxResultSize exactamente igual a la longitud calculada
    // ------------------------------------------------------------------

    @Test
    public void testMaxResultSizeEqualToComputedLengthDoesNotThrow() {
        // Para 3 bytes: 4 caracteres de datos + 2 bytes de separador (longitud calculada = 6).
        byte[] data = bytes("foo");
        byte[] result = Base64.encodeBase64(data, false, false, 6);
        assertArrayEquals(bytes("Zm9v"), result);
    }

    @Test
    public void testMaxResultSizeOneBelowComputedLengthThrows() {
        byte[] data = bytes("foo");
        try {
            Base64.encodeBase64(data, false, false, 5);
            fail("Se esperaba IllegalArgumentException con maxResultSize=5 (longitud calculada 6)");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 4. encodeInteger(ZERO) y decodeInteger(entrada vacía)
    // ------------------------------------------------------------------

    @Test
    public void testEncodeIntegerZeroProducesEmptyArray() {
        byte[] encoded = Base64.encodeInteger(BigInteger.ZERO);
        assertNotNull(encoded);
        assertEquals(0, encoded.length);
    }

    @Test
    public void testDecodeIntegerEmptyInputIsZero() {
        assertEquals(BigInteger.ZERO, Base64.decodeInteger(new byte[0]));
        assertEquals(BigInteger.ZERO, Base64.decodeInteger(bytes("")));
    }

    @Test
    public void testIntegerZeroRoundTrip() {
        assertEquals(BigInteger.ZERO, Base64.decodeInteger(Base64.encodeInteger(BigInteger.ZERO)));
    }

    // ------------------------------------------------------------------
    // 5. Round-trip de enteros negativos: el signo se descarta
    // ------------------------------------------------------------------

    @Test
    public void testEncodeIntegerNegativeDiscardsSign() {
        // -5 se codifica con el byte 0xFB -> "+w=="
        byte[] encoded = Base64.encodeInteger(BigInteger.valueOf(-5));
        assertArrayEquals(bytes("+w=="), encoded);
    }

    @Test
    public void testDecodeIntegerNegativeRoundTripReturnsPositiveValue() {
        BigInteger original = BigInteger.valueOf(-5);
        BigInteger decoded = Base64.decodeInteger(Base64.encodeInteger(original));

        assertEquals(BigInteger.valueOf(251), decoded);
        assertEquals(1, decoded.signum());
        assertFalse("El round-trip no debe preservar el signo negativo", original.equals(decoded));
    }

    // ------------------------------------------------------------------
    // 6. Longitud en bits múltiplo de 8 con bit alto activo (toIntegerBytes)
    // ------------------------------------------------------------------

    @Test
    public void testEncodeIntegerBitLengthMultipleOf8WithHighBitSet128() {
        // 128 = 0x80: bitLength 8, toByteArray = [0x00, 0x80]; debe usarse solo [0x80].
        byte[] encoded = Base64.encodeInteger(BigInteger.valueOf(128));
        assertArrayEquals(bytes("gA=="), encoded);
    }

    @Test
    public void testDecodeIntegerBitLengthMultipleOf8WithHighBitSet128() {
        assertEquals(BigInteger.valueOf(128), Base64.decodeInteger(bytes("gA==")));
        assertEquals(BigInteger.valueOf(128),
                Base64.decodeInteger(Base64.encodeInteger(BigInteger.valueOf(128))));
    }

    @Test
    public void testEncodeIntegerBitLengthMultipleOf8WithHighBitSet255() {
        // 255 = 0xFF: se codifica el byte 0xFF -> "/w=="
        assertArrayEquals(bytes("/w=="), Base64.encodeInteger(BigInteger.valueOf(255)));
        assertEquals(BigInteger.valueOf(255),
                Base64.decodeInteger(Base64.encodeInteger(BigInteger.valueOf(255))));
    }

    // ------------------------------------------------------------------
    // 7. Mezcla de alfabeto estándar (+ /) y URL-safe (- _) en la decodificación
    // ------------------------------------------------------------------

    @Test
    public void testDecodeMixedStandardAndUrlSafeSymbolsEqualsStandard() {
        // '+' y '-' se decodifican ambos como 62; '/' y '_' como 63.
        assertArrayEquals(Base64.decodeBase64(bytes("+/+/")), Base64.decodeBase64(bytes("+/-_")));
        assertArrayEquals(Base64.decodeBase64(bytes("+/+/")), Base64.decodeBase64(bytes("-_-_")));
    }

    @Test
    public void testDecodeMixedAlphabetExactBytes() {
        // 62,63,62,63 -> 0xFB 0xFF 0xBF
        assertArrayEquals(raw(0xFB, 0xFF, 0xBF), Base64.decodeBase64(bytes("+/-_")));
    }

    @Test
    public void testDecodeMixedAlphabetLongerInput() {
        byte[] expected = Base64.decodeBase64(bytes("YS+/YS+/"));
        byte[] mixed = Base64.decodeBase64(bytes("YS+/YS-_"));
        assertEquals(6, mixed.length);
        assertArrayEquals(expected, mixed);
    }

    // ------------------------------------------------------------------
    // 8. Reinicio de estado al alternar decodificación y codificación
    // ------------------------------------------------------------------

    @Test
    public void testSameInstanceAlternatesDecodeAndEncode() {
        Base64 base64 = new Base64();

        assertArrayEquals(bytes("foo"), base64.decode(bytes("Zm9v")));
        assertArrayEquals(bytes("Zm9v"), base64.encode(bytes("foo")));
        assertArrayEquals(bytes("foobar"), base64.decode(bytes("Zm9vYmFy")));
        assertArrayEquals(bytes("Zm9vYmFy"), base64.encode(bytes("foobar")));
    }

    @Test
    public void testDecodeWithLeftoverStateDoesNotLeakIntoNextDecode() {
        Base64 base64 = new Base64();

        // La primera decodificación deja símbolos sobrantes en el estado interno.
        assertArrayEquals(bytes("A"), base64.decode(bytes("QQ")));
        // La siguiente no debe verse afectada por ese residuo.
        assertArrayEquals(bytes("AAA"), base64.decode(bytes("QUFB")));
    }

    @Test
    public void testEncodeAfterDecodeWithPadStateDoesNotLeak() {
        Base64 base64 = new Base64();

        // Una decodificación que se detiene en '=' no debe afectar a la codificación posterior.
        assertArrayEquals(bytes("A"), base64.decode(bytes("QQ==QUFB")));
        assertArrayEquals(bytes("Zg=="), base64.encode(bytes("f")));
    }
}
