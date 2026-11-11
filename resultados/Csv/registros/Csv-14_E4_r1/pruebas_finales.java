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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/**
 * Pruebas adicionales de {@link CSVFormat} derivadas de su documentación (Javadoc).
 *
 * <p>
 * Nota: los tests {@code informixUnloadDelimiterIsComma} y
 * {@code withRecordSeparatorRejectsInvalidSeparator} verifican el comportamiento documentado.
 * Con la implementación actual de {@link CSVFormat} fallan a propósito, porque el código usa PIPE
 * en lugar de coma en INFORMIX_UNLOAD y no valida el separador de registro en withRecordSeparator(String).
 * </p>
 */
public class CSVFormatLLMTest {

    private static final String CRLF_STR = "\r\n";
    private static final String LF_STR = "\n";
    private static final String CR_STR = "\r";

    // ------------------------------------------------------------------
    // Requisito 1: formatos predefinidos y valueOf
    // ------------------------------------------------------------------

    @Test
    public void predefinedDefaultReturnsDefaultFormat() {
        assertSame(CSVFormat.DEFAULT, CSVFormat.Predefined.Default.getFormat());
    }

    @Test
    public void predefinedExcelReturnsExcelFormat() {
        assertSame(CSVFormat.EXCEL, CSVFormat.Predefined.Excel.getFormat());
    }

    @Test
    public void predefinedMySqlReturnsMySqlFormat() {
        assertSame(CSVFormat.MYSQL, CSVFormat.Predefined.MySQL.getFormat());
    }

    @Test
    public void predefinedRfc4180ReturnsRfc4180Format() {
        assertSame(CSVFormat.RFC4180, CSVFormat.Predefined.RFC4180.getFormat());
    }

    @Test
    public void predefinedTdfReturnsTdfFormat() {
        assertSame(CSVFormat.TDF, CSVFormat.Predefined.TDF.getFormat());
    }

    @Test
    public void valueOfDefaultNameReturnsDefaultFormat() {
        assertSame(CSVFormat.DEFAULT, CSVFormat.valueOf("Default"));
    }

    @Test
    public void valueOfExcelNameReturnsExcelFormat() {
        assertSame(CSVFormat.EXCEL, CSVFormat.valueOf("Excel"));
    }

    @Test
    public void valueOfMySqlNameReturnsMySqlFormat() {
        assertSame(CSVFormat.MYSQL, CSVFormat.valueOf("MySQL"));
    }

    @Test
    public void valueOfRfc4180NameReturnsRfc4180Format() {
        assertSame(CSVFormat.RFC4180, CSVFormat.valueOf("RFC4180"));
    }

