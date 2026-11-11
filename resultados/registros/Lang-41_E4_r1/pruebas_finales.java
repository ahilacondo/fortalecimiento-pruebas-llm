package org.apache.commons.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link ClassUtils}, orientadas a los requisitos
 * de documentación que no están cubiertos por la suite existente.
 */
public class ClassUtilsLLMTest {

    /** Registro de eventos de inicialización de la clase sonda {@link InitProbe}. */
    static final List<String> INIT_EVENTS = Collections.synchronizedList(new ArrayList<String>());

    /**
     * Clase sonda: su bloque estático registra cuándo se inicializa.
     * Solo se referencia por nombre en las pruebas, para no inicializarla accidentalmente.
     */
    public static class InitProbe {
        static {
            ClassUtilsLLMTest.INIT_EVENTS.add("InitProbe");
        }
    }

    // ------------------------------------------------------------------
    // Requisito 1: getPublicMethod lanza NoSuchMethodException si no existe el método
    // ------------------------------------------------------------------
    @Test
    public void testGetPublicMethodThrowsNoSuchMethodForUnknownName() {
        try {
            ClassUtils.getPublicMethod(String.class, "noSuchMethodXyz", new Class<?>[0]);
            fail("Se esperaba NoSuchMethodException para un nombre de método inexistente");
        } catch (NoSuchMethodException expected) {
            // comportamiento esperado
        }
    }

    @Test
    public void testGetPublicMethodThrowsNoSuchMethodForUnknownParameters() {
        try {
            // String.length() no tiene parámetros; pedir uno con String.class debe fallar
            ClassUtils.getPublicMethod(String.class, "length", new Class<?>[] {String.class});
            fail("Se esperaba NoSuchMethodException para una firma de parámetros inexistente");
        } catch (NoSuchMethodException expected) {
            // comportamiento esperado
        }
    }

    @Test
    public void testGetPublicMethodReturnsExistingPublicMethod() throws Exception {
        Method m = ClassUtils.getPublicMethod(String.class, "length", new Class<?>[0]);
        assertEquals("length", m.getName());
        assertEquals(int.class, m.getReturnType());
    }

    // ------------------------------------------------------------------
    // Requisito 2: getPublicMethod lanza NullPointerException si la clase es nula
    // ------------------------------------------------------------------
    @Test
    public void testGetPublicMethodThrowsNullPointerExceptionForNullClass() {
        try {
            ClassUtils.getPublicMethod(null, "toString", new Class<?>[0]);
            fail("Se esperaba NullPointerException para una clase nula");
        } catch (NullPointerException expected) {
            // comportamiento esperado
        } catch (NoSuchMethodException e) {
            fail("Se esperaba NullPointerException y se lanzó NoSuchMethodException");
        }
    }

    // ------------------------------------------------------------------
    // Requisitos 3 y 4: isAssignable(Class[], Class[], boolean) con autoboxing
    // ------------------------------------------------------------------
    @Test
    public void testIsAssignableArraysWithAutoboxingTrueAcceptsWrappersForPrimitives() {
        Class<?>[] from = {Integer.class, Boolean.class};
        Class<?>[] to = {int.class, boolean.class};
        assertTrue("Con autoboxing, Integer/Boolean deben asignarse a int/boolean",
                ClassUtils.isAssignable(from, to, true));
    }

    @Test
    public void testIsAssignableArraysWithAutoboxingTrueAllowsUnboxingThenWidening() {
        assertTrue("Integer -> long debe ser válido con autoboxing (unboxing + widening)",
                ClassUtils.isAssignable(new Class<?>[] {Integer.class}, new Class<?>[] {long.class}, true));
    }

    @Test
    public void testIsAssignableArraysWithAutoboxingTrueAllowsBoxingPrimitiveToObject() {
        assertTrue("int -> Object debe ser válido con autoboxing",
                ClassUtils.isAssignable(new Class<?>[] {int.class}, new Class<?>[] {Object.class}, true));
    }

    @Test
    public void testIsAssignableArraysWithAutoboxingFalseRejectsWrapperToPrimitive() {
        Class<?>[] from = {Integer.class};
        Class<?>[] to = {int.class};
        assertFalse("Sin autoboxing, Integer no debe asignarse a int",
                ClassUtils.isAssignable(from, to, false));
    }

    @Test
    public void testIsAssignableArraysWithAutoboxingFalseRejectsPrimitiveToWrapper() {
        assertFalse("Sin autoboxing, int no debe asignarse a Integer",
                ClassUtils.isAssignable(new Class<?>[] {int.class}, new Class<?>[] {Integer.class}, false));
    }

    @Test
    public void testIsAssignableArraysAutoboxingFlagChangesResultForPrimitiveWrapperPairs() {
        Class<?>[] primitives = {int.class, boolean.class, long.class};
        Class<?>[] wrappers = {Integer.class, Boolean.class, Long.class};

        assertFalse("int -> Integer sin autoboxing",
                ClassUtils.isAssignable(new Class<?>[] {int.class}, new Class<?>[] {Integer.class}, false));
        assertTrue("int -> Integer con autoboxing",
                ClassUtils.isAssignable(new Class<?>[] {int.class}, new Class<?>[] {Integer.class}, true));

        assertFalse("Integer -> int sin autoboxing",
                ClassUtils.isAssignable(new Class<?>[] {Integer.class}, new Class<?>[] {int.class}, false));
        assertTrue("Integer -> int con autoboxing",
                ClassUtils.isAssignable(new Class<?>[] {Integer.class}, new Class<?>[] {int.class}, true));

        assertFalse("boolean -> Boolean sin autoboxing",
                ClassUtils.isAssignable(new Class<?>[] {boolean.class}, new Class<?>[] {Boolean.class}, false));
        assertTrue("boolean -> Boolean con autoboxing",
                ClassUtils.isAssignable(new Class<?>[] {boolean.class}, new Class<?>[] {Boolean.class}, true));

        // Comprobación combinada: el mismo par de arrays cambia de resultado según el flag
        assertFalse(ClassUtils.isAssignable(primitives, wrappers, false));
        assertTrue(ClassUtils.isAssignable(primitives, wrappers, true));
    }

