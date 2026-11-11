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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.util.List;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link CSVFormat} usando solo su API pública.
 */
public class CSVFormatLLMTest {

    // ---------------------------------------------------------------
    // Formatos predefinidos
    // ---------------------------------------------------------------

    @Test
    public void defaultFormatHasExpectedSettings() {
        final CSVFormat f = CSVFormat.DEFAULT;
        assertEquals(',', f.getDelimiter());
        assertEquals(Character.valueOf('"'), f.getQuoteCharacter());
        assertNull(f.getQuoteMode());
        assertNull(f.getCommentMarker());
        assertNull(f.getEscapeCharacter());
        assertNull(f.getNullString());
        assertNull(f.getHeader());
        assertEquals("\r\n", f.getRecordSeparator());
        assertTrue(f.getIgnoreEmptyLines());
        assertFalse(f.getIgnoreSurroundingSpaces());
        assertFalse(f.getSkipHeaderRecord());
        assertFalse(f.getAllowMissingColumnNames());
    }

    @Test
    public void rfc4180FormatIgnoresNoEmptyLines() {
        final CSVFormat f = CSVFormat.RFC4180;
        assertEquals(',', f.getDelimiter());
        assertEquals(Character.valueOf('"'), f.getQuoteCharacter());
        assertEquals("\r\n", f.getRecordSeparator());
        assertFalse(f.getIgnoreEmptyLines());
        assertFalse(f.getIgnoreSurroundingSpaces());
    }

    @Test
    public void excelFormatSettings() {
        final CSVFormat f = CSVFormat.EXCEL;
        assertEquals(',', f.getDelimiter());
        assertEquals(Character.valueOf('"'), f.getQuoteCharacter());
        assertEquals("\r\n", f.getRecordSeparator());
        assertFalse(f.getIgnoreEmptyLines());
        assertFalse(f.getAllowMissingColumnNames() && !f.getIgnoreEmptyLines() == false);
    }

    @Test
    public void tdfFormatUsesTabAndTrimsSpaces() {
        final CSVFormat f = CSVFormat.TDF;
        assertEquals('\t', f.getDelimiter());
        assertEquals(Character.valueOf('"'), f.getQuoteCharacter());
        assertEquals("\r\n", f.getRecordSeparator());
        assertTrue(f.getIgnoreSurroundingSpaces());
    }

    @Test
    public void mysqlFormatSettings() {
        final CSVFormat f = CSVFormat.MYSQL;
        assertEquals('\t', f.getDelimiter());
        assertFalse(f.isQuoteCharacterSet());
        assertNull(f.getQuoteCharacter());
        assertEquals(Character.valueOf('\\'), f.getEscapeCharacter());
        assertTrue(f.isEscapeCharacterSet());
        assertEquals("\n", f.getRecordSeparator());
        assertFalse(f.getIgnoreEmptyLines());
    }

    // ---------------------------------------------------------------
    // newFormat
    // ---------------------------------------------------------------

    @Test
    public void newFormatSetsOnlyDelimiter() {
        final CSVFormat f = CSVFormat.newFormat(';');
        assertEquals(';', f.getDelimiter());
        assertNull(f.getQuoteCharacter());
        assertNull(f.getQuoteMode());
        assertNull(f.getCommentMarker());
        assertNull(f.getEscapeCharacter());
        assertNull(f.getNullString());
        assertNull(f.getRecordSeparator());
        assertNull(f.getHeader());
        assertFalse(f.getIgnoreEmptyLines());
        assertFalse(f.getIgnoreSurroundingSpaces());
        assertFalse(f.getSkipHeaderRecord());
        assertFalse(f.getAllowMissingColumnNames());
    }

    @Test(expected = IllegalArgumentException.class)
    public void newFormatRejectsLineFeedDelimiter() {
        CSVFormat.newFormat('\n');
    }

    @Test(expected = IllegalArgumentException.class)
    public void newFormatRejectsCarriageReturnDelimiter() {
        CSVFormat.newFormat('\r');
    }

    // ---------------------------------------------------------------
    // Inmutabilidad y propagación de ajustes
    // ---------------------------------------------------------------

