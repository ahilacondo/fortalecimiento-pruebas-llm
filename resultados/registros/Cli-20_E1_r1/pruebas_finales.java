package org.apache.commons.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

/**
 * Pruebas adicionales para {@link PosixParser}, escritas con JUnit 4 y
 * usando únicamente la API pública de las clases de commons-cli.
 */
public class PosixParserLLMTest
{
    private Options options;
    private PosixParser parser;

    @Before
    public void setUp()
    {
        options = new Options();
        options.addOption(new Option("a", "enable-a", false, "turn [a] on or off"));
        options.addOption(new Option("b", "bfile", true, "set the value of [b]"));
        options.addOption(new Option("c", "copt", false, "turn [c] on or off"));

        parser = new PosixParser();
    }

    @Test
    public void testShortOptionWithAttachedValueIsSplit() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "-btoast" });

        assertTrue("-b debe estar presente", cl.hasOption("b"));
        assertEquals("toast", cl.getOptionValue("b"));
        assertTrue("No deben quedar argumentos sueltos", cl.getArgList().isEmpty());
    }

    @Test
    public void testLongOptionWithEqualsSplitsNameAndValue() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "--bfile=toast" });

        assertTrue("--bfile debe estar presente", cl.hasOption("b"));
        assertEquals("toast", cl.getOptionValue("bfile"));
        assertTrue("No deben quedar argumentos sueltos", cl.getArgList().isEmpty());
    }

    @Test
    public void testLongOptionWithEqualsKeepsFurtherEqualsInValue() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "--bfile=a=b" });

        assertTrue(cl.hasOption("b"));
        assertEquals("El valor debe conservar los '=' posteriores al primero", "a=b", cl.getOptionValue("b"));
    }

    @Test
    public void testBurstedFlagsAreAllSet() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "-ac" });

        assertTrue("-a debe estar activa", cl.hasOption("a"));
        assertTrue("-c debe estar activa", cl.hasOption("c"));
        assertFalse("-b no debe estar activa", cl.hasOption("b"));
        assertTrue("No deben quedar argumentos sueltos", cl.getArgList().isEmpty());
    }

    @Test
    public void testShortOptionValueInNextToken() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "-b", "toast", "foo" });

        assertTrue(cl.hasOption("b"));
        assertEquals("toast", cl.getOptionValue("b"));
        assertEquals(Arrays.asList("foo"), cl.getArgList());
    }

    @Test
    public void testPositionalArgumentsKeepOrderWithoutStop() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "-a", "x", "-c", "y" });

        assertTrue("-a debe estar activa", cl.hasOption("a"));
        assertTrue("-c debe estar activa", cl.hasOption("c"));
        assertEquals(Arrays.asList("x", "y"), cl.getArgList());
    }

    @Test
    public void testNonOptionDoesNotStopProcessingWithoutStopFlag() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "foo", "-a" });

        assertTrue("-a debe procesarse tras un no-opción", cl.hasOption("a"));
        assertEquals(Arrays.asList("foo"), cl.getArgList());
    }

    @Test
    public void testStopAtNonOptionTreatsRemainingTokensAsArguments() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "foo", "-a", "-b", "x" }, true);

        assertFalse("-a no debe procesarse tras el primer no-opción", cl.hasOption("a"));
        assertFalse("-b no debe procesarse tras el primer no-opción", cl.hasOption("b"));
        assertEquals(Arrays.asList("foo", "-a", "-b", "x"), cl.getArgList());
    }

    @Test
    public void testStopAtUnknownShortOptionKeepsRemainingTokens() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "-z", "-a", "-btoast" }, true);

        assertFalse("-a no debe estar activa", cl.hasOption("a"));
        assertFalse("-b no debe estar activa", cl.hasOption("b"));
        assertEquals(3, cl.getArgList().size());
        assertTrue(cl.getArgList().contains("-a"));
        assertTrue(cl.getArgList().contains("-btoast"));
    }

    @Test
    public void testDoubleDashStopsOptionProcessing() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "--", "-a" });

        assertFalse("-a no debe procesarse tras '--'", cl.hasOption("a"));
        assertEquals(Arrays.asList("-a"), cl.getArgList());
    }

    @Test
    public void testSingleHyphenIsTreatedAsPositionalArgument() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "-a", "-" });

        assertTrue("-a debe estar activa", cl.hasOption("a"));
        assertEquals(Arrays.asList("-"), cl.getArgList());
    }

    @Test
    public void testEmptyArgumentsProduceNoOptionsAndNoArgs() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[0]);

        assertFalse(cl.hasOption("a"));
        assertFalse(cl.hasOption("b"));
        assertFalse(cl.hasOption("c"));
        assertTrue(cl.getArgList().isEmpty());
    }

    @Test
    public void testReusingParserDoesNotLeakStateBetweenParses() throws Exception
    {
        CommandLine first = parser.parse(options, new String[] { "-a" });
        assertTrue(first.hasOption("a"));

        CommandLine second = parser.parse(options, new String[] { "-c" });
        assertFalse("El estado de la primera llamada no debe filtrarse", second.hasOption("a"));
        assertTrue(second.hasOption("c"));
    }

    @Test
    public void testLongStopTokensAreKeptAsArguments() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "foo", "--copt" }, true);

        assertFalse("--copt no debe procesarse tras un no-opción", cl.hasOption("c"));
        assertEquals(Arrays.asList("foo", "--copt"), cl.getArgList());
    }

    @Test
    public void testUnrecognizedBurstedCharacterReportsWholeToken() throws Exception
    {
        try
        {
            parser.parse(options, new String[] { "-az" });
            fail("Se esperaba UnrecognizedOptionException");
        }
        catch (UnrecognizedOptionException e)
        {
            assertEquals("-az", e.getOption());
        }
    }

    @Test
    public void testMissingArgumentForShortOptionIsReported() throws Exception
    {
        try
        {
            parser.parse(options, new String[] { "-b" });
            fail("Se esperaba MissingArgumentException");
        }
        catch (MissingArgumentException e)
        {
            assertEquals("b", e.getOption().getOpt());
        }
    }

    @Test
    public void testMissingArgumentForLongOptionIsReported() throws Exception
    {
        try
        {
            parser.parse(options, new String[] { "--bfile" });
            fail("Se esperaba MissingArgumentException");
        }
        catch (MissingArgumentException e)
        {
            assertEquals("b", e.getOption().getOpt());
        }
    }
}
