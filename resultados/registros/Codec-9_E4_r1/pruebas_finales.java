package org.apache.commons.codec.binary;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link Base64} que cubren requisitos de documentación
 * no verificados por la suite existente. Solo usan la API pública de la clase.
 */
public class Base64LLMTest {

    /** Genera un array determinista de {@code n} bytes. */
    private static byte[] data(int n) {
        byte[] d = new byte[n];
        for (int i = 0; i < n; i++) {
            d[i] = (byte) (i * 31 + 7);
        }
        return d;
    }

    /** Genera {@code n} bytes que producen '+' en la codificación estándar (111110 = 62). */
    private static byte[] urlData(int n) {
        byte[] d = new byte[n];
        byte[] pattern = {(byte) 0xFB, (byte) 0xEF, (byte) 0xBE};
        for (int i = 0; i < n; i++) {
            d[i] = pattern[i % 3];
        }
        return d;
    }

    private static String ascii(byte[] b) {
        return new String(b, StandardCharsets.US_ASCII);
    }

    // Requisito 1: Base64(boolean) trocea en líneas de 76 separadas por CRLF.
    @Test
    public void testUrlSafeConstructorChunksAt76WithCrLf() {
        byte[] d = urlData(180); // 240 caracteres codificados, sin relleno
        String s = ascii(new Base64(true).encode(d));

        String[] lines = s.split("\r\n", -1);
        // 240 = 3 * 76 + 12 -> 3 líneas completas, 1 línea de 12, y cadena vacía final por el CRLF final
        assertEquals("Número de partes tras dividir por CRLF", 5, lines.length);
        assertEquals("La última parte tras el CRLF final debe estar vacía", "", lines[lines.length - 1]);
        assertEquals("Línea 1 debe tener 76 caracteres", 76, lines[0].length());
        assertEquals("Línea 2 debe tener 76 caracteres", 76, lines[1].length());
        assertEquals("Línea 3 debe tener 76 caracteres", 76, lines[2].length());
        assertEquals("Línea 4 debe tener 12 caracteres", 12, lines[3].length());
    }

    @Test
    public void testUrlSafeConstructorOutputMatchesUrlSafeChunkedStatic() {
        byte[] d = urlData(180);
        byte[] fromConstructor = new Base64(true).encode(d);
        byte[] fromStatic = Base64.encodeBase64(d, true, true);
        assertArrayEquals("new Base64(true) debe equivaler a encodeBase64(d, true, true)", fromStatic, fromConstructor);
    }

    @Test
    public void testUrlSafeConstructorUsesUrlSafeAlphabetAndRoundTrips() {
        byte[] d = urlData(180);
        byte[] enc = new Base64(true).encode(d);
        String s = ascii(enc);
        assertFalse("La salida URL-safe no debe contener '+'", s.contains("+"));
        assertFalse("La salida URL-safe no debe contener '/'", s.contains("/"));
        assertTrue("La salida URL-safe debe contener '-'", s.contains("-"));
        assertArrayEquals("La decodificación debe recuperar los datos originales", d, new Base64().decode(enc));
    }

    // Requisito 2: Base64() no trocea, también con entradas largas.
    @Test
    public void testDefaultConstructorDoesNotChunkLongInput() {
        byte[] d = data(300); // 300 bytes -> 400 caracteres
        byte[] enc = new Base64().encode(d);
        assertEquals("Longitud de salida sin troceado", 400, enc.length);
        String s = ascii(enc);
        assertFalse("No debe contener CR", s.indexOf('\r') >= 0);
        assertFalse("No debe contener LF", s.indexOf('\n') >= 0);
        assertArrayEquals("Roundtrip sin troceado", d, new Base64().decode(enc));
    }

    // Requisito 3: lineLength <= 0 produce salida sin saltos de línea en datos largos.
    @Test
    public void testNonPositiveLineLengthProducesNoLineBreaks() {
        byte[] d = data(300);
        byte[] expected = new Base64().encode(d);

        byte[] zero = new Base64(0).encode(d);
        byte[] negative = new Base64(-10).encode(d);

        assertArrayEquals("lineLength=0 debe equivaler a sin troceado", expected, zero);
        assertArrayEquals("lineLength<0 debe equivaler a sin troceado", expected, negative);
        assertFalse("lineLength=0 no debe producir CR", ascii(zero).indexOf('\r') >= 0);
        assertFalse("lineLength=0 no debe producir LF", ascii(zero).indexOf('\n') >= 0);
        assertFalse("lineLength<0 no debe producir LF", ascii(negative).indexOf('\n') >= 0);
    }

    // Requisito 4: el primer '=' marca el fin de los datos; lo posterior se ignora.
    @Test
    public void testFirstPadCharacterMarksEndOfData() {
        assertEquals("foo", new String(new Base64().decode("Zm9v=ZW1w"), StandardCharsets.US_ASCII));
        assertEquals("f", new String(new Base64().decode("Zg==QUJD"), StandardCharsets.US_ASCII));
        assertEquals("foob", new String(new Base64().decode("Zm9vYg==QUJD"), StandardCharsets.US_ASCII));
    }

    @Test
    public void testDataAfterPadIsIgnoredEvenWithoutFullQuantum() {
        byte[] result = Base64.decodeBase64("QQ=QUJDREVG".getBytes(StandardCharsets.US_ASCII));
        assertArrayEquals("Solo debe decodificarse 'A' antes del '='", new byte[] {'A'}, result);
    }

