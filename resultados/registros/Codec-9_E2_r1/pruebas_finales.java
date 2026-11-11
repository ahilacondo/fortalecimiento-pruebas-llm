package org.apache.commons.codec.binary;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.junit.Test;

/**
 * Pruebas adicionales para {@link Base64} que usan únicamente la API pública.
 */
public class Base64LLMTest {

    private static final String CRLF = "\r\n";

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    private static String str(byte[] b) {
        return new String(b, StandardCharsets.US_ASCII);
    }

    // ---------------------------------------------------------------
    // Fragmentación (chunking) y separadores de línea
    // ---------------------------------------------------------------

    @Test
    public void testLineLengthRoundsDownToMultipleOfFour() {
        // lineLength 7 se redondea a 4: cada 4 caracteres se inserta el separador.
        Base64 b64 = new Base64(7, ascii(CRLF));
        String result = str(b64.encode(new byte[6]));
        assertEquals("AAAA\r\nAAAA\r\n", result);
    }

    @Test
    public void testTrailingSeparatorAppendedAfterPartialLastLine() {
        Base64 b64 = new Base64(4, ascii(CRLF));
        String result = str(b64.encode(new byte[4]));
        assertEquals("AAAA\r\nAA==\r\n", result);
    }

    @Test
    public void testChunkedExactMultipleOfLineDoesNotDuplicateSeparator() {
        // 57 bytes -> exactamente 76 caracteres: un único CRLF al final.
        String result = str(Base64.encodeBase64Chunked(new byte[57]));
        assertEquals("A".repeat(76) + "\r\n", result);
        assertFalse("No debe haber CRLF duplicado", result.endsWith("\r\n\r\n"));
    }

    @Test
    public void testUrlSafeChunkedModeWithPartialLastGroup() {
        // 58 bytes: 76 caracteres + CRLF, luego "AA" sin padding + CRLF.
        String result = str(new Base64(true).encode(new byte[58]));
        assertEquals("A".repeat(76) + "\r\n" + "AA" + "\r\n", result);
    }
}
