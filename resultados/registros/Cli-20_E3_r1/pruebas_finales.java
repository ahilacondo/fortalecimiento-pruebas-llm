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

import java.util.Arrays;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link PosixParser} que cubren comportamientos
 * no verificados por {@code PosixParserTest}. Solo usan la API pública.
 */
public class PosixParserLLMTest {

    /**
     * Crea las opciones usadas en las pruebas:
     * -a/--enable-a (sin argumento), -b/--bfile (con argumento),
     * -c/--copt (sin argumento) y -J (con múltiples argumentos).
     */
    private static Options newOptions() {
        Options options = new Options();
        options.addOption("a", "enable-a", false, "turn [a] on or off");
        options.addOption("b", "bfile", true, "set the value of [b]");
        options.addOption("c", "copt", false, "turn [c] on or off");
        options.addOption(OptionBuilder.withValueSeparator().hasArgs(2).create('J'));
        return options;
    }

    // ------------------------------------------------------------------
    // 1. Opción larga con '=': separa nombre y valor; conserva los '=' restantes
    // ------------------------------------------------------------------

    @Test
    public void longOptionWithEqualsKeepsRemainingEqualsInValue() throws Exception {
        Parser parser = new PosixParser();

        CommandLine cl = parser.parse(newOptions(), new String[] { "--bfile=a=b" });

        assertTrue("-b debe estar presente", cl.hasOption("b"));
        assertEquals("El valor debe conservar los '=' posteriores", "a=b", cl.getOptionValue("b"));
        assertEquals("Acceso por nombre largo", "a=b", cl.getOptionValue("bfile"));
        assertTrue("No deben quedar argumentos sobrantes", cl.getArgList().isEmpty());
    }

    @Test
    public void longOptionWithEqualsAndManyEqualsSignsKeepsAllAfterFirst() throws Exception {
        Parser parser = new PosixParser();

        CommandLine cl = parser.parse(newOptions(), new String[] { "--bfile=x=y=z" });

        assertEquals("x=y=z", cl.getOptionValue("b"));
        assertTrue(cl.getArgList().isEmpty());
    }

    // ------------------------------------------------------------------
    // 2. Opción larga con '=' aplicada a una opción sin argumento
    // ------------------------------------------------------------------

    @Test
    public void longOptionWithEqualsOnNoArgOptionLeavesValueAsArgument() throws Exception {
        Parser parser = new PosixParser();

        // flatten() separa "--enable-a=xyz" en "--enable-a" y "xyz";
        // como -a no admite argumento, "xyz" queda como argumento suelto.
        CommandLine cl = parser.parse(newOptions(), new String[] { "--enable-a=xyz" });

        assertTrue("-a debe estar presente", cl.hasOption("a"));
        assertNull("La opción sin argumento no debe tener valor", cl.getOptionValue("a"));
        assertEquals("El valor tras '=' queda como argumento", Arrays.asList("xyz"), cl.getArgList());
    }

    private static void assertNull(String message, Object actual) {
        org.junit.Assert.assertNull(message, actual);
    }

    // ------------------------------------------------------------------
    // 3. Opción larga desconocida durante el aplanado
    // ------------------------------------------------------------------

    @Test
    public void unknownLongOptionThrowsUnrecognizedOptionException() throws Exception {
        Parser parser = new PosixParser();

        try {
            parser.parse(newOptions(), new String[] { "--foo" });
            fail("UnrecognizedOptionException no fue lanzada");
        } catch (UnrecognizedOptionException e) {
            assertEquals("--foo", e.getOption());
        }
    }

    @Test
    public void unknownLongOptionWithEqualsReportsNameWithoutValue() throws Exception {
        Parser parser = new PosixParser();

        try {
            parser.parse(newOptions(), new String[] { "--foo=bar" });
            fail("UnrecognizedOptionException no fue lanzada");
        } catch (UnrecognizedOptionException e) {
            assertEquals("El nombre reportado no debe incluir el valor", "--foo", e.getOption());
        }
    }

    // ------------------------------------------------------------------
    // 4. Opción larga que requiere argumento y aparece sin valor al final
    // ------------------------------------------------------------------

    @Test
    public void longOptionRequiringArgumentAtEndThrowsMissingArgumentException() throws Exception {
        Parser parser = new PosixParser();

        try {
            parser.parse(newOptions(), new String[] { "--bfile" });
            fail("MissingArgumentException no fue lanzada");
        } catch (MissingArgumentException e) {
            assertEquals("La opción que falta argumento es -b", "b", e.getOption().getOpt());
        }
    }

    @Test
    public void longOptionRequiringArgumentAfterOtherOptionThrowsMissingArgument() throws Exception {
        Parser parser = new PosixParser();

        try {
            parser.parse(newOptions(), new String[] { "--copt", "--bfile" });
            fail("MissingArgumentException no fue lanzada");
        } catch (MissingArgumentException e) {
            assertEquals("b", e.getOption().getOpt());
        }
    }

    // ------------------------------------------------------------------
    // 5. stopAtNonOption=true: el token siguiente a una opción con argumento
    //    se consume como su valor y no detiene el procesado
    // ------------------------------------------------------------------

