package org.apache.commons.lang3.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link HashCodeBuilder} que cubren constructores por defecto,
 * validaciones de argumentos, sobrecargas de reflexión no ejercitadas, manejo de nulos,
 * detección de ciclos y casos límite de los métodos {@code append}.
 */
public class HashCodeBuilderLLMTest {

    /** Fixture con campo estático, que debe ser ignorado por la reflexión. */
    static class WithStatic {
        static int counter = 99;
        int value;

        WithStatic(int value) {
            this.value = value;
        }
    }

    /** Fixture con tres campos de instancia para probar exclusiones. */
    static class ThreeFields {
        int one = 1;
        int two = 2;
        int three = 3;
    }

    /** Fixture base para la prueba de reflectUpToClass. */
    static class BaseFixture {
        int a;

        BaseFixture(int a) {
            this.a = a;
        }
    }

    /** Fixture hija para la prueba de reflectUpToClass. */
    static class SubFixture extends BaseFixture {
        int b;

        SubFixture(int a, int b) {
            super(a);
            this.b = b;
        }
    }

    /** Fixtures con referencia cíclica; hashCode delega en la reflexión. */
    static class CycleA {
        CycleB b;

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }

    static class CycleB {
        CycleA a;

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }

    /** Fixture simple para pruebas de reflexión sobre un campo entero. */
    static class SingleInt {
        int value;

        SingleInt(int value) {
            this.value = value;
        }
    }

