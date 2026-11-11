package org.apache.commons.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.junit.Test;

public class DefaultParserLLMTest {

    /* ------------------------------------------------------------------
     * 1. Propiedades sin argumento: "yes", "true" y "1" añaden la opción;
     *    "false", "no" y vacío no la añaden.
     * ------------------------------------------------------------------ */

    @Test
    public void propertyFlagWithYesTrueOrOneAddsOption() throws ParseException {
        String[] truthyValues = {"yes", "true", "1", "YES", "True"};
        for (String value : truthyValues) {
            Options options = new Options();
            options.addOption(new Option("v", false, "verbose"));

            Properties props = new Properties();
            props.setProperty("v", value);

            CommandLine cmd = new DefaultParser().parse(options, new String[0], props);

            assertNotNull("El resultado no debe ser nulo para valor " + value, cmd);
            assertTrue("La opción debe añadirse con valor '" + value + "'", cmd.hasOption("v"));
        }
    }

    @Test
    public void propertyFlagWithFalseNoOrEmptyDoesNotAddOption() throws ParseException {
        String[] falsyValues = {"false", "no", "", "FALSE", "0"};
        for (String value : falsyValues) {
            Options options = new Options();
            options.addOption(new Option("v", false, "verbose"));

            Properties props = new Properties();
            props.setProperty("v", value);

            CommandLine cmd = new DefaultParser().parse(options, new String[0], props);

            assertFalse("La opción no debe añadirse con valor '" + value + "'", cmd.hasOption("v"));
            assertEquals("No deben existir opciones añadidas con valor '" + value + "'",
                    0, cmd.getOptions().length);
        }
    }

    /* ------------------------------------------------------------------
     * 2. Propiedad con nombre que no corresponde a ninguna opción.
     * ------------------------------------------------------------------ */

    @Test
    public void propertyWithUndefinedNameThrowsUnrecognizedOptionException() {
        Options options = new Options();
        options.addOption(new Option("v", false, "verbose"));

        Properties props = new Properties();
        props.setProperty("desconocida", "true");

        try {
            new DefaultParser().parse(options, new String[0], props);
            fail("Se esperaba UnrecognizedOptionException para una propiedad no definida");
        } catch (UnrecognizedOptionException e) {
            assertEquals("El nombre de la opción no reconocida debe coincidir con la propiedad",
                    "desconocida", e.getOption());
        } catch (ParseException e) {
            fail("Se esperaba UnrecognizedOptionException pero llegó: " + e);
        }
    }

    /* ------------------------------------------------------------------
     * 3. Propiedad para una opción de grupo ignorada si otra opción del
     *    grupo ya está seleccionada.
     * ------------------------------------------------------------------ */

    @Test
    public void propertyForGroupOptionIgnoredWhenAnotherGroupOptionSelected() throws ParseException {
        Options options = new Options();
        OptionGroup group = new OptionGroup();
        group.addOption(new Option("a", false, "opción a"));
        group.addOption(new Option("b", false, "opción b"));
        options.addOptionGroup(group);

        Properties props = new Properties();
        props.setProperty("b", "true");

        CommandLine cmd = new DefaultParser().parse(options, new String[] {"-a"}, props);

        assertTrue("La opción -a de la línea de comandos debe estar presente", cmd.hasOption("a"));
        assertFalse("La propiedad para -b debe ignorarse al estar seleccionado -a", cmd.hasOption("b"));
    }

    /* ------------------------------------------------------------------
     * 4. Propiedad para opción con argumento: solo asigna valor si la
     *    opción aún no aparece en la línea de comandos.
     * ------------------------------------------------------------------ */

    @Test
    public void propertyValueAssignedToArgOptionWhenAbsentFromCommandLine() throws ParseException {
        Options options = new Options();
        options.addOption(new Option("f", true, "fichero"));

        Properties props = new Properties();
        props.setProperty("f", "desdePropiedades");

        CommandLine cmd = new DefaultParser().parse(options, new String[0], props);

        assertTrue("La opción -f debe añadirse desde la propiedad", cmd.hasOption("f"));
        assertEquals("El valor debe provenir de la propiedad", "desdePropiedades", cmd.getOptionValue("f"));
    }

    @Test
    public void propertyValueNotAssignedToArgOptionWhenPresentInCommandLine() throws ParseException {
        Options options = new Options();
        options.addOption(new Option("f", true, "fichero"));

        Properties props = new Properties();
        props.setProperty("f", "desdePropiedades");

        CommandLine cmd = new DefaultParser().parse(options, new String[] {"-f", "desdeCli"}, props);

        assertEquals("El valor de la línea de comandos debe prevalecer sobre la propiedad",
                "desdeCli", cmd.getOptionValue("f"));
        assertEquals("La opción -f no debe duplicarse", 1, cmd.getOptions().length);
    }

    /* ------------------------------------------------------------------
     * 5. Con stopAtNonOption=true, el primer token no reconocido detiene
     *    el análisis y los siguientes se añaden como argumentos.
     * ------------------------------------------------------------------ */

    @Test
    public void stopAtNonOptionTrueTreatsAllRemainingTokensAsArguments() throws ParseException {
        Options options = new Options();
        options.addOption(new Option("a", false, "opción a"));
        options.addOption(new Option("b", false, "opción b"));

        CommandLine cmd = new DefaultParser().parse(options, new String[] {"x", "-a", "-b"}, true);

        assertEquals("Los tokens tras el primer no reconocido deben ser argumentos",
                Arrays.asList("x", "-a", "-b"), Arrays.asList(cmd.getArgs()));
        assertFalse("-a no debe procesarse como opción", cmd.hasOption("a"));
        assertFalse("-b no debe procesarse como opción", cmd.hasOption("b"));
    }

