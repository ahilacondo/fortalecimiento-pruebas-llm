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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.junit.Test;

/**
 * Pruebas que verifican el comportamiento documentado en el Javadoc de {@link CSVFormat}
 * y que no está cubierto por {@code CSVFormatTest}.
 */
public class CSVFormatLLMTest {

    // ------------------------------------------------------------------
    // Requisito 1: DEFAULT
    // ------------------------------------------------------------------

    @Test
    public void testDefaultUsesCommaDelimiter() {
        assertEquals(',', CSVFormat.DEFAULT.getDelimiter());
    }

    @Test
    public void testDefaultUsesDoubleQuoteAsQuoteCharacter() {
        assertEquals(Character.valueOf('"'), CSVFormat.DEFAULT.getQuoteCharacter());
    }

    @Test
    public void testDefaultUsesCrlfAsRecordSeparator() {
        assertEquals("\r\n", CSVFormat.DEFAULT.getRecordSeparator());
    }

    @Test
    public void testDefaultIgnoresEmptyLines() {
        assertTrue(CSVFormat.DEFAULT.getIgnoreEmptyLines());
    }

    // ------------------------------------------------------------------
    // Requisito 2: EXCEL
    // ------------------------------------------------------------------

    @Test
    public void testExcelUsesCommaDelimiter() {
        assertEquals(',', CSVFormat.EXCEL.getDelimiter());
    }

    @Test
    public void testExcelUsesDoubleQuoteAsQuoteCharacter() {
        assertEquals(Character.valueOf('"'), CSVFormat.EXCEL.getQuoteCharacter());
    }

    @Test
    public void testExcelUsesCrlfAsRecordSeparator() {
        assertEquals("\r\n", CSVFormat.EXCEL.getRecordSeparator());
    }

    @Test
    public void testExcelDoesNotIgnoreEmptyLines() {
        assertFalse(CSVFormat.EXCEL.getIgnoreEmptyLines());
    }

    @Test
    public void testExcelAllowsMissingColumnNames() {
        assertTrue(CSVFormat.EXCEL.getAllowMissingColumnNames());
    }

    // ------------------------------------------------------------------
    // Requisito 3: TDF
    // ------------------------------------------------------------------

    @Test
    public void testTdfUsesTabAsDelimiter() {
        assertEquals('\t', CSVFormat.TDF.getDelimiter());
    }

    @Test
    public void testTdfUsesDoubleQuoteAsQuoteCharacter() {
        assertEquals(Character.valueOf('"'), CSVFormat.TDF.getQuoteCharacter());
    }

    @Test
    public void testTdfUsesCrlfAsRecordSeparator() {
        assertEquals("\r\n", CSVFormat.TDF.getRecordSeparator());
    }

    @Test
    public void testTdfIgnoresSurroundingSpaces() {
        assertTrue(CSVFormat.TDF.getIgnoreSurroundingSpaces());
    }

    // ------------------------------------------------------------------
    // Requisito 4: MYSQL
    // ------------------------------------------------------------------

    @Test
    public void testMysqlUsesTabAsDelimiter() {
        assertEquals('\t', CSVFormat.MYSQL.getDelimiter());
    }

    @Test
    public void testMysqlHasNoQuoteCharacter() {
        assertNull(CSVFormat.MYSQL.getQuoteCharacter());
        assertFalse(CSVFormat.MYSQL.isQuoteCharacterSet());
    }

    @Test
    public void testMysqlUsesLfAsRecordSeparator() {
        assertEquals("\n", CSVFormat.MYSQL.getRecordSeparator());
    }

    @Test
    public void testMysqlDoesNotIgnoreEmptyLines() {
        assertFalse(CSVFormat.MYSQL.getIgnoreEmptyLines());
    }

    @Test
    public void testMysqlUsesBackslashAsEscapeCharacter() {
        assertEquals(Character.valueOf('\\'), CSVFormat.MYSQL.getEscapeCharacter());
        assertTrue(CSVFormat.MYSQL.isEscapeCharacterSet());
    }

    // ------------------------------------------------------------------
    // Requisito 5: withHeader() sin argumentos y withHeader(null)
    // ------------------------------------------------------------------

