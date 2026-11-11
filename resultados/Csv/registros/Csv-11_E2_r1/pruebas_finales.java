package org.apache.commons.csv;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link CSVParser} que complementan CSVParserTest,
 * centradas en ramas y caminos no ejecutados por la suite existente.
 */
public class CSVParserLLMTest {

    @Test
    public void testNullStringConvertsCaseInsensitiveMatchToNull() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withNullString("NULL");
        final CSVParser parser = CSVParser.parse("NULL,null,Null,x\n", format);
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(1, records.size());
        assertArrayEquals(new String[]{null, null, null, "x"}, records.get(0).values());
        parser.close();
    }

    @Test
    public void testNullStringDoesNotMatchOtherValues() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withNullString("NULL");
        final CSVParser parser = CSVParser.parse("nil,x\n", format);
        final CSVRecord record = parser.nextRecord();
        assertNotNull(record);
        assertArrayEquals(new String[]{"nil", "x"}, record.values());
        parser.close();
    }

    @Test
    public void testWithoutNullStringValuesAreNeverNull() throws IOException {
        final CSVParser parser = CSVParser.parse("NULL,\n", CSVFormat.DEFAULT);
        final CSVRecord record = parser.nextRecord();
        assertNotNull(record);
        assertArrayEquals(new String[]{"NULL", ""}, record.values());
        parser.close();
    }

    @Test
    public void testMultipleCommentLinesAccumulateIntoRecordComment() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withCommentStart('#');
        final CSVParser parser = CSVParser.parse("# first\n# second\na,b\n", format);
        final CSVRecord record = parser.nextRecord();
        assertNotNull(record);
        assertArrayEquals(new String[]{"a", "b"}, record.values());
        final String comment = record.getComment();
        assertNotNull(comment);
        assertTrue(comment.contains("first"));
        assertTrue(comment.contains("second"));
        assertTrue(comment.indexOf('\n') > 0);
        parser.close();
    }

    @Test
    public void testCommentOnlyInputYieldsNoRecords() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withCommentStart('#');
        final CSVParser parser = CSVParser.parse("# only\n# comments\n", format);
        final List<CSVRecord> records = parser.getRecords();
        assertNotNull(records);
        assertTrue(records.isEmpty());
        parser.close();
    }

    @Test
    public void testExplicitHeaderWithoutSkipKeepsFirstLineAsData() throws IOException {
        final CSVParser parser = CSVParser.parse("a,b\n1,2\n", CSVFormat.DEFAULT.withHeader("A", "B"));
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(2, records.size());
        assertEquals("a", records.get(0).get("A"));
        assertEquals("b", records.get(0).get("B"));
        assertEquals("1", records.get(1).get("A"));
        parser.close();
    }

    @Test
    public void testExplicitHeaderWithSkipDropsFirstLine() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withHeader("A", "B").withSkipHeaderRecord(true);
        final CSVParser parser = CSVParser.parse("a,b\n1,2\n", format);
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(1, records.size());
        assertEquals("1", records.get(0).get("A"));
        assertEquals("2", records.get(0).get("B"));
        parser.close();
    }

    @Test
    public void testAutoHeaderOnEmptyInputGivesEmptyHeaderMap() throws IOException {
        final CSVParser parser = CSVParser.parse("", CSVFormat.DEFAULT.withHeader());
        final Map<String, Integer> headerMap = parser.getHeaderMap();
        assertNotNull(headerMap);
        assertTrue(headerMap.isEmpty());
        assertTrue(parser.getRecords().isEmpty());
        parser.close();
    }

    @Test
    public void testAutoHeaderSkipsLeadingCommentLines() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withCommentStart('#').withHeader();
        final CSVParser parser = CSVParser.parse("# comment\na,b\n1,2\n", format);
        final Map<String, Integer> headerMap = parser.getHeaderMap();
        assertEquals(Integer.valueOf(0), headerMap.get("a"));
        assertEquals(Integer.valueOf(1), headerMap.get("b"));
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(1, records.size());
        assertEquals("1", records.get(0).get("a"));
        parser.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExplicitDuplicateHeaderNamesAreRejected() throws IOException {
        CSVParser.parse("1,2\n", CSVFormat.DEFAULT.withHeader("x", "x"));
    }

    @Test
    public void testDuplicateEmptyHeadersAllowedWhenIgnored() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withHeader("", "").withIgnoreEmptyHeaders(true);
        final CSVParser parser = CSVParser.parse("1,2\n", format);
        assertNotNull(parser.getHeaderMap());
        parser.close();
    }

    @Test
    public void testHeaderMapIsDefensiveCopy() throws IOException {
        final CSVParser parser = CSVParser.parse("a,b,c\n1,2,3\n", CSVFormat.DEFAULT.withHeader());
        final Map<String, Integer> first = parser.getHeaderMap();
        first.clear();
        final Map<String, Integer> second = parser.getHeaderMap();
        assertEquals(3, second.size());
        assertEquals(Integer.valueOf(2), second.get("c"));
        parser.close();
    }

    @Test
    public void testIsClosedReflectsCloseState() throws IOException {
        final CSVParser parser = CSVParser.parse("a,b\n", CSVFormat.DEFAULT);
        assertFalse(parser.isClosed());
        parser.close();
        assertTrue(parser.isClosed());
    }

    @Test
    public void testIteratorHasNextFalseOnEmptyInput() throws IOException {
        final CSVParser parser = CSVParser.parse("", CSVFormat.DEFAULT);
        final Iterator<CSVRecord> iterator = parser.iterator();
        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasNext());
        parser.close();
    }

    @Test(expected = NoSuchElementException.class)
    public void testIteratorNextOnEmptyInputThrows() throws IOException {
        final CSVParser parser = CSVParser.parse("", CSVFormat.DEFAULT);
        try {
            parser.iterator().next();
        } finally {
            parser.close();
        }
    }

    @Test
    public void testIteratorNextWithoutHasNextReadsSequentially() throws IOException {
        final CSVParser parser = CSVParser.parse("a\nb", CSVFormat.DEFAULT);
        final Iterator<CSVRecord> iterator = parser.iterator();
        assertEquals("a", iterator.next().get(0));
        assertEquals("b", iterator.next().get(0));
        try {
            iterator.next();
            fail("NoSuchElementException expected");
        } catch (final NoSuchElementException expected) {
            assertNotNull(expected);
        }
        parser.close();
    }

    @Test
    public void testNextRecordReturnsNullRepeatedlyAfterEnd() throws IOException {
        final CSVParser parser = CSVParser.parse("a", CSVFormat.DEFAULT);
        assertNotNull(parser.nextRecord());
        assertNull(parser.nextRecord());
        assertNull(parser.nextRecord());
        parser.close();
    }

    @Test
    public void testGetRecordsOnConsumedParserReturnsEmptyList() throws IOException {
        final CSVParser parser = CSVParser.parse("a,b\n", CSVFormat.DEFAULT);
        assertEquals(1, parser.getRecords().size());
        final List<CSVRecord> again = parser.getRecords();
        assertNotNull(again);
        assertTrue(again.isEmpty());
        parser.close();
    }

    @Test
    public void testRecordNumberAfterGetRecords() throws IOException {
        final CSVParser parser = CSVParser.parse("a\nb\nc", CSVFormat.DEFAULT);
        parser.getRecords();
        assertEquals(3, parser.getRecordNumber());
        parser.close();
    }

    @Test
    public void testLastRecordWithoutTrailingNewlineIsParsed() throws IOException {
        final CSVParser parser = CSVParser.parse("a,b\nc,d", CSVFormat.DEFAULT);
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(2, records.size());
        assertArrayEquals(new String[]{"c", "d"}, records.get(1).values());
        parser.close();
    }

    @Test
    public void testQuotedValueWithEscapedQuoteAndDelimiter() throws IOException {
        final CSVParser parser = CSVParser.parse("\"a\"\"b\",\"c,d\"\n", CSVFormat.DEFAULT);
        final CSVRecord record = parser.nextRecord();
        assertNotNull(record);
        assertArrayEquals(new String[]{"a\"b", "c,d"}, record.values());
        parser.close();
    }

    @Test
    public void testTrailingEmptyFieldsArePreserved() throws IOException {
        final CSVParser parser = CSVParser.parse("a,,\n", CSVFormat.DEFAULT);
        assertArrayEquals(new String[]{"a", "", ""}, parser.nextRecord().values());
        parser.close();
    }

    @Test
    public void testCustomDelimiterIsUsed() throws IOException {
        final CSVParser parser = CSVParser.parse("a;b,c\n", CSVFormat.DEFAULT.withDelimiter(';'));
        assertArrayEquals(new String[]{"a", "b,c"}, parser.nextRecord().values());
        parser.close();
    }

    @Test
    public void testParseFileReadsRecords() throws IOException {
        final File file = File.createTempFile("csvparser-llm", ".csv");
        try {
            Files.write(file.toPath(), "a,b\n1,2\n".getBytes(StandardCharsets.UTF_8));
            final CSVParser parser = CSVParser.parse(file, StandardCharsets.UTF_8, CSVFormat.DEFAULT);
            try {
                final List<CSVRecord> records = parser.getRecords();
                assertEquals(2, records.size());
                assertArrayEquals(new String[]{"1", "2"}, records.get(1).values());
            } finally {
                parser.close();
            }
        } finally {
            assertTrue(file.delete() || !file.exists());
        }
    }

    @Test
    public void testParseUrlReadsRecords() throws IOException {
        final File file = File.createTempFile("csvparser-llm-url", ".csv");
        try {
            Files.write(file.toPath(), "x,y\n".getBytes(StandardCharsets.UTF_8));
            final URL url = file.toURI().toURL();
            final CSVParser parser = CSVParser.parse(url, StandardCharsets.UTF_8, CSVFormat.DEFAULT);
            try {
                final List<CSVRecord> records = parser.getRecords();
                assertEquals(1, records.size());
                assertArrayEquals(new String[]{"x", "y"}, records.get(0).values());
            } finally {
                parser.close();
            }
        } finally {
            assertTrue(file.delete() || !file.exists());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseNullUrlThrows() throws IOException {
        CSVParser.parse((URL) null, StandardCharsets.UTF_8, CSVFormat.DEFAULT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseNullStringThrows() throws IOException {
        CSVParser.parse((String) null, CSVFormat.DEFAULT);
    }

    @Test
    public void testConstructorWithReaderParsesContent() throws IOException {
        final CSVParser parser = new CSVParser(new StringReader("a,b\n"), CSVFormat.DEFAULT);
        assertArrayEquals(new String[]{"a", "b"}, parser.nextRecord().values());
        assertNull(parser.nextRecord());
        parser.close();
    }
}
