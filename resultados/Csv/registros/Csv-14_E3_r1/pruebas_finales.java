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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link CSVFormat} que cubren comportamientos no verificados por {@code CSVFormatTest}.
 * Solo utilizan la API pública de la clase.
 */
public class CSVFormatLLMTest {

    private static final String CRLF_SEP = "\r\n";

    // ------------------------------------------------------------------
    // 1. equals / hashCode ignoran ciertos atributos
    // ------------------------------------------------------------------

    @Test
    public void testEqualsIgnoresIgnoreHeaderCase() {
        final CSVFormat withCase = CSVFormat.DEFAULT.withIgnoreHeaderCase(true);
        assertEquals(CSVFormat.DEFAULT, withCase);
        assertEquals(withCase, CSVFormat.DEFAULT);
    }

    @Test
    public void testEqualsIgnoresTrim() {
        final CSVFormat withTrim = CSVFormat.DEFAULT.withTrim(true);
        assertEquals(CSVFormat.DEFAULT, withTrim);
        assertEquals(withTrim, CSVFormat.DEFAULT);
    }

    @Test
    public void testEqualsIgnoresTrailingDelimiter() {
        final CSVFormat withTrailing = CSVFormat.DEFAULT.withTrailingDelimiter(true);
        assertEquals(CSVFormat.DEFAULT, withTrailing);
        assertEquals(withTrailing, CSVFormat.DEFAULT);
    }

    @Test
    public void testEqualsIgnoresAllowMissingColumnNames() {
        final CSVFormat allowMissing = CSVFormat.DEFAULT.withAllowMissingColumnNames(true);
        assertEquals(CSVFormat.DEFAULT, allowMissing);
        assertEquals(allowMissing, CSVFormat.DEFAULT);
    }

    @Test
    public void testEqualsIgnoresHeaderComments() {
        final CSVFormat withComments = CSVFormat.DEFAULT.withHeaderComments("comentario");
        assertEquals(CSVFormat.DEFAULT, withComments);
        assertEquals(withComments, CSVFormat.DEFAULT);
    }

    @Test
    public void testHashCodeIgnoresTrimTrailingDelimiterAllowMissingAndHeaderComments() {
        assertEquals(CSVFormat.DEFAULT.hashCode(), CSVFormat.DEFAULT.withTrim(true).hashCode());
        assertEquals(CSVFormat.DEFAULT.hashCode(), CSVFormat.DEFAULT.withTrailingDelimiter(true).hashCode());
        assertEquals(CSVFormat.DEFAULT.hashCode(), CSVFormat.DEFAULT.withAllowMissingColumnNames(true).hashCode());
        assertEquals(CSVFormat.DEFAULT.hashCode(), CSVFormat.DEFAULT.withHeaderComments("x").hashCode());
    }

    // ------------------------------------------------------------------
    // 2. format() recorta espacios y saltos de línea sobrantes
    // ------------------------------------------------------------------

    @Test
    public void testFormatTrimsSurroundingWhitespaceOfOutput() {
        // Sin encapsulado: los espacios iniciales de la salida deben eliminarse por trim()
        final CSVFormat format = CSVFormat.newFormat(',')
                .withEscape('\\')
                .withQuoteMode(QuoteMode.NONE)
                .withRecordSeparator(CRLF_SEP);
        assertEquals("a,b", format.format(" a", "b"));
    }

    @Test
    public void testFormatRemovesTrailingRecordSeparator() {
        final String result = CSVFormat.DEFAULT.format("a", "b");
        assertEquals("a,b", result);
        assertFalse(result.endsWith("\r\n"));
        assertFalse(result.endsWith("\n"));
    }

    // ------------------------------------------------------------------
    // 3. Modos de cita ALL y NON_NUMERIC al imprimir
    // ------------------------------------------------------------------

    @Test
    public void testQuoteModeAllQuotesEveryValue() {
        final CSVFormat format = CSVFormat.DEFAULT.withQuoteMode(QuoteMode.ALL);
        assertEquals("\"a\",\"1\"", format.format("a", 1));
    }

    @Test
    public void testQuoteModeAllQuotesNumbersAndStrings() {
        final CSVFormat format = CSVFormat.DEFAULT.withQuoteMode(QuoteMode.ALL);
        assertEquals("\"x\",\"2.5\",\"\"", format.format("x", 2.5, ""));
    }

    @Test
    public void testQuoteModeNonNumericQuotesOnlyNonNumbers() {
        final CSVFormat format = CSVFormat.DEFAULT.withQuoteMode(QuoteMode.NON_NUMERIC);
        assertEquals("\"a\",1,2.5", format.format("a", 1, 2.5));
    }