    private static void assertIllegalArgument(String expectedMessage, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException ex) {
            assertEquals(expectedMessage, ex.getMessage());
            return;
        }
        fail("Se esperaba IllegalArgumentException con mensaje: " + expectedMessage);
    }

    // ----------------------------------------------------------------------
    // Constructores
    // ----------------------------------------------------------------------

    @Test
    public void defaultConstructorUsesSeventeenAndThirtySeven() {
        // iTotal = 17, iConstant = 37; append(1) => 17*37 + 1
        assertEquals(17 * 37 + 1, new HashCodeBuilder().append(1).toHashCode());
        assertEquals(17, new HashCodeBuilder().toHashCode());
    }

    @Test
    public void constructorAcceptsNegativeOddNumbers() {
        HashCodeBuilder hcb = new HashCodeBuilder(-3, 5);
        assertEquals(-3, hcb.toHashCode());
        assertEquals(-3 * 5 + 7, hcb.append(7).toHashCode());
    }

    @Test
    public void constructorRejectsZeroInitialValue() {
        assertIllegalArgument("HashCodeBuilder requires a non zero initial value",
                () -> new HashCodeBuilder(0, 17));
    }

    @Test
    public void constructorRejectsEvenInitialValue() {
        assertIllegalArgument("HashCodeBuilder requires an odd initial value",
                () -> new HashCodeBuilder(4, 17));
    }

    @Test
    public void constructorRejectsZeroMultiplier() {
        assertIllegalArgument("HashCodeBuilder requires a non zero multiplier",
                () -> new HashCodeBuilder(17, 0));
    }

    @Test
    public void constructorRejectsEvenMultiplier() {
        assertIllegalArgument("HashCodeBuilder requires an odd multiplier",
                () -> new HashCodeBuilder(17, 4));
    }

    @Test
    public void customConstantsAreAppliedInOrder() {
        // iTotal=1, iConstant=1: append(1) -> 2, append(1) -> 3
        assertEquals(3, new HashCodeBuilder(1, 1).append(1).append(1).toHashCode());
    }

    // ----------------------------------------------------------------------
    // Métodos append: primitivos y casos límite
    // ----------------------------------------------------------------------

    @Test
    public void appendReturnsSameBuilderInstance() {
        HashCodeBuilder hcb = new HashCodeBuilder(17, 37);
        assertSame(hcb, hcb.append(true));
        assertSame(hcb, hcb.append((byte) 1));
        assertSame(hcb, hcb.append('c'));
        assertSame(hcb, hcb.append(1.5d));
        assertSame(hcb, hcb.append(1.5f));
        assertSame(hcb, hcb.append(1));
        assertSame(hcb, hcb.append(1L));
        assertSame(hcb, hcb.append((short) 1));
        assertSame(hcb, hcb.append("text"));
        assertSame(hcb, hcb.appendSuper(3));
    }

    @Test
    public void appendLongNegativeOneHashesToZero() {
        // (-1 ^ (-1 >> 32)) == 0
        assertEquals(17 * 37, new HashCodeBuilder(17, 37).append(-1L).toHashCode());
    }

    @Test
    public void appendLongMinValueUsesFormula() {
        int expectedPart = (int) (Long.MIN_VALUE ^ (Long.MIN_VALUE >> 32));
        assertEquals(17 * 37 + expectedPart, new HashCodeBuilder(17, 37).append(Long.MIN_VALUE).toHashCode());
    }

    @Test
    public void appendNegativeZeroDiffersFromPositiveZero() {
        int hPositive = new HashCodeBuilder(17, 37).append(0.0d).toHashCode();
        int hNegative = new HashCodeBuilder(17, 37).append(-0.0d).toHashCode();
        assertNotEquals(hPositive, hNegative);
        assertEquals(17 * 37, hPositive);
    }

    @Test
    public void appendNaNIsConsistentForDoubleAndFloat() {
        assertEquals(new HashCodeBuilder(17, 37).append(Double.NaN).toHashCode(),
                new HashCodeBuilder(17, 37).append(0.0d / 0.0d).toHashCode());
        assertEquals(new HashCodeBuilder(17, 37).append(Float.NaN).toHashCode(),
                new HashCodeBuilder(17, 37).append(0.0f / 0.0f).toHashCode());
    }

    @Test
    public void appendEmptyArraysDoesNotChangeTotal() {
        assertEquals(17, new HashCodeBuilder().append(new int[0]).toHashCode());
        assertEquals(17, new HashCodeBuilder().append(new long[0]).toHashCode());
        assertEquals(17, new HashCodeBuilder().append(new Object[0]).toHashCode());
        assertEquals(17, new HashCodeBuilder().append((Object) new byte[0]).toHashCode());
    }

    @Test
    public void appendObjectArrayWithNullElementUsesNullContribution() {
        // Un elemento null aporta iTotal * iConstant
        assertEquals(17 * 37, new HashCodeBuilder(17, 37).append(new Object[] {null}).toHashCode());
        assertEquals((17 * 37) * 37,
                new HashCodeBuilder(17, 37).append(new Object[] {null, null}).toHashCode());
    }

    @Test
    public void appendObjectUsesHashCodeOfNonArrayObject() {
        String s = "abc";
        assertEquals(17 * 37 + s.hashCode(), new HashCodeBuilder(17, 37).append((Object) s).toHashCode());
    }

    @Test
    public void appendMultiDimensionalPrimitiveArrayAsObject() {
        int[][] matrix = {{1, 2}, {3}};
        int expected = ((17 * 37 + 1) * 37 + 2) * 37 + 3;
        assertEquals(expected, new HashCodeBuilder(17, 37).append((Object) matrix).toHashCode());
    }

    @Test
    public void appendSuperWithZeroAddsZeroContribution() {
        assertEquals(17 * 37, new HashCodeBuilder(17, 37).appendSuper(0).toHashCode());
    }

    @Test
    public void appendSuperChainsWithOtherValues() {
        int expected = (17 * 37 + 5) * 37 + 9;
        assertEquals(expected, new HashCodeBuilder(17, 37).appendSuper(5).append(9).toHashCode());
    }

    @Test
    public void toHashCodeIsIdempotent() {
        HashCodeBuilder hcb = new HashCodeBuilder(17, 37).append(42);
        int first = hcb.toHashCode();
        int second = hcb.toHashCode();
        assertEquals(first, second);
        assertEquals(17 * 37 + 42, first);
    }

    @Test
    public void equalSequencesProduceEqualHashCodes() {
        int h1 = new HashCodeBuilder(17, 37).append("x").append(7L).append(true).toHashCode();
        int h2 = new HashCodeBuilder(17, 37).append("x").append(7L).append(true).toHashCode();
        assertEquals(h1, h2);
    }

    @Test
    public void appendOrderAffectsHashCode() {
        int h1 = new HashCodeBuilder(17, 37).append(1).append(2).toHashCode();
        int h2 = new HashCodeBuilder(17, 37).append(2).append(1).toHashCode();
        assertNotEquals(h1, h2);
    }

    // ----------------------------------------------------------------------
    // reflectionHashCode: sobrecargas no ejercitadas y manejo de nulos
    // ----------------------------------------------------------------------

    @Test
    public void reflectionHashCodeThreeArgOverloadUsesCustomConstants() {
        // iTotal=13, iConstant=19; campo value=5 => 13*19 + 5
        assertEquals(13 * 19 + 5, HashCodeBuilder.reflectionHashCode(13, 19, new SingleInt(5)));
    }

    @Test
    public void reflectionHashCodeThreeArgOverloadRejectsNull() {
        assertIllegalArgument("The object to build a hash code for must not be null",
                () -> HashCodeBuilder.reflectionHashCode(13, 19, null));
    }

    @Test
    public void reflectionHashCodeTwoArgObjectBooleanRejectsNull() {
        assertIllegalArgument("The object to build a hash code for must not be null",
                () -> HashCodeBuilder.reflectionHashCode(null, false));
    }

    @Test
    public void reflectionHashCodeSingleArgRejectsNull() {
        assertIllegalArgument("The object to build a hash code for must not be null",
                () -> HashCodeBuilder.reflectionHashCode((Object) null));
    }

    @Test
    public void reflectionHashCodeStringArrayExcludeRejectsNull() {
        assertIllegalArgument("The object to build a hash code for must not be null",
                () -> HashCodeBuilder.reflectionHashCode((Object) null, new String[] {"value"}));
    }

    @Test
    public void reflectionHashCodeIgnoresStaticFields() {
        // counter es estático (99) y no debe contribuir
        assertEquals(17 * 37 + 5, HashCodeBuilder.reflectionHashCode(new WithStatic(5)));
    }

    @Test
    public void reflectionHashCodeWithCollectionExcludesNamedFields() {
        ThreeFields x = new ThreeFields();
        List<String> exclude = Arrays.asList("one", "three");
        assertEquals((17 * 37 + 2), HashCodeBuilder.reflectionHashCode(x, exclude));
    }

    @Test
    public void reflectionHashCodeWithEmptyCollectionIncludesAllFields() {
        ThreeFields x = new ThreeFields();
        int full = ((17 * 37 + 1) * 37 + 2) * 37 + 3;
        assertEquals(full, HashCodeBuilder.reflectionHashCode(x, new ArrayList<String>()));
        assertEquals(full, HashCodeBuilder.reflectionHashCode(x, Collections.<String>emptyList()));
    }

    @Test
    public void reflectionHashCodeWithCollectionExcludingAllFieldsReturnsInitial() {
        ThreeFields x = new ThreeFields();
        assertEquals(17, HashCodeBuilder.reflectionHashCode(x, Arrays.asList("one", "two", "three")));
    }

    @Test
    public void reflectionHashCodeFiveArgWithReflectUpToSubclassIgnoresSuperclassFields() {
        SubFixture s = new SubFixture(123456, 7890);
        // Solo se reflejan los campos de SubFixture (b); no se sube a BaseFixture
        assertEquals(17 * 37 + 7890,
                HashCodeBuilder.reflectionHashCode(17, 37, s, false, SubFixture.class));
    }

    @Test
    public void reflectionHashCodeFiveArgWithReflectUpToSuperclassIncludesHierarchy() {
        SubFixture s = new SubFixture(123456, 7890);
        int full = HashCodeBuilder.reflectionHashCode(17, 37, s, false, null);
        assertEquals(full,
                HashCodeBuilder.reflectionHashCode(17, 37, s, false, BaseFixture.class));
        assertEquals((17 * 37 + 7890) * 37 + 123456, full);
    }

    @Test
    public void reflectionHashCodeFiveArgWithNullReflectUpToIncludesHierarchy() {
        SubFixture s = new SubFixture(1, 2);
        assertEquals(((17 * 37 + 2) * 37) + 1,
                HashCodeBuilder.reflectionHashCode(17, 37, s, false, null));
    }

    @Test
    public void reflectionHashCodeFiveArgRejectsZeroConstant() {
        assertIllegalArgument("HashCodeBuilder requires a non zero initial value",
                () -> HashCodeBuilder.reflectionHashCode(0, 37, new SingleInt(1), false, null));
    }

    // ----------------------------------------------------------------------
    // reflectionHashCode: detección de ciclos
    // ----------------------------------------------------------------------

    @Test
    public void reflectionHashCodeTerminatesOnCyclicReferences() {
        CycleA a = new CycleA();
        CycleB b = new CycleB();
        a.b = b;
        b.a = a;

        // Trazado: A.hash = 17*37 + B.hash; B.hash = 17*37 + A.hash(ciclo => 17)
        int expected = 17 * 37 + (17 * 37 + 17);
        assertEquals(expected, HashCodeBuilder.reflectionHashCode(a));
    }

    @Test
    public void reflectionHashCodeIsStableAfterCyclicCall() {
        CycleA a = new CycleA();
        CycleB b = new CycleB();
        a.b = b;
        b.a = a;

        int first = HashCodeBuilder.reflectionHashCode(a);
        int second = HashCodeBuilder.reflectionHashCode(a);
        assertEquals(first, second);
        assertFalse(first == 17);
    }
}
