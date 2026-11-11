package org.apache.commons.codec.binary;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link Base64} que cubren requisitos de la documentación
 * no verificados por {@code Base64Test}. Solo se usa la API pública de la clase.
 */
public class Base64LLMTest {

    /** Triplete {0xfb, 0xff, 0xbf}: en alfabeto estándar codifica "+/+/", en URL-safe "-_-_". */
    private static final byte[] TRIPLET_FBFFBF = {(byte) 0xfb, (byte) 0xff, (byte) 0xbf};

    /** Pares {0xfb, 0xff}: en estándar codifica "+/8=", en URL-safe "-_8" (sin relleno). */
    private static final byte[] PAIR_FBFF = {(byte) 0xfb, (byte) 0xff};

    /**
     * Construye un array de {@code n} bytes repitiendo el patrón fb, ff, bf.
     */
    private static byte[] patternBytes(int n) {
        byte[] data = new byte[n];
        for (int i = 0; i < n; i++) {
            data[i] = TRIPLET_FBFFBF[i % 3];
        }
        return data;
    }

    private static String ascii(byte[] bytes) {
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    // ------------------------------------------------------------------
    // Requisito 1: Base64(true) codifica en URL-safe y isUrlSafe() es true
    // ------------------------------------------------------------------

    @Test
    public void urlSafeInstanceEncodesWithDashAndUnderscoreWithoutPadding() {
        Base64 urlSafe = new Base64(true);
        assertTrue("isUrlSafe() debe ser true para Base64(true)", urlSafe.isUrlSafe());
        assertEquals("-_8", ascii(urlSafe.encode(PAIR_FBFF)));
        assertEquals("-_-_", ascii(urlSafe.encode(TRIPLET_FBFFBF)));
    }

    @Test
    public void standardInstanceEncodesWithPlusSlashAndPadding() {
        Base64 standard = new Base64();
        assertFalse("isUrlSafe() debe ser false para Base64()", standard.isUrlSafe());
        assertFalse("isUrlSafe() debe ser false para Base64(false)", new Base64(false).isUrlSafe());
        assertEquals("+/8=", ascii(standard.encode(PAIR_FBFF)));
        assertEquals("+/+/", ascii(standard.encode(TRIPLET_FBFFBF)));
    }

    @Test
    public void urlSafeInstanceRoundTripDecodesCorrectly() {
        Base64 urlSafe = new Base64(true);
        byte[] encoded = urlSafe.encode(TRIPLET_FBFFBF);
        assertArrayEquals(TRIPLET_FBFFBF, urlSafe.decode(encoded));
    }

    // ------------------------------------------------------------------
    // Requisito 2: Base64() fragmenta en bloques de 76 caracteres con CRLF
    // ------------------------------------------------------------------

    /**
     * 60 bytes a cero producen 80 caracteres 'A'. La instancia por defecto debe
     * fragmentar en una línea de 76 caracteres, un CRLF y una línea final de 4 caracteres seguida de CRLF.
     */
    @Test
    public void defaultInstanceChunksOutputAt76CharsWithCrlf() {
        byte[] data = new byte[60];
        String expected = "A".repeat(76) + "\r\n" + "AAAA\r\n";
        assertEquals(expected, ascii(new Base64().encode(data)));
    }

    // ------------------------------------------------------------------
    // Requisito 3: Base64(lineLength) redondea a múltiplo de 4 superior
    // ------------------------------------------------------------------

    /**
     * Con lineLength 5 la longitud efectiva es 8 (múltiplo de 4 superior).
     * 12 bytes a cero producen 16 caracteres 'A' en dos líneas de 8 caracteres.
     */
    @Test
    public void lineLengthIsRoundedUpToMultipleOfFour() {
        byte[] data = new byte[12];
        String expected = "AAAAAAAA\r\nAAAAAAAA\r\n";
        assertEquals(expected, ascii(new Base64(5).encode(data)));
    }

    @Test
    public void lineLengthSixIsAlsoRoundedUpToEight() {
        byte[] data = new byte[12];
        String expected = "AAAAAAAA\r\nAAAAAAAA\r\n";
        assertEquals(expected, ascii(new Base64(6).encode(data)));
    }

    // ------------------------------------------------------------------
    // Requisito 4: lineLength <= 0 no fragmenta ni añade separador
    // ------------------------------------------------------------------

    @Test
    public void nonPositiveLineLengthProducesUnchunkedOutputWithoutSeparator() {
        byte[] data = new byte[100];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i * 7 + 3);
        }
        byte[] unchunked = Base64.encodeBase64(data);
        assertEquals("La salida debe tener 136 caracteres", 136, unchunked.length);

        byte[] zero = new Base64(0).encode(data);
        assertArrayEquals(unchunked, zero);
        assertFalse("No debe haber CR en la salida", ascii(zero).indexOf('\r') >= 0);
        assertFalse("No debe haber LF en la salida", ascii(zero).indexOf('\n') >= 0);

