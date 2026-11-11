package org.apache.commons.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link ClassUtils} que cubren comportamientos
 * no verificados por {@code ClassUtilsTest}.
 */
public class ClassUtilsLLMTest {

    // ------------------------------------------------------------------
    // Clases auxiliares
    // ------------------------------------------------------------------

    /** Clase con inicialización observable, para probar initialize=false. */
    public static class NoInitProbe {
        public static boolean initialized = false;
        static {
            initialized = true;
        }
    }

    /** Clase con inicialización observable, para probar initialize=true. */
    public static class InitProbe {
        public static boolean initialized = false;
        static {
            initialized = true;
        }
    }

    /**
     * Clase no pública con un método público y sin equivalente público
     * en su jerarquía (Object no declara {@code hiddenMethod}).
     */
    private static class HiddenWithPublicMethod {
        public void hiddenMethod() {
        }
    }

    // ------------------------------------------------------------------
    // 1. getShortClassName(String) con codificación de array de la JVM
    //    Nota: se documenta el comportamiento actual. El código contiene
    //    comentarios de manejo de arrays sin implementar.
    // ------------------------------------------------------------------

    @Test
    public void getShortClassName_jvmArrayEncodingOfObject_currentBehavior() {
        // Limitación conocida: la codificación "[L...;" no se decodifica;
        // solo se conserva el segmento tras el último punto.
        assertEquals("String;", ClassUtils.getShortClassName("[Ljava.lang.String;"));
    }

    @Test
    public void getShortClassName_jvmArrayEncodingOfPrimitive_currentBehavior() {
        // Sin punto en el nombre, se devuelve el nombre completo sin cambios.
        assertEquals("[I", ClassUtils.getShortClassName("[I"));
    }

    // ------------------------------------------------------------------
    // 2. getPackageName(String) con codificación de array o primitivo
    // ------------------------------------------------------------------

    @Test
    public void getPackageName_jvmArrayEncodingOfObject_currentBehavior() {
        // Limitación conocida: el prefijo "[L" forma parte del resultado.
        assertEquals("[Ljava.lang", ClassUtils.getPackageName("[Ljava.lang.String;"));
    }

    @Test
    public void getPackageName_jvmArrayEncodingOfPrimitive_returnsEmpty() {
        assertEquals("", ClassUtils.getPackageName("[I"));
    }

    @Test
    public void getPackageName_primitiveName_returnsEmpty() {
        assertEquals("", ClassUtils.getPackageName("int"));
    }

    @Test
    public void getPackageName_canonicalPrimitiveArray_returnsEmpty() {
        assertEquals("", ClassUtils.getPackageName("int[]"));
    }

    // ------------------------------------------------------------------
    // 3. getAllSuperclasses para Object, interfaces y primitivos
    // ------------------------------------------------------------------

    @Test
    public void getAllSuperclasses_object_returnsEmptyList() {
        List<Class<?>> list = ClassUtils.getAllSuperclasses(Object.class);
        assertNotNull(list);
        assertTrue("Object no tiene superclases", list.isEmpty());
    }

    @Test
    public void getAllSuperclasses_interface_returnsEmptyList() {
        List<Class<?>> list = ClassUtils.getAllSuperclasses(Runnable.class);
        assertNotNull(list);
        assertTrue("Una interfaz no tiene superclases", list.isEmpty());
    }

    @Test
    public void getAllSuperclasses_primitive_returnsEmptyList() {
        List<Class<?>> list = ClassUtils.getAllSuperclasses(int.class);
        assertNotNull(list);
        assertTrue("Un primitivo no tiene superclases", list.isEmpty());
    }

    @Test
    public void getAllSuperclasses_string_returnsObjectOnly() {
        List<Class<?>> list = ClassUtils.getAllSuperclasses(String.class);
        assertEquals(1, list.size());
        assertEquals(Object.class, list.get(0));
    }

    // ------------------------------------------------------------------
    // 4. isAssignable(Class[], Class[], autoboxing) con autoboxing = true
    // ------------------------------------------------------------------

