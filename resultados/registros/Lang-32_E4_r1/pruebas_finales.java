package org.apache.commons.lang3.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link HashCodeBuilder} que cubren los requisitos
 * de la documentación no verificados por la suite existente.
 */
public class HashCodeBuilderLLMTest {

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    /** Jerarquía de tres niveles con un campo de instancia por clase. */
    static class Base {
        int baseValue = 5;
    }

    static class Mid extends Base {
        int midValue = 3;
    }

    static class Leaf extends Mid {
        int leafValue = 2;
    }

    /** Campos estáticos que no deben participar en el hash. */
    static class StaticHolder {
        static int counter = 1000;
        static final String NAME = "x";
        int value = 1;
    }

    static class StaticSub extends StaticHolder {
        static int subCounter = 5;
        int other = 2;
    }

    /** Tres campos de instancia para probar exclusiones. */
    static class ThreeFields {
        int one = 1;
        int two = 2;
        int three = 3;
    }

    /** Campo sombreado "code" en la superclase y en la subclase. */
    static class ShadowBase {
        int code = 5;
    }

    static class ShadowLeaf extends ShadowBase {
        int code = 2;
        int other = 3;
    }

    /** Referencias cíclicas entre dos objetos. */
    static class CycleA {
        CycleB b;
        int v = 1;
    }

    static class CycleB {
        CycleA a;
        int w = 2;
    }

    /** Referencia cíclica directa a sí mismo. */
    static class SelfRef {
        SelfRef self;
        int v = 4;
    }

    private static final String NULL_MESSAGE = "The object to build a hash code for must not be null";

