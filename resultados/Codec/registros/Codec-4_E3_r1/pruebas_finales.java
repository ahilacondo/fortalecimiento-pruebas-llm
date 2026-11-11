package org.apache.commons.codec.binary;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link Base64} que usan únicamente la API pública.
 */
public class Base64LLMTest {

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    private static String str(byte[] b) {
        return new String(b, StandardCharsets.US_ASCII);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    private static byte[] sampleData(int n) {
        byte[] d = new byte[n];
        for (int i = 0; i < n; i++) {
            d[i] = (byte) (i * 7 + 3);
        }
        return d;
    }

    // ---------------------------------------------------------------------
    // Punto 1 (aproximado con API pública): codificar/decodificar por partes
    // ---------------------------------------------------------------------

    /**
     * Codificar por partes de longitud múltiplo de 3 y concatenar debe igualar la codificación del conjunto completo.
     */
    @Test
    public void testEncodeInPartsMultipleOfThreeEqualsWholeEncoding() {
        byte[] data = sampleData(100);
        byte[] part1 = new byte[33];
        byte[] part2 = new byte[33];
        byte[] part3 = new byte[34];
        System.arraycopy(data, 0, part1, 0, 33);
        System.arraycopy(data, 33, part2, 0, 33);
        System.arraycopy(data, 66, part3, 0, 34);

        byte[] whole = Base64.encodeBase64(data);
        byte[] concatenated = concat(concat(Base64.encodeBase64(part1), Base64.encodeBase64(part2)),
                Base64.encodeBase64(part3));

        assertArrayEquals("La codificación por partes debe igualar a la de una sola llamada",
                whole, concatenated);
    }

    /**
     * Decodificar por partes cortadas en múltiplos de 4 caracteres debe reconstruir los datos originales.
     */
    @Test
    public void testDecodeInPartsAtQuantumBoundaryRestoresData() {
        byte[] data = sampleData(100);
        byte[] encoded = Base64.encodeBase64(data); // 136 caracteres, sin chunking
        assertEquals(136, encoded.length);

        byte[] firstHalf = new byte[68];
        byte[] secondHalf = new byte[68];
        System.arraycopy(encoded, 0, firstHalf, 0, 68);
        System.arraycopy(encoded, 68, secondHalf, 0, 68);

        byte[] decoded = concat(Base64.decodeBase64(firstHalf), Base64.decodeBase64(secondHalf));
        assertArrayEquals("La decodificación por partes debe restaurar los datos", data, decoded);
    }

    // ---------------------------------------------------------------------
    // Punto 2 (aproximado con API pública): EOF con datos parciales y reutilización
    // ---------------------------------------------------------------------

    /**
     * Una entrada truncada (sin padding) debe vaciarse en el EOF y producir los bytes parciales correctos.
     */
    @Test
    public void testDecodeTruncatedTailFlushesAtEof() {
        assertEquals("A", str(Base64.decodeBase64(ascii("QQ"))));
        assertEquals("AB", str(Base64.decodeBase64(ascii("QUI"))));
        assertEquals("ABCD", str(Base64.decodeBase64(ascii("QUJDRA"))));
    }

    /**
     * Codificar dos bytes en EOF debe añadir exactamente un carácter de relleno.
     */
    @Test
    public void testEncodeTwoBytesFlushesWithSinglePad() {
        assertEquals("QUI=", str(Base64.encodeBase64(ascii("AB"))));
    }

    /**
     * Tras un EOF parcial, la misma instancia debe decodificar correctamente una nueva entrada.
     */
    @Test
    public void testReusedInstanceDecodesAfterPartialEofDecode() {
        Base64 b64 = new Base64();
        assertEquals("A", str(b64.decode(ascii("QQ"))));
        assertEquals("AB", str(b64.decode(ascii("QUI"))));
        assertEquals("ABCD", str(b64.decode(ascii("QUJDRA"))));
    }

    // ---------------------------------------------------------------------
    // Punto 4: reutilización de la instancia sin contaminar el estado
    // ---------------------------------------------------------------------

    /**
     * Un codificado de un solo byte (dejando modulus=1) no debe afectar al siguiente codificado.
     */
    @Test
    public void testReusedInstanceEncodeIsIndependentOfPreviousCall() {
        Base64 b64 = new Base64(0);
        assertEquals("YQ==", str(b64.encode(new byte[]{'a'})));
        assertEquals("AAAA", str(b64.encode(new byte[]{0, 0, 0})));
        assertEquals("SGVsbG8gV29ybGQ=", str(b64.encode(ascii("Hello World"))));
    }

    /**
     * La salida de la misma instancia debe ser idéntica a la de una instancia nueva.
     */
    @Test
    public void testReusedInstanceMatchesFreshInstance() {
        byte[] data = sampleData(50);
        Base64 reused = new Base64(0);
        reused.encode(sampleData(7));
        reused.decode(ascii("QUJDRA=="));

        assertArrayEquals(new Base64(0).encode(data), reused.encode(data));
    }

    /**
     * Codificar y luego decodificar con la misma instancia debe devolver los datos originales.
     */
    @Test
    public void testReusedInstanceEncodeThenDecodeRoundTrip() {
        Base64 b64 = new Base64();
        byte[] encoded = b64.encode(ascii("ab"));
        assertEquals("YWI=", str(encoded));
        assertEquals("ab", str(b64.decode(encoded)));
    }

    // ---------------------------------------------------------------------
    // Punto 5: el primer '=' termina la decodificación
    // ---------------------------------------------------------------------

    /**
     * Todo lo que sigue al primer '=' debe ignorarse, incluidos caracteres base64 válidos.
     */
    @Test
    public void testFirstPadStopsDecodingAndIgnoresValidCharactersAfterIt() {
        assertEquals("A", str(Base64.decodeBase64(ascii("QQ==QUJD"))));
        assertEquals("ABC", str(Base64.decodeBase64(ascii("QUJD=QUJD"))));
        assertEquals("A", str(Base64.decodeBase64(ascii("QQ=QUJD"))));
    }

    /**
     * Un '=' al inicio hace que la salida sea vacía aunque haya datos válidos después.
     */
    @Test
    public void testLeadingPadYieldsEmptyOutput() {
        assertEquals(0, Base64.decodeBase64(ascii("=QUJD")).length);
    }

    // ---------------------------------------------------------------------
    // Punto 6: carácter sobrante único (longitud módulo 4 = 1)
    // ---------------------------------------------------------------------

    /**
     * Un carácter sobrante al final se descarta sin excepción y sin bytes espurios.
     */
    @Test
    public void testSingleLeftoverCharacterIsDiscarded() {
        assertEquals("ABC", str(Base64.decodeBase64(ascii("QUJDR"))));
        assertEquals("ABC", str(Base64.decodeBase64(ascii("QUJDR=="))));
    }

    /**
     * Una entrada de un único carácter no produce bytes.
     */
    @Test
    public void testSingleCharacterInputProducesNoBytes() {
        byte[] result = Base64.decodeBase64(ascii("R"));
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    // ---------------------------------------------------------------------
    // Punto 7: maxResultSize exactamente igual al tamaño requerido
    // ---------------------------------------------------------------------

    /**
     * Con chunking y maxResultSize igual al tamaño requerido (6 bytes), la entrada se acepta.
     */
    @Test
    public void testEncodeChunkedAcceptsExactMaxResultSize() {
        byte[] result = Base64.encodeBase64(new byte[]{0, 0, 0}, true, false, 6);
        assertEquals("AAAA\r\n", str(result));
    }

    /**
     * Para 57 bytes (76 caracteres exactos más CRLF), el tamaño exacto de 78 se acepta.
     */
    @Test
    public void testEncodeChunkedAcceptsExactMaxResultSizeOnLineBoundary() {
        byte[] result = Base64.encodeBase64(new byte[57], true, false, 78);
        assertEquals(78, result.length);
        assertEquals(76, result.length - 2);
        for (int i = 0; i < 76; i++) {
            assertEquals("carácter " + i, 'A', result[i]);
        }
        assertEquals('\r', result[76]);
        assertEquals('\n', result[77]);
    }

    /**
     * Sin chunking, maxResultSize exactamente igual al tamaño requerido también se acepta.
     */
    @Test
    public void testEncodeNonChunkedAcceptsExactMaxResultSize() {
        byte[] result = Base64.encodeBase64(new byte[]{0, 0, 0}, false, false, 6);
        assertEquals("AAAA", str(result));
    }

    /**
     * Un tamaño máximo justo por debajo del requerido debe rechazarse.
     */
    @Test
    public void testEncodeChunkedRejectsOneBelowMaxResultSize() {
        try {
            Base64.encodeBase64(new byte[]{0, 0, 0}, true, false, 5);
            fail("Debería rechazar maxResultSize=5 cuando se requieren 6");
        } catch (IllegalArgumentException expected) {
            // esperado
        }
    }

    /**
     * Para 57 bytes, un límite de 77 (uno por debajo de 78) debe rechazarse.
     */
    @Test
    public void testEncodeChunkedRejectsOneBelowExactBoundary() {
        try {
            Base64.encodeBase64(new byte[57], true, false, 77);
            fail("Debería rechazar maxResultSize=77 cuando se requieren 78");
        } catch (IllegalArgumentException expected) {
            // esperado
        }
    }

    // ---------------------------------------------------------------------
    // Punto 8: casos límite de encodeInteger/decodeInteger
    // ---------------------------------------------------------------------

    /**
     * Cero se codifica como arreglo vacío y se decodifica como ZERO.
     */
    @Test
    public void testIntegerZeroEncodesToEmptyAndDecodesBack() {
        byte[] encoded = Base64.encodeInteger(BigInteger.ZERO);
        assertEquals(0, encoded.length);
        assertEquals(BigInteger.ZERO, Base64.decodeInteger(new byte[0]));
    }

    /**
     * 127 es el último valor de un byte sin bit de signo.
     */
    @Test
    public void testIntegerBoundary127() {
        byte[] encoded = Base64.encodeInteger(BigInteger.valueOf(127));
        assertEquals("fw==", str(encoded));
        assertEquals(BigInteger.valueOf(127), Base64.decodeInteger(encoded));
    }

    /**
     * 128 activa el bit de signo de un byte; no debe aparecer un byte de signo extra.
     */
    @Test
    public void testIntegerBoundary128() {
        byte[] encoded = Base64.encodeInteger(BigInteger.valueOf(128));
        assertEquals("gA==", str(encoded));
        assertEquals(BigInteger.valueOf(128), Base64.decodeInteger(encoded));
    }

    /**
     * 255 es el máximo de un byte y debe codificarse con un único byte de magnitud.
     */
    @Test
    public void testIntegerBoundary255() {
        byte[] encoded = Base64.encodeInteger(BigInteger.valueOf(255));
        assertEquals("/w==", str(encoded));
        assertEquals(BigInteger.valueOf(255), Base64.decodeInteger(encoded));
    }

    /**
     * 256 requiere dos bytes sin bit de signo sobrante.
     */
    @Test
    public void testIntegerBoundary256() {
        byte[] encoded = Base64.encodeInteger(BigInteger.valueOf(256));
        assertEquals("AQA=", str(encoded));
        assertEquals(BigInteger.valueOf(256), Base64.decodeInteger(encoded));
    }

    /**
     * 32768 (0x8000) tiene el bit de signo en el primer byte de dos; se conserva sin byte de signo extra.
     */
    @Test
    public void testIntegerMultiByteWithSignBitInMostSignificantByte() {
        byte[] encoded = Base64.encodeInteger(BigInteger.valueOf(32768));
        assertEquals("gAA=", str(encoded));
        assertEquals(BigInteger.valueOf(32768), Base64.decodeInteger(encoded));
    }

    /**
     * Los valores negativos no conservan el signo: la decodificación siempre devuelve un valor no negativo.
     */
    @Test
    public void testNegativeIntegersDecodeToNonNegativeValues() {
        BigInteger[] negatives = {
            BigInteger.valueOf(-1),
            BigInteger.valueOf(-2),
            BigInteger.valueOf(-128)
        };
        for (BigInteger value : negatives) {
            BigInteger decoded = Base64.decodeInteger(Base64.encodeInteger(value));
            assertTrue("decodificado de " + value + " debe ser no negativo", decoded.signum() >= 0);
        }
    }

    /**
     * Para -128 la codificación coincide con la de 128 (sin signo), confirmando que solo se usa la magnitud.
     */
    @Test
    public void testNegativeBoundaryMinus128UsesMagnitudeBytes() {
        assertArrayEquals(Base64.encodeInteger(BigInteger.valueOf(128)),
                Base64.encodeInteger(BigInteger.valueOf(-128)));
    }
}