    @Test
    public void valueOfTdfNameReturnsTdfFormat() {
        assertSame(CSVFormat.TDF, CSVFormat.valueOf("TDF"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void valueOfUnknownNameThrowsIllegalArgumentException() {
        CSVFormat.valueOf("NoExisteEsteFormato");
    }

    // ------------------------------------------------------------------
    // Requisito 2: DEFAULT
    // ------------------------------------------------------------------

    @Test
    public void defaultUsesComma() {
        assertEquals(',', CSVFormat.DEFAULT.getDelimiter());
    }

    @Test
    public void defaultUsesDoubleQuote() {
        assertEquals(Character.valueOf('"'), CSVFormat.DEFAULT.getQuoteCharacter());
    }

    @Test
    public void defaultUsesCrLfRecordSeparator() {
        assertEquals(CRLF_STR, CSVFormat.DEFAULT.getRecordSeparator());
    }

    @Test
    public void defaultIgnoresEmptyLines() {
        assertTrue(CSVFormat.DEFAULT.getIgnoreEmptyLines());
    }

    // ------------------------------------------------------------------
    // Requisito 3: EXCEL
    // ------------------------------------------------------------------

    @Test
    public void excelUsesComma() {
        assertEquals(',', CSVFormat.EXCEL.getDelimiter());
    }

    @Test
    public void excelUsesDoubleQuote() {
        assertEquals(Character.valueOf('"'), CSVFormat.EXCEL.getQuoteCharacter());
    }

    @Test
    public void excelUsesCrLfRecordSeparator() {
        assertEquals(CRLF_STR, CSVFormat.EXCEL.getRecordSeparator());
    }

    @Test
    public void excelDoesNotIgnoreEmptyLines() {
        assertFalse(CSVFormat.EXCEL.getIgnoreEmptyLines());
    }

    @Test
    public void excelAllowsMissingColumnNames() {
        assertTrue(CSVFormat.EXCEL.getAllowMissingColumnNames());
    }

    // ------------------------------------------------------------------
    // Requisito 4: MYSQL
    // ------------------------------------------------------------------

    @Test
    public void mySqlUsesTabDelimiter() {
        assertEquals('\t', CSVFormat.MYSQL.getDelimiter());
    }

    @Test
    public void mySqlHasNoQuoteCharacter() {
        assertNull(CSVFormat.MYSQL.getQuoteCharacter());
        assertFalse(CSVFormat.MYSQL.isQuoteCharacterSet());
    }

    @Test
    public void mySqlUsesBackslashEscape() {
        assertEquals(Character.valueOf('\\'), CSVFormat.MYSQL.getEscapeCharacter());
    }

    @Test
    public void mySqlUsesLfRecordSeparator() {
        assertEquals(LF_STR, CSVFormat.MYSQL.getRecordSeparator());
    }

    @Test
    public void mySqlDoesNotIgnoreEmptyLines() {
        assertFalse(CSVFormat.MYSQL.getIgnoreEmptyLines());
    }

    @Test
    public void mySqlUsesBackslashNWithNullString() {
        assertEquals("\\N", CSVFormat.MYSQL.getNullString());
        assertTrue(CSVFormat.MYSQL.isNullStringSet());
    }

    // ------------------------------------------------------------------
    // Requisito 5: TDF
    // ------------------------------------------------------------------

    @Test
    public void tdfUsesTabDelimiter() {
        assertEquals('\t', CSVFormat.TDF.getDelimiter());
    }

    @Test
    public void tdfUsesDoubleQuote() {
        assertEquals(Character.valueOf('"'), CSVFormat.TDF.getQuoteCharacter());
    }

    @Test
    public void tdfUsesCrLfRecordSeparator() {
        assertEquals(CRLF_STR, CSVFormat.TDF.getRecordSeparator());
    }

    @Test
    public void tdfIgnoresSurroundingSpaces() {
        assertTrue(CSVFormat.TDF.getIgnoreSurroundingSpaces());
    }

    // ------------------------------------------------------------------
    // Requisito 6: INFORMIX_UNLOAD e INFORMIX_UNLOAD_CSV
    // ------------------------------------------------------------------

    @Test
    public void informixUnloadUsesLfRecordSeparator() {
        assertEquals(LF_STR, CSVFormat.INFORMIX_UNLOAD.getRecordSeparator());
    }

    @Test
    public void informixUnloadUsesBackslashEscape() {
        assertEquals(Character.valueOf('\\'), CSVFormat.INFORMIX_UNLOAD.getEscapeCharacter());
    }

    @Test
    public void informixUnloadUsesDoubleQuote() {
        assertEquals(Character.valueOf('"'), CSVFormat.INFORMIX_UNLOAD.getQuoteCharacter());
    }

    /**
     * Según el Javadoc, INFORMIX_UNLOAD es un formato delimitado por comas.
     * La implementación actual usa PIPE, por lo que este test debe fallar hasta corregir el código o la documentación.
     */
    @Test
    public void informixUnloadDelimiterIsComma() {
        assertEquals("Javadoc indica delimitador coma", ',', CSVFormat.INFORMIX_UNLOAD.getDelimiter());
    }

    @Test
    public void informixUnloadCsvUsesCommaDelimiter() {
        assertEquals(',', CSVFormat.INFORMIX_UNLOAD_CSV.getDelimiter());
    }

    @Test
    public void informixUnloadCsvUsesLfRecordSeparator() {
        assertEquals(LF_STR, CSVFormat.INFORMIX_UNLOAD_CSV.getRecordSeparator());
    }

    @Test
    public void informixUnloadCsvUsesDoubleQuote() {
        assertEquals(Character.valueOf('"'), CSVFormat.INFORMIX_UNLOAD_CSV.getQuoteCharacter());
    }

    @Test
    public void informixUnloadCsvHasEscapeDisabled() {
        assertFalse(CSVFormat.INFORMIX_UNLOAD_CSV.isEscapeCharacterSet());
        assertNull(CSVFormat.INFORMIX_UNLOAD_CSV.getEscapeCharacter());
    }

    // ------------------------------------------------------------------
    // Requisito 7: inmutabilidad
    // ------------------------------------------------------------------

    @Test
    public void withNullStringReturnsNewInstanceAndKeepsExcelUnchanged() {
        final CSVFormat modified = CSVFormat.EXCEL.withNullString("N/A");

        assertNotSame(CSVFormat.EXCEL, modified);
        assertEquals("N/A", modified.getNullString());
        assertNull("EXCEL no debe cambiar", CSVFormat.EXCEL.getNullString());
    }

    @Test
    public void withDelimiterDoesNotModifyDefault() {
        final CSVFormat modified = CSVFormat.DEFAULT.withDelimiter(';');

        assertNotSame(CSVFormat.DEFAULT, modified);
        assertEquals(';', modified.getDelimiter());
        assertEquals(',', CSVFormat.DEFAULT.getDelimiter());
    }

    @Test
    public void withHeaderDoesNotModifyDefault() {
        final CSVFormat modified = CSVFormat.DEFAULT.withHeader("a", "b");

        assertNotSame(CSVFormat.DEFAULT, modified);
        assertNull("DEFAULT no debe tener cabecera", CSVFormat.DEFAULT.getHeader());
        assertEquals(2, modified.getHeader().length);
    }

    @Test
    public void withIgnoreEmptyLinesDoesNotModifyExcel() {
        final CSVFormat modified = CSVFormat.EXCEL.withIgnoreEmptyLines(true);

        assertNotSame(CSVFormat.EXCEL, modified);
        assertTrue(modified.getIgnoreEmptyLines());
        assertFalse("EXCEL no debe cambiar", CSVFormat.EXCEL.getIgnoreEmptyLines());
    }

    @Test
    public void withQuoteModeDoesNotModifyMySql() {
        final CSVFormat modified = CSVFormat.MYSQL.withQuote('"');

        assertNotSame(CSVFormat.MYSQL, modified);
        assertEquals(Character.valueOf('"'), modified.getQuoteCharacter());
        assertNull("MYSQL no debe cambiar", CSVFormat.MYSQL.getQuoteCharacter());
    }

    // ------------------------------------------------------------------
    // Requisito 8: withRecordSeparator(String)
    // ------------------------------------------------------------------

    @Test
    public void withRecordSeparatorAcceptsLf() {
        assertEquals(LF_STR, CSVFormat.DEFAULT.withRecordSeparator(LF_STR).getRecordSeparator());
    }

    @Test
    public void withRecordSeparatorAcceptsCr() {
        assertEquals(CR_STR, CSVFormat.DEFAULT.withRecordSeparator(CR_STR).getRecordSeparator());
    }

    @Test
    public void withRecordSeparatorAcceptsCrLf() {
        assertEquals(CRLF_STR, CSVFormat.DEFAULT.withRecordSeparator(CRLF_STR).getRecordSeparator());
    }

    /**
     * Según el Javadoc, withRecordSeparator(String) debe lanzar IllegalArgumentException si el separador
     * no es CR, LF ni CRLF. La implementación actual no valida esto, por lo que este test debe fallar
     * hasta corregir el código o la documentación.
     */
    @Test
    public void withRecordSeparatorRejectsInvalidSeparator() {
        try {
            CSVFormat.DEFAULT.withRecordSeparator("abc");
            fail("Se esperaba IllegalArgumentException para un separador no válido");
        } catch (final IllegalArgumentException expected) {
            assertNotEquals(null, expected.getMessage() == null ? null : "ok");
        }
    }

    private static void fail(final String message) {
        org.junit.Assert.fail(message);
    }
}
