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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link CSVParser} que usan únicamente su API pública.
 */
public class CSVParserLLMTest {

    @Test
    public void testSingleLineReturnsOneRecordWithValues() throws IOException {
        final CSVParser parser = CSVParser.parse("a,b,c\n", CSVFormat.DEFAULT);
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(1, records.size());
        assertArrayEquals(new String[]{"a", "b", "c"}, records.get(0).values());
        parser.close();
    }

    @Test
    public void testLastRecordWithoutTrailingNewlineIsParsed() throws IOException {
        final CSVParser parser = CSVParser.parse("a,b", CSVFormat.DEFAULT);
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(1, records.size());
        assertArrayEquals(new String[]{"a", "b"}, records.get(0).values());
        parser.close();
    }

    @Test
    public void testEmptyInputYieldsNoRecordsAndZeroRecordNumber() throws IOException {
        final CSVParser parser = CSVParser.parse("", CSVFormat.DEFAULT);
        final List<CSVRecord> records = parser.getRecords();
        assertNotNull(records);
        assertEquals(0, records.size());
        assertEquals(0L, parser.getRecordNumber());
        parser.close();
    }

    @Test
    public void testRecordNumbersAreSequentialStartingAtOne() throws IOException {
        final CSVParser parser = CSVParser.parse("a\nb\nc", CSVFormat.DEFAULT);
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(3, records.size());
        assertEquals(1L, records.get(0).getRecordNumber());
        assertEquals(2L, records.get(1).getRecordNumber());
        assertEquals(3L, records.get(2).getRecordNumber());
        assertEquals(3L, parser.getRecordNumber());
        parser.close();
    }

    @Test
    public void testCurrentLineNumberAfterAllRecordsIsedLineCount() throws IOException {
        final CSVParser parser = CSVParser.parse("a\nb\n", CSVFormat.DEFAULT);
        parser.getRecords();
        assertEquals(2L, parser.getCurrentLineNumber());
        parser.close();
    }

    @Test
    public void testQuotedValueKeepsEmbeddedDelimiter() throws IOException {
        final CSVParser parser = CSVParser.parse("\"a,b\",c\n", CSVFormat.DEFAULT);
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(1, records.size());
        assertArrayEquals(new String[]{"a,b", "c"}, records.get(0).values());
        parser.close();
    }

    @Test
    public void testDoubledQuoteInsideQuotedValueBecomesSingleQuote() throws IOException {
        final CSVParser parser = CSVParser.parse("\"x\"\"y\"\n", CSVFormat.DEFAULT);
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(1, records.size());
        assertEquals("x\"y", records.get(0).get(0));
        parser.close();
    }

    @Test
    public void testCustomDelimiterIsUsed() throws IOException {
        final CSVParser parser = CSVParser.parse("1;2;3\n", CSVFormat.DEFAULT.withDelimiter(';'));
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(1, records.size());
        assertArrayEquals(new String[]{"1", "2", "3"}, records.get(0).values());
        parser.close();
    }

    @Test
    public void testTabDelimiterIsUsed() throws IOException {
        final CSVParser parser = CSVParser.parse("x\ty\n", CSVFormat.DEFAULT.withDelimiter('\t'));
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(1, records.size());
        assertArrayEquals(new String[]{"x", "y"}, records.get(0).values());
        parser.close();
    }

    @Test
    public void testIgnoreSurroundingSpacesTrimsValues() throws IOException {
        final CSVParser parser = CSVParser.parse(" a , b \n",
                CSVFormat.DEFAULT.withIgnoreSurroundingSpaces(true));
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(1, records.size());
        assertArrayEquals(new String[]{"a", "b"}, records.get(0).values());
        parser.close();
    }

    @Test
    public void testEmptyLinesAreIgnoredByDefaultInCsvFormat() throws IOException {
        final CSVParser parser = CSVParser.parse("a\n\n\nb\n", CSVFormat.DEFAULT);
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(2, records.size());
        assertArrayEquals(new String[]{"a"}, records.get(0).values());
        assertArrayEquals(new String[]{"b"}, records.get(1).values());
        parser.close();
    }

