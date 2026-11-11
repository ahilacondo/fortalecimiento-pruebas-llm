package org.apache.commons.lang3;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link ClassUtils}, centradas en comportamientos
 * no cubiertos por la suite existente.
 */
public class ClassUtilsLLMTest {

    /** Registro de eventos de inicialización de las clases "sonda". */
    static final List<String> INIT_EVENTS = new ArrayList<String>();

    /** Sonda para verificar que initialize=false no ejecuta el bloque static. */
    public static class LazyFalseProbe {
        static {
            ClassUtilsLLMTest.INIT_EVENTS.add("LazyFalseProbe");
        }
    }

    /** Sonda para verificar que initialize=true sí ejecuta el bloque static. */
    public static class LazyTrueProbe {
        static {
            ClassUtilsLLMTest.INIT_EVENTS.add("LazyTrueProbe");
        }
    }

    /** Clase no pública con un método público que no aparece en ninguna jerarquía pública. */
    private static class HiddenHolder {
        public void hiddenMethod() {
        }
    }

    // ------------------------------------------------------------------
    // toClass
    // ------------------------------------------------------------------

    @Test
    public void toClass_nullInput_returnsNull() {
        assertNull(ClassUtils.toClass(null));
    }

    @Test
    public void toClass_emptyArray_returnsEmptyClassArray() {
        Class<?>[] result = ClassUtils.toClass(new Object[0]);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void toClass_mixedElements_returnsRuntimeClassOfEachElement() {
        Object[] input = new Object[] {"texto", Integer.valueOf(1), new int[0], new ClassUtils()};
        Class<?>[] result = ClassUtils.toClass(input);
        assertArrayEquals(
                new Class<?>[] {String.class, Integer.class, int[].class, ClassUtils.class},
                result);
    }

    @Test
    public void toClass_nullElement_throwsNullPointerException() {
        try {
            ClassUtils.toClass(new Object[] {"texto", null});
            fail("Se esperaba NullPointerException para un elemento null");
        } catch (NullPointerException expected) {
            // comportamiento esperado
        }
    }

    // ------------------------------------------------------------------
    // getPublicMethod
    // ------------------------------------------------------------------

    @Test
    public void getPublicMethod_nonExistentMethodName_throwsNoSuchMethodException() throws Exception {
        try {
            ClassUtils.getPublicMethod(Object.class, "noExisteEsteMetodo", new Class<?>[0]);
            fail("Se esperaba NoSuchMethodException para un método inexistente");
        } catch (NoSuchMethodException expected) {
            // comportamiento esperado
        }
    }

    @Test
    public void getPublicMethod_wrongParameterTypes_throwsNoSuchMethodException() throws Exception {
        try {
            ClassUtils.getPublicMethod(Object.class, "toString", new Class<?>[] {String.class});
            fail("Se esperaba NoSuchMethodException para firma incompatible");
        } catch (NoSuchMethodException expected) {
            // comportamiento esperado
        }
    }

    @Test
    public void getPublicMethod_noPublicMethodInHierarchy_throwsNoSuchMethodException() throws Exception {
        // El método es público pero pertenece a una clase no pública sin equivalente
        // en interfaces ni superclases públicas.
        try {
            ClassUtils.getPublicMethod(HiddenHolder.class, "hiddenMethod", new Class<?>[0]);
            fail("Se esperaba NoSuchMethodException cuando no hay método público en la jerarquía");
        } catch (NoSuchMethodException expected) {
            // comportamiento esperado
        }
    }

    @Test
    public void getPublicMethod_nullClass_throwsNullPointerException() throws Exception {
        try {
            ClassUtils.getPublicMethod(null, "toString", new Class<?>[0]);
            fail("Se esperaba NullPointerException para clase null");
        } catch (NullPointerException expected) {
            // comportamiento esperado
        }
    }

    // ------------------------------------------------------------------
    // getAllSuperclasses
    // ------------------------------------------------------------------

    @Test
    public void getAllSuperclasses_object_returnsEmptyList() {
        List<Class<?>> result = ClassUtils.getAllSuperclasses(Object.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getAllSuperclasses_interface_returnsEmptyList() {
        List<Class<?>> result = ClassUtils.getAllSuperclasses(Runnable.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getAllSuperclasses_primitive_returnsEmptyList() {
        List<Class<?>> result = ClassUtils.getAllSuperclasses(int.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ------------------------------------------------------------------
    // getClass(ClassLoader, String, boolean)
    // ------------------------------------------------------------------

    @Test
    public void getClass_initializeFalse_loadsWithoutInitializing() throws Exception {
        ClassLoader loader = ClassUtilsLLMTest.class.getClassLoader();
        Class<?> result = ClassUtils.getClass(loader, LazyFalseProbe.class.getName(), false);

        assertEquals(LazyFalseProbe.class, result);
        assertFalse("La clase no debería haberse inicializado",
                INIT_EVENTS.contains("LazyFalseProbe"));
    }

    @Test
    public void getClass_initializeTrue_initializesClass() throws Exception {
        ClassLoader loader = ClassUtilsLLMTest.class.getClassLoader();
        Class<?> result = ClassUtils.getClass(loader, LazyTrueProbe.class.getName(), true);

        assertEquals(LazyTrueProbe.class, result);
        assertTrue("La clase debería haberse inicializado",
                INIT_EVENTS.contains("LazyTrueProbe"));
    }

    @Test
    public void getClass_explicitLoader_resolvesPrimitivesAndArrays() throws Exception {
        ClassLoader loader = ClassUtilsLLMTest.class.getClassLoader();

        assertEquals(int.class, ClassUtils.getClass(loader, "int", false));
        assertEquals(boolean.class, ClassUtils.getClass(loader, "boolean", false));
        assertEquals(int[].class, ClassUtils.getClass(loader, "int[]", false));
        assertEquals(int[][].class, ClassUtils.getClass(loader, "int[][]", false));
        assertEquals(String[].class, ClassUtils.getClass(loader, "java.lang.String[]", false));
        assertEquals(LazyFalseProbe[].class,
                ClassUtils.getClass(loader, LazyFalseProbe.class.getName() + "[]", false));
    }

    @Test
    public void getClass_explicitLoaderWithoutApplicationClasses_throwsClassNotFoundException() throws Exception {
        // Un ClassLoader sin padre solo ve el bootstrap de la JVM.
        ClassLoader isolated = new ClassLoader(null) {
        };

        assertEquals(String.class, ClassUtils.getClass(isolated, "java.lang.String", false));

        try {
            ClassUtils.getClass(isolated, ClassUtilsLLMTest.class.getName(), false);
            fail("Se esperaba ClassNotFoundException: el ClassLoader explícito no ve la clase de prueba");
        } catch (ClassNotFoundException expected) {
            // comportamiento esperado: se usa el ClassLoader proporcionado
        }
    }

    // ------------------------------------------------------------------
    // convertClassNamesToClasses
    // ------------------------------------------------------------------

    @Test
    public void convertClassNamesToClasses_nullNameInList_producesNullEntry() {
        List<String> names = Arrays.asList("java.lang.String", null, "java.lang.Integer");
        List<Class<?>> result = ClassUtils.convertClassNamesToClasses(names);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(String.class, result.get(0));
        assertNull(result.get(1));
        assertEquals(Integer.class, result.get(2));
    }

    // ------------------------------------------------------------------
    // getShortCanonicalName / getPackageCanonicalName con null
    // ------------------------------------------------------------------

    @Test
    public void getShortCanonicalName_nullString_returnsEmptyString() {
        assertEquals("", ClassUtils.getShortCanonicalName((String) null));
    }

    @Test
    public void getShortCanonicalName_nullClass_returnsEmptyString() {
        assertEquals("", ClassUtils.getShortCanonicalName((Class<?>) null));
    }

    @Test
    public void getPackageCanonicalName_nullString_returnsEmptyString() {
        assertEquals("", ClassUtils.getPackageCanonicalName((String) null));
    }

    @Test
    public void getPackageCanonicalName_nullClass_returnsEmptyString() {
        assertEquals("", ClassUtils.getPackageCanonicalName((Class<?>) null));
    }
}
