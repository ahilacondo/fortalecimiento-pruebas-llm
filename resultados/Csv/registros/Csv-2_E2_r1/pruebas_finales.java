package org.apache.commons.csv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

public class CSVRecordLLMTest {

    private static final String[] VALUES = new String[] { "first", "second", "third" };

    private static Map<String, Integer> headerOf(final String... names) {
        final Map<String, Integer> map = new HashMap<String, Integer>();
        for (int i = 0; i < names.length; i++) {
            map.put(names[i], Integer.valueOf(i));
        }
        return map;
    }

    @Test
    public void testNullValuesAreReplacedByEmptyArray() {
        final CSVRecord record = new CSVRecord(null, null, null, 0);
        assertEquals("Un registro con valores nulos debe tener tamaño 0", 0, record.size());
        assertEquals("El toString de un registro vacío debe ser []", "[]", record.toString());
        assertFalse("El iterador de un registro vacío no debe tener elementos",
                record.iterator().hasNext());
    }

    @Test
    public void testSizeReturnsNumberOfValues() {
        final CSVRecord record = new CSVRecord(VALUES, null, null, 0);
        assertEquals(3, record.size());
    }

    @Test
    public void testSizeOfEmptyValuesIsZero() {
        final CSVRecord record = new CSVRecord(new String[0], null, null, 0);
        assertEquals(0, record.size());
    }

    @Test
    public void testToStringFormatsAllValues() {
        final CSVRecord record = new CSVRecord(VALUES, null, null, 0);
        assertEquals("[first, second, third]", record.toString());
    }

    @Test
    public void testToStringOfEmptyRecord() {
        final CSVRecord record = new CSVRecord(new String[0], null, null, 0);
        assertEquals("[]", record.toString());
    }

    @Test
    public void testGetCommentReturnsProvidedComment() {
        final CSVRecord record = new CSVRecord(VALUES, null, "un comentario", 5);
        assertEquals("un comentario", record.getComment());
    }

    @Test
    public void testGetCommentIsNullWhenNotProvided() {
        final CSVRecord record = new CSVRecord(VALUES, null, null, 5);
        assertNull("Sin comentario el resultado debe ser null", record.getComment());
    }

    @Test
    public void testGetRecordNumberReturnsProvidedNumber() {
        final CSVRecord record = new CSVRecord(VALUES, null, null, 42L);
        assertEquals(42L, record.getRecordNumber());
    }

    @Test
    public void testGetRecordNumberDefaultsToGivenZero() {
        final CSVRecord record = new CSVRecord(VALUES, null, null, 0L);
        assertEquals(0L, record.getRecordNumber());
    }

    @Test
    public void testGetByNameReturnsNullForUnknownColumn() {
        final CSVRecord record = new CSVRecord(VALUES, headerOf("first", "second", "third"), null, 0);
        assertNull("Una columna no mapeada debe devolver null", record.get("fourth"));
    }

    @Test
    public void testGetByNameIsCaseSensitive() {
        final CSVRecord record = new CSVRecord(VALUES, headerOf("first", "second", "third"), null, 0);
        assertNull("Los nombres de columna distinguen mayúsculas", record.get("FIRST"));
        assertEquals("first", record.get("first"));
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetByIndexOutOfRangeThrows() {
        final CSVRecord record = new CSVRecord(VALUES, null, null, 0);
        record.get(3);
    }

    @Test
    public void testIsConsistentFalseWhenMappingSmallerThanValues() {
        final CSVRecord record = new CSVRecord(VALUES, headerOf("first", "second"), null, 0);
        assertFalse("Un mapeo de tamaño distinto al de valores no es consistente",
                record.isConsistent());
    }

    @Test
    public void testIsMappedIsCaseSensitive() {
        final CSVRecord record = new CSVRecord(VALUES, headerOf("first", "second", "third"), null, 0);
        assertFalse(record.isMapped("First"));
        assertTrue(record.isMapped("first"));
    }

    @Test
    public void testIsSetFalseWhenMappedIndexIsBeyondValues() {
        final CSVRecord record = new CSVRecord(VALUES, headerOf("first", "second", "third", "fourth"), null, 0);
        assertTrue("La columna está mapeada", record.isMapped("fourth"));
        assertFalse("Un índice fuera de los valores no está establecido", record.isSet("fourth"));
    }

    @Test
    public void testIteratorYieldsAllValuesInOrder() {
        final CSVRecord record = new CSVRecord(VALUES, null, null, 0);
        final Iterator<String> itr = record.iterator();
        for (int i = 0; i < VALUES.length; i++) {
            assertTrue("Debe haber un elemento en la posición " + i, itr.hasNext());
            assertEquals(VALUES[i], itr.next());
        }
        assertFalse("No deben quedar elementos tras recorrer todos los valores", itr.hasNext());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIteratorRemoveIsNotSupported() {
        final CSVRecord record = new CSVRecord(VALUES, null, null, 0);
        final Iterator<String> itr = record.iterator();
        itr.next();
        itr.remove();
    }

    @Test
    public void testRecordIsIterableAndSerializable() {
        final CSVRecord record = new CSVRecord(VALUES, null, null, 0);
        assertTrue("CSVRecord debe implementar Iterable<String>", record instanceof Iterable);
        assertTrue("CSVRecord debe implementar Serializable", record instanceof Serializable);
    }
}
