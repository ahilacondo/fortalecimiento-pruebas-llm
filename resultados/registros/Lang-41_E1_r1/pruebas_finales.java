package org.apache.commons.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link ClassUtils} usando solo su API pública.
 */
public class ClassUtilsLLMTest {

    /** Clase no pública que implementa una interfaz pública. */
    static class Hidden implements Runnable {
        public void run() {
        }
    }

    // ------------------------------------------------------------------
    // Nombres cortos y de paquete
    // ------------------------------------------------------------------

    @Test
    public void getShortClassNameStringWithInnerClassUsesDotSeparator() {
        assertEquals("Map.Entry", ClassUtils.getShortClassName("java.util.Map$Entry"));
    }

    @Test
    public void getShortClassNameStringWithoutPackageReturnsSameName() {
        assertEquals("NoPackage", ClassUtils.getShortClassName("NoPackage"));
    }

    @Test
    public void getShortClassNameClassForStringReturnsString() {
        assertEquals("String", ClassUtils.getShortClassName(String.class));
    }

    @Test
    public void getShortClassNameNullClassReturnsEmptyString() {
        assertEquals("", ClassUtils.getShortClassName((Class<?>) null));
    }

    @Test
    public void getPackageNameStringWithInnerClassReturnsPackage() {
        assertEquals("java.util", ClassUtils.getPackageName("java.util.Map$Entry"));
    }

    @Test
    public void getPackageNameClassForStringReturnsJavaLang() {
        assertEquals("java.lang", ClassUtils.getPackageName(String.class));
    }

    @Test
    public void getPackageNameNullClassReturnsEmptyString() {
        assertEquals("", ClassUtils.getPackageName((Class<?>) null));
    }

    @Test
    public void getPackageNameNullObjectReturnsValueIfNull() {
        assertEquals("<none>", ClassUtils.getPackageName((Object) null, "<none>"));
    }

    // ------------------------------------------------------------------
    // Superclases e interfaces
    // ------------------------------------------------------------------

    @Test
    public void getAllSuperclassesOfObjectIsEmpty() {
        List<Class<?>> list = ClassUtils.getAllSuperclasses(Object.class);
        assertNotNull(list);
        assertEquals(0, list.size());
    }

    @Test
    public void getAllSuperclassesOfInterfaceIsEmpty() {
        List<Class<?>> list = ClassUtils.getAllSuperclasses(List.class);
        assertEquals(0, list.size());
    }

    @Test
    public void getAllSuperclassesOfArrayListIsOrderedUpwards() {
        List<Class<?>> list = ClassUtils.getAllSuperclasses(ArrayList.class);
        assertEquals(3, list.size());
        assertEquals(AbstractList.class, list.get(0));
        assertEquals(AbstractCollection.class, list.get(1));
        assertEquals(Object.class, list.get(2));
    }

    @Test
    public void getAllInterfacesOfPrimitiveIsEmpty() {
        List<Class<?>> list = ClassUtils.getAllInterfaces(int.class);
        assertEquals(0, list.size());
    }

    @Test
    public void getAllInterfacesOfArrayListIncludesHierarchy() {
        List<Class<?>> list = ClassUtils.getAllInterfaces(ArrayList.class);
        assertTrue(list.contains(List.class));
        assertTrue(list.contains(RandomAccess.class));
        assertTrue(list.contains(Cloneable.class));
        assertTrue(list.contains(Serializable.class));
        assertTrue("super-interface Collection debe incluirse", list.contains(Collection.class));
    }

    @Test
    public void getAllInterfacesContainsNoDuplicates() {
        List<Class<?>> list = ClassUtils.getAllInterfaces(ArrayList.class);
        assertEquals(new HashSet<Class<?>>(list).size(), list.size());
    }

    // ------------------------------------------------------------------
    // Conversión de listas
    // ------------------------------------------------------------------

