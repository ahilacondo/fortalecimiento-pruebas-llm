package org.apache.commons.cli;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Properties;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link DefaultParser} que cubren comportamientos
 * no verificados por la suite existente. Solo usan la API pública.
 */
public class DefaultParserLLMTest {

    /** Verifica que una opción simple se reconoce y que no quedan argumentos sueltos. */
    @Test
    public void basicShortOptionIsParsed() throws ParseException {
        Options options = new Options();
        options.addOption(new Option("a", "opción a"));

        CommandLine cmd = new DefaultParser().parse(options, new String[] {"-a"});

        assertTrue("La opción -a debe estar presente", cmd.hasOption("a"));
        assertTrue("No deben quedar argumentos", cmd.getArgList().isEmpty());
    }

    /** Una propiedad sin opción definida debe lanzar UnrecognizedOptionException. */
    @Test
    public void propertyWithoutDefinedOptionThrowsUnrecognizedOption() {
        Options options = new Options();
        options.addOption(new Option("a", "opción a"));

        Properties properties = new Properties();
        properties.setProperty("desconocida", "valor");

        try {
            new DefaultParser().parse(options, new String[0], properties);
            fail("Se esperaba UnrecognizedOptionException");
        } catch (UnrecognizedOptionException e) {
            assertEquals("desconocida", e.getOption());
        } catch (ParseException e) {
            fail("Se esperaba UnrecognizedOptionException y llegó " + e.getClass().getName());
        }
    }

    /** Si la opción ya aparece en la línea de comandos, la propiedad no debe sobrescribir su valor. */
    @Test
    public void propertyIsIgnoredWhenOptionAlreadyOnCommandLine() throws ParseException {
        Options options = new Options();
        options.addOption(new Option("v", true, "valor"));

        Properties properties = new Properties();
        properties.setProperty("v", "desde_propiedad");

        CommandLine cmd = new DefaultParser().parse(options, new String[] {"-v", "desde_cli"}, properties);

        assertEquals("desde_cli", cmd.getOptionValue("v"));
        assertEquals(1, cmd.getOptions().length);
    }

    /** Si otra opción del mismo grupo ya está seleccionada, la propiedad no se aplica. */
    @Test
    public void propertyIsIgnoredWhenAnotherGroupOptionIsSelected() throws ParseException {
        Options options = new Options();
        OptionGroup group = new OptionGroup();
        group.addOption(new Option("a", "opción a"));
        group.addOption(new Option("b", "opción b"));
        options.addOptionGroup(group);

        Properties properties = new Properties();
        properties.setProperty("b", "true");

        CommandLine cmd = new DefaultParser().parse(options, new String[] {"-a"}, properties);

        assertTrue("La opción -a debe estar presente", cmd.hasOption("a"));
        assertFalse("La opción -b no debe añadirse desde la propiedad", cmd.hasOption("b"));
    }

    /** Una opción booleana con valor distinto de yes/true/1 no debe añadirse. */
    @Test
    public void booleanPropertyWithNonAffirmativeValueIsNotAdded() throws ParseException {
        Options options = new Options();
        options.addOption(new Option("f", "bandera"));

        Properties properties = new Properties();
        properties.setProperty("f", "no");

        CommandLine cmd = new DefaultParser().parse(options, new String[0], properties);

        assertFalse("Con valor 'no' la bandera no debe añadirse", cmd.hasOption("f"));
    }

    /** Una opción booleana con valor yes/true/1 (sin distinguir mayúsculas) sí debe añadirse. */
    @Test
    public void booleanPropertyWithAffirmativeValueIsAdded() throws ParseException {
        Options options = new Options();
        options.addOption(new Option("f", "bandera"));

        Properties properties = new Properties();
        properties.setProperty("f", "TRUE");

        CommandLine cmd = new DefaultParser().parse(options, new String[0], properties);

        assertTrue("Con valor 'TRUE' la bandera debe añadirse", cmd.hasOption("f"));
    }

    /** Con stopAtNonOption=true, el primer token desconocido detiene el parseo sin error. */
    @Test
    public void stopAtNonOptionAddsRemainingTokensAsArguments() throws ParseException {
        Options options = new Options();
        options.addOption(new Option("a", "opción a"));

        CommandLine cmd = new DefaultParser().parse(
                options, new String[] {"-a", "fichero", "-b", "x"}, true);

        assertTrue("La opción -a debe estar presente", cmd.hasOption("a"));
        assertFalse("-b no debe interpretarse como opción", cmd.hasOption("b"));
        assertArrayEquals(new String[] {"fichero", "-b", "x"}, cmd.getArgList().toArray());
    }

