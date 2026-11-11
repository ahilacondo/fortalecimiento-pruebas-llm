package org.apache.commons.csv;

import java.io.StringReader;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Pruebas adicionales para {@link ExtendedBufferedReader}. Cubren el contador de líneas,
 * el manejo de CR/LF en {@code read()}, {@code read(char[], int, int)} y {@code readLine()},
 * y el estado de {@code readAgain()}. Las aserciones caracterizan el comportamiento actual
 * de la clase a través de su API pública (paquete).
 */
public class ExtendedBufferedReaderLLMTest {

    /**
     * read() incrementa el contador de líneas al leer '\n'.
     */
    @Test
    public void testReadIncrementaContadorAlLeerLF() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a\nb");

        assertEquals('a', br.read());
        assertEquals(0, br.getLineNumber());

        assertEquals('\n', br.read());
        assertEquals(1, br.getLineNumber());
        assertEquals('\n', br.readAgain());

        assertEquals('b', br.read());
        assertEquals(1, br.getLineNumber());
    }

    /**
     * read() no incrementa el contador al leer un '\r' aislado (sin '\n' posterior).
     */
    @Test
    public void testReadNoIncrementaContadorConCRAislado() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a\rb");

        assertEquals('a', br.read());
        assertEquals('\r', br.read());
        assertEquals('\r', br.readAgain());
        assertEquals(0, br.getLineNumber());

        assertEquals('b', br.read());
        assertEquals(0, br.getLineNumber());
    }

    /**
     * read(char[], int, int) cuenta un '\r' aislado y un '\n' que no sigue a '\r' dentro del bloque.
     * Entrada "a\rb\nc": '\r' suma 1 y '\n' (precedido por 'b') suma 1, total 2.
     */
    @Test
    public void testReadBloqueCuentaCRAisladoYLFSinCR() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a\rb\nc");
        char[] buf = new char[5];

        assertEquals(5, br.read(buf, 0, 5));
        assertArrayEquals(new char[] {'a', '\r', 'b', '\n', 'c'}, buf);
        assertEquals(2, br.getLineNumber());
        assertEquals('c', br.readAgain());
    }

    /**
     * read(char[], int, int) cuenta una secuencia CRLF completa dentro de un mismo bloque como un único salto.
     */
    @Test
    public void testReadBloqueCRLFDentroDelBloqueCuentaUnSalto() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("x\r\ny");
        char[] buf = new char[4];

        assertEquals(4, br.read(buf, 0, 4));
        assertEquals(1, br.getLineNumber());
        assertEquals('\n', br.readAgain());
    }

    /**
     * Una secuencia CRLF partida entre dos bloques cuenta un único salto, usando lastChar como carácter previo.
     * Bloque 1: "x\r" (cuenta el '\r'). Bloque 2: "\ny" ('\n' precedido por '\r' previo, no cuenta).
     */
    @Test
    public void testReadBloqueCRLFPartidoEntreBloquesCuentaUnSalto() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("x\r\ny");
        char[] buf = new char[2];

        assertEquals(2, br.read(buf, 0, 2));
        assertArrayEquals(new char[] {'x', '\r'}, buf);
        assertEquals(1, br.getLineNumber());
        assertEquals('\r', br.readAgain());

        assertEquals(2, br.read(buf, 0, 2));
        assertArrayEquals(new char[] {'\n', 'y'}, buf);
        assertEquals(1, br.getLineNumber());
        assertEquals('y', br.readAgain());
    }

    /**
     * Con offset > 0, un '\n' en la posición offset se compara con buf[offset-1] (contenido del buffer
     * del llamante) y no con el último carácter realmente leído. Aquí buf[0] contiene '\r' preexistente,
     * así que el '\n' no se cuenta, aunque en el flujo no hay ningún '\r' previo.
     * Comportamiento actual observado.
     */
    @Test
    public void testReadBloqueOffsetPositivoEvaluaBufferDelLlamante() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("\n");
        char[] buf = new char[] {'\r', '-'};

        assertEquals(1, br.read(buf, 1, 1));
        assertEquals('\n', buf[1]);
        assertEquals(0, br.getLineNumber());
        assertEquals('\n', br.readAgain());
    }

    /**
     * Con offset > 0 y un carácter previo distinto de '\r' en buf[offset-1], el '\n' sí se cuenta.
     */
    @Test
    public void testReadBloqueOffsetPositivoLFTrasCaracterNoCRCuenta() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("\n");
        char[] buf = new char[] {'a', '-'};

        assertEquals(1, br.read(buf, 1, 1));
        assertEquals('\n', buf[1]);
        assertEquals(1, br.getLineNumber());
    }

    /**
     * read(char[], int, int) establece readAgain() en END_OF_STREAM cuando el flujo está agotado (len == -1).
     */
    @Test
    public void testReadBloqueFinDeFlujoEstableceReadAgainEnEndOfStream() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a");
        char[] buf = new char[1];

        assertEquals(1, br.read(buf, 0, 1));
        assertEquals('a', br.readAgain());

        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.read(buf, 0, 1));
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.readAgain());
    }

    /**
     * Sobre un flujo vacío, read(char[], int, int) con longitud > 0 devuelve END_OF_STREAM
     * y deja readAgain() en END_OF_STREAM.
     */
    @Test
    public void testReadBloqueSobreFlujoVacioEstableceReadAgainEnEndOfStream() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("");
        char[] buf = new char[4];

        assertEquals(ExtendedBufferedReader.UNDEFINED, br.readAgain());
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.read(buf, 0, 4));
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.readAgain());
    }

    /**
     * readLine() que devuelve una línea vacía no modifica readAgain(), que conserva el último carácter leído previamente.
     */
    @Test
    public void testReadLineVaciaNoModificaReadAgain() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a\n\nb");

        assertEquals('a', br.read());
        assertEquals('a', br.readAgain());

        assertEquals("", br.readLine());
        assertEquals('a', br.readAgain());
        assertEquals(1, br.getLineNumber());

        assertEquals("b", br.readLine());
        assertEquals('b', br.readAgain());
        assertEquals(2, br.getLineNumber());
    }

    /**
     * readLine() establece readAgain() al último carácter de la línea leída, que nunca es CR ni LF.
     */
    @Test
    public void testReadLineEstableceReadAgainAlUltimoCaracterDeLaLinea() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("xy\nfoo\r\nbar");

        assertEquals("xy", br.readLine());
        assertEquals('y', br.readAgain());
        assertEquals(1, br.getLineNumber());

        assertEquals("foo", br.readLine());
        assertEquals('o', br.readAgain());
        assertEquals(2, br.getLineNumber());

        assertEquals("bar", br.readLine());
        assertEquals('r', br.readAgain());
        assertEquals(3, br.getLineNumber());
    }

    private ExtendedBufferedReader getBufferedReader(String s) {
        return new ExtendedBufferedReader(new StringReader(s));
    }
}