    @Test
    public void isAssignableArrays_autoboxing_wrapperToPrimitive_true() {
        Class<?>[] from = {Integer.class};
        Class<?>[] to = {int.class};
        assertTrue(ClassUtils.isAssignable(from, to, true));
    }

    @Test
    public void isAssignableArrays_noAutoboxing_wrapperToPrimitive_false() {
        Class<?>[] from = {Integer.class};
        Class<?>[] to = {int.class};
        assertFalse(ClassUtils.isAssignable(from, to, false));
    }

    @Test
    public void isAssignableArrays_autoboxing_primitiveToWrapper_true() {
        Class<?>[] from = {int.class};
        Class<?>[] to = {Integer.class};
        assertTrue(ClassUtils.isAssignable(from, to, true));
    }

    @Test
    public void isAssignableArrays_autoboxing_primitiveToSuperWrapper_true() {
        // int -> Integer -> Number
        Class<?>[] from = {int.class, String.class};
        Class<?>[] to = {Number.class, Object.class};
        assertTrue(ClassUtils.isAssignable(from, to, true));
    }

    @Test
    public void isAssignableArrays_autoboxing_incompatibleElement_false() {
        Class<?>[] from = {int.class, String.class};
        Class<?>[] to = {Long.class, Object.class};
        assertFalse(ClassUtils.isAssignable(from, to, true));
    }

    @Test
    public void isAssignableArrays_autoboxing_lengthMismatch_false() {
        Class<?>[] from = {Integer.class};
        Class<?>[] to = {int.class, int.class};
        assertFalse(ClassUtils.isAssignable(from, to, true));
    }

    @Test
    public void isAssignableArrays_autoboxing_nullNull_true() {
        assertTrue(ClassUtils.isAssignable((Class<?>[]) null, (Class<?>[]) null, true));
    }

    @Test
    public void isAssignableArrays_autoboxing_nullAgainstNonEmpty_false() {
        Class<?>[] to = {Integer.class};
        assertFalse(ClassUtils.isAssignable((Class<?>[]) null, to, true));
    }

    // ------------------------------------------------------------------
    // 5. getClass(ClassLoader, String, boolean)
    // ------------------------------------------------------------------

    private static ClassLoader explicitLoader() {
        return ClassUtilsLLMTest.class.getClassLoader();
    }

    @Test
    public void getClassWithLoader_noInitialize_doesNotInitializeClass() throws Exception {
        assertFalse("Precondición: no inicializada", NoInitProbe.initialized);
        Class<?> cls = ClassUtils.getClass(explicitLoader(), NoInitProbe.class.getName(), false);
        assertEquals(NoInitProbe.class, cls);
        assertFalse("initialize=false no debe inicializar la clase", NoInitProbe.initialized);
    }

    @Test
    public void getClassWithLoader_initialize_initializesClass() throws Exception {
        assertFalse("Precondición: no inicializada", InitProbe.initialized);
        Class<?> cls = ClassUtils.getClass(explicitLoader(), InitProbe.class.getName(), true);
        assertEquals(InitProbe.class, cls);
        assertTrue("initialize=true debe inicializar la clase", InitProbe.initialized);
    }

    @Test
    public void getClassWithLoader_noInitialize_standardClass() throws Exception {
        assertEquals(String.class,
                ClassUtils.getClass(explicitLoader(), "java.lang.String", false));
    }

    @Test
    public void getClassWithLoader_noInitialize_primitive() throws Exception {
        assertEquals(int.class, ClassUtils.getClass(explicitLoader(), "int", false));
    }

    @Test
    public void getClassWithLoader_noInitialize_canonicalArray() throws Exception {
        assertEquals(int[].class, ClassUtils.getClass(explicitLoader(), "int[]", false));
        assertEquals(String[].class,
                ClassUtils.getClass(explicitLoader(), "java.lang.String[]", false));
    }

    @Test
    public void getClassWithLoader_noInitialize_jvmArrayName() throws Exception {
        assertEquals(String[].class,
                ClassUtils.getClass(explicitLoader(), "[Ljava.lang.String;", false));
    }

