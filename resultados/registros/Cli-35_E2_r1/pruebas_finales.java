package org.apache.commons.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

public class OptionsLLMTest
{
    @Test
    public void testHasLongOptionAcceptsLongNameWithAndWithoutHyphens()
    {
        Options opts = new Options();
        opts.addOption("a", "alpha", false, "alpha option");

        assertTrue("long name without hyphens", opts.hasLongOption("alpha"));
        assertTrue("long name with double hyphens", opts.hasLongOption("--alpha"));
    }

    @Test
    public void testHasLongOptionRejectsShortNameAndUnknownName()
    {
        Options opts = new Options();
        opts.addOption("a", "alpha", false, "alpha option");
        opts.addOption("b", null, false, "short only");

        assertFalse("short name must not be a long option", opts.hasLongOption("a"));
        assertFalse("short-only name must not be a long option", opts.hasLongOption("b"));
        assertFalse("unknown name must not be a long option", opts.hasLongOption("zeta"));
    }

    @Test
    public void testHasShortOptionAcceptsShortNameWithAndWithoutHyphens()
    {
        Options opts = new Options();
        opts.addOption("a", "alpha", false, "alpha option");

        assertTrue("short name without hyphen", opts.hasShortOption("a"));
        assertTrue("short name with hyphen", opts.hasShortOption("-a"));
    }

    @Test
    public void testHasShortOptionRejectsLongNameAndUnknownName()
    {
        Options opts = new Options();
        opts.addOption("a", "alpha", false, "alpha option");
        opts.addOption("b", null, false, "short only");

        assertFalse("long name must not be a short option", opts.hasShortOption("alpha"));
        assertFalse("long name with hyphens must not be a short option", opts.hasShortOption("--alpha"));
        assertTrue("short-only name must be a short option", opts.hasShortOption("b"));
        assertFalse("unknown name must not be a short option", opts.hasShortOption("zeta"));
    }

    @Test
    public void testHasOptionMatchesShortOnlyLongOnlyAndUnknown()
    {
        Options opts = new Options();
        opts.addOption("a", "alpha", false, "both");
        opts.addOption("b", null, false, "short only");
        opts.addOption(new Option(null, "gamma", false, "long only"));

        assertTrue("short and long option by short name", opts.hasOption("a"));
        assertTrue("short and long option by long name", opts.hasOption("alpha"));
        assertTrue("short-only option", opts.hasOption("-b"));
        assertTrue("long-only option", opts.hasOption("--gamma"));
        assertFalse("unknown option", opts.hasOption("unknown"));
    }

    @Test
    public void testGetOptionByLongNameWithHyphensReturnsSameInstance()
    {
        Options opts = new Options();
        opts.addOption("a", "alpha", false, "alpha option");

        Option byShort = opts.getOption("a");
        Option byLong = opts.getOption("--alpha");

        assertNotNull(byShort);
        assertSame("same option via short and long name", byShort, byLong);
        assertEquals("alpha option", byLong.getDescription());
    }

    @Test
    public void testGetOptionReturnsNullForUnknownName()
    {
        Options opts = new Options();
        opts.addOption("a", "alpha", false, "alpha option");

        assertNull("unknown name must return null", opts.getOption("zeta"));
        assertNull("unknown long name must return null", opts.getOption("--zeta"));
    }

    @Test
    public void testAddOptionReturnsSameInstanceForChaining()
    {
        Options opts = new Options();

        assertSame(opts, opts.addOption("a", "desc"));
        assertSame(opts, opts.addOption("b", true, "desc"));
        assertSame(opts, opts.addOption("c", "gamma", false, "desc"));
        assertSame(opts, opts.addOption(new Option("d", "delta", false, "desc")));
    }

    @Test
    public void testAddOptionWithoutLongNameDoesNotRegisterLongName()
    {
        Options opts = new Options();
        opts.addOption("b", null, false, "short only");

        assertTrue(opts.hasShortOption("b"));
        assertFalse("no long name must have been registered", opts.hasLongOption("b"));
        assertTrue("getMatchingOptions must not report short-only options",
                   opts.getMatchingOptions("b").isEmpty());
    }

    @Test
    public void testGetMatchingOptionsStripsLeadingHyphens()
    {
        Options opts = new Options();
        opts.addOption("v", "version", false, "version");
        opts.addOption("b", "verbose", false, "verbose");
        opts.addOption("x", "xray", false, "xray");

        List<String> matches = opts.getMatchingOptions("--ver");

        assertEquals("both options starting with 'ver' must match", 2, matches.size());
        assertTrue(matches.contains("version"));
        assertTrue(matches.contains("verbose"));
        assertFalse(matches.contains("xray"));
    }

    @Test
    public void testGetMatchingOptionsIgnoresShortNames()
    {
        Options opts = new Options();
        opts.addOption("x", null, false, "short only");

        assertTrue("short names are not long options and must not match",
                   opts.getMatchingOptions("x").isEmpty());
    }

