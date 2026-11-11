package org.apache.commons.csv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link CSVPrinter} que usan únicamente su API pública.
 */
public class CSVPrinterLLMTest {

    private static final String RS = CSVFormat.DEFAULT.getRecordSeparator();

    /** Appendable que registra las llamadas a flush y close. */
    private static final class TrackingAppendable implements Appendable, Flushable, Closeable {
        private final StringBuilder sb = new StringBuilder();
        private boolean flushed;
        private boolean closed;

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
            flushed = true;
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }

    /** Appendable simple, sin Flushable ni Closeable. */
    private static Appendable plainAppendable(final StringBuilder sb) {
        return new Appendable() {
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
        };
    }

    /** Crea un ResultSet simulado mediante un proxy dinámico. */
    private static ResultSet fakeResultSet(final List<String[]> rows, final int columns) {
        final int[] cursor = { -1 };
        final ResultSetMetaData meta = (ResultSetMetaData) Proxy.newProxyInstance(
                CSVPrinterLLMTest.class.getClassLoader(),
                new Class<?>[] { ResultSetMetaData.class },
                (proxy, method, args) -> {
                    if ("getColumnCount".equals(method.getName())) {
                        return columns;
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
                        return cursor[0] < rows.size();
                    case "getString":
                        return rows.get(cursor[0])[(Integer) args[0] - 1];
                    default:
                        throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    @Test
    public void testConstructorKeepsSameAppendable() {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        assertSame(sw, printer.getOut());
        assertEquals("", sw.toString());
    }

    @Test
    public void testPrintWithoutNewlineDoesNotAddRecordSeparator() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        printer.print("abc");
        assertEquals("abc", sw.toString());
    }

    @Test
    public void testPrintNullAfterValueWritesEmptyField() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        printer.print("a");
        printer.print(null);
        assertEquals("a,", sw.toString());
    }

    @Test
    public void testPrintNullUsesCustomNullString() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withNullString("NULL"));
        printer.print(null);
        assertEquals("NULL", sw.toString());
    }