    @Test
    public void testQuoteModeNonNumericQuotesStringThatLooksNumeric() {
        final CSVFormat format = CSVFormat.DEFAULT.withQuoteMode(QuoteMode.NON_NUMERIC);
        // Un String "1" no es Number, por lo que se encapsula
        assertEquals("\"1\"", format.format("1"));
    }

    // ------------------------------------------------------------------
    // 4. Encapsulado de valores con comillas, delimitador, saltos y espacios
    // ------------------------------------------------------------------

    @Test
    public void testPrintDoublesEmbeddedQuote() {
        assertEquals("\"a\"\"b\"", CSVFormat.DEFAULT.format("a\"b"));
    }

    @Test
    public void testPrintQuotesValueWithDelimiter() {
        assertEquals("\"x,y\"", CSVFormat.DEFAULT.format("x,y"));
    }

    @Test
    public void testPrintQuotesValueWithLineFeed() {
        assertEquals("\"a\nb\"", CSVFormat.DEFAULT.format("a\nb"));
    }

    @Test
    public void testPrintQuotesValueWithCarriageReturn() {
        assertEquals("\"a\rb\"", CSVFormat.DEFAULT.format("a\rb"));
    }

    @Test
    public void testPrintQuotesValueWithLeadingSpace() {
        assertEquals("\" a\"", CSVFormat.DEFAULT.format(" a"));
    }

    @Test
    public void testPrintQuotesValueWithTrailingSpace() {
        assertEquals("\"a \"", CSVFormat.DEFAULT.format("a "));
    }

    @Test
    public void testPrintDoesNotQuoteSimpleValue() {
        assertEquals("abc,def", CSVFormat.DEFAULT.format("abc", "def"));
    }

    // ------------------------------------------------------------------
    // 5. Modo NONE con escape
    // ------------------------------------------------------------------

    @Test
    public void testQuoteModeNoneWithEscapeEscapesSpecialCharacters() {
        final CSVFormat format = CSVFormat.newFormat(',')
                .withEscape('\\')
                .withQuoteMode(QuoteMode.NONE);
        // delimitador -> \, LF -> \n, CR -> \r, escape -> \\
        final String result = format.format("a,b", "c\nd", "e\rf", "g\\h");
        assertEquals("a\\,b,c\\nd,e\\rf,g\\\\h", result);
    }

    @Test
    public void testQuoteModeNoneWithEscapeLeavesPlainValuesUntouched() {
        final CSVFormat format = CSVFormat.newFormat(',')
                .withEscape('\\')
                .withQuoteMode(QuoteMode.NONE);
        assertEquals("abc,def", format.format("abc", "def"));
    }

    // ------------------------------------------------------------------
    // 6. withRecordSeparator(String) no valida el separador
    // ------------------------------------------------------------------

    /**
     * La documentación indica que se lanza IllegalArgumentException si el separador no es CR, LF o CRLF,
     * pero el código actual no lo valida. Esta prueba documenta el comportamiento real: no lanza excepción
     * y conserva el valor. Esta discrepancia debe revisarse.
     */
    @Test
    public void testWithRecordSeparatorAcceptsArbitraryStringDocumentedBehaviorDiffers() {
        final CSVFormat format = CSVFormat.DEFAULT.withRecordSeparator("xyz");
        assertEquals("xyz", format.getRecordSeparator());
    }

    @Test
    public void testWithRecordSeparatorAcceptsCrLfString() {
        assertEquals(CRLF_SEP, CSVFormat.DEFAULT.withRecordSeparator(CRLF_SEP).getRecordSeparator());
    }

    // ------------------------------------------------------------------
    // 7. withTrim / getTrim / withTrailingDelimiter / println / nullString al escribir
    // ------------------------------------------------------------------

    @Test
    public void testGetTrimDefaultsToFalse() {
        assertFalse(CSVFormat.DEFAULT.getTrim());
    }

    @Test
    public void testWithTrimSetsAndUnsetsTrim() {
        assertTrue(CSVFormat.DEFAULT.withTrim().getTrim());
        assertTrue(CSVFormat.DEFAULT.withTrim(true).getTrim());
        assertFalse(CSVFormat.DEFAULT.withTrim(false).getTrim());
    }

    @Test
    public void testFormatWithTrimRemovesSurroundingBlanksFromValues() {
        assertEquals("a,b", CSVFormat.DEFAULT.withTrim().format(" a ", " b "));
    }

    @Test
    public void testFormatWithoutTrimKeepsValueQuotedWithBlanks() {
        assertEquals("\" a \",\" b \"", CSVFormat.DEFAULT.format(" a ", " b "));
    }

