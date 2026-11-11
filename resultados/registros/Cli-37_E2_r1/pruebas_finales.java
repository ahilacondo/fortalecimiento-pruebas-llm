package org.apache.commons.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Properties;

import org.junit.Test;

public class DefaultParserLLMTest {

    private static CommandLine parseArgs(Options options, String... args) throws ParseException {
        return new DefaultParser().parse(options, args);
    }

    private static CommandLine parseProps(Options options, Properties props, String... args) throws ParseException {
        return new DefaultParser().parse(options, args, props);
    }

    private static CommandLine parseStop(Options options, boolean stop, String... args) throws ParseException {
        return new DefaultParser().parse(options, args, stop);
    }

    // ---------------------------------------------------------------
    // Propiedades (handleProperties)
    // ---------------------------------------------------------------

    @Test
    public void unrecognizedPropertyThrowsUnrecognizedOptionException() {
        Options options = new Options();
        options.addOption("a", false, "flag a");
        Properties props = new Properties();
        props.setProperty("zzz", "true");
        try {
            parseProps(options, props);
            fail("Se esperaba UnrecognizedOptionException");
        } catch (UnrecognizedOptionException e) {
            assertEquals("zzz", e.getOption());
        } catch (ParseException e) {
            fail("Excepción inesperada: " + e);
        }
    }

    @Test
    public void booleanPropertyYesIsApplied() throws ParseException {
        Options options = new Options();
        options.addOption("a", false, "flag a");
        Properties props = new Properties();
        props.setProperty("a", "YES");
        CommandLine cl = parseProps(options, props);
        assertTrue(cl.hasOption("a"));
    }

    @Test
    public void booleanPropertyOneIsApplied() throws ParseException {
        Options options = new Options();
        options.addOption("a", false, "flag a");
        Properties props = new Properties();
        props.setProperty("a", "1");
        CommandLine cl = parseProps(options, props);
        assertTrue(cl.hasOption("a"));
    }

    @Test
    public void booleanPropertyNoIsNotApplied() throws ParseException {
        Options options = new Options();
        options.addOption("a", false, "flag a");
        Properties props = new Properties();
        props.setProperty("a", "no");
        CommandLine cl = parseProps(options, props);
        assertFalse(cl.hasOption("a"));
    }

    @Test
    public void argumentPropertyProvidesValueWhenAbsentFromCommandLine() throws ParseException {
        Options options = new Options();
        options.addOption("o", true, "output");
        Properties props = new Properties();
        props.setProperty("o", "fromProps");
        CommandLine cl = parseProps(options, props);
        assertTrue(cl.hasOption("o"));
        assertEquals("fromProps", cl.getOptionValue("o"));
    }

    @Test
    public void propertyDoesNotOverrideValueFromCommandLine() throws ParseException {
        Options options = new Options();
        options.addOption("o", true, "output");
        Properties props = new Properties();
        props.setProperty("o", "fromProps");
        CommandLine cl = parseProps(options, props, "-o", "fromCli");
        assertEquals("fromCli", cl.getOptionValue("o"));
    }

    @Test
    public void propertyIsIgnoredWhenGroupMemberAlreadySelected() throws ParseException {
        Options options = new Options();
        OptionGroup group = new OptionGroup();
        group.addOption(new Option("a", "alpha option"));
        group.addOption(new Option("b", "beta option"));
        options.addOptionGroup(group);
        Properties props = new Properties();
        props.setProperty("b", "true");
        CommandLine cl = parseProps(options, props, "-a");
        assertTrue(cl.hasOption("a"));
        assertFalse(cl.hasOption("b"));
    }

    @Test
    public void requiredOptionSatisfiedByProperty() throws ParseException {
        Options options = new Options();
        Option required = new Option("r", "required option");
        required.setRequired(true);
        options.addOption(required);
        Properties props = new Properties();
        props.setProperty("r", "true");
        CommandLine cl = parseProps(options, props);
        assertTrue(cl.hasOption("r"));
    }

    // ---------------------------------------------------------------
    // Opciones requeridas y grupos
    // ---------------------------------------------------------------

    @Test
    public void missingRequiredOptionThrowsMissingOptionException() {
        Options options = new Options();
        Option required = new Option("r", "required option");
        required.setRequired(true);
        options.addOption(required);
        try {
            parseArgs(options);
            fail("Se esperaba MissingOptionException");
        } catch (MissingOptionException e) {
            assertEquals(1, e.getMissingOptions().size());
        } catch (ParseException e) {
            fail("Excepción inesperada: " + e);
        }
    }

    @Test
    public void requiredGroupSatisfiedByOneMember() throws ParseException {
        Options options = new Options();
        OptionGroup group = new OptionGroup();
        group.addOption(new Option("a", "alpha option"));
        group.addOption(new Option("b", "beta option"));
        group.setRequired(true);
        options.addOptionGroup(group);
        CommandLine cl = parseArgs(options, "-b");
        assertTrue(cl.hasOption("b"));
        assertFalse(cl.hasOption("a"));
    }

    @Test
    public void unsatisfiedRequiredGroupThrowsMissingOptionException() {
        Options options = new Options();
        OptionGroup group = new OptionGroup();
        group.addOption(new Option("a", "alpha option"));
        group.addOption(new Option("b", "beta option"));
        group.setRequired(true);
        options.addOptionGroup(group);
        try {
            parseArgs(options);
            fail("Se esperaba MissingOptionException");
        } catch (MissingOptionException e) {
            assertEquals(1, e.getMissingOptions().size());
        } catch (ParseException e) {
            fail("Excepción inesperada: " + e);
        }
    }
}
