package org.apache.commons.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.StringWriter;

import org.junit.Test;

public class EntitiesLLMTest {

    @Test
    public void testXmlEscapeBasicEntities() {
        String input = "<>&\"'";
        String expected = "&lt;&gt;&amp;&quot;&apos;";
        assertEquals("XML escape debe usar lt, gt, amp, quot y apos",
                expected, Entities.XML.escape(input));
    }

    @Test
    public void testXmlEscapeLeavesPlainAsciiUnchanged() {
        String input = "Hello World 123";
        assertEquals("Caracteres ASCII sin entidad deben permanecer iguales",
                input, Entities.XML.escape(input));
    }

    @Test
    public void testEscapeEmptyStringReturnsEmpty() {
        assertEquals("Escapar cadena vacia debe devolver cadena vacia",
                "", Entities.HTML40.escape(""));
    }

    @Test
    public void testHtml32EscapeLatin1Copyright() {
        assertEquals("El signo de copyright (169) debe escaparse como &copy; en HTML32",
                "&copy;", Entities.HTML32.escape("\u00A9"));
    }

    @Test
    public void testHtml32EscapeUnknownCharacterUsesNumericReference() {
        assertEquals("El euro no pertenece a HTML32 y debe escaparse numericamente",
                "&#8364;", Entities.HTML32.escape("\u20AC"));
    }

    @Test
    public void testHtml40EscapeGreekLetter() {
        assertEquals("La alfa minuscula (945) debe escaparse como &alpha; en HTML40",
                "&alpha;", Entities.HTML40.escape("\u03B1"));
    }

    @Test
    public void testHtml40EscapeEuroSign() {
        assertEquals("El euro (8364) debe escaparse como &euro; en HTML40",
                "&euro;", Entities.HTML40.escape("\u20AC"));
    }

    @Test
    public void testEscapeUnmappedNonAsciiUsesNumericReference() {
        assertEquals("Un caracter no mapeado (U+4E2D = 20013) debe escaparse numericamente",
                "&#20013;", Entities.HTML32.escape("\u4E2D"));
    }

    @Test
    public void testEscapeWriterMatchesStringEscape() throws Exception {
        String input = "A\u00E9<\u20AC>&";
        StringWriter writer = new StringWriter();
        Entities.HTML40.escape(writer, input);
        assertEquals("La version Writer de escape debe producir el mismo resultado que la de String",
                Entities.HTML40.escape(input), writer.toString());
    }

    @Test
    public void testUnescapeXmlNamedEntities() {
        assertEquals("Las entidades XML basicas deben desescaparse",
                "<>&\"'", Entities.XML.unescape("&lt;&gt;&amp;&quot;&apos;"));
    }

    @Test
    public void testUnescapeDecimalNumericReference() {
        assertEquals("La referencia decimal &#65; debe producir 'A'",
                "A", Entities.XML.unescape("&#65;"));
    }

    @Test
    public void testUnescapeLowercaseHexNumericReference() {
        assertEquals("La referencia hexadecimal &#x41; debe producir 'A'",
                "A", Entities.XML.unescape("&#x41;"));
    }

    @Test
    public void testUnescapeHexNumericReferenceToEuro() {
        assertEquals("La referencia &#x20AC; debe producir el signo euro",
                "\u20AC", Entities.HTML40.unescape("&#x20AC;"));
    }

    @Test
    public void testUnescapeUnknownNameKeepsText() {
        assertEquals("Una entidad desconocida debe conservarse textualmente",
                "&unknown;", Entities.HTML40.unescape("&unknown;"));
    }

    @Test
    public void testUnescapeStringWithoutAmpersandIsUnchanged() {
        String input = "plain text";
        assertEquals("Una cadena sin '&' debe devolverse igual",
                input, Entities.XML.unescape(input));
    }

    @Test
    public void testUnescapeEmptyStringReturnsEmpty() {
        assertEquals("Desescapar cadena vacia debe devolver cadena vacia",
                "", Entities.XML.unescape(""));
    }

    @Test
    public void testUnescapeAmpersandWithoutSemicolonIsPreserved() {
        assertEquals("Un '&' sin ';' debe conservarse",
                "a & b", Entities.XML.unescape("a & b"));
    }

