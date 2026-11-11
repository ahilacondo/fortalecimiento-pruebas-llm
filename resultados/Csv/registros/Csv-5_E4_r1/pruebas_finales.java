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

package org.apache.commons.csv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.Flushable;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link CSVPrinter} que cubren los requisitos de documentación
 * no verificados por {@link CSVPrinterTest}. Solo usan la API pública.
 */
public class CSVPrinterLLMTest {

    /** Appendable que además es Flushable y cuenta las llamadas a flush(). */
    private static final class FlushableRecorder implements Appendable, Flushable {
        private final StringBuilder sb = new StringBuilder();
        private int flushCount;

        @Override
        public Appendable append(final CharSequence csq) {
            sb.append(csq);
            return this;
        }

        @Override
        public Appendable append(final CharSequence csq, final int start, final int end) {
            sb.append(csq, start, end);
            return this;
        }

        @Override
        public Appendable append(final char c) {
            sb.append(c);
            return this;
        }

        @Override
        public void flush() {
            flushCount++;
        }
    }

    /**
     * Crea un ResultSet simulado mediante un Proxy. Las filas se proporcionan como arrays de String
     * (null representa un valor SQL NULL).
     */
    private static ResultSet resultSetOf(final int columnCount, final String[]... rows) {
        final int[] cursor = { -1 };

        final ResultSetMetaData meta = (ResultSetMetaData) Proxy.newProxyInstance(
                CSVPrinterLLMTest.class.getClassLoader(),
                new Class<?>[] { ResultSetMetaData.class },
                (proxy, method, args) -> {
                    if ("getColumnCount".equals(method.getName())) {
                        return columnCount;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        return (ResultSet) Proxy.newProxyInstance(
                CSVPrinterLLMTest.class.getClassLoader(),
                new Class<?>[] { ResultSet.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                    case "getMetaData":
                        return meta;
                    case "next":
                        cursor[0]++;
                        return cursor[0] < rows.length;
                    case "getString":
                        return rows[cursor[0]][((Integer) args[0]) - 1];
                    case "close":
                        return null;
                    default:
                        throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    // ------------------------------------------------------------------
    // Requisito 1: flush() delega en Flushable y no falla si no lo es
    // ------------------------------------------------------------------

    @Test
    public void testFlushDelegatesToFlushableAppendable() throws IOException {
        final FlushableRecorder out = new FlushableRecorder();
        final CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT);

        assertEquals(0, out.flushCount);
        printer.flush();
        assertEquals(1, out.flushCount);
        printer.flush();
        assertEquals(2, out.flushCount);
    }

    @Test
    public void testFlushDoesNotFailWhenAppendableIsNotFlushable() throws IOException {
        final StringBuilder sb = new StringBuilder();
        final CSVPrinter printer = new CSVPrinter(sb, CSVFormat.DEFAULT);

        printer.flush();

        assertEquals("", sb.toString());
    }

    // ------------------------------------------------------------------
    // Requisito 2: getOut() devuelve el mismo Appendable del constructor
    // ------------------------------------------------------------------

    @Test
    public void testGetOutReturnsSameStringWriter() {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);

        assertSame(sw, printer.getOut());
    }

    @Test
    public void testGetOutReturnsSameInstanceForNonStandardAppendable() {
        final StringBuilder sb = new StringBuilder();
        final CSVPrinter printer = new CSVPrinter(sb, CSVFormat.EXCEL);

        assertSame(sb, printer.getOut());
    }

    // ------------------------------------------------------------------
    // Requisito 3: printComment inicia nueva línea si no estamos al inicio de registro
    // ------------------------------------------------------------------

    @Test
    public void testPrintCommentStartsNewLineWhenMidRecord() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withCommentStart('#');
        final String rs = format.getRecordSeparator();
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, format);

        printer.print("a");
        printer.printComment("x");

        assertEquals("a" + rs + "# x" + rs, sw.toString());
    }

    @Test
    public void testPrintCommentAtRecordStartDoesNotAddExtraNewLine() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withCommentStart('#');
        final String rs = format.getRecordSeparator();
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, format);

        printer.printComment("x");

        assertEquals("# x" + rs, sw.toString());
    }

    // ------------------------------------------------------------------
    // Requisito 4: CR y CRLF dentro del comentario son saltos de línea
    // ------------------------------------------------------------------

