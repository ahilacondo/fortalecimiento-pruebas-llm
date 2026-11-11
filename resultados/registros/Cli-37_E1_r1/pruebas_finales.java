package org.apache.commons.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link DefaultParser} que usan solo la API pública.
 */
public class DefaultParserLLMTest {

    // ------------------------------------------------------------------
    // Opciones cortas
    // ------------------------------------------------------------------

    @Test
    public void testShortFlagIsRecognized() throws Exception {
        Options options = new Options();
        options.addOption(new Option("a", "flag a"));

        CommandLine cl = new DefaultParser().parse(options, new String[] {"-a"});

        assertTrue("La opción -a debe estar presente", cl.hasOption("a"));
        assertEquals("No debe haber argumentos sobrantes", 0, cl.getArgs().length);
    }

    @Test
    public void testShortOptionWithSeparateValue() throws Exception {
        Options options = new Options();
        options.addOption(new Option("f", true, "archivo"));

        CommandLine cl = new DefaultParser().parse(options, new String[] {"-f", "fichero.txt"});

        assertTrue(cl.hasOption("f"));
        assertEquals("fichero.txt", cl.getOptionValue("f"));
    }

    @Test
    public void testShortOptionWithAttachedValue() throws Exception {
        Options options = new Options();
        options.addOption(new Option("f", true, "archivo"));

        CommandLine cl = new DefaultParser().parse(options, new String[] {"-fvalor"});

        assertTrue(cl.hasOption("f"));
        assertEquals("valor", cl.getOptionValue("f"));
    }

    @Test
    public void testConcatenatedShortFlags() throws Exception {
        Options options = new Options();
        options.addOption(new Option("a", "flag a"));
        options.addOption(new Option("b", "flag b"));

        CommandLine cl = new DefaultParser().parse(options, new String[] {"-ab"});

        assertTrue("-a debe estar presente", cl.hasOption("a"));
        assertTrue("-b debe estar presente", cl.hasOption("b"));
    }

    // ------------------------------------------------------------------
    // Opciones largas
    // ------------------------------------------------------------------

    @Test
    public void testLongOptionWithEqualsValue() throws Exception {
        Options options = new Options();
        options.addOption(new Option("f", "file", true, "archivo"));

        CommandLine cl = new DefaultParser().parse(options, new String[] {"--file=x.txt"});

        assertTrue(cl.hasOption("f"));
        assertEquals("x.txt", cl.getOptionValue("f"));
    }

    @Test
    public void testLongOptionWithSeparateValue() throws Exception {
        Options options = new Options();
        options.addOption(new Option("f", "file", true, "archivo"));

        CommandLine cl = new DefaultParser().parse(options, new String[] {"--file", "y.txt"});

        assertTrue(cl.hasOption("file"));
        assertEquals("y.txt", cl.getOptionValue("file"));
    }

    @Test
    public void testQuotedValueIsStripped() throws Exception {
        Options options = new Options();
        options.addOption(new Option("f", "file", true, "archivo"));

        CommandLine cl = new DefaultParser().parse(options, new String[] {"--file", "\"a b\""});

        assertEquals("a b", cl.getOptionValue("f"));
    }

    @Test
    public void testPartialLongOptionResolvesWhenUnique() throws Exception {
        Options options = new Options();
        options.addOption(new Option("v", "verbose", false, "verboso"));
        options.addOption(new Option("x", "extra", false, "extra"));

        CommandLine cl = new DefaultParser().parse(options, new String[] {"--verb"});

        assertTrue("El prefijo único --verb debe resolver a verbose", cl.hasOption("v"));
        assertFalse(cl.hasOption("x"));
    }