    @Test
    public void testEmptyLinesKeptWhenIgnoreEmptyLinesDisabled() throws IOException {
        final CSVParser parser = CSVParser.parse("a\n\nb",
                CSVFormat.DEFAULT.withIgnoreEmptyLines(false));
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(3, records.size());
        assertArrayEquals(new String[]{"a"}, records.get(0).values());
        assertArrayEquals(new String[]{""}, records.get(1).values());
        assertArrayEquals(new String[]{"b"}, records.get(2).values());
        parser.close();
    }

    @Test
    public void testNullStringIsConvertedToNullCaseInsensitively() throws IOException {
        final CSVParser parser = CSVParser.parse("NULL,x\nnull,\"NuLl\"\n",
                CSVFormat.DEFAULT.withNullString("NULL"));
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(2, records.size());
        assertNull(records.get(0).get(0));
        assertEquals("x", records.get(0).get(1));
        assertNull(records.get(1).get(0));
        assertNull(records.get(1).get(1));
        parser.close();
    }

    @Test
    public void testCommentLinesAreSkippedAndExposedAsComment() throws IOException {
        final CSVParser parser = CSVParser.parse("# hello\na,b\n",
                CSVFormat.DEFAULT.withCommentStart('#'));
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(1, records.size());
        assertArrayEquals(new String[]{"a", "b"}, records.get(0).values());
        assertNotNull(records.get(0).getComment());
        assertTrue(records.get(0).getComment().contains("hello"));
        parser.close();
    }

    @Test
    public void testProvidedHeaderMapsNamesToIndicesAndDoesNotConsumeFirstLine() throws IOException {
        final CSVParser parser = CSVParser.parse("1,2\n3,4\n", CSVFormat.DEFAULT.withHeader("A", "B"));
        final Map<String, Integer> headerMap = parser.getHeaderMap();
        assertNotNull(headerMap);
        assertEquals(2, headerMap.size());
        assertEquals(Integer.valueOf(0), headerMap.get("A"));
        assertEquals(Integer.valueOf(1), headerMap.get("B"));

        final List<CSVRecord> records = parser.getRecords();
        assertEquals(2, records.size());
        assertEquals("1", records.get(0).get("A"));
        assertEquals("4", records.get(1).get("B"));
        parser.close();
    }

    @Test
    public void testAutoHeaderIsConsumedFromFirstLine() throws IOException {
        final CSVParser parser = CSVParser.parse("name,age\nbob,3\n", CSVFormat.DEFAULT.withHeader());
        final Map<String, Integer> headerMap = parser.getHeaderMap();
        assertEquals(2, headerMap.size());
        assertEquals(Integer.valueOf(0), headerMap.get("name"));
        assertEquals(Integer.valueOf(1), headerMap.get("age"));

        final List<CSVRecord> records = parser.getRecords();
        assertEquals(1, records.size());
        assertEquals("bob", records.get(0).get("name"));
        assertEquals("3", records.get(0).get("age"));
        parser.close();
    }

    @Test
    public void testSkipHeaderRecordWithProvidedHeaderSkipsFirstLine() throws IOException {
        final CSVParser parser = CSVParser.parse("A,B\n1,2\n",
                CSVFormat.DEFAULT.withHeader("A", "B").withSkipHeaderRecord(true));
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(1, records.size());
        assertEquals("1", records.get(0).get("A"));
        assertEquals("2", records.get(0).get("B"));
        parser.close();
    }

    @Test
    public void testGetHeaderMapIsNullWithoutHeader() throws IOException {
        final CSVParser parser = CSVParser.parse("a,b\n", CSVFormat.DEFAULT);
        assertNull(parser.getHeaderMap());
        parser.close();
    }