    @Test
    public void stopAtNonOptionConsumesTokenAsValueAndContinuesParsing() throws Exception {
        Parser parser = new PosixParser();

        CommandLine cl = parser.parse(newOptions(), new String[] { "-b", "foo", "-a" }, true);

        assertEquals("'foo' debe ser el valor de -b", "foo", cl.getOptionValue("b"));
        assertTrue("El procesado debe continuar y activar -a", cl.hasOption("a"));
        assertTrue("No deben quedar argumentos sobrantes", cl.getArgList().isEmpty());
    }

    @Test
    public void stopAtNonOptionWithLongOptionConsumesValueAndContinues() throws Exception {
        Parser parser = new PosixParser();

        CommandLine cl = parser.parse(newOptions(), new String[] { "--bfile", "toast", "-c" }, true);

        assertEquals("toast", cl.getOptionValue("b"));
        assertTrue("-c debe procesarse tras el valor", cl.hasOption("c"));
        assertTrue(cl.getArgList().isEmpty());
    }

    // ------------------------------------------------------------------
    // 6. Ráfaga "-bc" donde -b requiere argumento: "c" es valor de -b
    // ------------------------------------------------------------------

    @Test
    public void burstWhereOptionWithArgPrecedesOthersTreatsRestAsValue() throws Exception {
        Parser parser = new PosixParser();

        CommandLine cl = parser.parse(newOptions(), new String[] { "-bc" });

        assertTrue("-b debe estar presente", cl.hasOption("b"));
        assertEquals("'c' debe ser el valor de -b", "c", cl.getOptionValue("b"));
        assertFalse("-c NO debe estar activada", cl.hasOption("c"));
        assertTrue("No deben quedar argumentos sobrantes", cl.getArgList().isEmpty());
    }

    @Test
    public void burstWithArgOptionFollowedByMoreCharsKeepsAllRemainingAsValue() throws Exception {
        Parser parser = new PosixParser();

        CommandLine cl = parser.parse(newOptions(), new String[] { "-abcxyz" });

        assertTrue("-a debe estar presente", cl.hasOption("a"));
        assertTrue("-b debe estar presente", cl.hasOption("b"));
        assertEquals("'cxyz' debe ser el valor de -b", "cxyz", cl.getOptionValue("b"));
        assertFalse("-c NO debe estar activada", cl.hasOption("c"));
    }

    // ------------------------------------------------------------------
    // 7. Opciones con múltiples argumentos (hasArgs) y stopAtNonOption=true
    // ------------------------------------------------------------------

    @Test
    public void hasArgsOptionWithStopAtNonOptionStopsValueConsumptionAtNonOption() throws Exception {
        Parser parser = new PosixParser();

        // En flatten(), process() consume "a" como valor de -J (hasArg() es true
        // para hasArgs(2)). El siguiente token "b" no tiene opción activa, por lo
        // que se emite "--" y el resto pasa a argumentos sin ser valores de -J.
        CommandLine cl = parser.parse(newOptions(), new String[] { "-J", "a", "b", "c" }, true);

        assertTrue("-J debe estar presente", cl.hasOption("J"));
        String[] values = cl.getOptionValues("J");
        assertNotNull("Los valores de -J no deben ser nulos", values);
        assertEquals("Solo el primer token debe ser valor de -J", Arrays.asList("a"), Arrays.asList(values));
        assertEquals("'b' y 'c' deben quedar como argumentos", Arrays.asList("b", "c"), cl.getArgList());
    }

    @Test
    public void hasArgsOptionWithoutStopAtNonOptionCollectsValuesUntilOption() throws Exception {
        Parser parser = new PosixParser();

        CommandLine cl = parser.parse(newOptions(), new String[] { "-J", "a", "b", "-c" });

        assertTrue("-J debe estar presente", cl.hasOption("J"));
        assertTrue("-c debe estar presente", cl.hasOption("c"));
        String[] values = cl.getOptionValues("J");
        assertNotNull(values);
        assertEquals(Arrays.asList("a", "b"), Arrays.asList(values));
        assertTrue("No deben quedar argumentos sobrantes", cl.getArgList().isEmpty());
    }

    // ------------------------------------------------------------------
    // 8. Token vacío "" tratado como argumento no opción
    // ------------------------------------------------------------------

    @Test
    public void emptyTokenIsTreatedAsNonOptionArgument() throws Exception {
        Parser parser = new PosixParser();

        CommandLine cl = parser.parse(newOptions(), new String[] { "", "foo" });

        assertEquals("El token vacío debe conservarse como argumento",
                Arrays.asList("", "foo"), cl.getArgList());
    }

    @Test
    public void emptyTokenAfterShortOptionIsKeptAsArgument() throws Exception {
        Parser parser = new PosixParser();

        CommandLine cl = parser.parse(newOptions(), new String[] { "-a", "", "foo" });

        assertTrue("-a debe estar presente", cl.hasOption("a"));
        assertEquals(Arrays.asList("", "foo"), cl.getArgList());
    }

    @Test
    public void emptyTokenWithStopAtNonOptionStopsParsingAndKeepsRest() throws Exception {
        Parser parser = new PosixParser();

        CommandLine cl = parser.parse(newOptions(), new String[] { "", "-a" }, true);

        assertFalse("Tras el token vacío no debe procesarse -a", cl.hasOption("a"));
        assertEquals(Arrays.asList("", "-a"), cl.getArgList());
    }
}
