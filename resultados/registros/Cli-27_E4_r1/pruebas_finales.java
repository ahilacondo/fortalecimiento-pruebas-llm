package org.apache.commons.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link OptionGroup} que cubren el comportamiento
 * documentado y no verificado por {@code OptionGroupTest}.
 */
public class OptionGroupLLMTest
{
    // ------------------------------------------------------------------
    // Requisito 1: addOption devuelve el mismo grupo (encadenamiento)
    // ------------------------------------------------------------------

    @Test
    public void addOptionReturnsSameGroupInstance()
    {
        OptionGroup group = new OptionGroup();
        Option option = new Option("f", "file", false, "file to process");

        OptionGroup returned = group.addOption(option);

        assertSame("addOption debe devolver el mismo grupo", group, returned);
    }

    @Test
    public void addOptionSupportsChainedCalls()
    {
        Option file = new Option("f", "file", false, "file to process");
        Option dir = new Option("d", "directory", false, "directory to process");

        OptionGroup group = new OptionGroup().addOption(file).addOption(dir);

        assertEquals("Ambas opciones deben estar en el grupo", 2, group.getOptions().size());
        assertTrue(group.getOptions().contains(file));
        assertTrue(group.getOptions().contains(dir));
    }

    // ------------------------------------------------------------------
    // Requisito 2: getOptions devuelve las opciones añadidas
    // ------------------------------------------------------------------

    @Test
    public void getOptionsIsEmptyForNewGroup()
    {
        OptionGroup group = new OptionGroup();

        assertNotNull("getOptions nunca debe devolver null", group.getOptions());
        assertTrue("Un grupo nuevo no tiene opciones", group.getOptions().isEmpty());
    }

    @Test
    public void getOptionsContainsExactlyTheAddedOptions()
    {
        Option file = new Option("f", "file", false, "file to process");
        Option dir = new Option("d", "directory", false, "directory to process");
        OptionGroup group = new OptionGroup();
        group.addOption(file);
        group.addOption(dir);

        assertEquals("El número de opciones debe coincidir", 2, group.getOptions().size());
        assertTrue("Debe contener la opción -f", group.getOptions().contains(file));
        assertTrue("Debe contener la opción -d", group.getOptions().contains(dir));
    }

    // ------------------------------------------------------------------
    // Requisitos 3 y 4: setSelected fija la selección; getSelected la devuelve;
    // null si no hay selección
    // ------------------------------------------------------------------

    @Test
    public void getSelectedIsNullWhenNothingSelected()
    {
        OptionGroup group = new OptionGroup();
        group.addOption(new Option("f", "file", false, "file to process"));

        assertNull("Sin selección previa, getSelected debe devolver null", group.getSelected());
    }

    @Test
    public void setSelectedStoresOptionNameReturnedByGetSelected() throws Exception
    {
        OptionGroup group = new OptionGroup();
        Option file = new Option("f", "file", false, "file to process");
        Option dir = new Option("d", "directory", false, "directory to process");
        group.addOption(file);
        group.addOption(dir);

        group.setSelected(file);

        assertEquals("getSelected debe devolver el nombre de la opción seleccionada", "f", group.getSelected());
    }

    // ------------------------------------------------------------------
    // Requisito 6: volver a seleccionar la misma opción no lanza excepción
    // ------------------------------------------------------------------

    @Test
    public void reselectingSameOptionDoesNotThrow() throws Exception
    {
        OptionGroup group = new OptionGroup();
        Option file = new Option("f", "file", false, "file to process");
        group.addOption(file);

        group.setSelected(file);
        group.setSelected(file);

        assertEquals("La selección debe seguir siendo -f", "f", group.getSelected());
    }

    @Test
    public void selectingDifferentOptionThrowsAndKeepsPreviousSelection() throws Exception
    {
        OptionGroup group = new OptionGroup();
        Option file = new Option("f", "file", false, "file to process");
        Option dir = new Option("d", "directory", false, "directory to process");
        group.addOption(file);
        group.addOption(dir);
        group.setSelected(file);

        try
        {
            group.setSelected(dir);
            fail("Seleccionar una segunda opción distinta debe lanzar AlreadySelectedException");
        }
        catch (AlreadySelectedException e)
        {
            assertEquals("La selección previa no debe cambiar", "f", group.getSelected());
        }
    }

    // ------------------------------------------------------------------
    // Requisito 5: setSelected(null) restablece la selección previa
    // ------------------------------------------------------------------

    @Test
    public void setSelectedNullResetsPreviousSelection() throws Exception
    {
        OptionGroup group = new OptionGroup();
        Option file = new Option("f", "file", false, "file to process");
        group.addOption(file);
        group.setSelected(file);

        group.setSelected(null);

        assertNull("Tras setSelected(null), getSelected debe devolver null", group.getSelected());
    }

    @Test
    public void afterResetAnotherOptionCanBeSelected() throws Exception
    {
        OptionGroup group = new OptionGroup();
        Option file = new Option("f", "file", false, "file to process");
        Option dir = new Option("d", "directory", false, "directory to process");
        group.addOption(file);
        group.addOption(dir);
        group.setSelected(file);
        group.setSelected(null);

        group.setSelected(dir);

        assertEquals("Tras el reset, se puede seleccionar otra opción", "d", group.getSelected());
    }

    // ------------------------------------------------------------------
    // Requisito 7: setRequired / isRequired
    // ------------------------------------------------------------------

    @Test
    public void isRequiredIsFalseByDefault()
    {
        OptionGroup group = new OptionGroup();

        assertFalse("Un grupo nuevo no es obligatorio", group.isRequired());
    }

    @Test
    public void setRequiredTrueIsReflectedByIsRequired()
    {
        OptionGroup group = new OptionGroup();

        group.setRequired(true);

        assertTrue("isRequired debe devolver true tras setRequired(true)", group.isRequired());
    }

    @Test
    public void setRequiredFalseIsReflectedByIsRequired()
    {
        OptionGroup group = new OptionGroup();
        group.setRequired(true);

        group.setRequired(false);

        assertFalse("isRequired debe devolver false tras setRequired(false)", group.isRequired());
    }

    // ------------------------------------------------------------------
    // Requisito 8: getNames incluye opciones que solo tienen nombre largo
    // ------------------------------------------------------------------

    @Test
    public void getNamesIncludesLongOnlyOptions()
    {
        OptionGroup group = new OptionGroup();
        group.addOption(new Option(null, "import", false, "section to process"));
        group.addOption(new Option(null, "export", false, "chapter to process"));

        assertNotNull("getNames no debe devolver null", group.getNames());
        assertEquals("Deben aparecer los dos nombres largos", 2, group.getNames().size());
        assertTrue("Debe contener 'import'", group.getNames().contains("import"));
        assertTrue("Debe contener 'export'", group.getNames().contains("export"));
    }

    @Test
    public void getNamesIncludesBothShortAndLongOnlyOptions()
    {
        OptionGroup group = new OptionGroup();
        group.addOption(new Option("f", "file", false, "file to process"));
        group.addOption(new Option(null, "import", false, "section to process"));

        assertEquals("Deben aparecer ambos nombres", 2, group.getNames().size());
        assertTrue("Debe contener la clave corta 'f'", group.getNames().contains("f"));
        assertTrue("Debe contener el nombre largo 'import'", group.getNames().contains("import"));
    }

    @Test
    public void getNamesIsEmptyForNewGroup()
    {
        OptionGroup group = new OptionGroup();

        assertNotNull("getNames no debe devolver null", group.getNames());
        assertTrue("Un grupo nuevo no tiene nombres", group.getNames().isEmpty());
    }
}