    /** Con stopAtNonOption=false, un token desconocido con guion debe lanzar excepción. */
    @Test
    public void unknownOptionWithoutStopAtNonOptionThrows() {
        Options options = new Options();
        options.addOption(new Option("a", "opción a"));

        try {
            new DefaultParser().parse(options, new String[] {"-a", "-b"}, false);
            fail("Se esperaba UnrecognizedOptionException");
        } catch (UnrecognizedOptionException e) {
            assertEquals("-b", e.getOption());
        } catch (ParseException e) {
            fail("Se esperaba UnrecognizedOptionException y llegó " + e.getClass().getName());
        }
    }

    /** Un número negativo tras una opción que acepta argumento se trata como su valor. */
    @Test
    public void negativeNumberIsTreatedAsValueOfPreviousOption() throws ParseException {
        Options options = new Options();
        options.addOption(new Option("n", true, "número"));

        CommandLine cmd = new DefaultParser().parse(options, new String[] {"-n", "-1"});

        assertEquals("-1", cmd.getOptionValue("n"));
        assertTrue("No deben quedar argumentos sueltos", cmd.getArgList().isEmpty());
    }

    /** Seleccionar dos opciones del mismo grupo debe lanzar AlreadySelectedException. */
    @Test
    public void selectingTwoOptionsOfSameGroupThrows() {
        Options options = new Options();
        OptionGroup group = new OptionGroup();
        group.addOption(new Option("a", "opción a"));
        group.addOption(new Option("b", "opción b"));
        options.addOptionGroup(group);

        try {
            new DefaultParser().parse(options, new String[] {"-a", "-b"});
            fail("Se esperaba AlreadySelectedException");
        } catch (AlreadySelectedException e) {
            assertNotNull(e);
        } catch (ParseException e) {
            fail("Se esperaba AlreadySelectedException y llegó " + e.getClass().getName());
        }
    }

    /** La selección de grupos se limpia entre llamadas a parse en el mismo parser. */
    @Test
    public void groupSelectionIsClearedBetweenParseCalls() throws ParseException {
        Options options = new Options();
        OptionGroup group = new OptionGroup();
        group.addOption(new Option("a", "opción a"));
        group.addOption(new Option("b", "opción b"));
        options.addOptionGroup(group);

        DefaultParser parser = new DefaultParser();
        CommandLine first = parser.parse(options, new String[] {"-a"});
        CommandLine second = parser.parse(options, new String[] {"-b"});

        assertTrue("Primera llamada: -a presente", first.hasOption("a"));
        assertFalse("Primera llamada: -b ausente", first.hasOption("b"));
        assertFalse("Segunda llamada: -a ausente", second.hasOption("a"));
        assertTrue("Segunda llamada: -b presente", second.hasOption("b"));
    }

    /** El formato -Dclave=valor debe producir los valores "clave" y "valor". */
    @Test
    public void javaStyleDefinePropertyIsParsed() throws ParseException {
        Options options = new Options();
        Option define = new Option("D", true, "propiedad");
        define.setArgs(2);
        options.addOption(define);

        CommandLine cmd = new DefaultParser().parse(options, new String[] {"-Dclave=valor"});

        assertTrue("La opción -D debe estar presente", cmd.hasOption("D"));
        assertArrayEquals(new String[] {"clave", "valor"}, cmd.getOptionValues("D"));
    }

    /** El formato -Xmx512m debe reconocer la opción larga "Xmx" como prefijo con valor "512m". */
    @Test
    public void longPrefixWithAttachedValueIsParsed() throws ParseException {
        Options options = new Options();
        options.addOption(Option.builder("X").longOpt("Xmx").hasArg().desc("memoria").build());

        CommandLine cmd = new DefaultParser().parse(options, new String[] {"-Xmx512m"});

        assertTrue("La opción -X/Xmx debe estar presente", cmd.hasOption("X"));
        assertEquals("512m", cmd.getOptionValue("X"));
    }

    /** Opciones cortas concatenadas cuyo último elemento acepta valor adjunto. */
    @Test
    public void concatenatedShortOptionsWithTrailingValueAreParsed() throws ParseException {
        Options options = new Options();
        options.addOption(new Option("a", "opción a"));
        options.addOption(new Option("n", true, "número"));

        CommandLine cmd = new DefaultParser().parse(options, new String[] {"-an5"});

        assertTrue("La opción -a debe estar presente", cmd.hasOption("a"));
        assertTrue("La opción -n debe estar presente", cmd.hasOption("n"));
        assertEquals("5", cmd.getOptionValue("n"));
        assertTrue("No deben quedar argumentos sueltos", cmd.getArgList().isEmpty());
    }
}