    @Test
    public void testGetHeaderMapReturnsDefensiveCopy() throws IOException {
        final CSVParser parser = CSVParser.parse("", CSVFormat.DEFAULT.withHeader("A", "B"));
        final Map<String, Integer> first = parser.getHeaderMap();
        first.clear();
        first.put("Z", Integer.valueOf(99));

        final Map<String, Integer> second = parser.getHeaderMap();
        assertEquals(2, second.size());
        assertEquals(Integer.valueOf(0), second.get("A"));
        assertEquals(Integer.valueOf(1), second.get("B"));
        assertFalse(second.containsKey("Z"));
        parser.close();
    }

    @Test
    public void testGetHeaderMapPreservesColumnOrder() throws IOException {
        final CSVParser parser = CSVParser.parse("", CSVFormat.DEFAULT.withHeader("z", "a", "m"));
        final List<String> keys = new ArrayList<String>(parser.getHeaderMap().keySet());
        assertEquals(3, keys.size());
        assertEquals("z", keys.get(0));
        assertEquals("a", keys.get(1));
        assertEquals("m", keys.get(2));
        parser.close();
    }

    @Test
    public void testAutoHeaderOnEmptyInputGivesEmptyNonNullMap() throws IOException {
        final CSVParser parser = CSVParser.parse("", CSVFormat.DEFAULT.withHeader());
        final Map<String, Integer> headerMap = parser.getHeaderMap();
        assertNotNull(headerMap);
        assertTrue(headerMap.isEmpty());
        assertEquals(0, parser.getRecords().size());
        parser.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateAutoHeaderNamesAreRejectedOnConstruction() throws IOException {
        CSVParser.parse("a,b,a\n1,2,3\n", CSVFormat.DEFAULT.withHeader());
    }

    @Test
    public void testIsClosedReflectsCloseState() throws IOException {
        final CSVParser parser = CSVParser.parse("a,b\n", CSVFormat.DEFAULT);
        assertFalse(parser.isClosed());
        parser.close();
        assertTrue(parser.isClosed());
    }

    @Test(expected = NoSuchElementException.class)
    public void testIteratorNextOnEmptyInputThrows() throws IOException {
        final CSVParser parser = CSVParser.parse("", CSVFormat.DEFAULT);
        final Iterator<CSVRecord> iterator = parser.iterator();
        assertFalse(iterator.hasNext());
        iterator.next();
    }

    @Test
    public void testStringAndReaderFactoriesProduceSameRecords() throws IOException {
        final String input = "a,\"b\nc\",d\r\n1,2,3\r\n";
        final List<CSVRecord> fromString = CSVParser.parse(input, CSVFormat.DEFAULT).getRecords();
        final List<CSVRecord> fromReader = new CSVParser(new StringReader(input), CSVFormat.DEFAULT).getRecords();

        assertEquals(fromString.size(), fromReader.size());
        for (int i = 0; i < fromString.size(); i++) {
            assertArrayEquals(fromString.get(i).values(), fromReader.get(i).values());
        }
        assertEquals(2, fromString.size());
        assertArrayEquals(new String[]{"a", "b\nc", "d"}, fromString.get(0).values());
    }

    @Test
    public void testIteratorYieldsSameRecordsAsGetRecords() throws IOException {
        final String input = "x,y\n1,2\n3,4\n";
        final List<CSVRecord> viaGet = CSVParser.parse(input, CSVFormat.DEFAULT).getRecords();

        final List<CSVRecord> viaIterator = new ArrayList<CSVRecord>();
        for (final CSVRecord record : CSVParser.parse(input, CSVFormat.DEFAULT)) {
            viaIterator.add(record);
        }

        assertEquals(viaGet.size(), viaIterator.size());
        for (int i = 0; i < viaGet.size(); i++) {
            assertArrayEquals(viaGet.get(i).values(), viaIterator.get(i).values());
        }
    }

    @Test
    public void testCrlfRecordSeparatorsProduceExpectedRecords() throws IOException {
        final CSVParser parser = CSVParser.parse("a,b\r\nc,d\r\n", CSVFormat.DEFAULT);
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(2, records.size());
        assertArrayEquals(new String[]{"a", "b"}, records.get(0).values());
        assertArrayEquals(new String[]{"c", "d"}, records.get(1).values());
        parser.close();
    }
}
