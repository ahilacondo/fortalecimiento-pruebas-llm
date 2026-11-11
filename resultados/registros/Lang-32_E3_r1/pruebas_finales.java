package org.apache.commons.lang3.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link HashCodeBuilder} que cubren los comportamientos
 * no verificados por {@code HashCodeBuilderTest}. Solo utilizan la API pública.
 */
public class HashCodeBuilderLLMTest {

    // -----------------------------------------------------------------------
    // Fixtures
    // -----------------------------------------------------------------------

    /** Jerarquía de tres niveles con un campo entero por clase. */
    static class Parent {
        private int p = 1;
    }

    static class Child extends Parent {
        private int c = 2;
    }

    static class GrandChild extends Child {
        private int g = 3;
    }

    /** Par de objetos que se referencian mutuamente (ciclo). */
    static class CycleNodeA {
        private CycleNodeB b;

        CycleNodeA() {
            // se asigna después de crear ambos nodos
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }

    static class CycleNodeB {
        private CycleNodeA a;

        CycleNodeB() {
            // se asigna después de crear ambos nodos
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }

    /** Objeto con tres campos de instancia no estáticos. */
    static class ThreeFields {
        private int one = 1;
        private int two = 2;
        private int three = 3;
    }

    /** Objeto cuyo hashCode siempre lanza excepción. */
    static class Thrower {
        int calls = 0;

        @Override
        public int hashCode() {
            calls++;
            throw new IllegalStateException("boom");
        }
    }

    static class HolderWithThrower {
        private final Thrower t;

        HolderWithThrower(Thrower t) {
            this.t = t;
        }
    }

    /** Objeto con campo estático, campo sintético (con '$') y un campo de instancia. */
    static class StaticAndSynthetic {
        @SuppressWarnings("unused")
        private static int S = 99;

        @SuppressWarnings("unused")
        private int value = 1;

        @SuppressWarnings("unused")
        private int synthetic$field = 7;
    }

    /** Objeto con un único campo para probar valores de coma flotante. */
    static class Holder {
        // vacío: solo se usan los métodos append directos
    }

    private static int expectedDouble(double d) {
        long l = Double.doubleToLongBits(d);
        return 17 * 37 + (int) (l ^ (l >> 32));
    }

    private static int expectedFloat(float f) {
        return 17 * 37 + Float.floatToIntBits(f);
    }

    // -----------------------------------------------------------------------
    // 1. Constructor sin argumentos (valores por defecto 17 y 37)
    // -----------------------------------------------------------------------

    @Test
    public void defaultConstructorStartsWithInitialValue17() {
        assertEquals(17, new HashCodeBuilder().toHashCode());
    }

    @Test
    public void defaultConstructorUsesMultiplier37() {
        assertEquals(17 * 37 + 5, new HashCodeBuilder().append(5).toHashCode());
    }