    @Test
    public void convertClassNamesToClassesEmptyListReturnsEmptyList() {
        List<Class<?>> result = ClassUtils.convertClassNamesToClasses(new ArrayList<String>());
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void convertClassNamesToClassesNullElementBecomesNull() {
        List<String> names = new ArrayList<String>();
        names.add(null);
        List<Class<?>> result = ClassUtils.convertClassNamesToClasses(names);
        assertEquals(1, result.size());
        assertNull(result.get(0));
    }

    @Test
    public void convertClassesToClassNamesEmptyListReturnsEmptyList() {
        List<String> result = ClassUtils.convertClassesToClassNames(new ArrayList<Class<?>>());
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void convertClassesToClassNamesRoundTripsWithConvertClassNamesToClasses() {
        List<String> names = Arrays.asList("java.lang.Integer", "java.util.List");
        List<Class<?>> classes = ClassUtils.convertClassNamesToClasses(names);
        assertEquals(names, ClassUtils.convertClassesToClassNames(classes));
    }

    // ------------------------------------------------------------------
    // isAssignable
    // ------------------------------------------------------------------

    @Test
    public void isAssignableArraysOfDifferentLengthReturnsFalse() {
        Class<?>[] one = new Class<?>[] {String.class};
        Class<?>[] none = new Class<?>[0];
        assertFalse(ClassUtils.isAssignable(one, none));
        assertFalse(ClassUtils.isAssignable(none, one));
    }

    @Test
    public void isAssignableArraysWithAutoboxingFlag() {
        Class<?>[] from = new Class<?>[] {int.class};
        Class<?>[] to = new Class<?>[] {Integer.class};
        assertFalse(ClassUtils.isAssignable(from, to));
        assertFalse(ClassUtils.isAssignable(from, to, false));
        assertTrue(ClassUtils.isAssignable(from, to, true));
    }

    @Test
    public void isAssignableIntWideningToLongAndFloatAndDouble() {
        assertTrue(ClassUtils.isAssignable(int.class, long.class));
        assertTrue(ClassUtils.isAssignable(int.class, float.class));
        assertTrue(ClassUtils.isAssignable(int.class, double.class));
    }

    @Test
    public void isAssignableLongNarrowingToIntIsFalse() {
        assertFalse(ClassUtils.isAssignable(long.class, int.class));
    }

    @Test
    public void isAssignableIntegerToLongPrimitiveRequiresAutoboxing() {
        assertFalse(ClassUtils.isAssignable(Integer.class, long.class));
        assertFalse(ClassUtils.isAssignable(Integer.class, long.class, false));
        assertTrue(ClassUtils.isAssignable(Integer.class, long.class, true));
    }

    @Test
    public void isAssignablePrimitiveToReferenceRequiresAutoboxing() {
        assertFalse(ClassUtils.isAssignable(int.class, Object.class, false));
        assertTrue(ClassUtils.isAssignable(int.class, Object.class, true));
        assertTrue(ClassUtils.isAssignable(int.class, Number.class, true));
    }

    @Test
    public void isAssignableArrayTypesFollowReferenceRules() {
        assertTrue(ClassUtils.isAssignable(String[].class, Object[].class));
        assertFalse(ClassUtils.isAssignable(Object[].class, String[].class));
        assertTrue(ClassUtils.isAssignable(int[].class, Object.class));
    }

    // ------------------------------------------------------------------
    // Wrappers
    // ------------------------------------------------------------------

    @Test
    public void wrapperToPrimitiveIntegerReturnsIntType() {
        assertEquals(int.class, ClassUtils.wrapperToPrimitive(Integer.class));
    }

    @Test
    public void primitivesToWrappersVoidStaysVoid() {
        Class<?>[] result = ClassUtils.primitivesToWrappers(new Class<?>[] {void.class});
        assertEquals(1, result.length);
        assertEquals(void.class, result[0]);
    }

    @Test
    public void primitivesToWrappersEmptyReturnsSameInstance() {
        Class<?>[] empty = new Class<?>[0];
        assertSame(empty, ClassUtils.primitivesToWrappers(empty));
    }

    @Test
    public void wrappersToPrimitivesNonWrapperYieldsNullElement() {
        Class<?>[] result = ClassUtils.wrappersToPrimitives(new Class<?>[] {String.class, Long.class});
        assertEquals(2, result.length);
        assertNull(result[0]);
        assertEquals(long.class, result[1]);
    }

    @Test
    public void wrappersToPrimitivesEmptyReturnsSameInstance() {
        Class<?>[] empty = new Class<?>[0];
        assertSame(empty, ClassUtils.wrappersToPrimitives(empty));
    }

    // ------------------------------------------------------------------
    // Clases internas
    // ------------------------------------------------------------------

    @Test
    public void isInnerClassMapEntryIsTrue() {
        assertTrue(ClassUtils.isInnerClass(Map.Entry.class));
    }

    @Test
    public void isInnerClassClassUtilsItselfIsFalse() {
        assertFalse(ClassUtils.isInnerClass(ClassUtils.class));
    }

    // ------------------------------------------------------------------
    // Carga de clases
    // ------------------------------------------------------------------

    @Test
    public void getClassWithWhitespaceInFullyQualifiedName() throws ClassNotFoundException {
        assertEquals(String.class, ClassUtils.getClass("  java.lang.String  "));
    }

    @Test
    public void getClassWithClassLoaderAndJvmArrayName() throws ClassNotFoundException {
        assertEquals(int[].class,
                ClassUtils.getClass(ClassUtils.class.getClassLoader(), "[I"));
    }

    @Test
    public void getClassWithClassLoaderAndAbbreviatedPrimitiveName() throws ClassNotFoundException {
        assertEquals(int.class,
                ClassUtils.getClass(ClassUtils.class.getClassLoader(), "int", false));
    }

    @Test
    public void getClassWithClassLoaderAndArraySyntax() throws ClassNotFoundException {
        assertEquals(String[].class,
                ClassUtils.getClass(ClassUtils.class.getClassLoader(), "java.lang.String[]"));
    }

    @Test(expected = ClassNotFoundException.class)
    public void getClassUnknownNameThrowsClassNotFound() throws ClassNotFoundException {
        ClassUtils.getClass("org.apache.commons.lang.DoesNotExistXyz");
    }

    // ------------------------------------------------------------------
    // toClass
    // ------------------------------------------------------------------

    @Test
    public void toClassEmptyArrayReturnsEmptyClassArray() {
        Class<?>[] result = ClassUtils.toClass(new Object[0]);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void toClassMixedObjectsReturnsRuntimeClasses() {
        Class<?>[] result = ClassUtils.toClass(new Object[] {"text", Long.valueOf(3L)});
        assertEquals(String.class, result[0]);
        assertEquals(Long.class, result[1]);
    }

    // ------------------------------------------------------------------
    // getPublicMethod
    // ------------------------------------------------------------------

    @Test
    public void getPublicMethodFromNonPublicClassReturnsInterfaceMethod() throws Exception {
        Method m = ClassUtils.getPublicMethod(Hidden.class, "run", new Class<?>[0]);
        assertEquals(Runnable.class, m.getDeclaringClass());
        assertEquals("run", m.getName());
    }

    @Test(expected = NoSuchMethodException.class)
    public void getPublicMethodMissingMethodThrows() throws Exception {
        ClassUtils.getPublicMethod(String.class, "noSuchMethodXyz", new Class<?>[0]);
    }

    // ------------------------------------------------------------------
    // Nombres canónicos
    // ------------------------------------------------------------------

    @Test
    public void getShortCanonicalNameOfMapEntryClass() {
        assertEquals("Map.Entry", ClassUtils.getShortCanonicalName(Map.Entry.class));
    }

    @Test
    public void getShortCanonicalNameNullStringReturnsEmpty() {
        assertEquals("", ClassUtils.getShortCanonicalName((String) null));
    }

    @Test
    public void getShortCanonicalNameIgnoresWhitespaceInArrayEncoding() {
        assertEquals("String[]", ClassUtils.getShortCanonicalName("[ Ljava.lang.String ;"));
    }

    @Test
    public void getPackageCanonicalNameOfJvmArrayOfInnerClass() {
        assertEquals("java.util",
                ClassUtils.getPackageCanonicalName("[Ljava.util.Map$Entry;"));
    }

    @Test
    public void getPackageCanonicalNameNullStringReturnsEmpty() {
        assertEquals("", ClassUtils.getPackageCanonicalName((String) null));
    }

    @Test
    public void getPackageCanonicalNameIgnoresWhitespaceInPrimitiveArrayEncoding() {
        assertEquals("", ClassUtils.getPackageCanonicalName("[ I"));
    }
}