    @Test
    public void withDelimiterDoesNotModifyOriginal() {
        final CSVFormat original = CSVFormat.DEFAULT;
        final CSVFormat changed = original.withDelimiter(';');
        assertEquals(';', changed.getDelimiter());
        assertEquals(',', original.getDelimiter());
        assertFalse(changed == original);
    }

    @Test
    public void withMethodsPreserveOtherSettings() {
        final CSVFormat f = CSVFormat.TDF.withDelimiter(';');
        assertEquals(';', f.getDelimiter());
        assertTrue(f.getIgnoreSurroundingSpaces());
        assertEquals(Character.valueOf('"'), f.getQuoteCharacter());
        assertEquals("\r\n", f.getRecordSeparator());
    }

    @Test
    public void headerIsPreservedByOtherWithMethods() {
        final CSVFormat f = CSVFormat.DEFAULT.withHeader("h1", "h2").withDelimiter(';');
        assertArrayEquals(new String[] {"h1", "h2"}, f.getHeader());
    }

    @Test
    public void withQuoteNullDisablesQuoting() {
        final CSVFormat f = CSVFormat.DEFAULT.withQuote((Character) null);
        assertFalse(f.isQuoteCharacterSet());
        assertNull(f.getQuoteCharacter());
    }

    @Test
    public void withQuoteCharChangesQuoteCharacter() {
        final CSVFormat f = CSVFormat.DEFAULT.withQuote('\'');
        assertTrue(f.isQuoteCharacterSet());
        assertEquals(Character.valueOf('\''), f.getQuoteCharacter());
    }

    @Test
    public void withCommentMarkerNullDisablesComments() {
        final CSVFormat f = CSVFormat.DEFAULT.withCommentMarker('#').withCommentMarker((Character) null);
        assertFalse(f.isCommentMarkerSet());
        assertNull(f.getCommentMarker());
    }

    @Test
    public void commentMarkerIsDisabledByDefaultAndEnabledWhenSet() {
        assertFalse(CSVFormat.DEFAULT.isCommentMarkerSet());
        final CSVFormat f = CSVFormat.DEFAULT.withCommentMarker('#');
        assertTrue(f.isCommentMarkerSet());
        assertEquals(Character.valueOf('#'), f.getCommentMarker());
    }

    @Test
    public void withEscapeNullDisablesEscape() {
        final CSVFormat f = CSVFormat.MYSQL.withEscape((Character) null);
        assertFalse(f.isEscapeCharacterSet());
        assertNull(f.getEscapeCharacter());
    }

