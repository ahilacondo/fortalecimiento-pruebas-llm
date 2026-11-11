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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

/**
 * Pruebas adicionales para {@link PosixParser} que usan solo la API pública.
 */
public class PosixParserLLMTest
{
    private Options options;
    private Parser parser;

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
    public void testEmptyArgumentsProduceNoOptionsAndNoArgs() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[0]);

        assertFalse("Confirm -a is not set", cl.hasOption("a"));
        assertFalse("Confirm -b is not set", cl.hasOption("b"));
        assertFalse("Confirm -c is not set", cl.hasOption("c"));
        assertTrue("Confirm no extra args", cl.getArgList().isEmpty());
    }

    @Test
    public void testSingleFlagFollowedByPositionalArgument() throws Exception
    {
        String[] args = new String[] { "-a", "file.txt" };

        CommandLine cl = parser.parse(options, args);

        assertTrue("Confirm -a is set", cl.hasOption("a"));
        assertFalse("Confirm -b is not set", cl.hasOption("b"));
        assertEquals("Confirm one extra arg", 1, cl.getArgList().size());
        assertEquals("Confirm value of extra arg", "file.txt", cl.getArgList().get(0));
    }

    @Test
    public void testAttachedShortOptionValueWithoutOtherTokens() throws Exception
    {
        String[] args = new String[] { "-bfoo" };

        CommandLine cl = parser.parse(options, args);

        assertTrue("Confirm -b is set", cl.hasOption("b"));
        assertEquals("Confirm attached value of -b", "foo", cl.getOptionValue("b"));
        assertTrue("Confirm no extra args", cl.getArgList().isEmpty());
    }

    @Test
    public void testCombinedFlagsAreAllSet() throws Exception
    {
        String[] args = new String[] { "-ac" };

        CommandLine cl = parser.parse(options, args);

        assertTrue("Confirm -a is set", cl.hasOption("a"));
        assertTrue("Confirm -c is set", cl.hasOption("c"));
        assertFalse("Confirm -b is not set", cl.hasOption("b"));
        assertTrue("Confirm no extra args", cl.getArgList().isEmpty());
    }

    @Test
    public void testLongOptionWithEqualsSignSetsValue() throws Exception
    {
        String[] args = new String[] { "--bfile=toast" };

        CommandLine cl = parser.parse(options, args);

        assertTrue("Confirm -b is set", cl.hasOption("b"));
        assertEquals("Confirm value of --bfile", "toast", cl.getOptionValue("bfile"));
        assertTrue("Confirm no extra args", cl.getArgList().isEmpty());
    }

    @Test
    public void testLongOptionWithEqualsSignSplitsOnFirstEqualsOnly() throws Exception
    {
        String[] args = new String[] { "--bfile=a=b" };

        CommandLine cl = parser.parse(options, args);

        assertEquals("Confirm value keeps later '=' characters", "a=b", cl.getOptionValue("b"));
    }

    @Test
    public void testLongOptionWithSeparateValueAndTrailingArgs() throws Exception
    {
        String[] args = new String[] { "--bfile", "toast", "foo" };

        CommandLine cl = parser.parse(options, args);

        assertEquals("Confirm value of --bfile", "toast", cl.getOptionValue("b"));
        assertEquals("Confirm one extra arg", 1, cl.getArgList().size());
        assertEquals("Confirm value of extra arg", "foo", cl.getArgList().get(0));
    }

    @Test
    public void testRepeatedFlagIsAccepted() throws Exception
    {
        String[] args = new String[] { "-a", "-a" };

        CommandLine cl = parser.parse(options, args);

        assertTrue("Confirm -a is set", cl.hasOption("a"));
        assertTrue("Confirm no extra args", cl.getArgList().isEmpty());
    }

    @Test
    public void testUnknownSingleShortOptionIsIgnored() throws Exception
    {
        String[] args = new String[] { "-z" };

        CommandLine cl = parser.parse(options, args);

        assertFalse("Confirm -a is not set", cl.hasOption("a"));
        assertFalse("Confirm -b is not set", cl.hasOption("b"));
        assertFalse("Confirm -c is not set", cl.hasOption("c"));
        assertTrue("Confirm no extra args", cl.getArgList().isEmpty());
    }

    @Test
    public void testUnrecognizedCharacterInBurstedTokenThrowsWithWholeToken() throws Exception
    {
        String[] args = new String[] { "-az" };

        try
        {
            parser.parse(options, args);
            fail("UnrecognizedOptionException wasn't thrown");
        }
        catch (UnrecognizedOptionException e)
        {
            assertEquals("Confirm offending token", "-az", e.getOption());
        }
    }

    @Test
    public void testMissingArgumentForStandaloneShortOption() throws Exception
    {
        String[] args = new String[] { "-b" };

        try
        {
            parser.parse(options, args);
            fail("MissingArgumentException wasn't thrown");
        }
        catch (MissingArgumentException e)
        {
            assertNotNull("Confirm option is reported", e.getOption());
            assertEquals("Confirm missing option", "b", e.getOption().getOpt());
        }
    }

    @Test
    public void testMissingArgumentForStandaloneLongOption() throws Exception
    {
        String[] args = new String[] { "--bfile" };

        try
        {
            parser.parse(options, args);
            fail("MissingArgumentException wasn't thrown");
        }
        catch (MissingArgumentException e)
        {
            assertNotNull("Confirm option is reported", e.getOption());
            assertEquals("Confirm missing option", "b", e.getOption().getOpt());
        }
    }

    @Test
    public void testSingleHyphenWithoutOptionsIsPositionalArgument() throws Exception
    {
        String[] args = new String[] { "-" };

        CommandLine cl = parser.parse(options, args);

        assertEquals("Confirm one extra arg", 1, cl.getArgList().size());
        assertEquals("Confirm value of extra arg", "-", cl.getArgList().get(0));
    }

    @Test
    public void testDoubleDashWithoutOptionsTreatsFollowingTokensAsArgs() throws Exception
    {
        String[] args = new String[] { "--", "-a" };

        CommandLine cl = parser.parse(options, args);

        assertFalse("Confirm -a is not set after --", cl.hasOption("a"));
        assertEquals("Confirm one extra arg", 1, cl.getArgList().size());
        assertEquals("Confirm value of extra arg", "-a", cl.getArgList().get(0));
    }

    @Test
    public void testStopAtNonOptionWithLeadingPositional() throws Exception
    {
        String[] args = new String[] { "foo", "-a" };

        CommandLine cl = parser.parse(options, args, true);

        assertFalse("Confirm -a is not set after first non-option", cl.hasOption("a"));
        assertEquals("Confirm two extra args", 2, cl.getArgList().size());
        assertEquals("Confirm first extra arg", "foo", cl.getArgList().get(0));
        assertEquals("Confirm second extra arg", "-a", cl.getArgList().get(1));
    }

    @Test
    public void testWithoutStopAtNonOptionOptionsAfterPositionalAreParsed() throws Exception
    {
        String[] args = new String[] { "foo", "-a" };

        CommandLine cl = parser.parse(options, args, false);

        assertTrue("Confirm -a is set", cl.hasOption("a"));
        assertEquals("Confirm one extra arg", 1, cl.getArgList().size());
        assertEquals("Confirm value of extra arg", "foo", cl.getArgList().get(0));
    }
}
