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

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Pruebas adicionales para {@link ExtendedBufferedReader} que cubren ramas
 * no ejecutadas por la suite existente: lectura de '\n' con read(), conteo de
 * saltos de línea en lecturas en bloque, comportamiento en EOF y lookAhead.
 */
public class ExtendedBufferedReaderLLMTest {

    @Test
    public void testReadSingleNewlineIncrementsLineNumber() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a\nb");

        assertEquals('a', br.read());
        assertEquals(0, br.getLineNumber());

        assertEquals('\n', br.read());
        assertEquals(1, br.getLineNumber());
        assertEquals('\n', br.readAgain());
    }

    @Test
    public void testReadCharsWithoutNewlineKeepsLineNumberZero() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("abc");
        char[] buf = new char[3];

        assertEquals(3, br.read(buf, 0, 3));
        assertArrayEquals(new char[] {'a', 'b', 'c'}, buf);
        assertEquals(0, br.getLineNumber());
        assertEquals('c', br.readAgain());
    }

    @Test
    public void testBulkReadCountsLineFeeds() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("ab\ncd\n");
        char[] buf = new char[6];

        assertEquals(6, br.read(buf, 0, 6));
        assertEquals(2, br.getLineNumber());
        assertEquals('\n', br.readAgain());
    }

    @Test
    public void testBulkReadCountsCRLFAsSingleLine() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("ab\r\ncd");
        drain(br, 16);

        assertEquals(1, br.getLineNumber());
    }

    @Test
    public void testBulkReadCountsLoneCarriageReturns() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a\rb\rc");
        drain(br, 16);

        assertEquals(2, br.getLineNumber());
    }

    @Test
    public void testBulkReadCountsConsecutiveLineFeedsSeparately() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a\n\nb");
        drain(br, 16);

        assertEquals(2, br.getLineNumber());
    }

    @Test
    public void testBulkReadCountsAcrossSingleCharCalls() throws Exception {
        // Cada llamada lee un solo carácter; el '\n' debe compararse con lastChar.
        ExtendedBufferedReader br = getBufferedReader("a\nb\n");
        drain(br, 1);

        assertEquals(2, br.getLineNumber());
    }

    @Test
    public void testCRLFSplitAcrossSingleCharBulkCallsCountsOnce() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a\r\nb");
        drain(br, 1);

        assertEquals(1, br.getLineNumber());
    }

    @Test
    public void testNewlineAtStartOfBufferAfterReadLFIsCounted() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("\n\nZ");

        assertEquals('\n', br.read());
        assertEquals(1, br.getLineNumber());

        char[] buf = new char[2];
        assertEquals(2, br.read(buf, 0, 2));
        assertEquals(2, br.getLineNumber());
        assertEquals('Z', buf[1]);
        assertEquals('Z', br.readAgain());
    }

    @Test
    public void testBulkReadWritesAtOffset() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("xyz");
        char[] buf = new char[5];

        assertEquals(3, br.read(buf, 2, 3));
        assertEquals(0, buf[0]);
        assertEquals(0, buf[1]);
        assertEquals('x', buf[2]);
        assertEquals('y', buf[3]);
        assertEquals('z', buf[4]);
        assertEquals('z', br.readAgain());
    }

    @Test
    public void testZeroLengthBulkReadPreservesReadAgain() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("xy");

        assertEquals('x', br.read());
        assertEquals(0, br.read(new char[4], 0, 0));
        assertEquals('x', br.readAgain());
        assertEquals('y', br.read());
    }

    @Test
    public void testBulkReadAtEndOfStreamReturnsMinusOneAndSetsEndMarker() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("ab\n");
        char[] buf = new char[10];

        assertEquals(3, br.read(buf, 0, 10));
        assertEquals(1, br.getLineNumber());

        assertEquals(-1, br.read(buf, 0, 10));
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.readAgain());
        assertEquals(1, br.getLineNumber());
    }

    @Test
    public void testReadAtEndOfStreamReturnsMinusOneRepeatedly() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("");

        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.read());
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.readAgain());
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.read());
        assertEquals(0, br.getLineNumber());
    }

    @Test
    public void testReadReturnsCharactersInOrder() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("xyz");

        assertEquals('x', br.read());
        assertEquals('y', br.read());
        assertEquals('z', br.read());
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.read());
    }

    @Test
    public void testReadAgainInitiallyUndefined() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a");

        assertEquals(ExtendedBufferedReader.UNDEFINED, br.readAgain());
        assertEquals(0, br.getLineNumber());
    }

    @Test
    public void testLookAheadOnNewlineDoesNotChangeLineNumberOrLastChar() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("\n");

        assertEquals('\n', br.lookAhead());
        assertEquals(0, br.getLineNumber());
        assertEquals(ExtendedBufferedReader.UNDEFINED, br.readAgain());
        assertEquals('\n', br.read());
        assertEquals(1, br.getLineNumber());
    }

    @Test
    public void testLookAheadAtEndOfStreamReturnsMinusOne() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("");

        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.lookAhead());
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.read());
    }

    @Test
    public void testReadLineSetsReadAgainToLastCharOfLine() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("abc\ndef");

        assertEquals("abc", br.readLine());
        assertEquals('c', br.readAgain());
        assertEquals(1, br.getLineNumber());
    }

    @Test
    public void testReadLineEmptyLineKeepsPreviousLastChar() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("ab\n\n");

        assertEquals("ab", br.readLine());
        assertEquals('b', br.readAgain());
        assertEquals("", br.readLine());
        assertEquals('b', br.readAgain());
        assertEquals(2, br.getLineNumber());
    }

    @Test
    public void testReadLineAtEndOfStreamSetsEndMarker() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("ab");

        assertEquals("ab", br.readLine());
        assertNull(br.readLine());
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, br.readAgain());
    }

    @Test
    public void testReadLineAfterReadUsesRemainingCharacters() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("xyz\nw");

        assertEquals('x', br.read());
        assertEquals("yz", br.readLine());
        assertEquals('z', br.readAgain());
        assertEquals(1, br.getLineNumber());
        assertEquals('w', br.read());
    }

    /**
     * Lee el contenido completo usando lecturas en bloque de tamaño fijo.
     */
    private static void drain(ExtendedBufferedReader br, int bufferSize) throws IOException {
        char[] buf = new char[bufferSize];
        while (br.read(buf, 0, buf.length) != -1) {
            // consumir todo el contenido
        }
    }

    private ExtendedBufferedReader getBufferedReader(String s) {
        return new ExtendedBufferedReader(new StringReader(s));
    }
}
