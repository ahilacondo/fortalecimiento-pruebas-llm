package org.apache.commons.csv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link CSVParser} que cubren fábricas, posición de lectura,
 * propagación de errores de E/S, copia del mapa de cabeceras, estado cerrado y formatos inconsistentes.
 */
public class CSVParserLLMTest {

    private static final String TWO_ROWS = "a,b\nc,d\n";

    /**
     * Reader que falla siempre al leer, para simular un error de E/S durante el parseo.
     */
    private static Reader failingReader() {
        return new Reader() {
            @Override
            public int read(final char[] cbuf, final int off, final int len) throws IOException {
                throw new IOException("simulated read failure");
            }

            @Override
            public void close() throws IOException {
                // nada que cerrar
            }
        };
    }

    // ---------------------------------------------------------------------
    // 1. parse(InputStream, Charset, CSVFormat)
    // ---------------------------------------------------------------------

    @Test
    public void testParseInputStreamReadsRecordsWithCharset() throws IOException {
        final byte[] data = "nombre,ciudad\nJos\u00e9,M\u00e1laga\n".getBytes(StandardCharsets.UTF_8);
        try (CSVParser parser = CSVParser.parse(new ByteArrayInputStream(data), StandardCharsets.UTF_8,
                CSVFormat.DEFAULT)) {
            final List<CSVRecord> records = parser.getRecords();
            assertEquals(2, records.size());
            assertEquals("nombre", records.get(0).get(0));
            assertEquals("Jos\u00e9", records.get(1).get(0));
            assertEquals("M\u00e1laga", records.get(1).get(1));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseInputStreamNullStreamThrowsIllegalArgumentException() throws IOException {
        CSVParser.parse((java.io.InputStream) null, StandardCharsets.UTF_8, CSVFormat.DEFAULT);
    }

    // ---------------------------------------------------------------------
    // 2. parse(Path, Charset, CSVFormat)
    // ---------------------------------------------------------------------

    @Test
    public void testParsePathReadsRecordsFromFile() throws IOException {
        final Path path = Files.createTempFile("csvparser-llm-", ".csv");
        try {
            Files.write(path, TWO_ROWS.getBytes(StandardCharsets.UTF_8));
            try (CSVParser parser = CSVParser.parse(path, StandardCharsets.UTF_8, CSVFormat.DEFAULT)) {
                final List<CSVRecord> records = parser.getRecords();
                assertEquals(2, records.size());
                assertEquals("a", records.get(0).get(0));
                assertEquals("b", records.get(0).get(1));
                assertEquals("c", records.get(1).get(0));
                assertEquals("d", records.get(1).get(1));
            }
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParsePathNullPathThrowsIllegalArgumentException() throws IOException {
        CSVParser.parse((Path) null, StandardCharsets.UTF_8, CSVFormat.DEFAULT);
    }

    // ---------------------------------------------------------------------
    // 3. parse(URL, Charset, CSVFormat) con url nula
    // ---------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testParseUrlNullUrlThrowsIllegalArgumentException() throws IOException {
        CSVParser.parse((URL) null, StandardCharsets.UTF_8, CSVFormat.DEFAULT);
    }

    // ---------------------------------------------------------------------
    // 4. getRecords() empieza en la posición actual
    // ---------------------------------------------------------------------

    @Test
    public void testGetRecordsStartsAtCurrentPositionAndSkipsConsumedRecords() throws IOException {
        try (CSVParser parser = CSVParser.parse("a,b\nc,d\ne,f\n", CSVFormat.DEFAULT)) {
            final Iterator<CSVRecord> iterator = parser.iterator();
            assertEquals("a", iterator.next().get(0));
            assertEquals(1, parser.getRecordNumber());

            final List<CSVRecord> rest = parser.getRecords();
            assertEquals(2, rest.size());
            assertEquals("c", rest.get(0).get(0));
            assertEquals("e", rest.get(1).get(0));
            assertEquals(3, parser.getRecordNumber());
        }
    }

    @Test
    public void testGetRecordsReturnsEmptyListWhenNoRecordsRemain() throws IOException {
        try (CSVParser parser = CSVParser.parse("a,b\n", CSVFormat.DEFAULT)) {
            final List<CSVRecord> all = parser.getRecords();
            assertEquals(1, all.size());

            final List<CSVRecord> again = parser.getRecords();
            assertTrue(again.isEmpty());
        }
    }

    // ---------------------------------------------------------------------
    // 5. Errores de E/S durante la iteración
    // ---------------------------------------------------------------------

    @Test
    public void testIteratorHasNextWrapsIOExceptionAsIllegalStateException() throws IOException {
        try (CSVParser parser = new CSVParser(failingReader(), CSVFormat.DEFAULT)) {
            final Iterator<CSVRecord> iterator = parser.iterator();
            try {
                iterator.hasNext();
                fail("Se esperaba IllegalStateException");
            } catch (final IllegalStateException e) {
                assertTrue(e.getCause() instanceof IOException);
            }
        }
    }

    @Test
    public void testIteratorNextWrapsIOExceptionAsIllegalStateException() throws IOException {
        try (CSVParser parser = new CSVParser(failingReader(), CSVFormat.DEFAULT)) {
            final Iterator<CSVRecord> iterator = parser.iterator();
            try {
                iterator.next();
                fail("Se esperaba IllegalStateException");
            } catch (final IllegalStateException e) {
                assertTrue(e.getCause() instanceof IOException);
            }
        }
    }

    @Test(expected = IOException.class)
    public void testGetRecordsPropagatesIOException() throws IOException {
        try (CSVParser parser = new CSVParser(failingReader(), CSVFormat.DEFAULT)) {
            parser.getRecords();
        }
    }

    // ---------------------------------------------------------------------
    // 6. getHeaderMap(): copia independiente e índices base 0
    // ---------------------------------------------------------------------

    @Test
    public void testGetHeaderMapValuesAreZeroBasedIndices() throws IOException {
        try (CSVParser parser = CSVParser.parse("a,b,c\n1,2,3\n", CSVFormat.DEFAULT.withHeader("A", "B", "C"))) {
            final Map<String, Integer> headerMap = parser.getHeaderMap();
            assertEquals(3, headerMap.size());
            assertEquals(Integer.valueOf(0), headerMap.get("A"));
            assertEquals(Integer.valueOf(1), headerMap.get("B"));
            assertEquals(Integer.valueOf(2), headerMap.get("C"));
        }
    }

    @Test
    public void testGetHeaderMapReturnsIndependentCopy() throws IOException {
        try (CSVParser parser = CSVParser.parse("a,b\n1,2\n", CSVFormat.DEFAULT.withHeader("A", "B"))) {
            final Map<String, Integer> copy = parser.getHeaderMap();
            copy.put("Z", Integer.valueOf(99));
            copy.remove("A");

            final Map<String, Integer> fresh = parser.getHeaderMap();
            assertNotSame(copy, fresh);
            assertEquals(2, fresh.size());
            assertEquals(Integer.valueOf(0), fresh.get("A"));
            assertEquals(Integer.valueOf(1), fresh.get("B"));
            assertFalse(fresh.containsKey("Z"));

            // El acceso a los registros por nombre no se ve afectado por la modificación de la copia
            final CSVRecord record = parser.iterator().next();
            assertEquals("1", record.get("A"));
            assertEquals("2", record.get("B"));
        }
    }

    @Test
    public void testGetHeaderMapIsNullWithoutHeader() throws IOException {
        try (CSVParser parser = CSVParser.parse("a,b\n", CSVFormat.DEFAULT)) {
            assertNull(parser.getHeaderMap());
        }
    }

    // ---------------------------------------------------------------------
    // 7. isClosed()
    // ---------------------------------------------------------------------

    @Test
    public void testIsClosedFalseBeforeCloseAndTrueAfterClose() throws IOException {
        final CSVParser parser = CSVParser.parse("a,b\n", CSVFormat.DEFAULT);
        assertFalse(parser.isClosed());
        parser.close();
        assertTrue(parser.isClosed());
    }

    // ---------------------------------------------------------------------
    // 8. Formato inconsistente -> IllegalArgumentException
    // ---------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testParseStringWithDuplicateHeaderFormatThrowsIllegalArgumentException() throws IOException {
        CSVParser.parse("a,b\n1,2\n", CSVFormat.DEFAULT.withHeader("X", "X"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseReaderWithDuplicateHeaderFormatThrowsIllegalArgumentException() throws IOException {
        CSVParser.parse(new StringReader("a,b\n1,2\n"), CSVFormat.DEFAULT.withHeader("X", "X"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseInputStreamWithDuplicateHeaderFormatThrowsIllegalArgumentException() throws IOException {
        final byte[] data = "a,b\n1,2\n".getBytes(StandardCharsets.UTF_8);
        CSVParser.parse(new ByteArrayInputStream(data), StandardCharsets.UTF_8,
                CSVFormat.DEFAULT.withHeader("X", "X"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithDuplicateHeaderFormatThrowsIllegalArgumentException() throws IOException {
        new CSVParser(new StringReader("a,b\n1,2\n"), CSVFormat.DEFAULT.withHeader("X", "X"));
    }
}