    @Test
    public void stopAtNonOptionTrueStillProcessesOptionsBeforeFirstNonOption() throws ParseException {
        Options options = new Options();
        options.addOption(new Option("a", false, "opción a"));
        options.addOption(new Option("b", false, "opción b"));

        CommandLine cmd = new DefaultParser().parse(options, new String[] {"-a", "x", "-b"}, true);

        assertTrue("-a anterior al primer no reconocido debe procesarse", cmd.hasOption("a"));
        assertFalse("-b posterior al primer no reconocido no debe procesarse", cmd.hasOption("b"));
        assertEquals("Los tokens restantes deben añadirse como argumentos",
                Arrays.asList("x", "-b"), Arrays.asList(cmd.getArgs()));
    }

    /* ------------------------------------------------------------------
     * 6. El token "-" aislado es un argumento posicional, no una opción.
     * ------------------------------------------------------------------ */

    @Test
    public void lonelyDashIsTreatedAsPositionalArgument() throws ParseException {
        Options options = new Options();
        options.addOption(new Option("a", false, "opción a"));

        CommandLine cmd = new DefaultParser().parse(options, new String[] {"-"});

        assertEquals("El token '-' debe añadirse como argumento", Collections.singletonList("-"),
                Arrays.asList(cmd.getArgs()));
        assertEquals("No debe registrarse ninguna opción", 0, cmd.getOptions().length);
    }

    @Test
    public void lonelyDashAfterOptionIsPositionalArgument() throws ParseException {
        Options options = new Options();
        options.addOption(new Option("a", false, "opción a"));

        CommandLine cmd = new DefaultParser().parse(options, new String[] {"-a", "-"});

        assertTrue("-a debe reconocerse", cmd.hasOption("a"));
        assertEquals("'-' debe quedar como argumento posicional", Collections.singletonList("-"),
                Arrays.asList(cmd.getArgs()));
    }

    /* ------------------------------------------------------------------
     * 7. Valor numérico negativo aceptado como argumento de una opción
     *    aunque coincida con el patrón de una opción corta.
     * ------------------------------------------------------------------ */

    @Test
    public void negativeNumberIsAcceptedAsOptionValueEvenIfMatchesShortOption() throws ParseException {
        Options options = new Options();
        options.addOption(new Option("n", true, "número"));
        options.addOption(new Option("5", false, "opción corta cuyo nombre es un dígito"));

        CommandLine cmd = new DefaultParser().parse(options, new String[] {"-n", "-5"});

        assertEquals("'-5' debe tomarse como valor de -n", "-5", cmd.getOptionValue("n"));
        assertFalse("'-5' no debe interpretarse como la opción corta '5'", cmd.hasOption("5"));
    }

    /* ------------------------------------------------------------------
     * 8. Reutilizar la misma instancia reinicia el estado entre llamadas.
     * ------------------------------------------------------------------ */

    @Test
    public void reusingParserResetsRequiredOptions() throws ParseException {
        Options options = new Options();
        Option required = new Option("r", false, "requerida");
        required.setRequired(true);
        options.addOption(required);

        DefaultParser parser = new DefaultParser();
        CommandLine first = parser.parse(options, new String[] {"-r"});
        assertTrue("La primera llamada debe encontrar -r", first.hasOption("r"));

        try {
            parser.parse(options, new String[0]);
            fail("La segunda llamada debe fallar porque -r vuelve a ser obligatoria");
        } catch (MissingOptionException e) {
            assertTrue("La excepción debe indicar la opción requerida",
                    e.getMissingOptions().contains("r"));
        }
    }

    @Test
    public void reusingParserResetsGroupSelection() throws ParseException {
        Options options = new Options();
        OptionGroup group = new OptionGroup();
        group.addOption(new Option("a", false, "opción a"));
        group.addOption(new Option("b", false, "opción b"));
        options.addOptionGroup(group);

        DefaultParser parser = new DefaultParser();
        CommandLine first = parser.parse(options, new String[] {"-a"});
        assertTrue("La primera llamada debe seleccionar -a", first.hasOption("a"));

        CommandLine second = parser.parse(options, new String[] {"-b"});

        assertTrue("La segunda llamada debe aceptar -b sin AlreadySelectedException", second.hasOption("b"));
        assertFalse("La selección anterior de -a no debe persistir", second.hasOption("a"));
    }

    @Test
    public void reusingParserResetsCurrentOption() throws ParseException {
        Options options = new Options();
        options.addOption(new Option("f", true, "fichero"));

        DefaultParser parser = new DefaultParser();
        try {
            parser.parse(options, new String[] {"-f"});
            fail("La primera llamada debe fallar porque -f no recibe argumento");
        } catch (MissingArgumentException e) {
            // esperado: deja currentOption apuntando a -f
        }

        CommandLine second = parser.parse(options, new String[] {"x"});

        assertEquals("La opción actual debe reiniciarse y 'x' debe ser un argumento",
                Collections.singletonList("x"), Arrays.asList(second.getArgs()));
        assertFalse("-f no debe aparecer en la segunda llamada", second.hasOption("f"));
    }
}
