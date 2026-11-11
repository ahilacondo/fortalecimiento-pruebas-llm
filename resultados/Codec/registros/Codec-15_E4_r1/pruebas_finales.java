package org.apache.commons.codec.language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.codec.EncoderException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Pruebas adicionales para {@link Soundex} que cubren los requisitos de la fase 2:
 * mapeos personalizados, valor 0 como "no codificar", letras fuera del mapeo,
 * equivalencia de encode(Object) y encode(String), rechazo de tipos no String,
 * uso concurrente, independencia de maxLength y rango de difference.
 */
public class SoundexLLMTest {

    private static final String DEFAULT_MAPPING = Soundex.US_ENGLISH_MAPPING_STRING;

    private static char[] mappingWith(final int index, final char value) {
        final char[] mapping = DEFAULT_MAPPING.toCharArray();
        mapping[index] = value;
        return mapping;
    }

    private static char[] uniformMapping(final char value) {
        final char[] mapping = new char[26];
        Arrays.fill(mapping, value);
        return mapping;
    }

    private static void assertEncoderException(final Soundex encoder, final Object input) {
        try {
            encoder.encode(input);
            Assert.fail("Se esperaba EncoderException para la entrada: " + input);
        } catch (final EncoderException expected) {
            // comportamiento esperado
        }
    }

    private static void assertIllegalArgument(final Soundex encoder, final String input) {
        try {
            encoder.encode(input);
            Assert.fail("Se esperaba IllegalArgumentException para la entrada: " + input);
        } catch (final IllegalArgumentException expected) {
            // comportamiento esperado
        }
    }

    // ---------------------------------------------------------------
    // Requisito 1: los constructores usan el mapeo proporcionado
    // ---------------------------------------------------------------

    @Test
    public void testCustomMappingDrivesGeneratedCodes() {
        // B pasa de '1' a '7'; el resto del mapeo es el por defecto.
        final Soundex custom = new Soundex(mappingWith(1, '7'));
        Assert.assertEquals("A723", custom.encode("ABCD"));
        Assert.assertEquals("A123", new Soundex().encode("ABCD"));
    }

    @Test
    public void testStringConstructorUsesProvidedMapping() {
        final Soundex custom = new Soundex("07230120022455012623010202");
        Assert.assertEquals("A723", custom.encode("ABCD"));
    }

    @Test
    public void testCharArrayConstructorCopiesMappingAndUsesIt() {
        final char[] source = mappingWith(1, '7');
        final Soundex custom = new Soundex(source);
        // Modificar el array original no debe alterar el mapeo ya construido.
        source[1] = '9';
        Assert.assertEquals("A723", custom.encode("ABCD"));
    }

    @Test
    public void testUniformMappingCollapsesAllLettersToFirstLetter() {
        final Soundex uniform = new Soundex(uniformMapping('1'));
        Assert.assertEquals("R000", uniform.encode("Robert"));
        Assert.assertEquals("B000", uniform.encode("Bcdfg"));
    }

    @Test
    public void testStringConstructorWithDefaultMappingMatchesDefaultEncoder() {
        final String[] words = {"Robert", "Williams", "Tymczak", "Ashcraft", "Pfister", "Lee", ""};
        final Soundex fromString = new Soundex(DEFAULT_MAPPING);
        for (final String word : words) {
            Assert.assertEquals(Soundex.US_ENGLISH.encode(word), fromString.encode(word));
            Assert.assertEquals(new Soundex().encode(word), fromString.encode(word));
        }
    }

    // ---------------------------------------------------------------
    // Requisito 2: el valor 0 en el mapeo significa "no codificar"
    // ---------------------------------------------------------------

    @Test
    public void testZeroCharInMappingMeansLetterIsNotEncoded() {
        // D pasa a '0' (no codificar). En "MDL", D no aporta código.
        Assert.assertEquals("M400", new Soundex(mappingWith(3, '0')).encode("MDL"));
        Assert.assertEquals("M400", new Soundex("01200120022455012623010202").encode("MDL"));
        // Con el mapeo por defecto, D sí aporta '3'.
        Assert.assertEquals("M340", new Soundex().encode("MDL"));
    }

