package org.apache.commons.cli;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link PosixParser} que cubren los requisitos de
 * documentación no verificados por {@code PosixParserTest}.
 *
 * <p>Nota: {@code flatten} es protegido; esta clase está en el mismo paquete
 * que {@code PosixParser}, por lo que puede invocarlo directamente.</p>
 */
public class PosixParserLLMTest {

    /** Opciones comunes: -a (sin argumento), -b (un argumento), -c (sin argumento). */
    private static Options baseOptions() {
        Options options = new Options();
        options.addOption("a", false, "flag a");
        options.addOption("b", true, "argumento b");
        options.addOption("c", false, "flag c");
        return options;
    }

    /** Invoca flatten sobre una instancia nueva de PosixParser. */
    private static String[] flatten(Options options, String[] args, boolean stopAtNonOption) {
        return new PosixParser().flatten(options, args, stopAtNonOption);
    }

    // Requisito 1: con stopAtNonOption=false, las opciones tras un no-opción siguen interpretándose.
    @Test
    public void testOptionsAfterNonOptionAreStillProcessedWhenNotStopping() throws Exception {
        Options options = baseOptions();
        String[] args = { "foo", "-a", "-b", "toast" };

        String[] tokens = flatten(options, args, false);

        assertArrayEquals(new String[] { "foo", "-a", "-b", "toast" }, tokens);
    }

    @Test
    public void testParseRecognizesOptionAfterNonOptionWhenNotStopping() throws Exception {
        Options options = baseOptions();
        String[] args = { "foo", "-a" };

        CommandLine cl = new PosixParser().parse(options, args);

        assertTrue("-a debe reconocerse tras un no-opción", cl.hasOption("a"));
        assertEquals("foo", cl.getArgList().get(0));
    }

    // Requisito 2: con stopAtNonOption=false, "-x" (dos caracteres, no válida) debe ignorarse en flatten.
    @Test
    public void testUnknownTwoCharOptionIsIgnoredWhenNotStopping() throws Exception {
        Options options = baseOptions();
        String[] args = { "-x", "-a" };

        String[] tokens = flatten(options, args, false);

        assertArrayEquals("\"-x\" no válida debe ignorarse", new String[] { "-a" }, tokens);
    }

    // Requisito 3: con stopAtNonOption=false, carácter no válido en token agrupado se añade como "-" + carácter.
    @Test
    public void testUnknownCharInBurstedTokenIsAddedAsSingleOptionWhenNotStopping() throws Exception {
        Options options = baseOptions();
        String[] args = { "-az" };

        String[] tokens = flatten(options, args, false);

        assertArrayEquals("'z' no válida debe añadirse como \"-z\"",
                new String[] { "-a", "-z" }, tokens);
    }

    // Requisito 4: con stopAtNonOption=true, "--" y "-" tras el primer no-opción se copian sin procesar.
    @Test
    public void testDoubleDashAndSingleDashAfterNonOptionAreCopiedRaw() throws Exception {
        Options options = baseOptions();
        String[] args = { "foo", "--", "-", "-a" };

        String[] tokens = flatten(options, args, true);

        assertArrayEquals(new String[] { "--", "foo", "--", "-", "-a" }, tokens);
    }

    // Requisito 5: con stopAtNonOption=true, al encontrar carácter desconocido en token agrupado,
    // los tokens restantes se copian directamente.
    @Test
    public void testUnknownCharInBurstWithStopCopiesRemainingTokensDirectly() throws Exception {
        Options options = baseOptions();
        String[] args = { "-azc", "foo", "-b", "x" };

        String[] tokens = flatten(options, args, true);

        assertArrayEquals(new String[] { "-a", "--", "zc", "foo", "-b", "x" }, tokens);
    }

    // Requisito 6: una entrada no manejada por ninguna regla (cadena vacía) se añade tal cual.
    @Test
    public void testEmptyStringIsAddedAsIs() throws Exception {
        Options options = baseOptions();
        String[] args = { "" };

        String[] tokens = flatten(options, args, false);

        assertArrayEquals(new String[] { "" }, tokens);
    }

    // Requisito 7: para opciones con múltiples valores, process debe añadir los valores sucesivos.
    @Test
    public void testMultiValueOptionAddsSuccessiveValues() throws Exception {
        Options options = new Options();
        Option multi = new Option("J", true, "múltiples valores");
        multi.setArgs(2);
        options.addOption(multi);
        String[] args = { "-J", "a", "b", "c" };

        String[] tokens = flatten(options, args, true);

        assertArrayEquals("Los valores sucesivos de -J deben añadirse sin \"--\"",
                new String[] { "-J", "a", "b", "c" }, tokens);
    }

    // Requisito 8: init debe restablecer currentOption entre invocaciones consecutivas.
    @Test
    public void testPendingOptionDoesNotLeakIntoNextFlattenInvocation() throws Exception {
        Options options = baseOptions();
        PosixParser parser = new PosixParser();

        // Primera invocación deja "-b" como opción pendiente de argumento.
        String[] first = parser.flatten(options, new String[] { "-b" }, false);
        assertArrayEquals(new String[] { "-b" }, first);

        // Segunda invocación: "foo" no debe consumirse como argumento de "-b".
        String[] second = parser.flatten(options, new String[] { "foo" }, true);
        assertArrayEquals("currentOption debe ser null tras init",
                new String[] { "--", "foo" }, second);
    }
}
