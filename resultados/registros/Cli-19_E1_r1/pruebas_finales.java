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

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

/**
 * Pruebas adicionales para {@link PosixParser}, usando solo la API pública.
 */
public class PosixParserLLMTest
{
    private Options options;
    private CommandLineParser parser;

    @Before
    public void setUp()
    {
        options = new Options()
            .addOption("a", "enable-a", false, "turn [a] on or off")
            .addOption("b", "bfile", true, "set the value of [b]")
            .addOption("c", "copt", false, "turn [c] on or off");

        parser = new PosixParser();
    }

    @Test
    public void testAttachedArgumentToShortOption() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "-bfoo" });

        assertTrue("-b debe estar presente", cl.hasOption("b"));
        assertEquals("El valor adjunto a -b debe ser 'foo'", "foo", cl.getOptionValue("b"));
        assertEquals("No debe quedar ningún argumento posicional", 0, cl.getArgs().length);
    }

    @Test
    public void testSeparateArgumentToShortOption() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "-b", "foo" });

        assertTrue("-b debe estar presente", cl.hasOption("b"));
        assertEquals("El valor separado de -b debe ser 'foo'", "foo", cl.getOptionValue("b"));
        assertEquals("No debe quedar ningún argumento posicional", 0, cl.getArgs().length);
    }

    @Test
    public void testBurstSeveralFlags() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "-ac" });

        assertTrue("-a debe estar presente tras el bursting", cl.hasOption("a"));
        assertTrue("-c debe estar presente tras el bursting", cl.hasOption("c"));
        assertFalse("-b no debe estar presente", cl.hasOption("b"));
        assertEquals("No debe quedar ningún argumento posicional", 0, cl.getArgs().length);
    }

    @Test
    public void testBurstFlagsFollowedByAttachedArgument() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "-acbfoo" });

        assertTrue("-a debe estar presente", cl.hasOption("a"));
        assertTrue("-c debe estar presente", cl.hasOption("c"));
        assertTrue("-b debe estar presente", cl.hasOption("b"));
        assertEquals("El valor de -b debe ser 'foo'", "foo", cl.getOptionValue("b"));
        assertEquals("No debe quedar ningún argumento posicional", 0, cl.getArgs().length);
    }

    @Test
    public void testLongOptionWithEqualsSign() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "--bfile=toast" });

        assertTrue("--bfile debe estar presente", cl.hasOption("b"));
        assertEquals("El valor de --bfile=toast debe ser 'toast'", "toast", cl.getOptionValue("bfile"));
        assertEquals("No debe quedar ningún argumento posicional", 0, cl.getArgs().length);
    }

    @Test
    public void testLongOptionValueKeepsSubsequentEquals() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "--bfile=a=b" });

        assertTrue("--bfile debe estar presente", cl.hasOption("b"));
        assertEquals("Solo se divide en el primer '='", "a=b", cl.getOptionValue("b"));
    }

    @Test
    public void testLongFlagWithoutArgument() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "--enable-a" });

        assertTrue("--enable-a debe activar -a", cl.hasOption("a"));
        assertFalse("-b no debe estar presente", cl.hasOption("b"));
        assertEquals("No debe quedar ningún argumento posicional", 0, cl.getArgs().length);
    }

    @Test
    public void testSingleDashIsPositionalArgument() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "-" });

        assertFalse("-a no debe estar presente", cl.hasOption("a"));
        assertFalse("-b no debe estar presente", cl.hasOption("b"));
        assertFalse("-c no debe estar presente", cl.hasOption("c"));
        assertEquals("'-' debe tratarse como argumento posicional",
                     Arrays.asList("-"), Arrays.asList(cl.getArgs()));
    }

    @Test
    public void testEmptyArgumentArray() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[0]);

        assertFalse("-a no debe estar presente", cl.hasOption("a"));
        assertFalse("-b no debe estar presente", cl.hasOption("b"));
        assertFalse("-c no debe estar presente", cl.hasOption("c"));
        assertEquals("No debe haber argumentos posicionales", 0, cl.getArgs().length);
    }

    @Test
    public void testDoubleDashEndsOptionProcessing() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "--", "-a" });

        assertFalse("-a no debe procesarse tras '--'", cl.hasOption("a"));
        assertEquals("'-a' debe quedar como argumento posicional",
                     Arrays.asList("-a"), Arrays.asList(cl.getArgs()));
    }

    @Test
    public void testNonStopContinuesAfterPositionalArgument() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "foober", "-a" }, false);

        assertTrue("-a debe procesarse sin detenerse", cl.hasOption("a"));
        assertEquals("Solo 'foober' debe ser argumento posicional",
                     Arrays.asList("foober"), Arrays.asList(cl.getArgs()));
    }

    @Test
    public void testStopAtNonOptionCollectsRemainingArguments() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "foober", "-a" }, true);

        assertFalse("-a no debe procesarse tras un no-opción con stopAtNonOption", cl.hasOption("a"));
        assertEquals("Todo lo restante debe ser argumento posicional",
                     Arrays.asList("foober", "-a"), Arrays.asList(cl.getArgs()));
    }

    @Test
    public void testStopAtNonOptionAfterOptionValue() throws Exception
    {
        CommandLine cl = parser.parse(options,
                                      new String[] { "-a", "-b", "val", "rest" },
                                      true);

        assertTrue("-a debe estar presente", cl.hasOption("a"));
        assertEquals("El valor de -b debe ser 'val'", "val", cl.getOptionValue("b"));
        assertEquals("'rest' debe ser argumento posicional",
                     Arrays.asList("rest"), Arrays.asList(cl.getArgs()));
    }

    @Test
    public void testStopAtNonOptionInsideBurstedToken() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "-axb" }, true);

        assertTrue("-a debe estar presente", cl.hasOption("a"));
        assertFalse("-b no debe estar presente", cl.hasOption("b"));
        assertEquals("El resto 'xb' debe ser argumento posicional",
                     Arrays.asList("xb"), Arrays.asList(cl.getArgs()));
    }

    @Test
    public void testUnknownShortOptionIgnoredWhenNotStopping() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "-x", "-a" });

        assertTrue("-a debe estar presente", cl.hasOption("a"));
        assertEquals("La opción desconocida '-x' se ignora sin producir argumentos",
                     0, cl.getArgs().length);
    }

    @Test
    public void testUnknownLongOptionThrowsUnrecognized() throws Exception
    {
        try
        {
            parser.parse(options, new String[] { "--unknown" });
            fail("Se esperaba UnrecognizedOptionException");
        }
        catch (UnrecognizedOptionException e)
        {
            assertEquals("La opción no reconocida debe ser '--unknown'", "--unknown", e.getOption());
        }
    }

    @Test
    public void testMissingArgumentAtEndOfTokens() throws Exception
    {
        try
        {
            parser.parse(options, new String[] { "-a", "-b" });
            fail("Se esperaba MissingArgumentException");
        }
        catch (MissingArgumentException e)
        {
            assertEquals("La opción sin argumento debe ser 'b'", "b", e.getOption().getOpt());
        }
    }
}