    @Test
    public void testAmbiguousLongOptionThrows() throws Exception {
        Options options = new Options();
        options.addOption(new Option("v", "verbose", false, "verboso"));
        options.addOption(new Option("V", "version", false, "versión"));

        try {
            new DefaultParser().parse(options, new String[] {"--ver"});
            fail("Se esperaba AmbiguousOptionException");
        } catch (AmbiguousOptionException e) {
            assertNotNull(e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Valores múltiples, argumentos opcionales y casos especiales
    // ------------------------------------------------------------------

    @Test
    public void testMultipleValuesForUnlimitedOption() throws Exception {
        Option list = new Option("l", "list", true, "lista");
        list.setArgs(Option.UNLIMITED_VALUES);
        Options options = new Options();
        options.addOption(list);

        CommandLine cl = new DefaultParser().parse(options, new String[] {"-l", "a", "b"});

        assertEquals(Arrays.asList("a", "b"), Arrays.asList(cl.getOptionValues("l")));
        assertEquals(0, cl.getArgs().length);
    }

    @Test
    public void testExtraTokenAfterFullOptionBecomesArg() throws Exception {
        Options options = new Options();
        options.addOption(new Option("f", true, "archivo"));

        CommandLine cl = new DefaultParser().parse(options, new String[] {"-f", "val", "extra"});

        assertEquals("val", cl.getOptionValue("f"));
        assertEquals(Arrays.asList("extra"), Arrays.asList(cl.getArgs()));
    }

    @Test
    public void testNegativeNumberIsValueNotOption() throws Exception {
        Options options = new Options();
        options.addOption(new Option("n", true, "número"));

        CommandLine cl = new DefaultParser().parse(options, new String[] {"-n", "-5"});

        assertEquals("-5", cl.getOptionValue("n"));
    }

    @Test
    public void testEmptyAndSingleHyphenAreTreatedAsArgs() throws Exception {
        Options options = new Options();
        options.addOption(new Option("a", "flag a"));

        CommandLine cl = new DefaultParser().parse(options, new String[] {"", "-"});

        assertEquals(Arrays.asList("", "-"), Arrays.asList(cl.getArgs()));
    }

    @Test
    public void testOptionalArgumentMayBeOmitted() throws Exception {
        Option opt = new Option("o", true, "opcional");
        opt.setOptionalArg(true);
        Options options = new Options();
        options.addOption(opt);

        CommandLine cl = new DefaultParser().parse(options, new String[] {"-o"});

        assertTrue(cl.hasOption("o"));
        assertNull("Sin valor, getOptionValue debe devolver null", cl.getOptionValue("o"));
    }

    @Test
    public void testJavaStylePropertyOption() throws Exception {
        Option d = new Option("D", "propiedad", true, "propiedad estilo Java");
        d.setArgs(2);
        Options options = new Options();
        options.addOption(d);

        CommandLine cl = new DefaultParser().parse(options, new String[] {"-Dclave=valor"});

        assertTrue(cl.hasOption("D"));
        assertEquals(Arrays.asList("clave", "valor"), Arrays.asList(cl.getOptionValues("D")));
    }

    // ------------------------------------------------------------------
    // stopAtNonOption y separador "--"
    // ------------------------------------------------------------------

    @Test
    public void testStopAtNonOptionCollectsRemainingTokens() throws Exception {
        Options options = new Options();
        options.addOption(new Option("a", "flag a"));

        CommandLine cl = new DefaultParser().parse(options,
                new String[] {"-a", "arg1", "-z", "arg2"}, true);

        assertTrue(cl.hasOption("a"));
        assertEquals(Arrays.asList("arg1", "-z", "arg2"), Arrays.asList(cl.getArgs()));
    }

    @Test
    public void testDoubleDashStopsOptionProcessing() throws Exception {
        Options options = new Options();
        options.addOption(new Option("a", "flag a"));
        options.addOption(new Option("b", "flag b"));

        CommandLine cl = new DefaultParser().parse(options, new String[] {"-a", "--", "-b"});

        assertTrue(cl.hasOption("a"));
        assertFalse("-b tras '--' no debe interpretarse como opción", cl.hasOption("b"));
        assertEquals(Arrays.asList("-b"), Arrays.asList(cl.getArgs()));
    }

    // ------------------------------------------------------------------
    // Errores
    // ------------------------------------------------------------------

    @Test
    public void testUnrecognizedOptionThrows() throws Exception {
        Options options = new Options();
        options.addOption(new Option("a", "flag a"));

        try {
            new DefaultParser().parse(options, new String[] {"-z"});
            fail("Se esperaba UnrecognizedOptionException");
        } catch (UnrecognizedOptionException e) {
            assertEquals("-z", e.getOption());
        }
    }

    @Test
    public void testMissingArgumentThrows() throws Exception {
        Options options = new Options();
        options.addOption(new Option("f", true, "archivo"));

        try {
            new DefaultParser().parse(options, new String[] {"-f"});
            fail("Se esperaba MissingArgumentException");
        } catch (MissingArgumentException e) {
            assertEquals("f", e.getOption().getOpt());
        }
    }

    @Test
    public void testMissingRequiredOptionThrows() throws Exception {
        Option required = new Option("r", "requerida", false, "obligatoria");
        required.setRequired(true);
        Options options = new Options();
        options.addOption(required);

        try {
            new DefaultParser().parse(options, new String[0]);
            fail("Se esperaba MissingOptionException");
        } catch (MissingOptionException e) {
            List<?> missing = e.getMissingOptions();
            assertEquals(1, missing.size());
            assertTrue(missing.contains("r"));
        }
    }

    @Test
    public void testRequiredOptionSatisfiedDoesNotThrow() throws Exception {
        Option required = new Option("r", "requerida", false, "obligatoria");
        required.setRequired(true);
        Options options = new Options();
        options.addOption(required);

        CommandLine cl = new DefaultParser().parse(options, new String[] {"-r"});

        assertTrue(cl.hasOption("r"));
    }

    @Test
    public void testOptionGroupConflictThrows() throws Exception {
        Options options = new Options();
        Option a = new Option("a", "flag a");
        Option b = new Option("b", "flag b");
        OptionGroup group = new OptionGroup();
        group.addOption(a);
        group.addOption(b);
        options.addOptionGroup(group);

        try {
            new DefaultParser().parse(options, new String[] {"-a", "-b"});
            fail("Se esperaba AlreadySelectedException");
        } catch (AlreadySelectedException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testOptionGroupSingleSelectionAccepted() throws Exception {
        Options options = new Options();
        Option a = new Option("a", "flag a");
        Option b = new Option("b", "flag b");
        OptionGroup group = new OptionGroup();
        group.addOption(a);
        group.addOption(b);
        options.addOptionGroup(group);

        CommandLine cl = new DefaultParser().parse(options, new String[] {"-a"});

        assertTrue(cl.hasOption("a"));
        assertFalse(cl.hasOption("b"));
    }

    // ------------------------------------------------------------------
    // Argumentos nulos, estado y propiedades
    // ------------------------------------------------------------------

    @Test
    public void testNullArgumentsYieldEmptyCommandLine() throws Exception {
        Options options = new Options();
        options.addOption(new Option("a", "flag a"));

        CommandLine cl = new DefaultParser().parse(options, null);

        assertFalse(cl.hasOption("a"));
        assertEquals(0, cl.getArgs().length);
    }

    @Test
    public void testParserStateIsResetBetweenCalls() throws Exception {
        Options options = new Options();
        options.addOption(new Option("a", "flag a"));
        DefaultParser parser = new DefaultParser();

        CommandLine first = parser.parse(options, new String[] {"-a"});
        CommandLine second = parser.parse(options, new String[0]);

        assertTrue(first.hasOption("a"));
        assertFalse("La segunda llamada no debe heredar opciones previas", second.hasOption("a"));
    }

    @Test
    public void testPropertiesApplyWhenAbsentFromCommandLine() throws Exception {
        Options options = new Options();
        options.addOption(new Option("f", true, "archivo"));
        Properties props = new Properties();
        props.setProperty("f", "desdeProps");

        CommandLine cl = new DefaultParser().parse(options, new String[0], props);

        assertTrue(cl.hasOption("f"));
        assertEquals("desdeProps", cl.getOptionValue("f"));
    }

    @Test
    public void testCommandLineOverridesProperties() throws Exception {
        Options options = new Options();
        options.addOption(new Option("f", true, "archivo"));
        Properties props = new Properties();
        props.setProperty("f", "desdeProps");

        CommandLine cl = new DefaultParser().parse(options, new String[] {"-f", "desdeCli"}, props);

        assertEquals("desdeCli", cl.getOptionValue("f"));
    }

    @Test
    public void testBooleanPropertyTrueAddsFlag() throws Exception {
        Options options = new Options();
        options.addOption(new Option("v", "verbose", false, "verboso"));
        Properties props = new Properties();
        props.setProperty("v", "true");

        CommandLine cl = new DefaultParser().parse(options, new String[0], props);

        assertTrue(cl.hasOption("v"));
    }

    @Test
    public void testBooleanPropertyFalseIsIgnored() throws Exception {
        Options options = new Options();
        options.addOption(new Option("v", "verbose", false, "verboso"));
        Properties props = new Properties();
        props.setProperty("v", "false");

        CommandLine cl = new DefaultParser().parse(options, new String[0], props);

        assertFalse("Un valor 'false' no debe añadir la opción", cl.hasOption("v"));
    }

    @Test
    public void testUnknownPropertyThrows() throws Exception {
        Options options = new Options();
        options.addOption(new Option("a", "flag a"));
        Properties props = new Properties();
        props.setProperty("zzz", "1");

        try {
            new DefaultParser().parse(options, new String[0], props);
            fail("Se esperaba UnrecognizedOptionException");
        } catch (UnrecognizedOptionException e) {
            assertEquals("zzz", e.getOption());
        }
    }
}
