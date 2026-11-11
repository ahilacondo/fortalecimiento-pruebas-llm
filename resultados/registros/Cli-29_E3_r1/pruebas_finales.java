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

import org.junit.Assert;
import org.junit.Test;

/**
 * Pruebas adicionales JUnit 4 para {@link Util}, centradas en
 * {@code stripLeadingAndTrailingQuotes} y en los casos límite de
 * {@code stripLeadingHyphens} no cubiertos por {@code UtilTest}.
 */
public class UtilLLMTest
{
    // ---------------------------------------------------------------
    // stripLeadingAndTrailingQuotes
    // ---------------------------------------------------------------

    @Test
    public void stripQuotesRemovesBothQuotesWhenPresentAtBothEnds()
    {
        String result = Util.stripLeadingAndTrailingQuotes("\"one two\"");
        Assert.assertEquals("Debe eliminar ambas comillas de los extremos", "one two", result);
    }

    @Test
    public void stripQuotesRemovesOnlyLeadingQuoteWhenTrailingIsAbsent()
    {
        String result = Util.stripLeadingAndTrailingQuotes("\"abc");
        Assert.assertEquals("Debe eliminar solo la comilla inicial", "abc", result);
    }

    @Test
    public void stripQuotesRemovesOnlyTrailingQuoteWhenLeadingIsAbsent()
    {
        String result = Util.stripLeadingAndTrailingQuotes("abc\"");
        Assert.assertEquals("Debe eliminar solo la comilla final", "abc", result);
    }

    @Test
    public void stripQuotesReturnsUnchangedStringWithoutEdgeQuotes()
    {
        String result = Util.stripLeadingAndTrailingQuotes("abc");
        Assert.assertEquals("Una cadena sin comillas en los extremos debe devolverse igual", "abc", result);
    }

    @Test
    public void stripQuotesOnSingleQuoteReturnsEmptyString()
    {
        String result = Util.stripLeadingAndTrailingQuotes("\"");
        Assert.assertEquals("Una sola comilla debe producir cadena vacía", "", result);
    }

    @Test
    public void stripQuotesOnEmptyStringReturnsEmptyString()
    {
        String result = Util.stripLeadingAndTrailingQuotes("");
        Assert.assertEquals("Una cadena vacía debe devolver cadena vacía", "", result);
    }

    @Test(expected = NullPointerException.class)
    public void stripQuotesOnNullThrowsNullPointerException()
    {
        Util.stripLeadingAndTrailingQuotes(null);
    }

    // ---------------------------------------------------------------
    // stripLeadingHyphens: casos límite no cubiertos por UtilTest
    // ---------------------------------------------------------------

    @Test
    public void stripLeadingHyphensOnSingleHyphenReturnsEmptyString()
    {
        String result = Util.stripLeadingHyphens("-");
        Assert.assertEquals("Un guion simple debe eliminarse completamente", "", result);
    }

    @Test
    public void stripLeadingHyphensOnDoubleHyphenReturnsEmptyString()
    {
        String result = Util.stripLeadingHyphens("--");
        Assert.assertEquals("Dos guiones deben eliminarse completamente", "", result);
    }

    @Test
    public void stripLeadingHyphensOnEmptyStringReturnsEmptyString()
    {
        String result = Util.stripLeadingHyphens("");
        Assert.assertEquals("Una cadena vacía debe devolver cadena vacía", "", result);
    }
}
