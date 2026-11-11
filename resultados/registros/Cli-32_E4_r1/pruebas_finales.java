package org.apache.commons.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Comparator;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link HelpFormatter} que cubren los requisitos
 * documentados que no verifica la suite existente.
 */
public class HelpFormatterLLMTest {

    private static final String EOL = System.getProperty("line.separator");

    /** Descripción de 56 caracteres: con ancho 74 no se parte, con ancho 30 sí. */
    private static final String LONG_DESC =
            "alpha beta gamma delta epsilon zeta eta theta iota kappa";

    /**
     * Ejecuta la acción capturando lo que escriba en System.out.
     */
    private static String captureStdout(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString();
    }

    /**
     * Renderiza la ayuda usando la sobrecarga que recibe un PrintWriter.
     */
    private static String renderWith(HelpFormatter formatter, int width, String syntax,
                                     String header, Options options, String footer,
                                     boolean autoUsage) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        formatter.printHelp(pw, width, syntax, header, options,
                formatter.getLeftPadding(), formatter.getDescPadding(), footer, autoUsage);
        pw.flush();
        return sw.toString();
    }

    // ------------------------------------------------------------------
    // Requisito 1: valores por defecto de constantes y getters
    // ------------------------------------------------------------------

    @Test
    public void defaultConstantsHaveExpectedValues() {
        assertEquals(74, HelpFormatter.DEFAULT_WIDTH);
        assertEquals(1, HelpFormatter.DEFAULT_LEFT_PAD);
        assertEquals(3, HelpFormatter.DEFAULT_DESC_PAD);
        assertEquals("usage: ", HelpFormatter.DEFAULT_SYNTAX_PREFIX);
        assertEquals("-", HelpFormatter.DEFAULT_OPT_PREFIX);
        assertEquals("--", HelpFormatter.DEFAULT_LONG_OPT_PREFIX);
        assertEquals(" ", HelpFormatter.DEFAULT_LONG_OPT_SEPARATOR);
        assertEquals("arg", HelpFormatter.DEFAULT_ARG_NAME);
    }

    @Test
    public void defaultAccessorValuesMatchConstantsAndLineSeparator() {
        HelpFormatter formatter = new HelpFormatter();

        assertEquals(74, formatter.getWidth());
        assertEquals(1, formatter.getLeftPadding());
        assertEquals(3, formatter.getDescPadding());
        assertEquals("usage: ", formatter.getSyntaxPrefix());
        assertEquals("-", formatter.getOptPrefix());
        assertEquals("--", formatter.getLongOptPrefix());
        assertEquals(" ", formatter.getLongOptSeparator());
        assertEquals("arg", formatter.getArgName());
        assertEquals(System.getProperty("line.separator"), formatter.getNewLine());
    }

    // ------------------------------------------------------------------
    // Requisito 2: comparador por defecto (insensible a mayúsculas)
    // ------------------------------------------------------------------

    @SuppressWarnings("rawtypes")
    @Test
    public void defaultOptionComparatorIsNonNullAndCaseInsensitive() {
        HelpFormatter formatter = new HelpFormatter();
        Comparator comparator = formatter.getOptionComparator();

        assertNotNull(comparator);
        assertTrue(comparator.compare(new Option("B", "x"), new Option("a", "x")) > 0);
        assertTrue(comparator.compare(new Option("a", "x"), new Option("B", "x")) < 0);
        assertEquals(0, comparator.compare(new Option("a", "x"), new Option("A", "x")));
    }

    @Test
    public void defaultComparatorSortsMixedCaseKeysInUsage() {
        Options options = new Options();
        options.addOption(new Option("b", "second"));
        options.addOption(new Option("A", "first"));
        options.addOption(new Option("c", "third"));

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        new HelpFormatter().printUsage(pw, 80, "app", options);
        pw.flush();

        assertEquals("usage: app [-A] [-b] [-c]" + EOL, sw.toString());
    }

    @Test
    public void defaultComparatorSortsMixedCaseKeysInOptionList() {
        Options options = new Options();
        options.addOption("b", false, "second");
        options.addOption("A", false, "first");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        new HelpFormatter().printOptions(pw, 80, options, 1, 3);
        pw.flush();

        assertEquals(" -A   first" + EOL + " -b   second" + EOL, sw.toString());
    }

    // ------------------------------------------------------------------
    // Requisito 3: uso no automático (autoUsage = false)
    // ------------------------------------------------------------------

    @Test
    public void nonAutoUsagePrintsProvidedSyntaxInsteadOfGeneratedOne() {
        Options options = new Options().addOption("a", false, "first");
        HelpFormatter formatter = new HelpFormatter();

        String out = renderWith(formatter, 80, "myapp FILE", null, options, null, false);

        assertEquals("usage: myapp FILE" + EOL + " -a   first" + EOL, out);
        assertFalse(out.contains("[-a]"));
    }

    @Test
    public void autoUsageGeneratesUsageFromOptionsForContrast() {
        Options options = new Options().addOption("a", false, "first");
        HelpFormatter formatter = new HelpFormatter();

        String out = renderWith(formatter, 80, "myapp FILE", null, options, null, true);

        assertTrue(out.startsWith("usage: myapp FILE [-a]" + EOL));
    }

    // ------------------------------------------------------------------
    // Requisito 4: sobrecargas de printHelp que escriben en System.out
    // ------------------------------------------------------------------

    @Test
    public void printHelpToStdoutWithDefaultsWritesExpectedOutput() {
        Options options = new Options().addOption("a", false, "first");
        HelpFormatter formatter = new HelpFormatter();

        String out = captureStdout(() -> formatter.printHelp("app", options));

        assertEquals("usage: app" + EOL + " -a   first" + EOL, out);
    }

    @Test
    public void printHelpToStdoutWithAutoUsageWritesGeneratedUsage() {
        Options options = new Options().addOption("a", false, "first");
        HelpFormatter formatter = new HelpFormatter();

        String out = captureStdout(() -> formatter.printHelp("app", options, true));

        assertEquals("usage: app [-a]" + EOL + " -a   first" + EOL, out);
    }

    @Test
    public void printHelpToStdoutWithoutAutoUsageWritesProvidedSyntax() {
        Options options = new Options().addOption("a", false, "first");
        HelpFormatter formatter = new HelpFormatter();

        String out = captureStdout(() -> formatter.printHelp("app FILE", options, false));

        assertEquals("usage: app FILE" + EOL + " -a   first" + EOL, out);
    }

    @Test
    public void printHelpToStdoutWithHeaderAndFooterWritesAllSections() {
        Options options = new Options().addOption("a", false, "first");
        HelpFormatter formatter = new HelpFormatter();

        String out = captureStdout(
                () -> formatter.printHelp("app", "Head", options, "Foot", true));

        assertEquals("usage: app [-a]" + EOL
                + "Head" + EOL
                + " -a   first" + EOL
                + "Foot" + EOL, out);
    }

    // ------------------------------------------------------------------
    // Requisito 5: sobrecarga con width explícito y uso de setWidth
    // ------------------------------------------------------------------

    @Test
    public void overloadsWithoutWidthUseConfiguredWidth() {
        Options options = new Options().addOption("a", false, LONG_DESC);

        HelpFormatter defaultFormatter = new HelpFormatter();
        String atDefaultWidth = captureStdout(() -> defaultFormatter.printHelp("app", options));
        assertEquals("usage: app" + EOL + " -a   " + LONG_DESC + EOL, atDefaultWidth);

        HelpFormatter narrowFormatter = new HelpFormatter();
        narrowFormatter.setWidth(30);
        String viaSetWidth = captureStdout(() -> narrowFormatter.printHelp("app", options));
        String explicitWidth30 = renderWith(narrowFormatter, 30, "app", null, options, null, false);

        assertEquals(explicitWidth30, viaSetWidth);
        assertFalse(atDefaultWidth.equals(viaSetWidth));
    }

    @Test
    public void explicitWidthOverloadMatchesPrintWriterVariant() {
        Options options = new Options().addOption("a", false, LONG_DESC);
        HelpFormatter formatter = new HelpFormatter();

        String viaOverload = captureStdout(() -> formatter.printHelp(30, "app", null, options, null));
        String viaWriter = renderWith(formatter, 30, "app", null, options, null, false);
        String atDefaultWidth = captureStdout(() -> formatter.printHelp("app", options));

        assertEquals(viaWriter, viaOverload);
        assertFalse(atDefaultWidth.equals(viaOverload));
    }

    // ------------------------------------------------------------------
    // Requisito 6: el separador de línea personalizado se usa al renderizar
    // ------------------------------------------------------------------

    @Test
    public void customNewLineIsUsedBetweenRenderedOptions() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setNewLine("|");
        Options options = new Options()
                .addOption("a", false, "first")
                .addOption("b", false, "second");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        formatter.printOptions(pw, 80, options, 1, 3);
        pw.flush();

        // printOptions añade EOL al final (println), pero entre opciones se usa el separador propio
        assertEquals(" -a   first| -b   second" + EOL, sw.toString());
    }

    // ------------------------------------------------------------------
    // Requisito 7: header y footer vacíos o solo con espacios se omiten
    // ------------------------------------------------------------------

    @Test
    public void blankHeaderAndFooterAreOmitted() {
        Options options = new Options().addOption("a", false, "first");
        HelpFormatter formatter = new HelpFormatter();

        String withWhitespace = renderWith(formatter, 80, "app", "   ", options, "\t \t", false);
        String withEmpty = renderWith(formatter, 80, "app", "", options, "", false);

        String expected = "usage: app" + EOL + " -a   first" + EOL;
        assertEquals(expected, withWhitespace);
        assertEquals(expected, withEmpty);
    }

    @Test
    public void nullHeaderAndFooterAreOmitted() {
        Options options = new Options().addOption("a", false, "first");
        HelpFormatter formatter = new HelpFormatter();

        String out = renderWith(formatter, 80, "app", null, options, null, false);

        assertEquals("usage: app" + EOL + " -a   first" + EOL, out);
    }
}
