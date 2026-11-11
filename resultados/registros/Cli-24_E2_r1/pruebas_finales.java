package org.apache.commons.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Comparator;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link HelpFormatter} que usan exclusivamente
 * la API pública de la clase. Cubren ramas de impresión de ayuda, uso,
 * opciones y ajuste de texto que no ejecuta la suite existente.
 */
public class HelpFormatterLLMTest
{
    private static final String EOL = System.getProperty("line.separator");

    // ------------------------------------------------------------ utilidades

    private static String flushed(StringWriter sw, PrintWriter pw)
    {
        pw.flush();
        return sw.toString();
    }

    private static String captureStdout(Runnable action)
    {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true));
        try
        {
            action.run();
        }
        finally
        {
            System.setOut(original);
        }
        return buffer.toString();
    }

    private static Options alphaOptions()
    {
        Options options = new Options();
        options.addOption(new Option("a", false, "alpha"));
        return options;
    }

    // ------------------------------------------------- constantes y accesores

    @Test
    public void testDefaultConstantsAndAccessorsHaveExpectedValues()
    {
        assertEquals(74, HelpFormatter.DEFAULT_WIDTH);
        assertEquals(1, HelpFormatter.DEFAULT_LEFT_PAD);
        assertEquals(3, HelpFormatter.DEFAULT_DESC_PAD);
        assertEquals("usage: ", HelpFormatter.DEFAULT_SYNTAX_PREFIX);
        assertEquals("-", HelpFormatter.DEFAULT_OPT_PREFIX);
        assertEquals("--", HelpFormatter.DEFAULT_LONG_OPT_PREFIX);
        assertEquals("arg", HelpFormatter.DEFAULT_ARG_NAME);

        HelpFormatter formatter = new HelpFormatter();
        assertEquals(HelpFormatter.DEFAULT_WIDTH, formatter.getWidth());
        assertEquals(HelpFormatter.DEFAULT_LEFT_PAD, formatter.getLeftPadding());
        assertEquals(HelpFormatter.DEFAULT_DESC_PAD, formatter.getDescPadding());
        assertEquals(HelpFormatter.DEFAULT_SYNTAX_PREFIX, formatter.getSyntaxPrefix());
        assertEquals(HelpFormatter.DEFAULT_OPT_PREFIX, formatter.getOptPrefix());
        assertEquals(HelpFormatter.DEFAULT_LONG_OPT_PREFIX, formatter.getLongOptPrefix());
        assertEquals(HelpFormatter.DEFAULT_ARG_NAME, formatter.getArgName());
        assertEquals(EOL, formatter.getNewLine());
    }

    @Test
    public void testOptionComparatorDefaultIsNonNullAndNullRestoresDefault()
    {
        HelpFormatter formatter = new HelpFormatter();
        assertNotNull("comparador por defecto", formatter.getOptionComparator());

        Comparator custom = new Comparator()
        {
            @Override
            public int compare(Object o1, Object o2)
            {
                return 0;
            }
        };

        formatter.setOptionComparator(custom);
        assertSame("comparador personalizado", custom, formatter.getOptionComparator());

        formatter.setOptionComparator(null);
        assertNotNull("tras null se restaura el valor por defecto", formatter.getOptionComparator());
        assertNotSame("tras null ya no es el personalizado", custom, formatter.getOptionComparator());
    }

    // ----------------------------------------------- validación de sintaxis

    @Test
    public void testPrintHelpPrintWriterRejectsNullSyntax()
    {
        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try
        {
            formatter.printHelp(pw, 80, null, null, new Options(), 1, 3, null, false);
            fail("una sintaxis nula debe rechazarse");
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("cmdLineSyntax"));
        }
    }

    @Test
    public void testPrintHelpPrintWriterRejectsEmptySyntaxWithAutoUsage()
    {
        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try
        {
            formatter.printHelp(pw, 80, "", null, new Options(), 1, 3, null, true);
            fail("una sintaxis vacía debe rechazarse incluso con autoUsage");
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("cmdLineSyntax"));
        }
    }

    // ----------------------------------------------- printHelp con PrintWriter

    @Test
    public void testPrintHelpWithoutHeaderAndFooter()
    {
        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        formatter.printHelp(pw, 80, "app", null, alphaOptions(), 1, 3, null, false);

        assertEquals("usage: app" + EOL + " -a   alpha" + EOL, flushed(sw, pw));
    }

    @Test
    public void testPrintHelpIgnoresBlankHeaderAndFooter()
    {
        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        formatter.printHelp(pw, 80, "app", "   ", alphaOptions(), 1, 3, "\t ", false);

        assertEquals("usage: app" + EOL + " -a   alpha" + EOL, flushed(sw, pw));
    }

    @Test
    public void testPrintHelpWithHeaderAndFooter()
    {
        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        formatter.printHelp(pw, 80, "app", "Header text", alphaOptions(), 1, 3, "Footer text", false);

        assertEquals("usage: app" + EOL
                + "Header text" + EOL
                + " -a   alpha" + EOL
                + "Footer text" + EOL,
                flushed(sw, pw));
    }

    @Test
    public void testPrintHelpAutoUsageShowsRequiredArgumentWithArgName()
    {
        Option file = new Option("f", true, "file to read");
        file.setArgName("file");
        file.setRequired(true);
        Options options = new Options();
        options.addOption(file);

        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        formatter.printHelp(pw, 80, "app", null, options, 1, 3, null, true);

        assertEquals("usage: app -f <file>" + EOL
                + " -f <file>   file to read" + EOL,
                flushed(sw, pw));
    }

    @Test
    public void testPrintHelpWithNarrowWidthWrapsDescription()
    {
        Options options = new Options();
        options.addOption(new Option("a", false,
                "a long description that must be wrapped across several lines"));

        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        formatter.printHelp(pw, 30, "app", null, options, 1, 3, null, false);

        String output = flushed(sw, pw);
        for (String line : output.split(EOL))
        {
            assertTrue("línea demasiado larga: " + line, line.length() <= 30);
        }
        assertTrue("la descripción debe aparecer en la salida", output.contains("wrapped"));
    }
}