    // ---------------------------------------------------------------
    // Validaciones (IllegalArgumentException)
    // ---------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void withEscapeEqualToDelimiterThrows() {
        CSVFormat.DEFAULT.withEscape(',');
    }

    @Test(expected = IllegalArgumentException.class)
    public void withQuoteEqualToDelimiterThrows() {
        CSVFormat.DEFAULT.withQuote(',');
    }

    @Test(expected = IllegalArgumentException.class)
    public void withCommentMarkerEqualToQuoteThrows() {
        CSVFormat.DEFAULT.withCommentMarker('"');
    }

    @Test(expected = IllegalArgumentException.class)
    public void withCommentMarkerLineFeedThrows() {
        CSVFormat.DEFAULT.withCommentMarker('\n');
    }

    @Test(expected = IllegalArgumentException.class)
    public void quoteModeNoneWithoutEscapeThrows() {
        CSVFormat.DEFAULT.withQuoteMode(QuoteMode.NONE);
    }

    @Test
    public void quoteModeNoneAcceptedWhenEscapeIsSet() {
        final CSVFormat f = CSVFormat.DEFAULT.withEscape('\\').withQuoteMode(QuoteMode.NONE);
        assertEquals(QuoteMode.NONE, f.getQuoteMode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void headerWithDuplicatesInMiddleThrows() {
        CSVFormat.DEFAULT.withHeader("a", "b", "a");
    }

    // ---------------------------------------------------------------
    // Cabecera
    // ---------------------------------------------------------------

    @Test
    public void emptyHeaderMeansReadFromInput() {
        final String[] header = CSVFormat.DEFAULT.withHeader().getHeader();
        assertNotNull(header);
        assertEquals(0, header.length);
    }

    @Test
    public void nullHeaderDisablesHeader() {
        final CSVFormat f = CSVFormat.DEFAULT.withHeader("a").withHeader((String[]) null);
        assertNull(f.getHeader());
    }

    @Test
    public void getHeaderReturnsDefensiveCopy() {
        final CSVFormat f = CSVFormat.DEFAULT.withHeader("a", "b");
        f.getHeader()[0] = "x";
        assertEquals("a", f.getHeader()[0]);
    }

    @Test
    public void withHeaderDoesNotAliasInputArray() {
        final String[] input = {"a", "b"};
        final CSVFormat f = CSVFormat.DEFAULT.withHeader(input);
        input[0] = "changed";
        assertEquals("a", f.getHeader()[0]);
    }

    @Test
    public void skipHeaderRecordGetter() {
        assertFalse(CSVFormat.DEFAULT.getSkipHeaderRecord());
        assertTrue(CSVFormat.DEFAULT.withSkipHeaderRecord(true).getSkipHeaderRecord());
    }

    @Test
    public void allowMissingColumnNamesGetter() {
        assertFalse(CSVFormat.DEFAULT.getAllowMissingColumnNames());
        assertTrue(CSVFormat.DEFAULT.withAllowMissingColumnNames(true).getAllowMissingColumnNames());
    }

    @Test
    public void ignoreEmptyLinesGetter() {
        assertFalse(CSVFormat.DEFAULT.withIgnoreEmptyLines(false).getIgnoreEmptyLines());
        assertTrue(CSVFormat.DEFAULT.withIgnoreEmptyLines(true).getIgnoreEmptyLines());
    }

    // ---------------------------------------------------------------
    // Null string
    // ---------------------------------------------------------------

    @Test
    public void nullStringSetIsTrueForEmptyString() {
        assertFalse(CSVFormat.DEFAULT.isNullStringSet());
        final CSVFormat f = CSVFormat.DEFAULT.withNullString("");
        assertTrue(f.isNullStringSet());
        assertEquals("", f.getNullString());
    }

    // ---------------------------------------------------------------
    // Record separator
    // ---------------------------------------------------------------

    @Test
    public void withRecordSeparatorCharSetsStringValue() {
        final CSVFormat f = CSVFormat.DEFAULT.withRecordSeparator('|');
        assertEquals("|", f.getRecordSeparator());
    }

    // ---------------------------------------------------------------
    // format()
    // ---------------------------------------------------------------

    @Test
    public void formatUsesCustomDelimiter() {
        assertEquals("a;b", CSVFormat.newFormat(';').format("a", "b"));
    }

    @Test
    public void tdfFormatJoinsWithTab() {
        assertEquals("a\tb", CSVFormat.TDF.format("a", "b"));
    }

    @Test
    public void mysqlFormatDoesNotIncludeTrailingNewline() {
        assertEquals("a\tb", CSVFormat.MYSQL.format("a", "b"));
    }

    @Test
    public void formatQuotesValueContainingQuoteAndDoublesIt() {
        assertEquals("\"a\"\"b\"", CSVFormat.DEFAULT.format("a\"b"));
    }

    @Test
    public void formatQuotesValueContainingLineFeed() {
        assertEquals("\"a\nb\"", CSVFormat.DEFAULT.format("a\nb"));
    }

    @Test
    public void formatWritesNullAsNullString() {
        assertEquals("a,NULL", CSVFormat.DEFAULT.withNullString("NULL").format("a", null));
    }

    @Test
    public void formatWritesNullAsEmptyWhenNoNullString() {
        assertEquals("a,,b", CSVFormat.DEFAULT.format("a", null, "b"));
    }

    @Test
    public void formatConvertsNonStringValuesToText() {
        assertEquals("1,2.5,true", CSVFormat.DEFAULT.format(1, 2.5, true));
    }

    // ---------------------------------------------------------------
    // toString()
    // ---------------------------------------------------------------

    @Test
    public void toStringDescribesDefaultSettings() {
        final String s = CSVFormat.DEFAULT.toString();
        assertTrue(s.contains("Delimiter=<,>"));
        assertTrue(s.contains("QuoteChar=<\">"));
        assertTrue(s.contains("RecordSeparator=<\r\n>"));
        assertTrue(s.contains("EmptyLines:ignored"));
        assertTrue(s.contains("SkipHeaderRecord:false"));
    }

    @Test
    public void toStringIncludesHeader() {
        final String s = CSVFormat.DEFAULT.withHeader("a", "b").toString();
        assertTrue(s.contains("Header:[a, b]"));
    }

    // ---------------------------------------------------------------
    // equals / hashCode
    // ---------------------------------------------------------------

    @Test
    public void equalFormatsHaveEqualHashCodes() {
        final CSVFormat a = CSVFormat.newFormat(';').withQuote('"');
        final CSVFormat b = CSVFormat.newFormat(';').withQuote('"');
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void formatsWithEqualHeaderContentAreEqual() {
        final CSVFormat a = CSVFormat.DEFAULT.withHeader("a", "b");
        final CSVFormat b = CSVFormat.DEFAULT.withHeader("a", "b");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void defaultAndRfc4180AreNotEqualBecauseOfEmptyLines() {
        assertFalse(CSVFormat.DEFAULT.equals(CSVFormat.RFC4180));
        assertFalse(CSVFormat.RFC4180.equals(CSVFormat.DEFAULT));
    }

    @Test
    public void differentDelimitersAreNotEqual() {
        assertFalse(CSVFormat.newFormat(';').equals(CSVFormat.newFormat(',')));
    }

    @Test
    public void serializationPreservesSettingsAndHeader() throws Exception {
        final CSVFormat original = CSVFormat.DEFAULT
                .withHeader("a", "b")
                .withQuoteMode(QuoteMode.ALL)
                .withSkipHeaderRecord(true);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(original);
        }

        final CSVFormat copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            copy = (CSVFormat) in.readObject();
        }

        assertEquals(original, copy);
        assertArrayEquals(new String[] {"a", "b"}, copy.getHeader());
        assertEquals(QuoteMode.ALL, copy.getQuoteMode());
        assertTrue(copy.getSkipHeaderRecord());
    }

    // ---------------------------------------------------------------
    // parse()
    // ---------------------------------------------------------------

    @Test
    public void parseReadsTabDelimitedRecords() throws Exception {
        final CSVParser parser = CSVFormat.TDF.parse(new StringReader("a\tb\r\nc\td\r\n"));
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(2, records.size());
        assertEquals("a", records.get(0).get(0));
        assertEquals("b", records.get(0).get(1));
        assertEquals("c", records.get(1).get(0));
        assertEquals("d", records.get(1).get(1));
    }

    @Test
    public void parseTrimsSurroundingSpacesWhenConfigured() throws Exception {
        final CSVParser parser = CSVFormat.TDF.parse(new StringReader("a \t b\r\n"));
        final CSVRecord record = parser.getRecords().get(0);
        assertEquals("a", record.get(0));
        assertEquals("b", record.get(1));
    }

    @Test
    public void parseSkipsCommentLines() throws Exception {
        final CSVParser parser = CSVFormat.DEFAULT.withCommentMarker('#')
                .parse(new StringReader("#comment\r\na,b\r\n"));
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(1, records.size());
        assertEquals("a", records.get(0).get(0));
    }

    @Test
    public void parseIgnoresEmptyLinesWhenConfigured() throws Exception {
        final List<CSVRecord> records = CSVFormat.DEFAULT
                .parse(new StringReader("a\r\n\r\nb\r\n")).getRecords();
        assertEquals(2, records.size());
    }

    @Test
    public void parseKeepsEmptyLinesWhenNotIgnored() throws Exception {
        final List<CSVRecord> records = CSVFormat.RFC4180
                .parse(new StringReader("a\r\n\r\nb\r\n")).getRecords();
        assertEquals(3, records.size());
    }

    @Test
    public void parseConvertsNullStringToNull() throws Exception {
        final CSVRecord record = CSVFormat.DEFAULT.withNullString("NULL")
                .parse(new StringReader("a,NULL\r\n")).getRecords().get(0);
        assertEquals("a", record.get(0));
        assertNull(record.get(1));
    }
}