    @Test
    public void testGetTrailingDelimiterDefaultsToFalse() {
        assertFalse(CSVFormat.DEFAULT.getTrailingDelimiter());
        assertTrue(CSVFormat.DEFAULT.withTrailingDelimiter().getTrailingDelimiter());
        assertFalse(CSVFormat.DEFAULT.withTrailingDelimiter(false).getTrailingDelimiter());
    }

    @Test
    public void testFormatWithTrailingDelimiterAddsDelimiterAtEnd() {
        assertEquals("a,b,", CSVFormat.DEFAULT.withTrailingDelimiter().format("a", "b"));
    }

    @Test
    public void testPrintlnAppendsRecordSeparatorOnly() throws IOException {
        final StringBuilder sb = new StringBuilder();
        CSVFormat.DEFAULT.println(sb);
        assertEquals(CRLF_SEP, sb.toString());
    }

    @Test
    public void testPrintlnAppendsTrailingDelimiterBeforeRecordSeparator() throws IOException {
        final StringBuilder sb = new StringBuilder();
        CSVFormat.DEFAULT.withTrailingDelimiter().println(sb);
        assertEquals("," + CRLF_SEP, sb.toString());
    }

    @Test
    public void testPrintlnWithoutRecordSeparatorAppendsNothing() throws IOException {
        final StringBuilder sb = new StringBuilder();
        CSVFormat.newFormat(',').println(sb);
        assertEquals("", sb.toString());
    }

    @Test
    public void testPrintNullWritesNullStringWhenDefined() {
        assertEquals("a,NULL,b", CSVFormat.DEFAULT.withNullString("NULL").format("a", null, "b"));
    }

    @Test
    public void testPrintNullWritesEmptyWhenNullStringNotDefined() {
        assertEquals("a,,b", CSVFormat.DEFAULT.format("a", null, "b"));
    }

    @Test
    public void testPrintNullWritesNullStringUnquoted() {
        // El nullString no debe encapsularse aunque contenga caracteres que lo requerirían
        assertEquals("a,N/A,b", CSVFormat.DEFAULT.withNullString("N/A").format("a", null, "b"));
    }

    // ------------------------------------------------------------------
    // 8. toString, header comments, inmutabilidad y formatos predefinidos
    // ------------------------------------------------------------------

    @Test
    public void testToStringDefaultContainsExpectedParts() {
        final String s = CSVFormat.DEFAULT.toString();
        assertTrue(s.contains("Delimiter=<,>"));
        assertTrue(s.contains("QuoteChar=<\">"));
        assertTrue(s.contains("EmptyLines:ignored"));
        assertTrue(s.contains("SkipHeaderRecord:false"));
        assertFalse(s.contains("Escape=<"));
        assertFalse(s.contains("CommentStart=<"));
        assertFalse(s.contains("Header:"));
    }

    @Test
    public void testToStringTdfContainsTabDelimiterAndSurroundingSpaces() {
        final String s = CSVFormat.TDF.toString();
        assertTrue(s.contains("Delimiter=<\t>"));
        assertTrue(s.contains("SurroundingSpaces:ignored"));
    }

    @Test
    public void testToStringIncludesHeaderAndEscape() {
        final String s = CSVFormat.DEFAULT.withEscape('\\').withHeader("A", "B").toString();
        assertTrue(s.contains("Escape=<\\>"));
        assertTrue(s.contains("Header:[A, B]"));
    }

    @Test
    public void testHeaderCommentsNullByDefault() {
        assertNull(CSVFormat.DEFAULT.getHeaderComments());
    }

    @Test
    public void testWithHeaderCommentsStoresValuesAsStrings() {
        final CSVFormat format = CSVFormat.DEFAULT.withHeaderComments("a", 1, null);
        assertArrayEquals(new String[] { "a", "1", null }, format.getHeaderComments());
    }

    @Test
    public void testGetHeaderCommentsReturnsCopy() {
        final CSVFormat format = CSVFormat.DEFAULT.withHeaderComments("a", "b");
        final String[] copy = format.getHeaderComments();
        copy[0] = "modificado";
        assertArrayEquals(new String[] { "a", "b" }, format.getHeaderComments());
        assertNotSame(copy, format.getHeaderComments());
    }

