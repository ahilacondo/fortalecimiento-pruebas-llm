package org.apache.commons.csv;

import java.io.StringReader;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExtendedBufferedReaderLLMTest {

    @Test
    public void testReadReturnsCharsInOrderAndThenEndOfStream() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("ab");
        assertEquals('a', br.read());
        assertEquals('b', br.read());
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.read());
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.read());
    }

    @Test
    public void testReadAgainIsUndefinedBeforeAnyRead() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a");
        assertEquals(ExtendedBufferedReader.UNDEFINED, br.readAgain());
    }

    @Test
    public void testReadAgainReturnsLastCharReturnedByRead() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("xy");
        assertEquals('x', br.read());
        assertEquals('x', br.readAgain());
        assertEquals('y', br.read());
        assertEquals('y', br.readAgain());
    }

    @Test
    public void testReadAgainIsEndOfStreamAfterReadReachesEof() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a");
        assertEquals('a', br.read());
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.read());
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.readAgain());
    }

    @Test
    public void testLookAheadReturnsNextCharWithoutConsumingIt() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("xy");
        assertEquals('x', br.lookAhead());
        assertEquals('x', br.lookAhead());
        assertEquals('x', br.read());
        assertEquals('y', br.lookAhead());
        assertEquals('y', br.read());
    }

    @Test
    public void testLookAheadDoesNotChangeReadAgain() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("xy");
        assertEquals('x', br.lookAhead());
        assertEquals(ExtendedBufferedReader.UNDEFINED, br.readAgain());

        assertEquals('x', br.read());
        assertEquals('y', br.lookAhead());
        assertEquals('x', br.readAgain());
    }

    @Test
    public void testLookAheadAtEndOfStreamReturnsEndOfStream() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a");
        assertEquals('a', br.read());
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.lookAhead());
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.lookAhead());
        assertEquals('a', br.readAgain());
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.read());
    }

    @Test
    public void testLookAheadDoesNotIncrementLineNumber() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("\n");
        assertEquals('\n', br.lookAhead());
        assertEquals(0, br.getLineNumber());
        assertEquals('\n', br.read());
        assertEquals(1, br.getLineNumber());
    }

    @Test
    public void testReadIncrementsLineNumberOnLineFeed() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a\nb\n");
        assertEquals(0, br.getLineNumber());
        assertEquals('a', br.read());
        assertEquals(0, br.getLineNumber());
        assertEquals('\n', br.read());
        assertEquals(1, br.getLineNumber());
        assertEquals('b', br.read());
        assertEquals(1, br.getLineNumber());
        assertEquals('\n', br.read());
        assertEquals(2, br.getLineNumber());
    }

    @Test
    public void testReadIncrementsLineNumberOnCarriageReturnOnlyNotCountedByRead() throws Exception {
        // read() only counts '\n'; a lone '\r' does not advance the counter.
        ExtendedBufferedReader br = getBufferedReader("a\rb");
        assertEquals('a', br.read());
        assertEquals('\r', br.read());
        assertEquals(0, br.getLineNumber());
        assertEquals('b', br.read());
        assertEquals(0, br.getLineNumber());
    }

    @Test
    public void testReadLineReturnsLineWithoutTerminator() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("abc\ndef\n");
        assertEquals("abc", br.readLine());
        assertEquals("def", br.readLine());
        assertNull(br.readLine());
    }

    @Test
    public void testReadLineHandlesCrLfAsSingleTerminator() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a\r\nb");
        assertEquals("a", br.readLine());
        assertEquals(1, br.getLineNumber());
        assertEquals("b", br.readLine());
        assertEquals(2, br.getLineNumber());
    }

    @Test
    public void testReadLineHandlesCarriageReturnAsTerminator() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("foo\rbar");
        assertEquals("foo", br.readLine());
        assertEquals(1, br.getLineNumber());
        assertEquals("bar", br.readLine());
        assertEquals(2, br.getLineNumber());
    }

    @Test
    public void testReadLineIncrementsLineNumberForEmptyLines() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("\n\n");
        assertEquals("", br.readLine());
        assertEquals(1, br.getLineNumber());
        assertEquals("", br.readLine());
        assertEquals(2, br.getLineNumber());
        assertNull(br.readLine());
        assertEquals(2, br.getLineNumber());
    }

    @Test
    public void testReadLineSetsReadAgainToLastCharOfLine() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("abc\n");
        assertEquals("abc", br.readLine());
        assertEquals('c', br.readAgain());
    }

    @Test
    public void testReadLineOnEmptyLineLeavesReadAgainUnchanged() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("\nx");
        assertEquals("", br.readLine());
        assertEquals(ExtendedBufferedReader.UNDEFINED, br.readAgain());
    }

    @Test
    public void testReadLineAtEndOfStreamSetsReadAgainToEndOfStream() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("");
        assertNull(br.readLine());
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.readAgain());
        assertEquals(0, br.getLineNumber());
    }

    @Test
    public void testReadContinuesAfterReadLine() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("ab\ncd");
        assertEquals("ab", br.readLine());
        assertEquals('c', br.read());
        assertEquals('d', br.read());
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.read());
    }

    @Test
    public void testReadArrayWithZeroLengthReturnsZeroAndKeepsReadAgain() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("abc");
        assertEquals('a', br.read());
        assertEquals(0, br.read(new char[5], 0, 0));
        assertEquals('a', br.readAgain());
        assertEquals('b', br.read());
    }

    @Test
    public void testReadArrayReturnsEndOfStreamAndSetsReadAgainOnEmptyInput() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("");
        char[] buf = new char[5];
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.read(buf, 0, 5));
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.readAgain());
    }

    @Test
    public void testReadArrayCopiesCharsAtOffset() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("hello");
        char[] buf = new char[6];
        assertEquals(3, br.read(buf, 2, 3));
        assertEquals('\0', buf[0]);
        assertEquals('\0', buf[1]);
        assertEquals('h', buf[2]);
        assertEquals('e', buf[3]);
        assertEquals('l', buf[4]);
        assertEquals('\0', buf[5]);
    }

    @Test
    public void testReadArraySetsReadAgainToLastCharReadIntoBuffer() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("xyz");
        char[] buf = new char[5];
        assertEquals(2, br.read(buf, 0, 2));
        assertEquals('y', br.readAgain());
        assertEquals('z', br.lookAhead());
    }

    @Test
    public void testReadArrayCountsLineFeeds() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a\nb\nc");
        char[] buf = new char[10];
        int total = 0;
        int n;
        while ((n = br.read(buf, 0, 10)) != -1) {
            total += n;
        }
        assertEquals(5, total);
        assertEquals(2, br.getLineNumber());
    }

    @Test
    public void testReadArrayCountsCrLfAsSingleLine() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a\r\nb");
        char[] buf = new char[10];
        while (br.read(buf, 0, 10) != -1) {
            // consume all input
        }
        assertEquals(1, br.getLineNumber());
    }

    @Test
    public void testReadArrayCountsLoneCarriageReturnAsLine() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a\rb\rc");
        char[] buf = new char[10];
        while (br.read(buf, 0, 10) != -1) {
            // consume all input
        }
        assertEquals(2, br.getLineNumber());
    }

    @Test
    public void testReadArrayAndReadLineShareLineCounter() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a\nbc\nd\n");
        assertEquals('a', br.read());
        assertEquals('\n', br.read());
        assertEquals(1, br.getLineNumber());
        assertEquals("bc", br.readLine());
        assertEquals(2, br.getLineNumber());
        assertEquals('d', br.read());
        assertEquals('\n', br.read());
        assertEquals(2, br.getLineNumber());
    }

    @Test
    public void testLineNumberIsZeroForFreshReader() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a\nb");
        assertEquals(0, br.getLineNumber());
    }

    private ExtendedBufferedReader getBufferedReader(String s) {
        return new ExtendedBufferedReader(new StringReader(s));
    }
}
