package org.apache.commons.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link DefaultParser} que cubren los requisitos
 * de documentación no verificados por las pruebas existentes.
 */
public class DefaultParserLLMTest {

    private final DefaultParser parser = new DefaultParser();

    /**
     * Requisito 1: parse lanza MissingOptionException con la lista de opciones
     * requeridas que faltan.
     */
    @Test
    public void testParseThrowsMissingOptionExceptionListingAllMissingRequiredOptions() {
        Options options = new Options();
        options.addOption(Option.builder("a").required().build());
        options.addOption(Option.builder("b").required().build());

        try {
            parser.parse(options, new String[0]);
            fail("Se esperaba MissingOptionException");
        } catch (MissingOptionException e) {
            List<?> missing = e.getMissingOptions();
            assertEquals(2, missing.size());
            assertTrue("Debe incluir 'a'", missing.contains("a"));
            assertTrue("Debe incluir 'b'", missing.contains("b"));
        } catch (ParseException e) {
            fail("Excepción inesperada: " + e);
        }
    }

    /**
     * Requisito 1 (complemento): solo se listan las requeridas que no aparecen
     * en la línea de comandos.
     */
    @Test
    public void testParseMissingOptionExceptionOnlyListsRemainingRequiredOptions() {
        Options options = new Options();
        options.addOption(Option.builder("a").required().build());
        options.addOption(Option.builder("b").required().build());

        try {
            parser.parse(options, new String[] {"-a"});
            fail("Se esperaba MissingOptionException");
        } catch (MissingOptionException e) {
            List<?> missing = e.getMissingOptions();
            assertEquals(1, missing.size());
            assertTrue("Debe incluir 'b'", missing.contains("b"));
            assertFalse("No debe incluir 'a'", missing.contains("a"));
        } catch (ParseException e) {
            fail("Excepción inesperada: " + e);
        }
    }

    /**
     * Requisito 2: parse lanza MissingArgumentException cuando la última opción
     * requiere argumento y no lo recibe.
     */
    @Test
    public void testParseThrowsMissingArgumentExceptionWhenLastOptionLacksArgument() {
        Options options = new Options();
        options.addOption(Option.builder("f").hasArg().build());

        try {
            parser.parse(options, new String[] {"-f"});
            fail("Se esperaba MissingArgumentException");
        } catch (MissingArgumentException e) {
            assertEquals("f", e.getOption().getOpt());
        } catch (ParseException e) {
            fail("Excepción inesperada: " + e);
        }
    }

    /**
     * Requisito 2 (contraste): con el argumento presente no se lanza excepción
     * y el valor queda asociado a la opción.
     */
    @Test
    public void testParseAcceptsOptionWhenArgumentIsProvided() throws ParseException {
        Options options = new Options();
        options.addOption(Option.builder("f").hasArg().build());

        CommandLine cmd = parser.parse(options, new String[] {"-f", "valor"});

        assertTrue(cmd.hasOption("f"));
        assertEquals("valor", cmd.getOptionValue("f"));
    }

    /**
     * Requisito 3: con stopAtNonOption en false, un token desconocido que empieza
     * por guion (forma corta) lanza UnrecognizedOptionException.
     */
    @Test
    public void testUnknownShortTokenThrowsUnrecognizedOptionWhenStopAtNonOptionIsFalse() {
        Options options = new Options();
        options.addOption(Option.builder("v").build());

        try {
            parser.parse(options, new String[] {"-z"}, false);
            fail("Se esperaba UnrecognizedOptionException");
        } catch (UnrecognizedOptionException e) {
            assertEquals("-z", e.getOption());
        } catch (ParseException e) {
            fail("Excepción inesperada: " + e);
        }
    }

    /**
     * Requisito 3 (variante larga): un token largo desconocido también lanza
     * UnrecognizedOptionException con stopAtNonOption en false.
     */
    @Test
    public void testUnknownLongTokenThrowsUnrecognizedOptionWhenStopAtNonOptionIsFalse() {
        Options options = new Options();
        options.addOption(Option.builder("v").build());

        try {
            parser.parse(options, new String[] {"--desconocida"}, false);
            fail("Se esperaba UnrecognizedOptionException");
        } catch (UnrecognizedOptionException e) {
            assertEquals("--desconocida", e.getOption());
        } catch (ParseException e) {
            fail("Excepción inesperada: " + e);
        }
    }

    /**
     * Requisito 4: con stopAtNonOption en true, el primer token no reconocido
     * detiene el análisis y todos los restantes se añaden tal cual como argumentos,
     * incluidos los que parecen opciones conocidas o desconocidas.
     */
    @Test
    public void testStopAtNonOptionAddsRemainingTokensAsArgumentsUnchanged() throws ParseException {
        Options options = new Options();
        options.addOption(Option.builder("v").build());

        CommandLine cmd = parser.parse(options, new String[] {"-v", "foo", "-x", "-v", "bar"}, true);

        assertTrue("La opción -v previa al token no reconocido debe estar presente", cmd.hasOption("v"));
        assertEquals(Arrays.asList("foo", "-x", "-v", "bar"), cmd.getArgList());
    }