    @Test
    public void testWithMethodsDoNotMutateOriginalFormat() {
        final CSVFormat base = CSVFormat.DEFAULT;
        final CSVFormat derived = base.withDelimiter(';')
                .withQuote('\'')
                .withNullString("NA")
                .withHeader("A", "B")
                .withTrim()
                .withTrailingDelimiter()
                .withIgnoreHeaderCase()
                .withSkipHeaderRecord()
                .withCommentMarker('#')
                .withEscape('\\')
                .withQuoteMode(QuoteMode.ALL)
                .withRecordSeparator(LFChar());

        // El formato original no debe cambiar
        assertEquals(',', base.getDelimiter());
        assertEquals(Character.valueOf('"'), base.getQuoteCharacter());
        assertNull(base.getNullString());
        assertNull(base.getHeader());
        assertFalse(base.getTrim());
        assertFalse(base.getTrailingDelimiter());
        assertFalse(base.getIgnoreHeaderCase());
        assertFalse(base.getSkipHeaderRecord());
        assertNull(base.getCommentMarker());
        assertNull(base.getEscapeCharacter());
        assertNull(base.getQuoteMode());
        assertEquals(CRLF_SEP, base.getRecordSeparator());

        // El derivado sí refleja los cambios
        assertEquals(';', derived.getDelimiter());
        assertEquals("NA", derived.getNullString());
        assertNotEquals(base, derived);
    }

    private static char LFChar() {
        return '\n';
    }

    @Test
    public void testPredefinedExcelConfiguration() {
        final CSVFormat excel = CSVFormat.EXCEL;
        assertEquals(',', excel.getDelimiter());
        assertEquals(Character.valueOf('"'), excel.getQuoteCharacter());
        assertEquals(CRLF_SEP, excel.getRecordSeparator());
        assertFalse(excel.getIgnoreEmptyLines());
        assertTrue(excel.getAllowMissingColumnNames());
        assertFalse(excel.getIgnoreSurroundingSpaces());
        assertNull(excel.getEscapeCharacter());
        assertNull(excel.getCommentMarker());
        assertNull(excel.getNullString());
        assertNull(excel.getHeader());
        assertFalse(excel.getSkipHeaderRecord());
    }

    @Test
    public void testPredefinedMySqlConfiguration() {
        final CSVFormat mysql = CSVFormat.MYSQL;
        assertEquals('\t', mysql.getDelimiter());
        assertNull(mysql.getQuoteCharacter());
        assertEquals(Character.valueOf('\\'), mysql.getEscapeCharacter());
        assertEquals("\n", mysql.getRecordSeparator());
        assertFalse(mysql.getIgnoreEmptyLines());
        assertEquals("\\N", mysql.getNullString());
    }

    @Test
    public void testPredefinedMySqlFormatWritesNullAsNullString() {
        assertEquals("a\t\\N", CSVFormat.MYSQL.format("a", null));
    }

    @Test
    public void testPredefinedTdfConfiguration() {
        final CSVFormat tdf = CSVFormat.TDF;
        assertEquals('\t', tdf.getDelimiter());
        assertEquals(Character.valueOf('"'), tdf.getQuoteCharacter());
        assertEquals(CRLF_SEP, tdf.getRecordSeparator());
        assertTrue(tdf.getIgnoreSurroundingSpaces());
        assertTrue(tdf.getIgnoreEmptyLines());
        assertNull(tdf.getEscapeCharacter());
        assertNull(tdf.getNullString());
    }

    @Test
    public void testPredefinedInformixUnloadConfiguration() {
        final CSVFormat informix = CSVFormat.INFORMIX_UNLOAD;
        assertEquals('|', informix.getDelimiter());
        assertEquals(Character.valueOf('"'), informix.getQuoteCharacter());
        assertEquals(Character.valueOf('\\'), informix.getEscapeCharacter());
        assertEquals("\n", informix.getRecordSeparator());
        assertTrue(informix.getIgnoreEmptyLines());
        assertNull(informix.getNullString());
    }

    @Test
    public void testPredefinedInformixUnloadCsvConfiguration() {
        final CSVFormat informixCsv = CSVFormat.INFORMIX_UNLOAD_CSV;
        assertEquals(',', informixCsv.getDelimiter());
        assertEquals(Character.valueOf('"'), informixCsv.getQuoteCharacter());
        assertNull(informixCsv.getEscapeCharacter());
        assertEquals("\n", informixCsv.getRecordSeparator());
    }

    @Test
    public void testPredefinedEnumReturnsSameInstances() {
        assertSame(CSVFormat.DEFAULT, CSVFormat.Predefined.Default.getFormat());
        assertSame(CSVFormat.EXCEL, CSVFormat.Predefined.Excel.getFormat());
        assertSame(CSVFormat.MYSQL, CSVFormat.Predefined.MySQL.getFormat());
        assertSame(CSVFormat.TDF, CSVFormat.Predefined.TDF.getFormat());
        assertSame(CSVFormat.INFORMIX_UNLOAD, CSVFormat.Predefined.InformixUnload.getFormat());
        assertSame(CSVFormat.INFORMIX_UNLOAD_CSV, CSVFormat.Predefined.InformixUnloadCsv.getFormat());
    }
}
