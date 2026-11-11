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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link CSVParser} que usan únicamente la API pública.
 */
public class CSVParserLLMTest {

    @Test
    public void testRecordsAreReturnedInInputOrder() throws IOException {
        try (final CSVParser parser = CSVParser.parse("a,b\nc,d\ne,f", CSVFormat.DEFAULT)) {
            final List<CSVRecord> records = parser.getRecords();
            assertEquals(3, records.size());
            assertArrayEquals(new String[] { "a", "b" }, records.get(0).values());
            assertArrayEquals(new String[] { "c", "d" }, records.get(1).values());
            assertArrayEquals(new String[] { "e", "f" }, records.get(2).values());
        }
    }

    @Test
    public void testQuotedFieldKeepsEmbeddedDelimiter() throws IOException {
        try (final CSVParser parser = CSVParser.parse("\"a,b\",c", CSVFormat.DEFAULT)) {
            final CSVRecord record = parser.nextRecord();
            assertNotNull(record);
            assertArrayEquals(new String[] { "a,b", "c" }, record.values());
        }
    }

    @Test
    public void testDoubledQuotesAreUnescaped() throws IOException {
        try (final CSVParser parser = CSVParser.parse("\"he said \"\"hi\"\"\"", CSVFormat.DEFAULT)) {
            final CSVRecord record = parser.nextRecord();
            assertNotNull(record);
            assertEquals("he said \"hi\"", record.get(0));
            assertEquals(1, record.size());
        }
    }

    @Test
    public void testTrailingNewlineDoesNotProduceExtraRecord() throws IOException {
        try (final CSVParser parser = CSVParser.parse("a,b\n", CSVFormat.DEFAULT)) {
            final List<CSVRecord> records = parser.getRecords();
            assertEquals(1, records.size());
            assertArrayEquals(new String[] { "a", "b" }, records.get(0).values());
        }
    }

    @Test
    public void testEmptyLinesAreIgnoredByDefaultFormat() throws IOException {
        try (final CSVParser parser = CSVParser.parse("a\n\nb\n\n", CSVFormat.DEFAULT)) {
            final List<CSVRecord> records = parser.getRecords();
            assertEquals(2, records.size());
            assertEquals("a", records.get(0).get(0));
            assertEquals("b", records.get(1).get(0));
        }
    }

    @Test
    public void testNullStringIsMappedToNull() throws IOException {
        try (final CSVParser parser = CSVParser.parse("NULL,x", CSVFormat.DEFAULT.withNullString("NULL"))) {
            final CSVRecord record = parser.nextRecord();
            assertNotNull(record);
            assertNull(record.get(0));
            assertEquals("x", record.get(1));
        }
    }

    @Test
    public void testTrimRemovesWhitespaceFromQuotedValue() throws IOException {
        try (final CSVParser parser = CSVParser.parse("\" a \"", CSVFormat.DEFAULT.withTrim())) {
            assertEquals("a", parser.nextRecord().get(0));
        }
    }

    @Test
    public void testQuotedWhitespaceIsPreservedWithoutTrim() throws IOException {
        try (final CSVParser parser = CSVParser.parse("\" a \"", CSVFormat.DEFAULT)) {
            assertEquals(" a ", parser.nextRecord().get(0));
        }
    }

    @Test
    public void testCustomDelimiterIsUsed() throws IOException {
        try (final CSVParser parser = CSVParser.parse("a;b;c", CSVFormat.DEFAULT.withDelimiter(';'))) {
            final CSVRecord record = parser.nextRecord();
            assertArrayEquals(new String[] { "a", "b", "c" }, record.values());
        }
    }

    @Test
    public void testCommentLinesAreSkipped() throws IOException {
        try (final CSVParser parser = CSVParser.parse("#comentario\na,b\n",
                CSVFormat.DEFAULT.withCommentMarker('#'))) {
            final List<CSVRecord> records = parser.getRecords();
            assertEquals(1, records.size());
            assertArrayEquals(new String[] { "a", "b" }, records.get(0).values());
        }
    }

    @Test
    public void testHeaderMapKeepsColumnOrderAndIndices() throws IOException {
        try (final CSVParser parser = CSVParser.parse("1,2", CSVFormat.DEFAULT.withHeader("X", "Y"))) {
            final Map<String, Integer> headerMap = parser.getHeaderMap();
            assertEquals(2, headerMap.size());
            assertEquals(Integer.valueOf(0), headerMap.get("X"));
            assertEquals(Integer.valueOf(1), headerMap.get("Y"));
        }
    }