    @Test
    public void testUnescapeAmpersandBetweenTokensIsPreserved() {
        assertEquals("Un patron &...&...; no debe interpretarse como entidad",
                "&a&b;", Entities.XML.unescape("&a&b;"));
    }

    @Test
    public void testUnescapeInvalidDecimalReferenceIsPreserved() {
        assertEquals("Una referencia decimal invalida debe conservarse",
                "&#abc;", Entities.XML.unescape("&#abc;"));
    }

    @Test
    public void testUnescapeEmptyHexReferenceIsPreserved() {
        assertEquals("Una referencia hexadecimal vacia debe conservarse",
                "&#x;", Entities.XML.unescape("&#x;"));
    }

    @Test
    public void testUnescapeWriterMatchesStringUnescape() throws Exception {
        String input = "&lt;tag&gt; &#65; &unknown; &amp;";
        StringWriter writer = new StringWriter();
        Entities.HTML40.unescape(writer, input);
        assertEquals("La version Writer de unescape debe producir el mismo resultado que la de String",
                Entities.HTML40.unescape(input), writer.toString());
    }

    @Test
    public void testHtml40RoundTrip() {
        String original = "\u00C4 & \u03B1 < \u20AC";
        assertEquals("Escapar y desescapar con HTML40 debe recuperar el original",
                original, Entities.HTML40.unescape(Entities.HTML40.escape(original)));
    }

    @Test
    public void testEntityValueForKnownName() {
        assertEquals("El valor de 'euro' en HTML40 debe ser 8364",
                8364, Entities.HTML40.entityValue("euro"));
    }

    @Test
    public void testEntityValueForNameAbsentFromHtml32() {
        assertEquals("'euro' no existe en HTML32 y su valor debe ser -1",
                -1, Entities.HTML32.entityValue("euro"));
    }

    @Test
    public void testEntityNameForKnownValue() {
        assertEquals("El nombre del valor 945 en HTML40 debe ser 'alpha'",
                "alpha", Entities.HTML40.entityName(945));
    }

    @Test
    public void testEntityValueForUnknownNameIsMinusOne() {
        assertEquals("Un nombre desconocido debe devolver -1",
                -1, Entities.HTML40.entityValue("nosuchentity"));
    }

    @Test
    public void testEntityNameForUnknownValueIsNull() {
        assertNull("Un valor sin entidad asociada debe devolver null",
                Entities.XML.entityName(9999));
    }

    @Test
    public void testNewInstanceIsEmpty() {
        Entities empty = new Entities();
        assertEquals("Una instancia nueva no debe conocer 'amp'",
                -1, empty.entityValue("amp"));
        assertEquals("Una instancia nueva no debe desescapar '&amp;'",
                "&amp;", empty.unescape("&amp;"));
    }

    @Test
    public void testAddEntityHighValueIsEscapedAndUnescaped() {
        Entities e = new Entities();
        e.addEntity("myent", 1000);
        assertEquals("El nombre asociado al valor 1000 debe ser 'myent'",
                "myent", e.entityName(1000));
        assertEquals("El valor asociado a 'myent' debe ser 1000",
                1000, e.entityValue("myent"));
        assertEquals("Un caracter con entidad personalizada debe escaparse con su nombre",
                "&myent;", e.escape("\u03E8"));
        assertEquals("La entidad personalizada debe desescaparse a su caracter",
                "\u03E8", e.unescape("&myent;"));
    }

    @Test
    public void testAddEmptyEntitiesArrayHasNoEffect() {
        Entities e = new Entities();
        e.addEntities(new String[0][]);
        assertNull("Anadir un arreglo vacio no debe registrar entidades",
                e.entityName(100));
    }

    @Test
    public void testAddEntitiesArrayRegistersEntriesInOrder() {
        Entities e = new Entities();
        String[][] array = {{"alpha1", "2000"}, {"beta1", "2001"}};
        e.addEntities(array);
        assertEquals("alpha1", e.entityName(2000));
        assertEquals("beta1", e.entityName(2001));
        assertEquals(2000, e.entityValue("alpha1"));
        assertEquals(2001, e.entityValue("beta1"));
    }
}