    @Test
    public void testEmptyValueAsFirstFieldIsQuoted() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        printer.printRecord("");
        assertEquals("\"\"" + RS, sw.toString());
    }

    @Test
    public void testEmptyValueAfterDelimiterIsNotQuoted() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        printer.print("a");
        printer.print("");
        assertEquals("a,", sw.toString());
    }

    @Test
    public void testFirstValueStartingWithMinusIsQuoted() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        printer.print("-1");
        assertEquals("\"-1\"", sw.toString());
    }

    @Test
    public void testMiddleValueStartingWithMinusIsNotQuoted() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        printer.print("a");
        printer.print("-1");
        assertEquals("a,-1", sw.toString());
    }

    @Test
    public void testLeadingSpaceIsQuoted() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        printer.print(" a");
        assertEquals("\" a\"", sw.toString());
    }

    @Test
    public void testLeadingHashIsQuoted() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        printer.print("#x");
        assertEquals("\"#x\"", sw.toString());
    }

    @Test
    public void testTrailingSpaceInMiddleFieldIsQuoted() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        printer.print("a");
        printer.print("b ");
        assertEquals("a,\"b \"", sw.toString());
    }

    @Test
    public void testValueWithOnlyQuoteCharIsQuotedAndDoubled() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        printer.print("\"");
        assertEquals("\"\"\"\"", sw.toString());
    }

    @Test
    public void testQuotesInsideValueAreDoubled() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        printer.print("say \"hi\"");
        assertEquals("\"say \"\"hi\"\"\"", sw.toString());
    }

    @Test
    public void testCustomDelimiterTriggersQuotingOnlyForThatDelimiter() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withDelimiter(';'));
        printer.printRecord("a,b", "c;d");
        assertEquals("a,b;\"c;d\"" + RS, sw.toString());
    }

    @Test
    public void testPrintRecordWithNumbersIsNotQuotedWithMinimalPolicy() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        printer.printRecord(Integer.valueOf(1), Double.valueOf(2.5));
        assertEquals("1,2.5" + RS, sw.toString());
    }

    @Test
    public void testQuoteAllQuotesNullAsEmptyQuotedField() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuotePolicy(Quote.ALL));
        printer.print(null);
        assertEquals("\"\"", sw.toString());
    }

    @Test
    public void testNonNumericPolicyQuotesNumericStringButNotNumber() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuotePolicy(Quote.NON_NUMERIC));
        printer.printRecord("1", Double.valueOf(1.5));
        assertEquals("\"1\",1.5" + RS, sw.toString());
    }

    @Test
    public void testNonNumericPolicyQuotesNullObject() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withQuotePolicy(Quote.NON_NUMERIC));
        printer.print(null);
        assertEquals("\"\"", sw.toString());
    }

    @Test
    public void testQuotePolicyNoneEscapesDelimiterAndNewline() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw,
                CSVFormat.DEFAULT.withEscape('!').withQuotePolicy(Quote.NONE));
        printer.print("a\nb");
        assertEquals("a!nb", sw.toString());
    }

    @Test
    public void testQuotePolicyNoneEscapesEscapeCharacter() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw,
                CSVFormat.DEFAULT.withEscape('!').withQuotePolicy(Quote.NONE));
        printer.print("x!y");
        assertEquals("x!!y", sw.toString());
    }

    @Test
    public void testEscapeModeEscapesBackslashItself() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw,
                CSVFormat.DEFAULT.withQuoteChar(null).withEscape('\\'));
        printer.print("a\\b");
        assertEquals("a\\\\b", sw.toString());
    }

    @Test
    public void testEscapeModeEscapesCarriageReturnAsLiteralR() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw,
                CSVFormat.DEFAULT.withQuoteChar(null).withEscape('\\'));
        printer.print("a\rb");
        assertEquals("a\\rb", sw.toString());
    }

    @Test
    public void testPrintAfterPrintlnDoesNotWriteLeadingDelimiter() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        printer.print("a");
        printer.println();
        printer.print("b");
        assertEquals("a" + RS + "b", sw.toString());
    }

    @Test
    public void testPrintlnWritesOnlyRecordSeparator() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        printer.println();
        assertEquals(RS, sw.toString());
    }

    @Test
    public void testPrintRecordWithNoValuesWritesOnlyRecordSeparator() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        printer.printRecord();
        assertEquals(RS, sw.toString());
    }

    @Test
    public void testPrintRecordWithEmptyIterableWritesOnlyRecordSeparator() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        printer.printRecord(Collections.emptyList());
        assertEquals(RS, sw.toString());
    }

    @Test
    public void testPrintRecordWithIterableQuotesDelimiterValues() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        printer.printRecord(Arrays.asList("a,b", "c"));
        assertEquals("\"a,b\",c" + RS, sw.toString());
    }

    @Test
    public void testPrintRecordsIterableOfPlainValuesPrintsOneRecordPerValue() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        printer.printRecords(Arrays.asList("x", "y"));
        assertEquals("x" + RS + "y" + RS, sw.toString());
    }

    @Test
    public void testPrintRecordsMixedArrayAndIterableAndScalar() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        final List<Object> rows = new ArrayList<>();
        rows.add(new Object[] { "a", "b" });
        rows.add(Arrays.asList("c", "d"));
        rows.add("e");
        printer.printRecords(rows);
        assertEquals("a,b" + RS + "c,d" + RS + "e" + RS, sw.toString());
    }

    @Test
    public void testPrintRecordsObjectArrayWithNullWritesEmptyField() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        printer.printRecords(new Object[] { new Object[] { "c", null } });
        assertEquals("c," + RS, sw.toString());
    }

    @Test
    public void testPrintRecordsFromResultSetWithQuotedAndNullValues() throws Exception {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT);
        final List<String[]> rows = new ArrayList<>();
        rows.add(new String[] { "1", "a,b" });
        rows.add(new String[] { "2", null });
        printer.printRecords(fakeResultSet(rows, 2));
        assertEquals("1,\"a,b\"" + RS + "2," + RS, sw.toString());
    }

    @Test
    public void testCommentOnFreshPrinterHasNoLeadingRecordSeparator() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withCommentStart('#'));
        printer.printComment("x");
        assertEquals("# x" + RS, sw.toString());
    }

    @Test
    public void testCommentAfterValueStartsNewLine() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withCommentStart('#'));
        printer.print("a");
        printer.printComment("c");
        assertEquals("a" + RS + "# c" + RS, sw.toString());
    }

    @Test
    public void testEmptyCommentWritesCommentStartAndSpace() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withCommentStart('#'));
        printer.printComment("");
        assertEquals("# " + RS, sw.toString());
    }

    @Test
    public void testCommentWithCrLfProducesTwoCommentLines() throws IOException {
        final StringWriter sw = new StringWriter();
        final CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withCommentStart('#'));
        printer.printComment("l1\r\nl2");
        assertEquals("# l1" + RS + "# l2" + RS, sw.toString());
    }

    @Test
    public void testFlushDelegatesToFlushableAppendable() throws IOException {
        final TrackingAppendable out = new TrackingAppendable();
        final CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT);
        assertFalse(out.flushed);
        printer.flush();
        assertTrue(out.flushed);
    }

    @Test
    public void testCloseDelegatesToCloseableAppendable() throws IOException {
        final TrackingAppendable out = new TrackingAppendable();
        final CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT);
        assertFalse(out.closed);
        printer.close();
        assertTrue(out.closed);
    }

    @Test
    public void testFlushAndCloseOnNonFlushableAppendableDoNotFail() throws IOException {
        final StringBuilder sb = new StringBuilder();
        final CSVPrinter printer = new CSVPrinter(plainAppendable(sb), CSVFormat.DEFAULT);
        printer.printRecord("a", "b");
        printer.flush();
        printer.close();
        assertEquals("a,b" + RS, sb.toString());
    }
}