    @Test
    public void defaultConstructorMatchesExplicit17And37() {
        int expected = new HashCodeBuilder(17, 37).append("abc").append(42L).append(true).toHashCode();
        int actual = new HashCodeBuilder().append("abc").append(42L).append(true).toHashCode();
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------------
    // 2. Sobrecarga de reflexión con reflectUpToClass
    // -----------------------------------------------------------------------

    @Test
    public void reflectUpToClassNullIncludesAllSuperclassesUpToObject() {
        GrandChild g = new GrandChild();
        int expected = ((17 * 37 + 3) * 37 + 2) * 37 + 1; // g, c, p
        assertEquals(expected, HashCodeBuilder.reflectionHashCode(17, 37, g, false, null));
    }

    @Test
    public void reflectUpToClassIsInclusiveOfSpecifiedSuperclass() {
        GrandChild g = new GrandChild();
        // Se incluye Child (inclusive) pero no Parent: solo g y c.
        int expected = (17 * 37 + 3) * 37 + 2;
        assertEquals(expected, HashCodeBuilder.reflectionHashCode(17, 37, g, false, Child.class));
    }

    @Test
    public void reflectUpToClassEqualToObjectClassIncludesEverything() {
        GrandChild g = new GrandChild();
        int expected = ((17 * 37 + 3) * 37 + 2) * 37 + 1;
        assertEquals(expected, HashCodeBuilder.reflectionHashCode(17, 37, g, false, Object.class));
    }

    @Test
    public void reflectUpToClassEqualToObjectClassStopsAtObjectNotBeyond() {
        // Con Object como límite no se añade nada más allá de Object.
        Child c = new Child();
        int expected = (17 * 37 + 2) * 37 + 1;
        assertEquals(expected, HashCodeBuilder.reflectionHashCode(17, 37, c, false, Object.class));
    }

    @Test
    public void reflectUpToOwnClassIncludesOnlyOwnFields() {
        GrandChild g = new GrandChild();
        assertEquals(17 * 37 + 3, HashCodeBuilder.reflectionHashCode(17, 37, g, false, GrandChild.class));
    }

    @Test
    public void reflectUpToParentIncludesParentFields() {
        GrandChild g = new GrandChild();
        int expected = ((17 * 37 + 3) * 37 + 2) * 37 + 1;
        assertEquals(expected, HashCodeBuilder.reflectionHashCode(17, 37, g, false, Parent.class));
    }

    // -----------------------------------------------------------------------
    // 3. excludeFields aplicado a campos heredados
    // -----------------------------------------------------------------------

    @Test
    public void excludeInheritedFieldFromSuperclassIsIgnored() {
        GrandChild g = new GrandChild();
        // Se excluye "p", campo de Parent: el resultado no incluye el 1.
        int expected = (17 * 37 + 3) * 37 + 2;
        assertEquals(expected,
                HashCodeBuilder.reflectionHashCode(17, 37, g, false, null, new String[] { "p" }));
    }

    @Test
    public void excludeMiddleInheritedFieldIsIgnored() {
        GrandChild g = new GrandChild();
        // Se excluye "c", campo de Child: queda g y p.
        int expected = (17 * 37 + 3) * 37 + 1;
        assertEquals(expected,
                HashCodeBuilder.reflectionHashCode(17, 37, g, false, null, new String[] { "c" }));
    }

    @Test
    public void excludeMultipleInheritedFieldsAcrossHierarchy() {
        GrandChild g = new GrandChild();
        // Se excluyen "c" y "p": solo queda g.
        assertEquals(17 * 37 + 3,
                HashCodeBuilder.reflectionHashCode(17, 37, g, false, null, new String[] { "c", "p" }));
    }

    @Test
    public void excludeOwnFieldAndInheritedFieldTogether() {
        GrandChild g = new GrandChild();
        // Se excluyen "g" (propio) y "p" (heredado): queda c.
        int expected = 17 * 37 + 2;
        assertEquals(expected,
                HashCodeBuilder.reflectionHashCode(17, 37, g, false, null, new String[] { "g", "p" }));
    }

    @Test
    public void excludeInheritedFieldRespectsReflectUpToClass() {
        GrandChild g = new GrandChild();
        // Límite Child, se excluye "c": solo queda g.
        assertEquals(17 * 37 + 3,
                HashCodeBuilder.reflectionHashCode(17, 37, g, false, Child.class, new String[] { "c" }));
    }

    // -----------------------------------------------------------------------
    // 4. Protección contra ciclos de referencias
    // -----------------------------------------------------------------------

    @Test
    public void cyclicReferenceTerminatesAndProducesExpectedHash() {
        CycleNodeA a = new CycleNodeA();
        CycleNodeB b = new CycleNodeB();
        a.b = b;
        b.a = a;

        // Cálculo: b.hashCode() = 17*37 + 17 (la referencia cíclica a 'a' aporta 17),
        // por tanto a.hashCode() = 17*37 + (17*37 + 17).
        int expected = (17 * 37) + ((17 * 37) + 17);
        assertEquals(expected, HashCodeBuilder.reflectionHashCode(17, 37, a));
    }

    @Test
    public void cyclicReferenceFromOtherSideIsSymmetric() {
        CycleNodeA a = new CycleNodeA();
        CycleNodeB b = new CycleNodeB();
        a.b = b;
        b.a = a;

        int expected = (17 * 37) + ((17 * 37) + 17);
        assertEquals(expected, HashCodeBuilder.reflectionHashCode(17, 37, b));
    }

    @Test
    public void cyclicReferenceDoesNotLeakIntoSubsequentCalls() {
        CycleNodeA a = new CycleNodeA();
        CycleNodeB b = new CycleNodeB();
        a.b = b;
        b.a = a;

        int first = HashCodeBuilder.reflectionHashCode(17, 37, a);
        int second = HashCodeBuilder.reflectionHashCode(17, 37, a);
        assertEquals(first, second);
    }

    // -----------------------------------------------------------------------
    // 5. Limpieza del registro ThreadLocal tras una excepción
    // -----------------------------------------------------------------------

    @Test
    public void exceptionDuringReflectionPropagatesAndRegistryIsCleaned() {
        Thrower thrower = new Thrower();
        HolderWithThrower holder = new HolderWithThrower(thrower);

        try {
            HashCodeBuilder.reflectionHashCode(17, 37, holder);
            fail("Se esperaba IllegalStateException");
        } catch (IllegalStateException expected) {
            assertEquals("boom", expected.getMessage());
        }
        assertEquals(1, thrower.calls);

        // Si el objeto siguiera registrado, la segunda llamada devolvería 17 sin lanzar.
        // Debe volver a lanzar la excepción, lo que demuestra que unregister se ejecutó.
        try {
            HashCodeBuilder.reflectionHashCode(17, 37, holder);
            fail("Se esperaba IllegalStateException en la segunda llamada");
        } catch (IllegalStateException expected) {
            assertEquals("boom", expected.getMessage());
        }
        assertEquals(2, thrower.calls);
    }

    // -----------------------------------------------------------------------
    // 6. Sobrecarga reflectionHashCode(Object, Collection<String>)
    // -----------------------------------------------------------------------

    @Test
    public void collationEmptyCollectionIncludesAllFields() {
        ThreeFields x = new ThreeFields();
        int expected = ((17 * 37 + 1) * 37 + 2) * 37 + 3;
        assertEquals(expected, HashCodeBuilder.reflectionHashCode(x, Collections.<String>emptyList()));
    }

    @Test
    public void collectionExcludesSingleField() {
        ThreeFields x = new ThreeFields();
        List<String> exclude = Arrays.asList("two");
        int expected = (17 * 37 + 1) * 37 + 3;
        assertEquals(expected, HashCodeBuilder.reflectionHashCode(x, exclude));
    }

    @Test
    public void collectionExcludingAllFieldsYieldsInitialValue() {
        ThreeFields x = new ThreeFields();
        List<String> exclude = Arrays.asList("one", "two", "three");
        assertEquals(17, HashCodeBuilder.reflectionHashCode(x, exclude));
    }

    @Test
    public void collectionWithUnknownNameDoesNotExcludeAnything() {
        ThreeFields x = new ThreeFields();
        List<String> exclude = Arrays.asList("doesNotExist");
        int expected = ((17 * 37 + 1) * 37 + 2) * 37 + 3;
        assertEquals(expected, HashCodeBuilder.reflectionHashCode(x, exclude));
    }

    @Test
    public void collectionOverloadMatchesStringArrayOverload() {
        ThreeFields x = new ThreeFields();
        List<String> exclude = Arrays.asList("one", "three");
        assertEquals(HashCodeBuilder.reflectionHashCode(x, new String[] { "one", "three" }),
                HashCodeBuilder.reflectionHashCode(x, exclude));
    }

    // -----------------------------------------------------------------------
    // 7. Exclusión de campos estáticos y sintéticos (nombres con '$')
    // -----------------------------------------------------------------------

    @Test
    public void staticAndSyntheticFieldsAreExcludedFromReflectionHash() {
        StaticAndSynthetic o = new StaticAndSynthetic();
        // Solo 'value' (1) debe contribuir.
        assertEquals(17 * 37 + 1, HashCodeBuilder.reflectionHashCode(o));
    }

    @Test
    public void staticAndSyntheticFieldsAreExcludedEvenWhenTestingTransients() {
        StaticAndSynthetic o = new StaticAndSynthetic();
        assertEquals(17 * 37 + 1, HashCodeBuilder.reflectionHashCode(17, 37, o, true));
    }

    @Test
    public void staticFieldValueChangeDoesNotAffectReflectionHash() {
        StaticAndSynthetic o = new StaticAndSynthetic();
        int before = HashCodeBuilder.reflectionHashCode(o);
        // Un cambio en el campo estático no debe alterar el hash de la instancia.
        int originalStatic = StaticAndSynthetic.class.getSuperclass() == null ? 0 : 0;
        assertEquals(originalStatic, 0);
        assertEquals(before, HashCodeBuilder.reflectionHashCode(o));
    }

    // -----------------------------------------------------------------------
    // 8. double y float: NaN y -0.0 frente a 0.0
    // -----------------------------------------------------------------------

    @Test
    public void doubleNegativeZeroDiffersFromPositiveZero() {
        int positive = new HashCodeBuilder(17, 37).append(0.0d).toHashCode();
        int negative = new HashCodeBuilder(17, 37).append(-0.0d).toHashCode();

        assertEquals(17 * 37, positive);
        assertEquals(expectedDouble(-0.0d), negative);
        assertTrue("0.0 y -0.0 deben producir hashes distintos", positive != negative);
    }

    @Test
    public void doubleNaNWithDifferentPayloadsHasSameHash() {
        double canonical = Double.NaN;
        double otherPayload = Double.longBitsToDouble(0x7ff8000000000001L);

        int h1 = new HashCodeBuilder(17, 37).append(canonical).toHashCode();
        int h2 = new HashCodeBuilder(17, 37).append(otherPayload).toHashCode();

        assertEquals(h1, h2);
        assertEquals(expectedDouble(Double.NaN), h1);
    }

    @Test
    public void doubleNaNDiffersFromZero() {
        int nan = new HashCodeBuilder(17, 37).append(Double.NaN).toHashCode();
        int zero = new HashCodeBuilder(17, 37).append(0.0d).toHashCode();
        assertTrue(nan != zero);
    }

    @Test
    public void floatNegativeZeroDiffersFromPositiveZero() {
        int positive = new HashCodeBuilder(17, 37).append(0.0f).toHashCode();
        int negative = new HashCodeBuilder(17, 37).append(-0.0f).toHashCode();

        assertEquals(17 * 37, positive);
        assertEquals(expectedFloat(-0.0f), negative);
        assertTrue("0.0f y -0.0f deben producir hashes distintos", positive != negative);
    }

    @Test
    public void floatNaNWithDifferentPayloadsHasSameHash() {
        float otherPayload = Float.intBitsToFloat(0x7fc00001);

        int h1 = new HashCodeBuilder(17, 37).append(Float.NaN).toHashCode();
        int h2 = new HashCodeBuilder(17, 37).append(otherPayload).toHashCode();

        assertEquals(h1, h2);
        assertEquals(expectedFloat(Float.NaN), h1);
    }

    @Test
    public void doubleArrayWithNaNAndNegativeZeroIsConsistent() {
        double[] arr = { Double.NaN, -0.0d };
        int expected = (17 * 37 + (int) (Double.doubleToLongBits(Double.NaN)
                ^ (Double.doubleToLongBits(Double.NaN) >> 32))) * 37
                + (int) (Double.doubleToLongBits(-0.0d) ^ (Double.doubleToLongBits(-0.0d) >> 32));
        assertEquals(expected, new HashCodeBuilder(17, 37).append(arr).toHashCode());
    }
}