    /**
     * Requisito 4 (verificación): tras detenerse, una opción definida que aparece
     * después no se procesa como opción.
     */
    @Test
    public void testStopAtNonOptionDoesNotProcessDefinedOptionsAfterStop() throws ParseException {
        Options options = new Options();
        options.addOption(Option.builder("v").build());

        CommandLine cmd = parser.parse(options, new String[] {"foo", "-v"}, true);

        assertFalse("-v tras el primer no-opción no debe procesarse", cmd.hasOption("v"));
        assertEquals(Arrays.asList("foo", "-v"), cmd.getArgList());
    }

    /**
     * Requisito 5: tras el token "--", todos los tokens siguientes se tratan como
     * argumentos aunque empiecen por guion, y el propio "--" no se añade.
     */
    @Test
    public void testTokensAfterEndOfOptionsMarkerAreTreatedAsArguments() throws ParseException {
        Options options = new Options();
        options.addOption(Option.builder("v").build());

        CommandLine cmd = parser.parse(options, new String[] {"--", "-v", "--w", "-"}, false);

        assertFalse("-v tras '--' no debe procesarse como opción", cmd.hasOption("v"));
        assertEquals(Arrays.asList("-v", "--w", "-"), cmd.getArgList());
        assertFalse("El token '--' no debe aparecer en los argumentos", cmd.getArgList().contains("--"));
    }

    /**
     * Requisito 6: handleProperties lanza UnrecognizedOptionException cuando una
     * clave de las propiedades no corresponde a ninguna opción definida.
     */
    @Test
    public void testPropertyWithUndefinedKeyThrowsUnrecognizedOptionException() {
        Options options = new Options();
        options.addOption(Option.builder("v").build());

        Properties properties = new Properties();
        properties.setProperty("desconocida", "true");

        try {
            parser.parse(options, new String[0], properties);
            fail("Se esperaba UnrecognizedOptionException");
        } catch (UnrecognizedOptionException e) {
            assertEquals("desconocida", e.getOption());
        } catch (ParseException e) {
            fail("Excepción inesperada: " + e);
        }
    }

    /**
     * Requisito 7: las propiedades con valor yes, true o 1 (sin distinguir
     * mayúsculas) activan una opción sin argumento.
     */
    @Test
    public void testPropertyWithYesTrueOrOneActivatesOptionWithoutArgument() throws ParseException {
        String[] activatingValues = {"yes", "true", "1", "YES", "True"};

        for (String value : activatingValues) {
            Options options = new Options();
            options.addOption(Option.builder("v").build());

            Properties properties = new Properties();
            properties.setProperty("v", value);

            CommandLine cmd = parser.parse(options, new String[0], properties);

            assertTrue("El valor '" + value + "' debe activar la opción", cmd.hasOption("v"));
        }
    }

    /**
     * Requisito 7 (complemento): otros valores no activan la opción sin argumento
     * y esta se omite de la línea de comandos.
     */
    @Test
    public void testPropertyWithOtherValuesDoesNotActivateOptionWithoutArgument() throws ParseException {
        String[] nonActivatingValues = {"no", "false", "0", "", "x"};

        for (String value : nonActivatingValues) {
            Options options = new Options();
            options.addOption(Option.builder("v").build());

            Properties properties = new Properties();
            properties.setProperty("v", value);

            CommandLine cmd = parser.parse(options, new String[0], properties);

            assertFalse("El valor '" + value + "' no debe activar la opción", cmd.hasOption("v"));
        }
    }

    /**
     * Requisito 8: el Javadoc de handleConcatenatedOptions indica que, con
     * stopAtNonOption activo, debe añadirse el token especial "--" seguido de los
     * caracteres restantes. El código no añade "--": el carácter desconocido se
     * añade directamente como argumento y el resto de tokens también.
     * Esta prueba documenta el comportamiento real del código.
     */
    @Test
    public void testConcatenatedOptionsWithStopAtNonOptionDoesNotAddEndOfOptionsToken() throws ParseException {
        Options options = new Options();
        options.addOption(Option.builder("a").build());

        CommandLine cmd = parser.parse(options, new String[] {"-ax", "y"}, true);

        assertTrue("La opción 'a' del token concatenado debe reconocerse", cmd.hasOption("a"));
        assertEquals(Arrays.asList("x", "y"), cmd.getArgList());
        assertFalse("No se debe añadir el token '--' según el código actual",
                cmd.getArgList().contains("--"));
    }
}