    @Test
    public void testWithHeaderNoArgumentsReturnsEmptyArray() {
        final String[] header = CSVFormat.DEFAULT.withHeader().getHeader();
        assertNotNull("La cabecera no debe ser null cuando se lee del origen", header);
        assertEquals(0, header.length);
    }

    @Test
    public void testWithHeaderNoArgumentsUsesFirstRowAsHeader() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withHeader();
        final List<CSVRecord> records = format.parse(new StringReader("Col1,Col2\na,b\n")).getRecords();

        assertEquals("La primera fila debe consumirse como cabecera", 1, records.size());
        assertEquals("a", records.get(0).get("Col1"));
        assertEquals("b", records.get(0).get("Col2"));
    }

    @Test
    public void testWithHeaderNullDisablesHeader() {
        final CSVFormat format = CSVFormat.DEFAULT.withHeader((String[]) null);
        assertNull(format.getHeader());
    }

    // ------------------------------------------------------------------
    // Requisito 6: withSkipHeaderRecord(true) al parsear
    // ------------------------------------------------------------------

    @Test
    public void testSkipHeaderRecordTrueOmitsFirstRecord() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT
                .withHeader("Col1", "Col2")
                .withSkipHeaderRecord(true);
        final List<CSVRecord> records = format.parse(new StringReader("Col1,Col2\na,b\n")).getRecords();

        assertEquals(1, records.size());
        assertEquals("a", records.get(0).get("Col1"));
    }

    @Test
    public void testSkipHeaderRecordFalseKeepsFirstRecord() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT
                .withHeader("Col1", "Col2")
                .withSkipHeaderRecord(false);
        final List<CSVRecord> records = format.parse(new StringReader("Col1,Col2\na,b\n")).getRecords();

        assertEquals(2, records.size());
        assertEquals("Col1", records.get(0).get("Col1"));
    }

    // ------------------------------------------------------------------
    // Requisito 7: newFormat(delimiter) y valores por defecto
    // ------------------------------------------------------------------

    @Test
    public void testNewFormatSetsDelimiter() {
        assertEquals('!', CSVFormat.newFormat('!').getDelimiter());
    }

    @Test
    public void testNewFormatLeavesQuoteSettingsUnset() {
        final CSVFormat format = CSVFormat.newFormat('!');
        assertNull(format.getQuoteCharacter());
        assertNull(format.getQuoteMode());
    }

    @Test
    public void testNewFormatLeavesCommentAndEscapeUnset() {
        final CSVFormat format = CSVFormat.newFormat('!');
        assertNull(format.getCommentMarker());
        assertNull(format.getEscapeCharacter());
    }

    @Test
    public void testNewFormatLeavesBooleanFlagsFalse() {
        final CSVFormat format = CSVFormat.newFormat('!');
        assertFalse(format.getIgnoreSurroundingSpaces());
        assertFalse(format.getIgnoreEmptyLines());
        assertFalse(format.getAllowMissingColumnNames());
        assertFalse(format.getSkipHeaderRecord());
    }

    @Test
    public void testNewFormatLeavesStringsAndHeaderUnset() {
        final CSVFormat format = CSVFormat.newFormat('!');
        assertNull(format.getRecordSeparator());
        assertNull(format.getNullString());
        assertNull(format.getHeader());
    }

    // ------------------------------------------------------------------
    // Requisito 8: withRecordSeparator debe validar el separador
    // ------------------------------------------------------------------

    @Test
    public void testRecordSeparatorAcceptsCrLfAndLf() {
        assertEquals("\r", CSVFormat.DEFAULT.withRecordSeparator("\r").getRecordSeparator());
        assertEquals("\n", CSVFormat.DEFAULT.withRecordSeparator("\n").getRecordSeparator());
        assertEquals("\r\n", CSVFormat.DEFAULT.withRecordSeparator("\r\n").getRecordSeparator());
    }

    /**
     * Según el Javadoc, withRecordSeparator(String) debe lanzar IllegalArgumentException
     * si el separador no es CR, LF ni CRLF. Esta prueba detecta que el código no lo valida.
     */
    @Test
    public void testRecordSeparatorRejectsNonLineBreakString() {
        try {
            CSVFormat.DEFAULT.withRecordSeparator("xyz");
            fail("Se esperaba IllegalArgumentException para un separador distinto de CR, LF o CRLF");
        } catch (final IllegalArgumentException expected) {
            // comportamiento documentado
        }
    }
}