    // Requisito 5: isBase64(byte) acepta '=' y rechaza valores fuera del alfabeto.
    @Test
    public void testIsBase64ByteAcceptsPadAndAlphabet() {
        assertTrue("'=' debe ser válido", Base64.isBase64((byte) '='));
        assertTrue("'A' debe ser válido", Base64.isBase64((byte) 'A'));
        assertTrue("'z' debe ser válido", Base64.isBase64((byte) 'z'));
        assertTrue("'0' debe ser válido", Base64.isBase64((byte) '0'));
        assertTrue("'+' debe ser válido", Base64.isBase64((byte) '+'));
        assertTrue("'/' debe ser válido", Base64.isBase64((byte) '/'));
    }

    @Test
    public void testIsBase64ByteRejectsValuesOutsideAlphabet() {
        assertFalse("'$' debe ser inválido", Base64.isBase64((byte) '$'));
        assertFalse("'!' debe ser inválido", Base64.isBase64((byte) '!'));
        assertFalse("'%' debe ser inválido", Base64.isBase64((byte) '%'));
        assertFalse("espacio no pertenece al alfabeto en esta sobrecarga", Base64.isBase64((byte) ' '));
        assertFalse("LF no pertenece al alfabeto en esta sobrecarga", Base64.isBase64((byte) '\n'));
        assertFalse("Byte.MIN_VALUE debe ser inválido", Base64.isBase64(Byte.MIN_VALUE));
        assertFalse("-1 debe ser inválido", Base64.isBase64((byte) -1));
        assertFalse("Byte.MAX_VALUE debe ser inválido", Base64.isBase64(Byte.MAX_VALUE));
    }

    // Requisito 6: un límite igual al tamaño exacto no lanza excepción.
    @Test
    public void testMaxResultSizeEqualToExactChunkedSizeDoesNotThrow() {
        byte[] d = data(100);
        byte[] exact = Base64.encodeBase64(d, true);
        byte[] limited = Base64.encodeBase64(d, true, false, exact.length);
        assertArrayEquals("Con límite exacto la salida debe ser idéntica", exact, limited);
    }

    @Test
    public void testMaxResultSizeOneBelowExactChunkedSizeThrows() {
        byte[] d = data(100);
        int exactLength = Base64.encodeBase64(d, true).length;
        try {
            Base64.encodeBase64(d, true, false, exactLength - 1);
            fail("Debe lanzar IllegalArgumentException si el límite es menor que el tamaño exacto");
        } catch (IllegalArgumentException expected) {
            // esperado
        }
    }

    // Requisito 7 (streaming, observable mediante API pública): reutilizar la instancia
    // no debe filtrar estado entre llamadas.
    @Test
    public void testReusedInstanceProducesSameOutputAsFreshInstance() {
        byte[] first = data(200);
        byte[] second = urlData(97);

        Base64 reused = new Base64(0);
        byte[] encFirst = reused.encode(first);
        byte[] encSecond = reused.encode(second);

        assertArrayEquals("Misma entrada repetida debe dar misma salida",
                encFirst, reused.encode(first));
        assertArrayEquals("Entrada posterior no debe depender de la anterior",
                new Base64(0).encode(second), encSecond);
    }

    @Test
    public void testReusedInstanceDecodingIsConsistent() {
        byte[] d = data(150);
        Base64 b64 = new Base64();
        byte[] enc = b64.encode(d);

        assertArrayEquals("Primera decodificación", d, b64.decode(enc));
        assertArrayEquals("Segunda decodificación con la misma instancia", d, b64.decode(enc));
    }

    @Test
    public void testChunkedAndUnchunkedInputDecodeToSameData() {
        byte[] d = data(300);
        byte[] plain = new Base64().encode(d);
        byte[] chunked = new Base64(true).encode(d);
        assertArrayEquals("Decodificar texto troceado debe igualar al no troceado",
                d, new Base64().decode(chunked));
        assertArrayEquals("Decodificar texto sin trocear debe igualar a los datos",
                d, new Base64().decode(plain));
    }

    // Requisito 8: lineSeparator nulo desactiva el troceado aunque lineLength > 0.
    @Test
    public void testNullLineSeparatorDisablesChunkingForLongInput() {
        byte[] d = data(200);
        byte[] expected = Base64.encodeBase64(d, false);

        byte[] enc = new Base64(76, null).encode(d);

        assertArrayEquals("Con separador nulo no debe haber troceado", expected, enc);
        assertFalse("No debe contener LF", ascii(enc).indexOf('\n') >= 0);
        assertFalse("No debe contener CR", ascii(enc).indexOf('\r') >= 0);
    }

    @Test
    public void testNullLineSeparatorWithSmallLineLengthDisablesChunking() {
        byte[] d = data(120);
        String s = ascii(new Base64(4, null).encode(d));
        assertFalse("lineLength=4 con separador nulo no debe emitir LF", s.indexOf('\n') >= 0);
        assertFalse("lineLength=4 con separador nulo no debe emitir CR", s.indexOf('\r') >= 0);
        assertArrayEquals("Roundtrip con separador nulo", d, new Base64().decode(s));
    }
}
