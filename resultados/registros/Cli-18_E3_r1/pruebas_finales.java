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

import org.junit.Test;

/**
 * Pruebas adicionales para {@link PosixParser} que cubren comportamientos
 * no verificados por {@link PosixParserTest}. Solo usan la API pública.
 */
public class PosixParserLLMTest
{
    /**
     * Crea las mismas opciones básicas usadas por la suite existente:
     * -a/--enable-a (sin argumento), -b/--bfile (con argumento), -c/--copt (sin argumento).
     */
    private static Options createOptions()
    {
        Options options = new Options();
        options.addOption("a", "enable-a", false, "turn [a] on or off");
        options.addOption("b", "bfile", true, "set the value of [b]");
        options.addOption("c", "copt", false, "turn [c] on or off");
        return options;
    }

    /**
     * Comportamiento 1: "--opción=valor" se divide en nombre y valor en el primer '='.
     */
    @Test
    public void testLongOptionWithEqualsSplitsNameAndValue() throws Exception
    {
        Parser parser = new PosixParser();
        CommandLine cl = parser.parse(createOptions(), new String[] { "--bfile=toast" });

        assertTrue("La opción larga --bfile debe estar presente", cl.hasOption("bfile"));
        assertTrue("La opción corta -b debe estar presente por el mismo Option", cl.hasOption("b"));
        assertEquals("El valor tras '=' debe asignarse a bfile", "toast", cl.getOptionValue("bfile"));
        assertTrue("No deben quedar argumentos posicionales", cl.getArgList().isEmpty());
    }

    /**
     * Comportamiento 2: el valor conserva los '=' posteriores al primero.
     */
    @Test
    public void testLongOptionValueKeepsSubsequentEquals() throws Exception
    {
        Parser parser = new PosixParser();
        CommandLine cl = parser.parse(createOptions(), new String[] { "--bfile=a=b=c" });

        assertTrue("La opción --bfile debe estar presente", cl.hasOption("bfile"));
        assertEquals("El valor debe incluir todo tras el primer '='", "a=b=c", cl.getOptionValue("bfile"));
        assertTrue("No deben quedar argumentos posicionales", cl.getArgList().isEmpty());
    }

    /**
     * Comportamiento 3: una opción corta desconocida de dos caracteres, sin stopAtNonOption,
     * se descarta sin añadirse a los tokens y sin lanzar excepción.
     */
    @Test
    public void testUnknownShortOptionDiscardedWithoutStopAtNonOption() throws Exception
    {
        Parser parser = new PosixParser();
        CommandLine cl = parser.parse(createOptions(), new String[] { "-x", "foo" });

        assertFalse("-x no es una opción conocida y no debe marcarse", cl.hasOption("x"));
        assertEquals("Solo 'foo' debe quedar como argumento", 1, cl.getArgList().size());
        assertEquals("El argumento restante debe ser 'foo'", "foo", cl.getArgList().get(0));
        assertFalse("'-x' no debe aparecer como argumento", cl.getArgList().contains("-x"));
    }

    /**
     * Comportamiento 4: una opción corta desconocida de dos caracteres, con stopAtNonOption,
     * detiene el procesamiento y copia el resto sin interpretarlo.
     */
    @Test
    public void testUnknownShortOptionStopsProcessingWithStopAtNonOption() throws Exception
    {
        Parser parser = new PosixParser();
        CommandLine cl = parser.parse(createOptions(), new String[] { "-x", "foo", "-a" }, true);

        assertFalse("-a tras la parada no debe interpretarse como opción", cl.hasOption("a"));
        assertEquals("Deben quedar exactamente 2 argumentos", 2, cl.getArgList().size());
        assertEquals("Primer argumento copiado sin procesar", "foo", cl.getArgList().get(0));
        assertEquals("Segundo argumento copiado sin procesar", "-a", cl.getArgList().get(1));
    }

    /**
     * Comportamiento 5: una ráfaga que termina en una opción con argumento
     * consume el siguiente token como valor.
     */
    @Test
    public void testBurstEndingWithArgOptionConsumesNextToken() throws Exception
    {
        Parser parser = new PosixParser();
        CommandLine cl = parser.parse(createOptions(), new String[] { "-acb", "toast", "foo" });

        assertTrue("-a debe estar activada", cl.hasOption("a"));
        assertTrue("-c debe estar activada", cl.hasOption("c"));
        assertTrue("-b debe estar presente", cl.hasOption("b"));
        assertEquals("El token siguiente debe ser el valor de -b", "toast", cl.getOptionValue("b"));
        assertEquals("Solo 'foo' debe quedar como argumento", 1, cl.getArgList().size());
        assertEquals("El argumento posicional debe ser 'foo'", "foo", cl.getArgList().get(0));
    }

    /**
     * Comportamiento 6: una opción con argumento al final, sin valor, lanza MissingArgumentException.
     */
    @Test
    public void testTrailingArgOptionWithoutValueThrowsMissingArgument() throws Exception
    {
        Parser parser = new PosixParser();
        try
        {
            parser.parse(createOptions(), new String[] { "-b" });
            fail("Se esperaba MissingArgumentException para -b sin valor");
        }
        catch (MissingArgumentException e)
        {
            assertEquals("La excepción debe referirse a la opción b", "b", e.getOption().getOpt());
        }
    }

    /**
     * Comportamiento 7: una cadena vacía se trata como argumento posicional.
     */
    @Test
    public void testEmptyStringIsPositionalArgument() throws Exception
    {
        Parser parser = new PosixParser();
        CommandLine cl = parser.parse(createOptions(), new String[] { "-a", "", "foo" });

        assertTrue("-a debe estar activada", cl.hasOption("a"));
        assertEquals("Deben quedar 2 argumentos posicionales", 2, cl.getArgList().size());
        assertEquals("La cadena vacía debe conservarse como argumento", "", cl.getArgList().get(0));
        assertEquals("'foo' debe seguir a la cadena vacía", "foo", cl.getArgList().get(1));
    }

    /**
     * Comportamiento 8: una opción larga desconocida se añade sin validación previa
     * y el Parser la rechaza con UnrecognizedOptionException.
     */
    @Test
    public void testUnknownLongOptionIsRejectedByParser() throws Exception
    {
        Parser parser = new PosixParser();
        try
        {
            parser.parse(createOptions(), new String[] { "--xyz" });
            fail("Se esperaba UnrecognizedOptionException para --xyz");
        }
        catch (UnrecognizedOptionException e)
        {
            assertEquals("La excepción debe contener la opción desconocida", "--xyz", e.getOption());
        }
    }
}