    @Test
    public void testNullCharInMappingSkipsLetterWithoutSeparatingEqualCodes() {
        // Con el carácter '\u0000', la letra D se omite por completo y no reinicia el "último" código.
        // Por tanto las dos M (código 5) quedan juntas y se colapsan.
        final Soundex nullZero = new Soundex(mappingWith(3, (char) 0));
        Assert.assertEquals("M000", nullZero.encode("MDM"));
        // Con el carácter '0', la letra D sí actúa como separador y la segunda M se codifica.
        final Soundex charZero = new Soundex(mappingWith(3, '0'));
        Assert.assertEquals("M500", charZero.encode("MDM"));
    }

    // ---------------------------------------------------------------
    // Requisito 3: letras fuera de la longitud del mapeo lanzan IllegalArgumentException
    // ---------------------------------------------------------------

    @Test
    public void testLetterBeyondCustomMappingLengthThrows() {
        final Soundex shortMapping = new Soundex("01230");
        assertIllegalArgument(shortMapping, "AZ");
        assertIllegalArgument(shortMapping, "F");
    }

    @Test
    public void testLetterBeyondCustomMappingLengthThrowsForCharArrayConstructor() {
        final Soundex shortMapping = new Soundex("01230".toCharArray());
        assertIllegalArgument(shortMapping, "AZ");
        assertIllegalArgument(shortMapping, "F");
    }

    @Test
    public void testLettersWithinCustomMappingLengthAreEncoded() {
        final Soundex shortMapping = new Soundex("01230");
        Assert.assertEquals("A123", shortMapping.encode("ABCDE"));
        Assert.assertEquals("E000", shortMapping.encode("E"));
    }

    // ---------------------------------------------------------------
    // Requisito 4: encode(Object) devuelve lo mismo que encode(String) para Strings
    // ---------------------------------------------------------------

    @Test
    public void testEncodeObjectMatchesEncodeStringForStrings() throws EncoderException {
        final String[] words = {"Robert", "Rupert", "Williams", "Tymczak", "Pfister", "Lee", "A"};
        final Soundex encoder = Soundex.US_ENGLISH;
        for (final String word : words) {
            final Object asObject = word;
            Assert.assertEquals(encoder.encode(word), encoder.encode(asObject));
        }
    }

    // ---------------------------------------------------------------
    // Requisito 5: rechazo de tipos que no son String
    // ---------------------------------------------------------------

    @Test
    public void testEncodeObjectRejectsNonStringTypes() {
        final Soundex encoder = Soundex.US_ENGLISH;
        assertEncoderException(encoder, Integer.valueOf(123));
        assertEncoderException(encoder, Double.valueOf(1.5));
        assertEncoderException(encoder, new Object());
    }

    // ---------------------------------------------------------------
    // Requisito 6: uso concurrente seguro
    // ---------------------------------------------------------------

    @Test
    public void testConcurrentEncodingProducesConsistentResults() throws Exception {
        final Soundex encoder = Soundex.US_ENGLISH;
        final ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            final List<Future<String>> futures = new ArrayList<Future<String>>();
            for (int i = 0; i < 200; i++) {
                futures.add(pool.submit(new Callable<String>() {
                    @Override
                    public String call() {
                        return encoder.encode("Robert");
                    }
                }));
            }
            for (final Future<String> future : futures) {
                Assert.assertEquals("R163", future.get());
            }
        } finally {
            pool.shutdownNow();
        }
    }

    // ---------------------------------------------------------------
    // Requisito 7: independencia de maxLength
    // ---------------------------------------------------------------

    @Test
    public void testMaxLengthOfOneInstanceDoesNotAffectSharedInstance() {
        final Soundex other = new Soundex();
        other.setMaxLength(6);
        Assert.assertEquals("R163", Soundex.US_ENGLISH.encode("Robert"));
    }

    // ---------------------------------------------------------------
    // Requisito 8: rango de difference
    // ---------------------------------------------------------------

    @Test
    public void testDifferenceIsWithinZeroToFour() throws EncoderException {
        final Soundex encoder = Soundex.US_ENGLISH;
        Assert.assertEquals(4, encoder.difference("Robert", "Rupert"));
        Assert.assertEquals(4, encoder.difference("Robert", "Robert"));
        final int d = encoder.difference("Robert", "Zzzz");
        Assert.assertTrue("difference fuera de rango: " + d, d >= 0 && d <= 4);
    }
}
