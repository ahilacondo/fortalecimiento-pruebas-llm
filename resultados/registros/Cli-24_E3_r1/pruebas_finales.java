/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link HelpFormatter} que cubren comportamientos
 * no verificados por {@code HelpFormatterTest}. Sólo usan la API pública.
 */
public class HelpFormatterLLMTest
{
    private static final String EOL = System.getProperty("line.separator");

    private static final String DESC =
            "one two three four five six seven eight nine ten";

    /** Ejecuta la acción capturando lo que se escribe en System.out. */
    private static String captureStdout(Runnable action)
    {
        PrintStream original = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos, true));
        try
        {
            action.run();
        }
        finally
        {
            System.setOut(original);
        }
        return baos.toString();
    }

    // ------------------------------------------------------------------
    // 1. Sobrecargas de printHelp sin PrintWriter escriben en System.out
    // ------------------------------------------------------------------

    @Test
    public void testPrintHelpWithoutWriterWritesToSystemOut()
    {
        final HelpFormatter formatter = new HelpFormatter();
        final Options options = new Options().addOption("a", false, "alpha");

        String out = captureStdout(() -> formatter.printHelp("app", options));

        assertEquals("salida de printHelp sin PrintWriter",
                "usage: app" + EOL + " -a   alpha" + EOL, out);
    }

    @Test
    public void testPrintHelpWithoutWriterUsesDefaultWidth()
    {
        final HelpFormatter formatter = new HelpFormatter();
        final Options options = new Options().addOption("a", false, DESC);

        // Con el ancho por defecto (74) la descripción cabe en una sola línea
        String wide = captureStdout(() -> formatter.printHelp("app", options));
        assertTrue("con ancho por defecto la descripción no se parte",
                wide.contains(" -a   " + DESC + EOL));

        // Con un ancho pequeño la misma salida debe ocupar más líneas
        formatter.setWidth(20);
        String narrow = captureStdout(() -> formatter.printHelp("app", options));

        assertTrue("con ancho 20 la salida debe tener más líneas",
                narrow.split(EOL).length > wide.split(EOL).length);
    }

    // ------------------------------------------------------------------
    // 2. autoUsage = false imprime la sintaxis literal recibida
    // ------------------------------------------------------------------

    @Test
    public void testPrintHelpWithoutAutoUsagePrintsLiteralSyntax()
    {
        HelpFormatter formatter = new HelpFormatter();
        Options options = new Options().addOption("a", false, "alpha");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        formatter.printHelp(pw, 80, "myapp --input FILE", null, options, 1, 3, null, false);
        pw.flush();
        String out = sw.toString();

        assertTrue("debe imprimirse la sintaxis literal",
                out.startsWith("usage: myapp --input FILE" + EOL));
        assertFalse("no debe generarse uso automático a partir de las opciones",
                out.contains("[-a]"));
    }

    @Test
    public void testPrintHelpWithAutoUsageGeneratesUsageFromOptions()
    {
        HelpFormatter formatter = new HelpFormatter();
        Options options = new Options().addOption("a", false, "alpha");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        formatter.printHelp(pw, 80, "myapp --input FILE", null, options, 1, 3, null, true);
        pw.flush();

        assertTrue("con autoUsage=true el uso se genera desde las opciones",
                sw.toString().startsWith("usage: myapp --input FILE [-a]" + EOL));
    }

    // ------------------------------------------------------------------
    // 3. Header/footer nulos o en blanco se omiten
    // ------------------------------------------------------------------

    @Test
    public void testNullOrBlankHeaderAndFooterAreOmitted()
    {
        HelpFormatter formatter = new HelpFormatter();
        Options options = new Options().addOption("a", false, "alpha");

        StringWriter swNull = new StringWriter();
        PrintWriter pwNull = new PrintWriter(swNull);
        formatter.printHelp(pwNull, 80, "app", null, options, 1, 3, null, false);
        pwNull.flush();

        StringWriter swBlank = new StringWriter();
        PrintWriter pwBlank = new PrintWriter(swBlank);
        formatter.printHelp(pwBlank, 80, "app", "   ", options, 1, 3, "\t ", false);
        pwBlank.flush();

        assertEquals("header/footer en blanco deben producir la misma salida que nulos",
                swNull.toString(), swBlank.toString());
    }

    @Test
    public void testNonBlankHeaderAndFooterAreIncluded()
    {
        HelpFormatter formatter = new HelpFormatter();
        Options options = new Options().addOption("a", false, "alpha");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        formatter.printHelp(pw, 80, "app", "HEADER TEXT", options, 1, 3, "FOOTER TEXT", false);
        pw.flush();
        String out = sw.toString();

        assertTrue("el header debe aparecer", out.contains("HEADER TEXT" + EOL));
        assertTrue("el footer debe aparecer", out.contains("FOOTER TEXT" + EOL));
        assertTrue("el header debe ir antes que las opciones",
                out.indexOf("HEADER TEXT") < out.indexOf(" -a"));
        assertTrue("el footer debe ir después de las opciones",
                out.indexOf("FOOTER TEXT") > out.indexOf(" -a"));
    }

    // ------------------------------------------------------------------
    // 4. printUsage(pw, width, cmdLineSyntax): prefijo y sangría
    // ------------------------------------------------------------------

    @Test
    public void testPrintUsageSimpleSyntaxHasPrefix()
    {
        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        formatter.printUsage(pw, 80, "app -a");
        pw.flush();

        assertEquals("usage: app -a" + EOL, sw.toString());
    }

    @Test
    public void testPrintUsageWrappedLinesAreIndentedAfterSyntaxPrefix()
    {
        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // "usage: " (7) + "app " (4) => sangría esperada de 11 espacios
        formatter.printUsage(pw, 30, "app -alpha <a> -beta <b> -gamma <c> -delta <d>");
        pw.flush();

        String[] lines = sw.toString().split("\\R");
        assertTrue("la sintaxis debe ocupar varias líneas", lines.length >= 2);
        assertTrue("la primera línea debe empezar por el prefijo",
                lines[0].startsWith("usage: app -alpha"));

        String indent = " ".repeat(11);
        for (int i = 0; i < lines.length; i++)
        {
            assertTrue("la línea " + i + " no debe exceder el ancho 30",
                    lines[i].length() <= 30);
            if (i > 0)
            {
                assertTrue("la línea " + i + " debe tener sangría de 11 espacios",
                        lines[i].startsWith(indent));
                assertTrue("la línea " + i + " debe comenzar con texto tras la sangría",
                        lines[i].charAt(11) != ' ');
            }
        }
    }

    // ------------------------------------------------------------------
    // 5. findWrapPos respeta \n y \t dentro del ancho (vía printWrapped)
    // ------------------------------------------------------------------

    @Test
    public void testPrintWrappedBreaksAtNewlineWithinWidth()
    {
        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        formatter.printWrapped(pw, 80, "line1\nline2 more words here");
        pw.flush();

        assertEquals("line1" + EOL + "line2 more words here" + EOL, sw.toString());
    }

    @Test
    public void testPrintWrappedBreaksAtTabWithinWidth()
    {
        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        formatter.printWrapped(pw, 80, "a\tb");
        pw.flush();

        assertEquals("a" + EOL + "b" + EOL, sw.toString());
    }

    // ------------------------------------------------------------------
    // 6. IllegalStateException cuando nextLineTabStop >= width
    // ------------------------------------------------------------------

    @Test
    public void testPrintWrappedThrowsWhenTabStopNotLessThanWidth()
    {
        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        try
        {
            formatter.printWrapped(pw, 10, 10, "aaaa bbbb cccc dddd eeee");
            fail("se esperaba IllegalStateException cuando nextLineTabStop >= width");
        }
        catch (IllegalStateException e)
        {
            // esperado
        }
    }

    @Test
    public void testPrintWrappedDoesNotThrowWhenTextFitsEvenIfTabStopIsLarge()
    {
        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // Sin necesidad de ajuste de línea no se alcanza la comprobación
        formatter.printWrapped(pw, 10, 10, "short");
        pw.flush();

        assertEquals("short" + EOL, sw.toString());
    }

    // ------------------------------------------------------------------
    // 7. Opción sin descripción
    // ------------------------------------------------------------------

    @Test
    public void testOptionWithoutDescriptionRendersOnlyPrefix()
    {
        HelpFormatter formatter = new HelpFormatter();
        Options options = new Options();
        options.addOption(new Option("a", null));
        options.addOption(new Option("b", "bee desc"));

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        formatter.printOptions(pw, 80, options, 1, 3);
        pw.flush();

        assertEquals(" -a" + EOL + " -b   bee desc" + EOL, sw.toString());
    }

    // ------------------------------------------------------------------
    // 8. Prefijos personalizados
    // ------------------------------------------------------------------

    @Test
    public void testCustomOptPrefixesAreAppliedInOptionListing()
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptPrefix("+");
        formatter.setLongOptPrefix("++");

        Options options = new Options().addOption("a", "aaa", false, "desc A");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        formatter.printOptions(pw, 80, options, 1, 3);
        pw.flush();

        assertEquals(" +a,++aaa   desc A" + EOL, sw.toString());
    }

    /**
     * Documenta la inconsistencia actual: el uso automático ignora los
     * prefijos personalizados y sigue usando "-" codificado. Si el
     * comportamiento se corrige, esta prueba debe actualizarse.
     */
    @Test
    public void testAutoUsageIgnoresCustomOptPrefixes()
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptPrefix("+");
        formatter.setLongOptPrefix("++");

        Options options = new Options().addOption("a", "aaa", false, "desc A");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        formatter.printUsage(pw, 80, "app", options);
        pw.flush();

        assertEquals("usage: app [-a]" + EOL, sw.toString());
        assertFalse("el uso automático no debe aplicar el prefijo personalizado",
                sw.toString().contains("[+a]"));
    }
}