    @Test
    public void testHeaderMapReturnedIsACopy() throws IOException {
        try (final CSVParser parser = CSVParser.parse("1,2", CSVFormat.DEFAULT.withHeader("X", "Y"))) {
            final Map<String, Integer> copy = parser.getHeaderMap();
            copy.put("Z", Integer.valueOf(9));
            assertEquals(2, parser.getHeaderMap().size());
            assertFalse(parser.getHeaderMap().containsKey("Z"));
        }
    }

    @Test
    public void testAutoHeaderWithOnlyHeaderLineYieldsNoRecords() throws IOException {
        try (final CSVParser parser = CSVParser.parse("a,b\n", CSVFormat.DEFAULT.withHeader())) {
            assertEquals(0, parser.getRecords().size());
            assertEquals(2, parser.getHeaderMap().size());
            assertEquals(Integer.valueOf(1), parser.getHeaderMap().get("b"));
        }
    }

    @Test
    public void testExplicitSkippedHeaderAdvancesRecordNumber() throws IOException {
        try (final CSVParser parser = CSVParser.parse("a,b\n1,2",
                CSVFormat.DEFAULT.withHeader("A", "B").withSkipHeaderRecord())) {
            final CSVRecord record = parser.nextRecord();
            assertNotNull(record);
            assertEquals("1", record.get("A"));
            assertEquals("2", record.get("B"));
            assertEquals(2, record.getRecordNumber());
        }
    }

    @Test
    public void testRecordNumberTracksRecordsAndStaysAtEnd() throws IOException {
        try (final CSVParser parser = CSVParser.parse("a\nb\nc", CSVFormat.DEFAULT)) {
            assertEquals(0, parser.getRecordNumber());
            parser.getRecords();
            assertEquals(3, parser.getRecordNumber());
            assertNull(parser.nextRecord());
            assertEquals(3, parser.getRecordNumber());
        }
    }

    @Test
    public void testGetRecordsReturnsOnlyRemainingRecords() throws IOException {
        try (final CSVParser parser = CSVParser.parse("a\nb\nc", CSVFormat.DEFAULT)) {
            assertEquals("a", parser.nextRecord().get(0));
            final List<CSVRecord> rest = parser.getRecords();
            assertEquals(2, rest.size());
            assertEquals("b", rest.get(0).get(0));
            assertEquals("c", rest.get(1).get(0));
        }
    }

    @Test
    public void testConstructorWithOffsetAndRecordNumber() throws IOException {
        try (final CSVParser parser = new CSVParser(new StringReader("a,b\n"), CSVFormat.DEFAULT, 10, 5)) {
            final CSVRecord record = parser.nextRecord();
            assertNotNull(record);
            assertEquals(5, record.getRecordNumber());
            assertEquals(10, record.getCharacterPosition());
        }
    }

    @Test
    public void testTrailingEmptyFieldIsKept() throws IOException {
        try (final CSVParser parser = CSVParser.parse("a,\"\",", CSVFormat.DEFAULT)) {
            final CSVRecord record = parser.nextRecord();
            assertArrayEquals(new String[] { "a", "", "" }, record.values());
        }
    }

    @Test
    public void testIsClosedAfterClose() throws IOException {
        final CSVParser parser = CSVParser.parse("a,b", CSVFormat.DEFAULT);
        assertFalse(parser.isClosed());
        parser.close();
        assertTrue(parser.isClosed());
    }

    @Test
    public void testClosedParserIteratorHasNoNext() throws IOException {
        final CSVParser parser = CSVParser.parse("a\nb", CSVFormat.DEFAULT);
        final Iterator<CSVRecord> iterator = parser.iterator();
        parser.close();
        assertFalse(iterator.hasNext());
    }

    @Test(expected = NoSuchElementException.class)
    public void testExhaustedIteratorNextThrowsNoSuchElement() throws IOException {
        try (final CSVParser parser = CSVParser.parse("a", CSVFormat.DEFAULT)) {
            final Iterator<CSVRecord> iterator = parser.iterator();
            assertEquals("a", iterator.next().get(0));
            iterator.next();
            fail("Se esperaba NoSuchElementException");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateExplicitHeaderIsRejected() throws IOException {
        CSVParser.parse("1,2", CSVFormat.DEFAULT.withHeader("x", "x"));
    }
}