    @Test(expected = ClassNotFoundException.class)
    public void getClassWithLoader_missingClass_throwsClassNotFound() throws Exception {
        ClassUtils.getClass(explicitLoader(), "com.example.DoesNotExist", false);
    }

    // ------------------------------------------------------------------
    // 6. getPublicMethod: caminos de excepción
    // ------------------------------------------------------------------

    @Test
    public void getPublicMethod_noSuchMethod_throwsNoSuchMethodException() throws Exception {
        try {
            ClassUtils.getPublicMethod(Object.class, "noSuchMethodXyz", new Class<?>[0]);
            fail("Debe lanzar NoSuchMethodException");
        } catch (NoSuchMethodException expected) {
            // esperado
        }
    }

    @Test
    public void getPublicMethod_noPublicEquivalent_throwsNoSuchMethodException() throws Exception {
        // El método existe, pero está declarado en una clase no pública
        // y ninguna superclase o interfaz pública lo declara.
        try {
            ClassUtils.getPublicMethod(HiddenWithPublicMethod.class, "hiddenMethod", new Class<?>[0]);
            fail("Debe lanzar NoSuchMethodException cuando no hay método público equivalente");
        } catch (NoSuchMethodException ex) {
            assertTrue("Mensaje inesperado: " + ex.getMessage(),
                    ex.getMessage().startsWith("Can't find a public method for hiddenMethod"));
        }
    }

    @Test(expected = NullPointerException.class)
    public void getPublicMethod_nullClass_throwsNullPointerException() throws Exception {
        ClassUtils.getPublicMethod(null, "toString", new Class<?>[0]);
    }

    @Test
    public void getPublicMethod_publicDeclaringClass_returnsMethod() throws Exception {
        Method m = ClassUtils.getPublicMethod(String.class, "length", new Class<?>[0]);
        assertEquals(String.class.getMethod("length"), m);
    }

    // ------------------------------------------------------------------
    // 7. Asimetría Void: wrapperToPrimitive(Void.class)
    // ------------------------------------------------------------------

    @Test
    public void wrapperToPrimitive_voidWrapper_returnsNull() {
        assertNull("Void.class no está mapeada a un primitivo",
                ClassUtils.wrapperToPrimitive(Void.class));
    }

    @Test
    public void wrapperToPrimitive_voidPrimitive_returnsNull() {
        assertNull(ClassUtils.wrapperToPrimitive(Void.TYPE));
    }

    @Test
    public void primitiveToWrapper_voidPrimitive_returnsVoidTypeNotVoidClass() {
        Class<?> result = ClassUtils.primitiveToWrapper(Void.TYPE);
        assertSame("primitiveToWrapper(Void.TYPE) devuelve Void.TYPE", Void.TYPE, result);
        assertFalse("No debe devolver Void.class", Void.class.equals(result));
    }

    @Test
    public void wrappersToPrimitives_voidWrapper_containsNull() {
        Class<?>[] result = ClassUtils.wrappersToPrimitives(new Class<?>[] {Void.class});
        assertEquals(1, result.length);
        assertNull(result[0]);
    }

    // ------------------------------------------------------------------
    // 8. toClass con elemento null
    // ------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void toClass_nullElement_throwsNullPointerException() {
        ClassUtils.toClass(new Object[] {"texto", null});
    }

    @Test(expected = NullPointerException.class)
    public void toClass_nullElementFirst_throwsNullPointerException() {
        ClassUtils.toClass(new Object[] {null});
    }

    @Test
    public void toClass_nonNullElements_returnsRuntimeClasses() {
        Class<?>[] result = ClassUtils.toClass(new Object[] {"a", Integer.valueOf(1)});
        assertEquals(2, result.length);
        assertEquals(String.class, result[0]);
        assertEquals(Integer.class, result[1]);
    }

    // ------------------------------------------------------------------
    // Utilidad local
    // ------------------------------------------------------------------

    private static void assertNotNull(Object o) {
        assertTrue("Se esperaba un valor no nulo", o != null);
    }
}