        byte[] negativeWithSeparator = new Base64(-1, new byte[] {'$'}).encode(data);
        assertArrayEquals("El separador '$' no debe aparecer si lineLength <= 0",
                unchunked, negativeWithSeparator);
    }

    // ------------------------------------------------------------------
    // Requisito 5: separador personalizado + URL-safe en el constructor
    // ------------------------------------------------------------------

    @Test
    public void customSeparatorWithUrlSafeConstructorIsAcceptedAndEncodesUrlSafe() {
        Base64 b64 = new Base64(64, new byte[] {'$'}, true);
        assertTrue("isUrlSafe() debe ser true", b64.isUrlSafe());
        assertEquals("-_8", ascii(b64.encode(PAIR_FBFF)));
    }

    @Test
    public void customSeparatorContainingBase64CharacterIsRejected() {
        try {
            new Base64(64, new byte[] {'A'}, true);
            fail("Debe rechazar un separador que contiene caracteres Base64");
        } catch (IllegalArgumentException expected) {
            // esperado
        }
    }

    /**
     * 90 bytes producen 120 caracteres URL-safe. Con lineLength 64 y separador '$'
     * debe haber un '$' tras los primeros 64 caracteres y otro al final.
     */
    @Test
    public void customSeparatorAndUrlSafeAreAppliedTogetherWhenChunking() {
        byte[] data = patternBytes(90);
        String expected = "-_-_".repeat(16) + "$" + "-_-_".repeat(14) + "$";
        assertEquals(expected, ascii(new Base64(64, new byte[] {'$'}, true).encode(data)));
    }

    // ------------------------------------------------------------------
    // Requisito 6: isArrayByteBase64 trata los espacios en blanco como válidos
    // ------------------------------------------------------------------

    @Test
    public void isArrayByteBase64AcceptsWhitespaceCharacters() {
        assertTrue(Base64.isArrayByteBase64(new byte[] {' '}));
        assertTrue(Base64.isArrayByteBase64(new byte[] {'\t'}));
        assertTrue(Base64.isArrayByteBase64(new byte[] {'\r'}));
        assertTrue(Base64.isArrayByteBase64(new byte[] {'\n'}));
        assertTrue(Base64.isArrayByteBase64(" \t\r\n".getBytes(StandardCharsets.US_ASCII)));
        assertTrue(Base64.isArrayByteBase64("QUFB \r\nQUFB\t".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    public void isArrayByteBase64RejectsNonWhitespaceNonAlphabetCharacters() {
        assertFalse(Base64.isArrayByteBase64("QUFB$".getBytes(StandardCharsets.US_ASCII)));
        assertFalse(Base64.isArrayByteBase64(new byte[] {' ', '%'}));
    }

    // ------------------------------------------------------------------
    // Requisito 7: isBase64(byte) con '=' y con espacios en blanco
    // ------------------------------------------------------------------

    @Test
    public void isBase64ReturnsTrueForPadCharacter() {
        assertTrue("'=' es carácter de relleno y debe ser true", Base64.isBase64((byte) '='));
    }

    @Test
    public void isBase64ReturnsFalseForWhitespace() {
        assertFalse(Base64.isBase64((byte) ' '));
        assertFalse(Base64.isBase64((byte) '\t'));
        assertFalse(Base64.isBase64((byte) '\r'));
        assertFalse(Base64.isBase64((byte) '\n'));
    }

    @Test
    public void isBase64ReturnsTrueForAlphabetAndUrlSafeCharacters() {
        assertTrue(Base64.isBase64((byte) 'A'));
        assertTrue(Base64.isBase64((byte) '+'));
        assertTrue(Base64.isBase64((byte) '/'));
        assertTrue(Base64.isBase64((byte) '-'));
        assertTrue(Base64.isBase64((byte) '_'));
        assertFalse(Base64.isBase64((byte) '$'));
    }

    // ------------------------------------------------------------------
    // Requisito 8: encodeBase64(data, chunked, urlSafe) combina 76 caracteres y URL-safe sin relleno
    // ------------------------------------------------------------------

    /**
     * 92 bytes producen 122 caracteres en URL-safe sin relleno. Con fragmentación de 76
     * caracteres: 76 caracteres, CRLF, 47 caracteres ("-_-_" x11 + "-_8"), CRLF.
     */
    @Test
    public void chunkedUrlSafeEncodingUsesUrlAlphabetWithoutPaddingAndChunksAt76() {
        byte[] data = patternBytes(92);
        String expected = "-_-_".repeat(19) + "\r\n" + "-_-_".repeat(11) + "-_8" + "\r\n";
        byte[] result = Base64.encodeBase64(data, true, true);
        assertEquals(expected, ascii(result));
        assertEquals(127, result.length);
    }

    @Test
    public void chunkedUrlSafeEncodingContainsNoStandardOnlyCharacters() {
        String result = ascii(Base64.encodeBase64(patternBytes(92), true, true));
        assertFalse("No debe contener '+'", result.indexOf('+') >= 0);
        assertFalse("No debe contener '/'", result.indexOf('/') >= 0);
        assertFalse("No debe contener relleno '='", result.indexOf('=') >= 0);
    }
}
