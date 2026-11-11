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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link Base64InputStream} que cubren los requisitos de la documentación
 * no verificados por las pruebas existentes.
 */
public class Base64InputStreamLLMTest {

    private static final byte[] CRLF = {(byte) '\r', (byte) '\n'};

    private static final byte[] LF = {(byte) '\n'};

    /** Genera datos deterministas de la longitud indicada. */
    private static byte[] sampleData(int length) {
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = (byte) (i * 7 + 3);
        }
        return data;
    }

    /** Codificación Base64 de referencia (java.util.Base64, sin saltos de línea). */
    private static String referenceEncode(byte[] data) {
        return java.util.Base64.getEncoder().encodeToString(data);
    }

    /** Parte la cadena en líneas de {@code lineLength} caracteres, terminando cada línea con {@code sep}. */
    private static String chunked(String s, int lineLength, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i += lineLength) {
            sb.append(s, i, Math.min(s.length(), i + lineLength)).append(sep);
        }
        return sb.toString();
    }

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * Lee el stream en fragmentos de tamaño {@code chunk}, verificando que cada llamada devuelva
     * como máximo {@code chunk} bytes.
     */
    private static byte[] readWithChunks(InputStream in, int chunk) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[chunk];
        int iterations = 0;
        int n;
        while ((n = in.read(buf, 0, chunk)) != -1) {
            assertTrue("read(b, 0, " + chunk + ") devolvió más bytes de los solicitados: " + n, n <= chunk);
            assertTrue("read(b, 0, " + chunk + ") devolvió un valor negativo distinto de EOF: " + n, n >= 0);
            out.write(buf, 0, n);
            iterations++;
            assertTrue("Demasiadas iteraciones de lectura (posible bucle infinito)", iterations < 1000000);
        }
        return out.toByteArray();
    }

    /** Lee completamente el stream usando read() byte a byte. */
    private static int[] readSingleBytes(InputStream in) throws IOException {
        java.util.List<Integer> values = new java.util.ArrayList<Integer>();
        int v;
        while ((v = in.read()) != -1) {
            values.add(v);
        }
        int[] result = new int[values.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Requisito 1: constructor (in, doEncode=true) usa lineLength 76 y CRLF por defecto.
    // ------------------------------------------------------------------

    @Test
    public void testDefaultEncodingUses76CharLinesAndCrlf() throws IOException {
        byte[] data = sampleData(100); // 136 caracteres codificados: 76 + 60
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(data), true);

        byte[] output = readWithChunks(in, 1024);
        String expected = chunked(referenceEncode(data), 76, "\r\n");

        assertArrayEquals("El constructor por defecto debe codificar en líneas de 76 caracteres terminadas en CRLF",
                ascii(expected), output);
    }

    @Test
    public void testDefaultEncodingFirstLineIsExactly76CharsFollowedByCrlf() throws IOException {
        byte[] data = sampleData(100);
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(data), true);

        byte[] output = readWithChunks(in, 1024);

        assertTrue("La salida debe tener al menos 78 bytes", output.length >= 78);
        assertEquals("El carácter 77 (índice 76) debe ser CR", '\r', output[76]);
        assertEquals("El carácter 78 (índice 77) debe ser LF", '\n', output[77]);
        for (int i = 0; i < 76; i++) {
            assertTrue("Los primeros 76 caracteres deben ser base64 (no separadores)", output[i] != '\r' && output[i] != '\n');
        }
    }

    // ------------------------------------------------------------------
    // Requisito 2: lineLength no múltiplo de 4 se redondea hacia abajo al múltiplo de 4.
    // ------------------------------------------------------------------

    @Test
    public void testLineLengthNotMultipleOf4IsRoundedDownTo8() throws IOException {
        byte[] data = sampleData(31); // 44 caracteres codificados: 8*5 + 4
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(data), true, 10, LF);

        byte[] output = readWithChunks(in, 1024);
        String expected = chunked(referenceEncode(data), 8, "\n");

        assertArrayEquals("lineLength=10 debe redondearse a 8 caracteres por línea", ascii(expected), output);
    }

    @Test
    public void testLineLengthSevenIsRoundedDownTo4() throws IOException {
        byte[] data = sampleData(31); // 44 caracteres codificados
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(data), true, 7, LF);

        byte[] output = readWithChunks(in, 1024);
        String expected = chunked(referenceEncode(data), 4, "\n");

        assertArrayEquals("lineLength=7 debe redondearse a 4 caracteres por línea", ascii(expected), output);
    }

    // ------------------------------------------------------------------
    // Requisito 3: lineLength negativo produce salida sin saltos de línea y sin separador.
    // ------------------------------------------------------------------

    @Test
    public void testNegativeLineLengthProducesNoLineBreaks() throws IOException {
        byte[] data = sampleData(100);
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(data), true, -4, CRLF);

        byte[] output = readWithChunks(in, 1024);

        assertArrayEquals("lineLength=-4 no debe insertar separadores", ascii(referenceEncode(data)), output);
    }

    @Test
    public void testNegativeOneLineLengthProducesNoLineBreaks() throws IOException {
        byte[] data = sampleData(100);
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(data), true, -1, LF);

        byte[] output = readWithChunks(in, 1024);

        assertArrayEquals("lineLength=-1 no debe insertar separadores", ascii(referenceEncode(data)), output);
    }

    // ------------------------------------------------------------------
    // Requisito 4: con doEncode=false, lineLength se ignora.
    // ------------------------------------------------------------------

    @Test
    public void testLineLengthIgnoredWhenDecoding() throws IOException {
        String encodedWithBreaks = chunked("SGVsbG8gV29ybGQ=", 8, "\n");
        int[] lineLengths = {4, 76, 0, -1, 1000};

        for (int lineLength : lineLengths) {
            Base64InputStream in = new Base64InputStream(
                    new ByteArrayInputStream(ascii(encodedWithBreaks)), false, lineLength, CRLF);
            byte[] decoded = readWithChunks(in, 1024);

            assertArrayEquals("Con doEncode=false, lineLength=" + lineLength + " debe ignorarse",
                    ascii("Hello World"), decoded);
        }
    }

    // ------------------------------------------------------------------
    // Requisito 5: con doEncode=false, lineSeparator se ignora.
    // ------------------------------------------------------------------

    @Test
    public void testLineSeparatorIgnoredWhenDecoding() throws IOException {
        String encoded = "SGVs\r\nbG8g\r\nV29y\r\nbGQ=\r\n";
        byte[][] separators = {CRLF, LF, new byte[]{(byte) '#'}};

        for (byte[] separator : separators) {
            Base64InputStream in = new Base64InputStream(
                    new ByteArrayInputStream(ascii(encoded)), false, 76, separator);
            byte[] decoded = readWithChunks(in, 1024);

            assertArrayEquals("Con doEncode=false, lineSeparator debe ignorarse", ascii("Hello World"), decoded);
        }
    }

    // ------------------------------------------------------------------
    // Requisito 6: read() devuelve valores en el rango 0..255 también para bytes negativos.
    // ------------------------------------------------------------------

    @Test
    public void testReadReturnsUnsignedValuesForNegativeBytes() throws IOException {
        // "//4=" decodifica a {(byte)0xFF, (byte)0xFE}
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(ascii("//4=")));

        assertEquals("read() debe devolver 255 para 0xFF", 255, in.read());
        assertEquals("read() debe devolver 254 para 0xFE", 254, in.read());
        assertEquals("EOF esperado", -1, in.read());
    }

    @Test
    public void testReadReturnsUnsignedValueForByte0x80() throws IOException {
        // "gA==" decodifica a {(byte)0x80}
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(ascii("gA==")));

        int value = in.read();
        assertEquals("read() debe devolver 128 para el byte 0x80", 128, value);
        assertTrue("El valor debe estar en el rango 0..255", value >= 0 && value <= 255);
        assertEquals("EOF esperado", -1, in.read());
    }

    @Test
    public void testReadAllValuesAreWithinZeroTo255() throws IOException {
        byte[] data = new byte[256];
        for (int i = 0; i < 256; i++) {
            data[i] = (byte) i;
        }
        Base64InputStream in = new Base64InputStream(
                new ByteArrayInputStream(ascii(referenceEncode(data))));

        int[] values = readSingleBytes(in);

        assertEquals("Número de bytes decodificados incorrecto", 256, values.length);
        for (int i = 0; i < values.length; i++) {
            assertTrue("read() fuera del rango 0..255 en la posición " + i + ": " + values[i],
                    values[i] >= 0 && values[i] <= 255);
            assertEquals("Valor sin signo incorrecto en la posición " + i, i, values[i]);
        }
    }

    // ------------------------------------------------------------------
    // Requisito 7: read(b, off, len) no escribe fuera de [off, off+len).
    // ------------------------------------------------------------------

    @Test
    public void testReadDoesNotWriteOutsideRequestedRange() throws IOException {
        final byte sentinel = (byte) 0x5A;
        byte[] buf = new byte[10];
        java.util.Arrays.fill(buf, sentinel);

        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(ascii("SGVsbG8=")));
        int n = in.read(buf, 2, 3);

        assertTrue("read(buf, 2, 3) debe devolver entre 1 y 3 bytes, devolvió " + n, n >= 1 && n <= 3);
        byte[] expectedPrefix = ascii("Hello");
        for (int i = 0; i < n; i++) {
            assertEquals("Contenido incorrecto en buf[" + (2 + i) + "]", expectedPrefix[i], buf[2 + i]);
        }
        assertEquals("buf[0] fue modificado", sentinel, buf[0]);
        assertEquals("buf[1] fue modificado", sentinel, buf[1]);
        for (int i = 5; i < buf.length; i++) {
            assertEquals("buf[" + i + "] fue modificado fuera del rango solicitado", sentinel, buf[i]);
        }
    }

    @Test
    public void testReadWithZeroLengthDoesNotModifyBuffer() throws IOException {
        final byte sentinel = (byte) 0x33;
        byte[] buf = new byte[8];
        java.util.Arrays.fill(buf, sentinel);

        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(ascii("SGVsbG8=")));
        int n = in.read(buf, 3, 0);

        assertEquals("read(buf, 3, 0) debe devolver 0", 0, n);
        for (int i = 0; i < buf.length; i++) {
            assertEquals("buf[" + i + "] no debe modificarse con len=0", sentinel, buf[i]);
        }
    }

    // ------------------------------------------------------------------
    // Requisito 8: read(b, off, len) devuelve como máximo len bytes por llamada.
    // ------------------------------------------------------------------

    @Test
    public void testReadNeverReturnsMoreThanLenEncoding() throws IOException {
        byte[] data = sampleData(100);
        String expected = chunked(referenceEncode(data), 76, "\r\n");
        int[] chunkSizes = {1, 2, 3, 5, 7, 64, 1000};

        for (int chunk : chunkSizes) {
            Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(data), true);
            byte[] output = readWithChunks(in, chunk);

            assertArrayEquals("Lectura con len=" + chunk + " produjo datos incorrectos", ascii(expected), output);
        }
    }

    @Test
    public void testReadNeverReturnsMoreThanLenDecoding() throws IOException {
        byte[] data = sampleData(100);
        String encoded = chunked(referenceEncode(data), 76, "\r\n");
        int[] chunkSizes = {1, 2, 3, 4, 5, 13, 500};

        for (int chunk : chunkSizes) {
            Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(ascii(encoded)));
            byte[] output = readWithChunks(in, chunk);

            assertArrayEquals("Decodificación con len=" + chunk + " produjo datos incorrectos", data, output);
        }
    }
}