    @Test
    public void testIsAssignableArraysWithAutoboxingNullArrays() {
        assertTrue("Dos arrays nulos se consideran asignables",
                ClassUtils.isAssignable((Class<?>[]) null, (Class<?>[]) null, true));
    }

    @Test
    public void testIsAssignableArraysWithAutoboxingDifferentLengthsIsFalse() {
        assertFalse(ClassUtils.isAssignable(
                new Class<?>[] {Integer.class},
                new Class<?>[] {int.class, int.class},
                true));
    }

    // ------------------------------------------------------------------
    // Requisito 5: convertClassNamesToClasses almacena null para elementos nulos
    // ------------------------------------------------------------------
    @Test
    public void testConvertClassNamesToClassesStoresNullForNullElement() {
        List<String> names = new ArrayList<String>();
        names.add("java.lang.String");
        names.add(null);
        names.add("java.lang.Object");

        List<Class<?>> result = ClassUtils.convertClassNamesToClasses(names);

        assertEquals("El resultado debe tener el mismo tamaño que la entrada", 3, result.size());
        assertSame(String.class, result.get(0));
        assertNull("Un elemento nulo de entrada debe producir null en la salida", result.get(1));
        assertSame(Object.class, result.get(2));
    }

    @Test
    public void testConvertClassNamesToClassesWithOnlyNullElement() {
        List<String> names = new ArrayList<String>();
        names.add(null);

        List<Class<?>> result = ClassUtils.convertClassNamesToClasses(names);

        assertEquals(1, result.size());
        assertNull(result.get(0));
    }

    // ------------------------------------------------------------------
    // Requisito 6: getPackageName(String) devuelve "" para nombres sin paquete
    // ------------------------------------------------------------------
    @Test
    public void testGetPackageNameReturnsEmptyForUnpackagedName() {
        assertEquals("", ClassUtils.getPackageName("String"));
    }

    @Test
    public void testGetPackageNameReturnsEmptyForUnpackagedPrimitiveLikeName() {
        assertEquals("", ClassUtils.getPackageName("Integer"));
    }

    @Test
    public void testGetPackageNameReturnsPackageForQualifiedName() {
        assertEquals("java.lang", ClassUtils.getPackageName("java.lang.String"));
    }

    // ------------------------------------------------------------------
    // Requisito 7: valores de las constantes de separadores
    // ------------------------------------------------------------------
    @Test
    public void testPackageSeparatorCharConstant() {
        assertEquals('.', ClassUtils.PACKAGE_SEPARATOR_CHAR);
    }

    @Test
    public void testPackageSeparatorConstant() {
        assertEquals(".", ClassUtils.PACKAGE_SEPARATOR);
    }

    @Test
    public void testInnerClassSeparatorCharConstant() {
        assertEquals('$', ClassUtils.INNER_CLASS_SEPARATOR_CHAR);
    }

    @Test
    public void testInnerClassSeparatorConstant() {
        assertEquals("$", ClassUtils.INNER_CLASS_SEPARATOR);
    }

    @Test
    public void testSeparatorConstantsAreConsistentWithCharVersions() {
        assertEquals(String.valueOf(ClassUtils.PACKAGE_SEPARATOR_CHAR), ClassUtils.PACKAGE_SEPARATOR);
        assertEquals(String.valueOf(ClassUtils.INNER_CLASS_SEPARATOR_CHAR), ClassUtils.INNER_CLASS_SEPARATOR);
    }

    // ------------------------------------------------------------------
    // Requisito 8: getClass(ClassLoader, String, false) no inicializa la clase
    // ------------------------------------------------------------------
    @Test
    public void testGetClassWithoutInitializationLoadsButDoesNotInitialize() throws ClassNotFoundException {
        ClassLoader loader = ClassUtilsLLMTest.class.getClassLoader();
        String probeName = InitProbe.class.getName();

        INIT_EVENTS.clear();
        Class<?> loaded = ClassUtils.getClass(loader, probeName, false);

        assertSame("La clase debe cargarse correctamente", InitProbe.class, loaded);
        assertFalse("Con initialize=false la clase no debe inicializarse",
                INIT_EVENTS.contains("InitProbe"));
    }

    @Test
    public void testGetClassWithInitializationRunsStaticInitializer() throws ClassNotFoundException {
        ClassLoader loader = ClassUtilsLLMTest.class.getClassLoader();
        String probeName = InitProbe.class.getName();

        INIT_EVENTS.clear();
        Class<?> loaded = ClassUtils.getClass(loader, probeName, true);

        assertSame(InitProbe.class, loaded);
        assertTrue("Con initialize=true la clase debe inicializarse",
                INIT_EVENTS.contains("InitProbe"));
    }

    @Test
    public void testGetClassWithoutInitializationForJdkClass() throws ClassNotFoundException {
        Class<?> cls = ClassUtils.getClass(ClassUtilsLLMTest.class.getClassLoader(), "java.lang.String", false);
        assertSame(String.class, cls);
    }
}
