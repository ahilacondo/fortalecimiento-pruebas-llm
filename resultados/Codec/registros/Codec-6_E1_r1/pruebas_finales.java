package org.apache.commons.codec.binary;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link Base64InputStream} que usan únicamente la API pública de la clase.
 */
public class Base64InputStreamLLMTest {

    private static final byte[] CRLF = {(byte) '\r', (byte) '\n'};

    private static final byte[] LF = {(byte) '\n'};

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    private static String asciiString(byte[] data) {
        return new String(data, StandardCharsets.US_ASCII);
    }

    /** Lee todo el contenido del stream usando lecturas de tamaño 7 para ejercitar read(byte[],int,int). */
    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[7];
        int n;
        while ((n = in.read(buf, 0, buf.length)) != -1) {
            if (n > 0) {
                out.write(buf, 0, n);
            }
        }
        return out.toByteArray();
    }

    /** Fuente que entrega como máximo un byte por llamada a read. */
    private static InputStream oneByteAtATime(byte[] data) {
        final InputStream src = new ByteArrayInputStream(data);
        return new InputStream() {
            @Override
            public int read() throws IOException {
                return src.read();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (len == 0) {
                    return 0;
                }
                int v = src.read();
                if (v == -1) {
                    return -1;
                }
                b[off] = (byte) v;
                return 1;
            }
        };
    }

    private static byte[] sequentialData(int length) {
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = (byte) (i % 256);
        }
        return data;
    }

    @Test
    public void testDecodeSimpleStringWithoutLineEnding() throws IOException {
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(ascii("SGVsbG8gV29ybGQ=")));
        byte[] decoded = readAll(in);
        assertEquals("Hello World", asciiString(decoded));
    }

    @Test
    public void testEncodeWithoutLineBreaksWhenLineLengthIsZero() throws IOException {
        Base64InputStream in = new Base64InputStream(
                new ByteArrayInputStream(ascii("Hello World")), true, 0, CRLF);
        assertEquals("SGVsbG8gV29ybGQ=", asciiString(readAll(in)));
    }

    @Test
    public void testEncodeSplitsLinesAndTerminatesEachWithSeparator() throws IOException {
        Base64InputStream in = new Base64InputStream(
                new ByteArrayInputStream(ascii("Hello World")), true, 4, CRLF);
        assertEquals("SGVs\r\nbG8g\r\nV29y\r\nbGQ=\r\n", asciiString(readAll(in)));
    }

    @Test
    public void testEncodeUsesLfSeparatorWhenProvided() throws IOException {
        Base64InputStream in = new Base64InputStream(
                new ByteArrayInputStream(ascii("Hello World")), true, 4, LF);
        assertEquals("SGVs\nbG8g\nV29y\nbGQ=\n", asciiString(readAll(in)));
    }

    @Test
    public void testDecodeIgnoresLineSeparators() throws IOException {
        Base64InputStream in = new Base64InputStream(
                new ByteArrayInputStream(ascii("SGVs\r\nbG8g\r\nV29y\r\nbGQ=\r\n")));
        assertEquals("Hello World", asciiString(readAll(in)));
    }

    @Test
    public void testTwoArgConstructorWithFalseDecodes() throws IOException {
        Base64InputStream in = new Base64InputStream(
                new ByteArrayInputStream(ascii("SGVsbG8gV29ybGQ=")), false);
        assertEquals("Hello World", asciiString(readAll(in)));
    }

    @Test
    public void testEmptyDecodeReturnsEof() throws IOException {
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(new byte[0]));
        assertEquals(-1, in.read());
        assertEquals(-1, in.read(new byte[10], 0, 10));
    }

    @Test
    public void testEmptyEncodeReturnsEof() throws IOException {
        Base64InputStream in = new Base64InputStream(
                new ByteArrayInputStream(new byte[0]), true, 76, CRLF);
        assertEquals(-1, in.read());
        assertEquals(-1, in.read(new byte[10], 0, 10));
    }

    @Test
    public void testReadSingleByteReturnsUnsignedValue() throws IOException {
        // "/w==" decodifica a 0xFF; read() debe devolver 255, no -1.
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(ascii("/w==")));
        assertEquals(255, in.read());
        assertEquals(-1, in.read());
    }

    @Test
    public void testReadSingleByteSequenceOfDecodedText() throws IOException {
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(ascii("SGk=")));
        assertEquals('H', in.read());
        assertEquals('i', in.read());
        assertEquals(-1, in.read());
    }

    @Test
    public void testReadIntoOffsetWritesOnlyRequestedRegion() throws IOException {
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(ascii("SGVsbG8=")));
        byte[] buf = new byte[10];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = 'X';
        }
        int total = 0;
        while (total < 5) {
            int n = in.read(buf, 2 + total, 5 - total);
            if (n == -1) {
                break;
            }
            total += n;
        }
        assertEquals("Número de bytes leídos", 5, total);
        assertEquals('X', buf[0]);
        assertEquals('X', buf[1]);
        assertEquals("Hello", new String(buf, 2, 5, StandardCharsets.US_ASCII));
        assertEquals('X', buf[7]);
        assertEquals('X', buf[8]);
        assertEquals('X', buf[9]);
        assertEquals(-1, in.read(buf, 0, 1));
    }

    @Test
    public void testReadWithZeroLengthReturnsZeroWithoutConsuming() throws IOException {
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(ascii("SGk=")));
        byte[] buf = new byte[4];
        assertEquals(0, in.read(buf, 0, 0));
        assertEquals('H', in.read());
    }

    @Test
    public void testReadRequestingMoreThanRemainingReturnsAvailableThenEof() throws IOException {
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(ascii("AA==")));
        byte[] buf = new byte[16];
        int n = in.read(buf, 0, buf.length);
        assertEquals("Solo un byte decodificado", 1, n);
        assertEquals(0, buf[0]);
        assertEquals(-1, in.read(buf, 0, buf.length));
    }

    @Test
    public void testReadNullArrayThrowsNullPointerException() throws IOException {
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(ascii("SGk=")));
        try {
            in.read(null, 0, 1);
            fail("Se esperaba NullPointerException con array nulo");
        } catch (NullPointerException expected) {
            // esperado
        }
    }

    @Test
    public void testReadLengthExceedingArrayThrowsIndexOutOfBounds() throws IOException {
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(ascii("SGk=")));
        try {
            in.read(new byte[4], 0, 5);
            fail("Se esperaba IndexOutOfBoundsException cuando len > b.length");
        } catch (IndexOutOfBoundsException expected) {
            // esperado
        }
    }

    @Test
    public void testReadOffsetAtEndWithLengthThrowsIndexOutOfBounds() throws IOException {
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(ascii("SGk=")));
        try {
            in.read(new byte[4], 4, 1);
            fail("Se esperaba IndexOutOfBoundsException cuando offset + len > b.length");
        } catch (IndexOutOfBoundsException expected) {
            // esperado
        }
    }

    @Test
    public void testMarkSupportedIsFalseForEncodeAndDecode() {
        Base64InputStream decode = new Base64InputStream(new ByteArrayInputStream(new byte[0]));
        Base64InputStream encode = new Base64InputStream(
                new ByteArrayInputStream(new byte[0]), true, 76, CRLF);
        assertFalse(decode.markSupported());
        assertFalse(encode.markSupported());
    }

    @Test
    public void testEncodeLongDataProducesExpectedLineLayout() throws IOException {
        // 300 bytes -> 400 caracteres base64 sin padding; con líneas de 76 caracteres.
        byte[] data = sequentialData(300);
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(data), true, 76, CRLF);
        String encoded = asciiString(readAll(in));

        String[] lines = encoded.split("\r\n", -1);
        // 5 líneas completas de 76 + 1 línea de 20 + elemento final vacío tras el último CRLF
        assertEquals("Número de elementos tras split", 7, lines.length);
        for (int i = 0; i < 5; i++) {
            assertEquals("Longitud de la línea " + i, 76, lines[i].length());
        }
        assertEquals("Longitud de la última línea de datos", 20, lines[5].length());
        assertEquals("Tras el último CRLF no debe haber contenido", "", lines[6]);
    }

    @Test
    public void testRoundTripEncodeThenDecodeRestoresOriginal() throws IOException {
        byte[] data = sequentialData(300);
        Base64InputStream encoder = new Base64InputStream(
                new ByteArrayInputStream(data), true, 76, CRLF);
        Base64InputStream decoder = new Base64InputStream(encoder);
        assertArrayEquals("La codificación seguida de decodificación debe ser identidad",
                data, readAll(decoder));
    }

    @Test
    public void testDecodeFromSingleByteSourceMatchesBulkSource() throws IOException {
        String encoded = "SGVsbG8gV29ybGQ=";
        byte[] bulk = readAll(new Base64InputStream(new ByteArrayInputStream(ascii(encoded))));
        byte[] fragmented = readAll(new Base64InputStream(oneByteAtATime(ascii(encoded))));
        assertArrayEquals(bulk, fragmented);
        assertEquals("Hello World", asciiString(fragmented));
    }

    @Test
    public void testEncodeFromSingleByteSourceMatchesBulkSource() throws IOException {
        byte[] data = sequentialData(150);
        byte[] bulk = readAll(new Base64InputStream(
                new ByteArrayInputStream(data), true, 76, CRLF));
        byte[] fragmented = readAll(new Base64InputStream(
                oneByteAtATime(data), true, 76, CRLF));
        assertArrayEquals(bulk, fragmented);
    }

    @Test
    public void testEncodedOutputOfBinaryDataDecodesBackByteByByte() throws IOException {
        byte[] data = sequentialData(64);
        byte[] encoded = readAll(new Base64InputStream(
                new ByteArrayInputStream(data), true, 0, CRLF));

        Base64InputStream decoder = new Base64InputStream(new ByteArrayInputStream(encoded));
        byte[] out = new byte[data.length];
        for (int i = 0; i < out.length; i++) {
            int v = decoder.read();
            assertTrue("Lectura prematura de EOF en posición " + i, v != -1);
            out[i] = (byte) v;
        }
        assertEquals(-1, decoder.read());
        assertArrayEquals(data, out);
    }
}