    @Test
    public void testPrintCommentTreatsCrAndCrLfAsLineBreaks() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withCommentStart('#');
        final String rs = format.getRecordSeparator();
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, format);

        printer.printComment("uno\rdos\r\ntres\nfin");

        assertEquals("# uno" + rs + "# dos" + rs + "# tres" + rs + "# fin" + rs, sw.toString());
    }

    @Test
    public void testPrintCommentCrLfIsSingleLineBreak() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withCommentStart('#');
        final String rs = format.getRecordSeparator();
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, format);

        printer.printComment("a\r\nb");

        assertEquals("# a" + rs + "# b" + rs, sw.toString());
    }

    // ------------------------------------------------------------------
    // Requisito 5: printRecords(ResultSet) imprime NULL según print y no emite nada si está vacío
    // ------------------------------------------------------------------

    @Test
    public void testPrintRecordsResultSetWritesNullAsEmptyString() throws IOException, SQLException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);

        printer.printRecords(resultSetOf(2, new String[] { "1", null }, new String[] { "2", "b" }));

        final String rs = CSVFormat.DEFAULT.getRecordSeparator();
        assertEquals("1," + rs + "2,b" + rs, sw.toString());
    }

    @Test
    public void testPrintRecordsResultSetWritesNullAsNullString() throws IOException, SQLException {
        final CSVFormat format = CSVFormat.DEFAULT.withNullString("NULL");
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, format);

        printer.printRecords(resultSetOf(2, new String[] { "1", null }, new String[] { null, "b" }));

        final String rs = format.getRecordSeparator();
        assertEquals("1,NULL" + rs + "NULL,b" + rs, sw.toString());
    }

    @Test
    public void testPrintRecordsEmptyResultSetPrintsNothing() throws IOException, SQLException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);

        printer.printRecords(resultSetOf(2));

        assertEquals("", sw.toString());
    }

    // ------------------------------------------------------------------
    // Requisito 6: printRecords(Iterable) y printRecords(Object[]) imprimen escalares como registros de un campo
    // ------------------------------------------------------------------

    @Test
    public void testPrintRecordsIterableScalarsAreSingleFieldRecords() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);

        printer.printRecords(Arrays.asList("a", 1, "b c"));

        final String rs = CSVFormat.DEFAULT.getRecordSeparator();
        assertEquals("a" + rs + "1" + rs + "b c" + rs, sw.toString());
    }

    @Test
    public void testPrintRecordsObjectArrayScalarsAreSingleFieldRecords() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);

        printer.printRecords(new Object[] { "a", 1, "b c" });

        final String rs = CSVFormat.DEFAULT.getRecordSeparator();
        assertEquals("a" + rs + "1" + rs + "b c" + rs, sw.toString());
    }

    @Test
    public void testPrintRecordsObjectArrayMixesArraysAndScalars() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);

        printer.printRecords(new Object[] { new String[] { "x", "y" }, "z" });

        final String rs = CSVFormat.DEFAULT.getRecordSeparator();
        assertEquals("x,y" + rs + "z" + rs, sw.toString());
    }

    // ------------------------------------------------------------------
    // Requisito 7: println() emite el separador configurado y restablece el estado de nuevo registro
    // ------------------------------------------------------------------

    @Test
    public void testPrintlnEmitsCustomRecordSeparator() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withRecordSeparator("\n");
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, format);

        printer.println();

        assertEquals("\n", sw.toString());
    }

    @Test
    public void testPrintlnResetsNewRecordState() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withRecordSeparator("\n");
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, format);

        printer.print("a");
        printer.println();
        printer.print("b");

        // Tras println no debe añadirse delimitador antes de "b".
        assertEquals("a\nb", sw.toString());
    }

    @Test
    public void testPrintRecordUsesCustomRecordSeparatorAtEnd() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withRecordSeparator("|");
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, format);

        printer.printRecord("a", "b");

        assertEquals("a,b|", sw.toString());
    }

    // ------------------------------------------------------------------
    // Requisito 8: formatos híbridos (encapsulación y escape con caracteres distintos)
    // ------------------------------------------------------------------

    @Test
    public void testConstructorRejectsHybridFormat() {
        // Encapsulación con '"' y escape con '\\' simultáneamente: formato híbrido no soportado.
        final CSVFormat hybrid = CSVFormat.DEFAULT.withEscape('\\');
        try {
            new CSVPrinter(new StringWriter(), hybrid);
            fail("Se esperaba IllegalArgumentException para un formato híbrido");
        } catch (final IllegalArgumentException expected) {
            // comportamiento esperado
        }
    }

    @Test
    public void testConstructorAcceptsPureEscapingFormat() throws IOException {
        final CSVFormat escaping = CSVFormat.DEFAULT.withQuoteChar(null).withEscape('\\');
        final StringWriter sw = new StringWriter();

        final CSVPrinter printer = new CSVPrinter(sw, escaping);
        assertNotNull(printer);
        printer.print("a,b");

        assertEquals("a\\,b", sw.toString());
    }

    @Test
    public void testConstructorAcceptsPureEncapsulationFormat() throws IOException {
        final StringWriter sw = new StringWriter();

        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        assertNotNull(printer);
        printer.print("a,b");

        assertEquals("\"a,b\"", sw.toString());
    }
}