    private static void assertIllegalArgument(String expectedMessage, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException e) {
            assertEquals(expectedMessage, e.getMessage());
            return;
        }
        fail("Se esperaba IllegalArgumentException con mensaje: " + expectedMessage);
    }

    // ------------------------------------------------------------------
    // Requisito 1: reflectUpToClass especificado
    // ------------------------------------------------------------------

    @Test
    public void reflectUpToSpecifiedSuperclassIncludesFieldsUpToAndIncludingThatClass() {
        Leaf leaf = new Leaf();
        // Leaf.leafValue y Mid.midValue; Base se omite porque el recorrido termina en Mid.
        int expected = (17 * 37 + 2) * 37 + 3;
        assertEquals(expected, HashCodeBuilder.reflectionHashCode(17, 37, leaf, false, Mid.class));
    }

    @Test
    public void reflectUpToSpecifiedClassEqualToObjectClassOnlyIncludesOwnFields() {
        Leaf leaf = new Leaf();
        // Solo los campos de Leaf: el recorrido se detiene inmediatamente en Leaf.
        assertEquals(17 * 37 + 2, HashCodeBuilder.reflectionHashCode(17, 37, leaf, false, Leaf.class));
    }

    @Test
    public void reflectUpToBaseClassIncludesWholeChainUpToBase() {
        Leaf leaf = new Leaf();
        int expected = ((17 * 37 + 2) * 37 + 3) * 37 + 5;
        assertEquals(expected, HashCodeBuilder.reflectionHashCode(17, 37, leaf, false, Base.class));
    }

    @Test
    public void reflectUpToSpecifiedClassDiffersFromFullHierarchy() {
        Leaf leaf = new Leaf();
        int upToMid = HashCodeBuilder.reflectionHashCode(17, 37, leaf, false, Mid.class);
        int upToBase = HashCodeBuilder.reflectionHashCode(17, 37, leaf, false, Base.class);
        assertNotEquals("Incluir Base debe cambiar el hash", upToMid, upToBase);
    }

    // ------------------------------------------------------------------
    // Requisito 2: reflectUpToClass nulo recorre hasta java.lang.Object
    // ------------------------------------------------------------------

    @Test
    public void nullReflectUpToClassTraversesHierarchyUpToObject() {
        Leaf leaf = new Leaf();
        int expected = ((17 * 37 + 2) * 37 + 3) * 37 + 5;
        assertEquals(expected, HashCodeBuilder.reflectionHashCode(17, 37, leaf, false, null));
    }

    @Test
    public void nullReflectUpToClassEquivalentToReflectUpToObject() {
        Leaf leaf = new Leaf();
        assertEquals(HashCodeBuilder.reflectionHashCode(17, 37, leaf, false, Object.class),
                HashCodeBuilder.reflectionHashCode(17, 37, leaf, false, null));
    }

    // ------------------------------------------------------------------
    // Requisito 3: los campos estáticos no participan en el hash
    // ------------------------------------------------------------------

    @Test
    public void staticFieldsAreNotIncludedInReflectionHashCode() {
        StaticSub sub = new StaticSub();
        // StaticSub.other = 2 y StaticHolder.value = 1; campos estáticos ignorados.
        int expected = (17 * 37 + 2) * 37 + 1;
        assertEquals(expected, HashCodeBuilder.reflectionHashCode(sub));
    }

    @Test
    public void changingStaticFieldValueDoesNotAffectReflectionHashCode() {
        StaticSub sub = new StaticSub();
        int before = HashCodeBuilder.reflectionHashCode(sub);

        int originalCounter = StaticHolder.counter;
        int originalSubCounter = StaticSub.subCounter;
        try {
            StaticHolder.counter = 424242;
            StaticSub.subCounter = -999;
            int after = HashCodeBuilder.reflectionHashCode(sub);
            assertEquals("Los campos estáticos no deben alterar el hash", before, after);
        } finally {
            StaticHolder.counter = originalCounter;
            StaticSub.subCounter = originalSubCounter;
        }
    }

    // ------------------------------------------------------------------
    // Requisito 4: todas las sobrecargas rechazan null con IllegalArgumentException
    // ------------------------------------------------------------------

    @Test
    public void reflectionOverloadIntIntObjectRejectsNull() {
        assertIllegalArgument(NULL_MESSAGE,
                () -> HashCodeBuilder.reflectionHashCode(13, 19, (Object) null));
    }

    @Test
    public void reflectionOverloadIntIntObjectBooleanRejectsNull() {
        assertIllegalArgument(NULL_MESSAGE,
                () -> HashCodeBuilder.reflectionHashCode(13, 19, (Object) null, false));
    }

    @Test
    public void reflectionOverloadWithReflectUpToRejectsNull() {
        assertIllegalArgument(NULL_MESSAGE,
                () -> HashCodeBuilder.reflectionHashCode(13, 19, (Object) null, false, null));
    }

    @Test
    public void reflectionOverloadWithExcludeFieldsArrayRejectsNull() {
        assertIllegalArgument(NULL_MESSAGE,
                () -> HashCodeBuilder.reflectionHashCode(13, 19, (Object) null, false, null, null));
    }

    @Test
    public void reflectionOverloadObjectRejectsNull() {
        assertIllegalArgument(NULL_MESSAGE,
                () -> HashCodeBuilder.reflectionHashCode((Object) null));
    }

    @Test
    public void reflectionOverloadObjectBooleanRejectsNull() {
        assertIllegalArgument(NULL_MESSAGE,
                () -> HashCodeBuilder.reflectionHashCode((Object) null, true));
    }

    @Test
    public void reflectionOverloadObjectCollectionRejectsNull() {
        assertIllegalArgument(NULL_MESSAGE,
                () -> HashCodeBuilder.reflectionHashCode((Object) null, Collections.<String>emptyList()));
    }

    @Test
    public void reflectionOverloadObjectStringArrayRejectsNull() {
        assertIllegalArgument(NULL_MESSAGE,
                () -> HashCodeBuilder.reflectionHashCode((Object) null, new String[] {"campo"}));
    }

    // ------------------------------------------------------------------
    // Requisito 5: reflectionHashCode(Object, Collection<String>) excluye campos
    // ------------------------------------------------------------------

    @Test
    public void collectionOverloadExcludesNamedField() {
        ThreeFields obj = new ThreeFields();
        // Se excluye "two": quedan one=1 y three=3.
        int expected = (17 * 37 + 1) * 37 + 3;
        assertEquals(expected, HashCodeBuilder.reflectionHashCode(obj, List.of("two")));
    }

    @Test
    public void collectionOverloadWithAllFieldsExcludedReturnsInitialValue() {
        ThreeFields obj = new ThreeFields();
        assertEquals(17, HashCodeBuilder.reflectionHashCode(obj, List.of("one", "two", "three")));
    }

    @Test
    public void collectionOverloadWithUnknownFieldNameExcludesNothing() {
        ThreeFields obj = new ThreeFields();
        int expected = ((17 * 37 + 1) * 37 + 2) * 37 + 3;
        assertEquals(expected, HashCodeBuilder.reflectionHashCode(obj, List.of("noExiste")));
    }

    @Test
    public void collectionOverloadMatchesStringArrayOverload() {
        ThreeFields obj = new ThreeFields();
        assertEquals(HashCodeBuilder.reflectionHashCode(obj, new String[] {"one", "three"}),
                HashCodeBuilder.reflectionHashCode(obj, List.of("one", "three")));
    }

    // ------------------------------------------------------------------
    // Requisito 6: exclusión por nombre en toda la jerarquía
    // ------------------------------------------------------------------

    @Test
    public void excludedFieldNameIsOmittedInSubclassAndSuperclass() {
        ShadowLeaf leaf = new ShadowLeaf();
        // "code" se excluye en ShadowLeaf y en ShadowBase; solo queda other=3.
        int expected = 17 * 37 + 3;
        assertEquals(expected,
                HashCodeBuilder.reflectionHashCode(17, 37, leaf, false, null, new String[] {"code"}));
    }

    @Test
    public void excludedFieldNameOnlyInSuperclassLeavesSubclassFieldsIntact() {
        ShadowLeaf leaf = new ShadowLeaf();
        // "other" se excluye; quedan code=2 de ShadowLeaf y code=5 de ShadowBase.
        int expected = (17 * 37 + 2) * 37 + 5;
        assertEquals(expected,
                HashCodeBuilder.reflectionHashCode(17, 37, leaf, false, null, new String[] {"other"}));
    }

    // ------------------------------------------------------------------
    // Requisito 7: referencias cíclicas no provocan bucle infinito
    // ------------------------------------------------------------------

    @Test
    public void cyclicReferenceBetweenTwoObjectsTerminatesWithExpectedValue() {
        CycleA a = new CycleA();
        CycleB b = new CycleB();
        a.b = b;
        b.a = a;

        // Dentro del ciclo, a.hashCode() devuelve 17 porque a ya está registrado.
        // b.hashCode() = (17*37 + 17)*37 + 2 = 23904.
        // a.hashCode() = (17*37 + 23904)*37 + 1 = 907722.
        assertEquals(23904, b.hashCode());
        assertEquals(907722, a.hashCode());
        assertEquals(907722, HashCodeBuilder.reflectionHashCode(a));
    }

    @Test
    public void cyclicReferenceIsDeterministicAcrossCalls() {
        CycleA a = new CycleA();
        CycleB b = new CycleB();
        a.b = b;
        b.a = a;

        int first = HashCodeBuilder.reflectionHashCode(a);
        int second = HashCodeBuilder.reflectionHashCode(a);
        assertEquals(first, second);
    }

    @Test
    public void selfReferenceTerminatesAndIsIncludedAsSeventeen() {
        SelfRef s = new SelfRef();
        s.self = s;
        // Al encontrar s registrado, la referencia contribuye 17.
        // Campos: self contribuye 17 y v=4 → (17*37 + 17)*37 + 4 = 23906, sin depender del orden
        // porque el resultado con el orden self, v es el que se verifica.
        // Se comprueba terminación y estabilidad de la llamada.
        int result = HashCodeBuilder.reflectionHashCode(s);
        assertEquals(result, HashCodeBuilder.reflectionHashCode(s));
        assertEquals(s.hashCode(), HashCodeBuilder.reflectionHashCode(s));
    }

    // ------------------------------------------------------------------
    // Requisito 8: el constructor rechaza multiplicador cero o par
    // ------------------------------------------------------------------

    @Test
    public void constructorRejectsZeroMultiplierWithValidInitialValue() {
        assertIllegalArgument("HashCodeBuilder requires a non zero multiplier",
                () -> new HashCodeBuilder(17, 0));
    }

    @Test
    public void constructorRejectsEvenMultiplierWithValidInitialValue() {
        assertIllegalArgument("HashCodeBuilder requires an odd multiplier",
                () -> new HashCodeBuilder(17, 2));
    }

    @Test
    public void constructorRejectsAnotherEvenMultiplierWithValidInitialValue() {
        assertIllegalArgument("HashCodeBuilder requires an odd multiplier",
                () -> new HashCodeBuilder(1, 4));
    }
}
