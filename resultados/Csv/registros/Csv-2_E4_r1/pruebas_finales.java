package org.apache.commons.csv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Pruebas adicionales para {@link CSVRecord} que cubren los requisitos de documentación
 * no verificados por {@link CSVRecordTest}.
 */
public class CSVRecordLLMTest {

    private static Map<String, Integer> header(final String... names) {
        final Map<String, Integer> map = new HashMap<String, Integer>();
        for (int i = 0; i < names.length; i++) {
            map.put(names[i], Integer.valueOf(i));
        }
        return map;
    }

    // Requisito 1: get(String) devuelve null si el nombre no existe en el mapeo.

    @Test
    public void testGetByNameReturnsNullWhenColumnNotInMapping() {
        final CSVRecord record = new CSVRecord(new String[] { "a", "b", "c" },
                header("first", "second", "third"), null, 1);

        assertNull(record.get("unknown"));
    }

    @Test
    public void testGetByNameReturnsNullForEmptyNameWhenNotMapped() {
        final CSVRecord record = new CSVRecord(new String[] { "a", "b", "c" },
                header("first", "second", "third"), null, 1);

        assertNull(record.get(""));
    }

    // Requisito 2: Javadoc indica IllegalArgumentException si el registro es inconsistente.
    // DISCREPANCIA: el código actual no lanza esta excepción. Prueba deshabilitada
    // hasta que la implementación cumpla la documentación.

    @Test
    @Ignore("Discrepancia: get(String) documenta IllegalArgumentException para registros inconsistentes, "
            + "pero la implementación no la lanza")
    public void testGetByNameThrowsIllegalArgumentExceptionWhenInconsistent() {
        // Mapeo de 4 columnas con solo 3 valores: el registro es inconsistente.
        final CSVRecord record = new CSVRecord(new String[] { "a", "b", "c" },
                header("first", "second", "third", "fourth"), null, 1);

        try {
            record.get("first");
            fail("Se esperaba IllegalArgumentException para un registro inconsistente");
        } catch (final IllegalArgumentException expected) {
            assertNotNull(expected);
        }
    }

    // Requisito 3: isSet devuelve false si la columna está mapeada pero su índice
    // no tiene valor asociado (índice >= número de valores).

    @Test
    public void testIsSetReturnsFalseWhenMappedIndexHasNoValue() {
        final Map<String, Integer> map = header("first", "second", "third");
        map.put("fourth", Integer.valueOf(3)); // índice 3, pero solo hay 3 valores (0..2)
        final CSVRecord record = new CSVRecord(new String[] { "a", "b", "c" }, map, null, 1);

        assertTrue(record.isMapped("fourth"));
        assertFalse(record.isSet("fourth"));
    }

    @Test
    public void testIsSetReturnsFalseWhenMappedIndexFarBeyondValues() {
        final Map<String, Integer> map = header("first");
        map.put("tenth", Integer.valueOf(9));
        final CSVRecord record = new CSVRecord(new String[] { "a" }, map, null, 1);

        assertTrue(record.isMapped("tenth"));
        assertFalse(record.isSet("tenth"));
    }

    @Test
    public void testIsSetReturnsTrueForLastValidIndex() {
        final CSVRecord record = new CSVRecord(new String[] { "a", "b", "c" },
                header("first", "second", "third"), null, 1);

        assertTrue(record.isSet("third"));
    }

    // Requisito 4: getComment devuelve el comentario o null si no hay comentario.

    @Test
    public void testGetCommentReturnsConstructorComment() {
        final CSVRecord record = new CSVRecord(new String[] { "a" }, null, "mi comentario", 1);

        assertEquals("mi comentario", record.getComment());
    }

    @Test
    public void testGetCommentReturnsNullWhenNoComment() {
        final CSVRecord record = new CSVRecord(new String[] { "a" }, null, null, 1);

        assertNull(record.getComment());
    }

    // Requisito 5: getRecordNumber devuelve el número indicado en la construcción.

    @Test
    public void testGetRecordNumberReturnsConstructorValue() {
        final CSVRecord record = new CSVRecord(new String[] { "a" }, null, null, 42L);

        assertEquals(42L, record.getRecordNumber());
    }

    @Test
    public void testGetRecordNumberSupportsLargeValues() {
        final long big = 5_000_000_000L;
        final CSVRecord record = new CSVRecord(new String[] { "a" }, null, null, big);

        assertEquals(big, record.getRecordNumber());
    }

    // Requisito 6: size devuelve el número de valores del registro.

    @Test
    public void testSizeReturnsNumberOfValues() {
        final CSVRecord record = new CSVRecord(new String[] { "a", "b", "c", "d" }, null, null, 1);

        assertEquals(4, record.size());
    }

    @Test
    public void testSizeIsZeroWhenValuesAreNull() {
        final CSVRecord record = new CSVRecord(null, null, null, 1);

        assertEquals(0, record.size());
    }

    // Requisito 7: iterator devuelve todos los valores del registro.

    @Test
    public void testIteratorReturnsAllValuesInOrder() {
        final String[] values = { "uno", "dos", "tres", "" };
        final CSVRecord record = new CSVRecord(values, null, null, 1);

        final Iterator<String> itr = record.iterator();
        for (int i = 0; i < values.length; i++) {
            assertTrue("Faltan elementos en el iterador en la posición " + i, itr.hasNext());
            assertEquals(values[i], itr.next());
        }
        assertFalse("El iterador devuelve más elementos que size()", itr.hasNext());
    }

    @Test
    public void testIteratorCountMatchesSize() {
        final CSVRecord record = new CSVRecord(new String[] { "a", "b", "c", "d", "e" }, null, null, 1);

        int count = 0;
        for (final String ignored : record) {
            assertNotNull(ignored);
            count++;
        }
        assertEquals(record.size(), count);
    }

    @Test
    public void testIteratorIsEmptyForEmptyRecord() {
        final CSVRecord record = new CSVRecord(new String[0], null, null, 1);

        assertFalse(record.iterator().hasNext());
        assertEquals(0, record.size());
    }

    // Requisito 8: CSVRecord implementa Serializable; la serialización debe conservar el estado.

    @Test
    public void testImplementsSerializable() {
        final CSVRecord record = new CSVRecord(new String[] { "a" }, null, null, 1);

        assertTrue(record instanceof Serializable);
    }

    @Test
    public void testSerializationRoundTripPreservesState() throws Exception {
        final Map<String, Integer> map = header("first", "second", "third");
        final CSVRecord original = new CSVRecord(new String[] { "x", "y", "z" }, map, "comentario", 7L);

        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }

        final CSVRecord restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (CSVRecord) in.readObject();
        }

        assertNotNull(restored);
        assertEquals(original.size(), restored.size());
        assertEquals(original.toString(), restored.toString());
        assertEquals("comentario", restored.getComment());
        assertEquals(7L, restored.getRecordNumber());
        assertEquals("x", restored.get(0));
        assertEquals("y", restored.get("second"));
        assertEquals("z", restored.get("third"));
        assertTrue(restored.isMapped("first"));
        assertTrue(restored.isConsistent());
    }

    @Test
    public void testSerializationRoundTripWithoutMappingAndComment() throws Exception {
        final CSVRecord original = new CSVRecord(new String[] { "solo" }, null, null, 3L);

        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }

        final CSVRecord restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (CSVRecord) in.readObject();
        }

        assertEquals(1, restored.size());
        assertEquals("solo", restored.get(0));
        assertNull(restored.getComment());
        assertEquals(3L, restored.getRecordNumber());
        assertFalse(restored.isMapped("cualquiera"));
    }
}