    @Test
    public void testGetOptionsReturnsUnmodifiableCollection()
    {
        Options opts = new Options();
        opts.addOption("a", "alpha", false, "alpha");
        opts.addOption("b", null, false, "beta");

        Collection<Option> all = opts.getOptions();

        assertEquals("two options must be returned", 2, all.size());
        try
        {
            all.add(new Option("c", "gamma", false, "gamma"));
            fail("Expected UnsupportedOperationException");
        }
        catch (UnsupportedOperationException expected)
        {
            // expected: the returned collection is read-only
        }
        assertEquals("collection must not have changed", 2, opts.getOptions().size());
    }

    @Test
    public void testGetRequiredOptionsContainsRequiredKeyAndIsUnmodifiable()
    {
        Options opts = new Options();
        opts.addOption(new Option("f", "file", true, "file"));
        opts.addOption(new Option("o", "output", false, "output not required"));
        Option required = new Option("r", "required", false, "required");
        required.setRequired(true);
        opts.addOption(required);

        List<?> requiredOpts = opts.getRequiredOptions();

        assertEquals("only required options must be listed", 1, requiredOpts.size());
        assertTrue("required key must be present", requiredOpts.contains("r"));
        assertFalse("non-required key must be absent", requiredOpts.contains("o"));
        try
        {
            opts.getRequiredOptions().add("x");
            fail("Expected UnsupportedOperationException");
        }
        catch (UnsupportedOperationException expected)
        {
            // expected: the returned list is read-only
        }
    }

    @Test
    public void testAddingSameRequiredOptionTwiceKeepsSingleEntry()
    {
        Options opts = new Options();
        Option first = new Option("f", "file", true, "first");
        first.setRequired(true);
        Option second = new Option("f", "file", true, "second");
        second.setRequired(true);

        opts.addOption(first);
        opts.addOption(second);

        assertEquals("duplicate required key must be stored once", 1, opts.getRequiredOptions().size());
        assertEquals("last one wins", "second", opts.getOption("f").getDescription());
    }

    @Test
    public void testAddRequiredOptionGroupRegistersGroupAsRequired()
    {
        Options opts = new Options();
        OptionGroup group = new OptionGroup();
        group.addOption(new Option("a", "alpha", false, "alpha"));
        group.addOption(new Option("b", "beta", false, "beta"));
        group.setRequired(true);

        opts.addOptionGroup(group);

        List<?> requiredOpts = opts.getRequiredOptions();
        assertEquals("required group must be registered", 1, requiredOpts.size());
        assertSame("the registered entry must be the group itself", group, requiredOpts.get(0));
    }

    @Test
    public void testAddOptionGroupMarksMembersNotRequired()
    {
        Options opts = new Options();
        Option a = new Option("a", "alpha", false, "alpha");
        a.setRequired(true);
        OptionGroup group = new OptionGroup();
        group.addOption(a);
        group.setRequired(true);

        opts.addOptionGroup(group);

        assertFalse("option inside a group cannot be required on its own", a.isRequired());
        assertTrue("option must still be registered", opts.hasOption("a"));
    }

    @Test
    public void testAddNonRequiredOptionGroupDoesNotAddToRequired()
    {
        Options opts = new Options();
        OptionGroup group = new OptionGroup();
        group.addOption(new Option("a", "alpha", false, "alpha"));
        group.addOption(new Option("b", "beta", false, "beta"));

        opts.addOptionGroup(group);

        assertTrue("non-required group must not be listed as required",
                   opts.getRequiredOptions().isEmpty());
        assertTrue(opts.hasOption("a"));
        assertTrue(opts.hasOption("b"));
    }

    @Test
    public void testGetOptionGroupReturnsGroupForMembersAndNullForOthers()
    {
        Options opts = new Options();
        OptionGroup group = new OptionGroup();
        Option a = new Option("a", "alpha", false, "alpha");
        group.addOption(a);
        opts.addOptionGroup(group);
        Option standalone = new Option("s", "solo", false, "solo");
        opts.addOption(standalone);

        assertSame("group member must map to its group", group, opts.getOptionGroup(a));
        assertNull("option outside any group must map to null", opts.getOptionGroup(standalone));
    }

    @Test
    public void testGetOptionGroupsContainsAddedGroupsOnly()
    {
        Options opts = new Options();
        OptionGroup group = new OptionGroup();
        group.addOption(new Option("a", "alpha", false, "alpha"));
        opts.addOptionGroup(group);
        opts.addOption(new Option("s", "solo", false, "solo"));

        Collection<OptionGroup> groups = opts.getOptionGroups();

        assertEquals("only one group must be reported", 1, groups.size());
        assertTrue(groups.contains(group));
    }

    @Test
    public void testAddOptionGroupReturnsSameInstance()
    {
        Options opts = new Options();
        OptionGroup group = new OptionGroup();
        group.addOption(new Option("a", "alpha", false, "alpha"));

        assertSame(opts, opts.addOptionGroup(group));
    }

    @Test
    public void testToStringContainsShortAndLongSections()
    {
        Options opts = new Options();
        opts.addOption("a", "alpha", false, "alpha");

        String s = opts.toString();

        assertNotNull(s);
        assertTrue("must start with the options header", s.startsWith("[ Options:"));
        assertTrue("must contain the short section", s.contains("short"));
        assertTrue("must contain the long section", s.contains("long"));
    }
}
