/*
 * Pruebas JUnit 4 adicionales para org.apache.commons.lang.ClassUtils.
 * No modifican ni sustituyen las pruebas existentes en ClassUtilsTest.
 */
package org.apache.commons.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class ClassUtilsLLMTest {

    // ------------------------------------------------------------------
    // Tipos auxiliares
    // ------------------------------------------------------------------
    private static interface PrivIf {
        void m();
    }

    private static class PrivImpl implements PrivIf {
        public void m() {
        }
    }

    private static class PrivRunnable implements Runnable {
        public void run() {
        }
    }

    private static class PrivNoInterface {
        public void foo() {
        }
    }

    // ------------------------------------------------------------------
    // getShortClassName
    // ------------------------------------------------------------------
    @Test
    public void getShortClassNameStringWithInnerSeparatorReplacesDollarWithDot() {
        assertEquals("ClassUtils.Foo",
                ClassUtils.getShortClassName("org.apache.commons.lang.ClassUtils$Foo"));
    }

    @Test
    public void getShortClassNameStringWithoutPackageReturnsSameName() {
        assertEquals("NoPackage", ClassUtils.getShortClassName("NoPackage"));
    }

    @Test
    public void getShortClassNameClassForNestedTypeUsesDot() {
        assertEquals("Map.Entry", ClassUtils.getShortClassName(Map.Entry.class));
    }

    @Test
    public void getShortClassNameClassForNullReturnsEmptyString() {
        assertEquals("", ClassUtils.getShortClassName((Class<?>) null));
    }

    @Test
    public void getShortClassNameObjectReturnsSimpleNameOfInteger() {
        assertEquals("Integer", ClassUtils.getShortClassName(Integer.valueOf(5), "<null>"));
    }

    // ------------------------------------------------------------------
    // getPackageName
    // ------------------------------------------------------------------
    @Test
    public void getPackageNameClassForStringReturnsJavaLang() {
        assertEquals("java.lang", ClassUtils.getPackageName(String.class));
    }

    @Test
    public void getPackageNameClassForNullReturnsEmptyString() {
        assertEquals("", ClassUtils.getPackageName((Class<?>) null));
    }

    @Test
    public void getPackageNameObjectForNullReturnsValueIfNull() {
        assertEquals("fallback", ClassUtils.getPackageName((Object) null, "fallback"));
    }

    @Test
    public void getPackageNameStringWithoutPackageReturnsEmptyString() {
        assertEquals("", ClassUtils.getPackageName("NoPackage"));
    }

    // ------------------------------------------------------------------
    // getAllSuperclasses / getAllInterfaces
    // ------------------------------------------------------------------
    @Test
    public void getAllSuperclassesOfObjectIsEmpty() {
        List<Class<?>> list = ClassUtils.getAllSuperclasses(Object.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void getAllSuperclassesOfInterfaceIsEmpty() {
        List<Class<?>> list = ClassUtils.getAllSuperclasses(Runnable.class);
        assertTrue(list.isEmpty());
    }

    @Test
    public void getAllSuperclassesOfIntegerStartsWithNumber() {
        List<Class<?>> list = ClassUtils.getAllSuperclasses(Integer.class);
        assertEquals(Number.class, list.get(0));
        assertEquals(Object.class, list.get(1));
        assertEquals(2, list.size());
    }

    @Test
    public void getAllInterfacesOfArrayListContainsListAndRandomAccess() {
        List<Class<?>> list = ClassUtils.getAllInterfaces(ArrayList.class);
        assertTrue(list.contains(List.class));
        assertTrue(list.contains(java.util.RandomAccess.class));
    }

    @Test
    public void getAllInterfacesHasNoDuplicates() {
        List<Class<?>> list = ClassUtils.getAllInterfaces(ArrayList.class);
        assertEquals(new HashSet<Class<?>>(list).size(), list.size());
    }

    @Test
    public void getAllInterfacesOfObjectIsEmpty() {
        List<Class<?>> list = ClassUtils.getAllInterfaces(Object.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ------------------------------------------------------------------
    // convertClassNamesToClasses / convertClassesToClassNames
    // ------------------------------------------------------------------
    @Test
    public void convertClassNamesToClassesEmptyNameYieldsNull() {
        List<String> names = Arrays.asList("");
        List<Class<?>> result = ClassUtils.convertClassNamesToClasses(names);
        assertEquals(1, result.size());
        assertNull(result.get(0));
    }

    @Test
    public void convertClassNamesToClassesPrimitiveKeywordYieldsNull() {
        List<String> names = Arrays.asList("int");
        List<Class<?>> result = ClassUtils.convertClassNamesToClasses(names);
        assertNull(result.get(0));
    }

    @Test
    public void convertClassNamesToClassesReturnsNewList() {
        List<String> names = new ArrayList<String>(Arrays.asList("java.lang.Integer"));
        List<Class<?>> result = ClassUtils.convertClassNamesToClasses(names);
        assertNotSame(names, result);
        assertEquals(Integer.class, result.get(0));
    }

    @Test
    public void convertClassesToClassNamesReturnsNewList() {
        List<Class<?>> classes = new ArrayList<Class<?>>(Arrays.<Class<?>>asList(Integer.class));
        List<String> result = ClassUtils.convertClassesToClassNames(classes);
        assertNotSame(classes, result);
        assertEquals("java.lang.Integer", result.get(0));
    }

    // ------------------------------------------------------------------
    // isAssignable con autoboxing
    // ------------------------------------------------------------------
    @Test
    public void isAssignableArraysLengthMismatchIsFalse() {
        assertFalse(ClassUtils.isAssignable(
                new Class<?>[] {int.class},
                new Class<?>[] {Integer.class, Integer.class}, true));
    }

    @Test
    public void isAssignableArraysNullArraysWithAutoboxingAreTrue() {
        assertTrue(ClassUtils.isAssignable((Class<?>[]) null, (Class<?>[]) null, true));
    }

    @Test
    public void isAssignableArraysWithAutoboxingAcceptsPrimitiveForWrapper() {
        assertTrue(ClassUtils.isAssignable(
                new Class<?>[] {int.class}, new Class<?>[] {Integer.class}, true));
    }

    @Test
    public void isAssignableArraysWithoutAutoboxingRejectsPrimitiveForWrapper() {
        assertFalse(ClassUtils.isAssignable(
                new Class<?>[] {int.class}, new Class<?>[] {Integer.class}, false));
    }

    @Test
    public void isAssignableArraysNullElementAssignableToReference() {
        assertTrue(ClassUtils.isAssignable(
                new Class<?>[] {null}, new Class<?>[] {Object.class}, true));
    }

    @Test
    public void isAssignableWithAutoboxingPrimitiveToSuperclassOfWrapper() {
        assertTrue(ClassUtils.isAssignable(int.class, Number.class, true));
        assertTrue(ClassUtils.isAssignable(int.class, Object.class, true));
    }

    @Test
    public void isAssignableWithoutAutoboxingPrimitiveToReferenceIsFalse() {
        assertFalse(ClassUtils.isAssignable(int.class, Object.class, false));
    }

    @Test
    public void isAssignableWithAutoboxingNonWrapperToPrimitiveIsFalse() {
        assertFalse(ClassUtils.isAssignable(String.class, int.class, true));
    }

    @Test
    public void isAssignableWithAutoboxingNullToPrimitiveIsFalse() {
        assertFalse(ClassUtils.isAssignable((Class<?>) null, int.class, true));
    }

    @Test
    public void isAssignableWithAutoboxingWrapperToWideningPrimitive() {
        assertTrue(ClassUtils.isAssignable(Integer.class, double.class, true));
        assertFalse(ClassUtils.isAssignable(Integer.class, double.class, false));
    }

    @Test
    public void isAssignableWithAutoboxingNullToNullTargetIsFalse() {
        assertFalse(ClassUtils.isAssignable(int.class, null, true));
    }

    // ------------------------------------------------------------------
    // primitiveToWrapper / wrapperToPrimitive / arrays
    // ------------------------------------------------------------------
    @Test
    public void primitiveToWrapperNonPrimitiveArrayReturnsSameClass() {
        assertEquals(int[].class, ClassUtils.primitiveToWrapper(int[].class));
    }

    @Test
    public void wrapperToPrimitiveVoidWrapperIsNull() {
        assertNull(ClassUtils.wrapperToPrimitive(Void.class));
    }

    @Test
    public void wrapperToPrimitiveForIntegerIsIntType() {
        assertEquals(int.class, ClassUtils.wrapperToPrimitive(Integer.class));
    }

    @Test
    public void primitivesToWrappersNullElementStaysNull() {
        Class<?>[] result = ClassUtils.primitivesToWrappers(new Class<?>[] {null});
        assertEquals(1, result.length);
        assertNull(result[0]);
    }

    @Test
    public void primitivesToWrappersConvertsIntToInteger() {
        Class<?>[] result = ClassUtils.primitivesToWrappers(new Class<?>[] {int.class});
        assertEquals(Integer.class, result[0]);
    }

    @Test
    public void wrappersToPrimitivesNonWrapperYieldsNull() {
        Class<?>[] result = ClassUtils.wrappersToPrimitives(new Class<?>[] {String.class});
        assertEquals(1, result.length);
        assertNull(result[0]);
    }

    @Test
    public void wrappersToPrimitivesConvertsIntegerToInt() {
        Class<?>[] result = ClassUtils.wrappersToPrimitives(new Class<?>[] {Integer.class});
        assertEquals(int.class, result[0]);
    }

    // ------------------------------------------------------------------
    // isInnerClass
    // ------------------------------------------------------------------
    @Test
    public void isInnerClassPrimitiveIsFalse() {
        assertFalse(ClassUtils.isInnerClass(int.class));
    }

    @Test
    public void isInnerClassPrivateNestedClassIsTrue() {
        assertTrue(ClassUtils.isInnerClass(PrivImpl.class));
    }

    // ------------------------------------------------------------------
    // getClass
    // ------------------------------------------------------------------
    @Test
    public void getClassWithLoaderAndNoInitializeReturnsPrimitive() throws Exception {
        assertEquals(int.class,
                ClassUtils.getClass(ClassUtils.class.getClassLoader(), "int", false));
    }

    @Test
    public void getClassWithLoaderAndNoInitializeReturnsClass() throws Exception {
        assertEquals(String.class,
                ClassUtils.getClass(ClassUtils.class.getClassLoader(), "java.lang.String", false));
    }

    @Test
    public void getClassWithLoaderReturnsArrayOfObjectArray() throws Exception {
        assertEquals(Object[][].class,
                ClassUtils.getClass(ClassUtils.class.getClassLoader(), "java.lang.Object[][]"));
    }

    @Test
    public void getClassStringJvmArrayNameForIntReturnsIntArray() throws Exception {
        assertEquals(int[].class, ClassUtils.getClass("[I"));
    }

    @Test
    public void getClassStringJvmArrayNameForStringReturnsStringArray() throws Exception {
        assertEquals(String[].class, ClassUtils.getClass("[Ljava.lang.String;"));
    }

    @Test
    public void getClassStringWithoutInitializeReturnsClass() throws Exception {
        assertEquals(String.class, ClassUtils.getClass("java.lang.String", false));
    }

    @Test
    public void getClassNestedTypeWithDollarName() throws Exception {
        assertEquals(Map.Entry.class, ClassUtils.getClass("java.util.Map$Entry"));
    }

    // ------------------------------------------------------------------
    // getPublicMethod
    // ------------------------------------------------------------------
    @Test
    public void getPublicMethodFromPrivateClassUsesPublicInterface() throws Exception {
        Method m = ClassUtils.getPublicMethod(PrivRunnable.class, "run", new Class<?>[0]);
        assertEquals(Runnable.class, m.getDeclaringClass());
        assertEquals("run", m.getName());
    }

    @Test(expected = NoSuchMethodException.class)
    public void getPublicMethodNoPublicCandidateThrows() throws Exception {
        ClassUtils.getPublicMethod(PrivImpl.class, "m", new Class<?>[0]);
    }

    @Test(expected = NoSuchMethodException.class)
    public void getPublicMethodPrivateClassWithoutPublicOwnerThrows() throws Exception {
        ClassUtils.getPublicMethod(PrivNoInterface.class, "foo", new Class<?>[0]);
    }

    @Test(expected = NoSuchMethodException.class)
    public void getPublicMethodMissingMethodThrows() throws Exception {
        ClassUtils.getPublicMethod(Object.class, "noSuchMethodHere", new Class<?>[0]);
    }

    @Test
    public void getPublicMethodWithParameterTypes() throws Exception {
        Method m = ClassUtils.getPublicMethod(String.class, "equals", new Class<?>[] {Object.class});
        assertEquals(String.class, m.getDeclaringClass());
        assertEquals("equals", m.getName());
    }

    @Test(expected = NullPointerException.class)
    public void getPublicMethodNullClassThrowsNpe() throws Exception {
        ClassUtils.getPublicMethod(null, "toString", new Class<?>[0]);
    }

    // ------------------------------------------------------------------
    // toClass
    // ------------------------------------------------------------------
    @Test
    public void toClassMixedArrayReturnsRuntimeClasses() {
        Class<?>[] result = ClassUtils.toClass(new Object[] {"a", Integer.valueOf(1)});
        assertEquals(2, result.length);
        assertEquals(String.class, result[0]);
        assertEquals(Integer.class, result[1]);
    }

    // ------------------------------------------------------------------
    // getShortCanonicalName / getPackageCanonicalName
    // ------------------------------------------------------------------
    @Test
    public void getShortCanonicalNameStringNullReturnsEmpty() {
        assertEquals("", ClassUtils.getShortCanonicalName((String) null));
    }

    @Test
    public void getShortCanonicalNameStringEmptyReturnsEmpty() {
        assertEquals("", ClassUtils.getShortCanonicalName(""));
    }

    @Test
    public void getShortCanonicalNameClassNullReturnsEmpty() {
        assertEquals("", ClassUtils.getShortCanonicalName((Class<?>) null));
    }

    @Test
    public void getShortCanonicalNameStringWithoutSemicolonForObjectArray() {
        assertEquals("String[]",
                ClassUtils.getShortCanonicalName("[Ljava.lang.String"));
    }

    @Test
    public void getShortCanonicalNameJvmDoubleArray() {
        assertEquals("double[]", ClassUtils.getShortCanonicalName("[D"));
    }

    @Test
    public void getShortCanonicalNameJvmLongTwoDimensionalArray() {
        assertEquals("long[][]", ClassUtils.getShortCanonicalName("[[J"));
    }

    @Test
    public void getShortCanonicalNameJvmBooleanArray() {
        assertEquals("boolean[]", ClassUtils.getShortCanonicalName("[Z"));
    }

    @Test
    public void getShortCanonicalNameNestedTypeName() {
        assertEquals("Map.Entry", ClassUtils.getShortCanonicalName("java.util.Map$Entry"));
    }

    @Test
    public void getShortCanonicalNameIgnoresWhitespace() {
        assertEquals("String", ClassUtils.getShortCanonicalName(" java.lang.String "));
    }

    @Test
    public void getShortCanonicalNameClassForString() {
        assertEquals("String", ClassUtils.getShortCanonicalName(String.class));
    }

    @Test
    public void getPackageCanonicalNameStringNullReturnsEmpty() {
        assertEquals("", ClassUtils.getPackageCanonicalName((String) null));
    }

    @Test
    public void getPackageCanonicalNameClassNullReturnsEmpty() {
        assertEquals("", ClassUtils.getPackageCanonicalName((Class<?>) null));
    }

    @Test
    public void getPackageCanonicalNameJvmNestedObjectArray() {
        assertEquals("java.util",
                ClassUtils.getPackageCanonicalName("[Ljava.util.Map$Entry;"));
    }

    @Test
    public void getPackageCanonicalNameClassForString() {
        assertEquals("java.lang", ClassUtils.getPackageCanonicalName(String.class));
    }

    @Test
    public void getPackageCanonicalNameStringWithoutPackageIsEmpty() {
        assertEquals("", ClassUtils.getPackageCanonicalName("NoPackage"));
    }

    // ------------------------------------------------------------------
    // Consistencia de constantes públicas
    // ------------------------------------------------------------------
    @Test
    public void packageSeparatorConstantsAreConsistent() {
        assertEquals(String.valueOf(ClassUtils.PACKAGE_SEPARATOR_CHAR), ClassUtils.PACKAGE_SEPARATOR);
        assertEquals('.', ClassUtils.PACKAGE_SEPARATOR_CHAR);
    }

    @Test
    public void innerClassSeparatorConstantsAreConsistent() {
        assertEquals(String.valueOf(ClassUtils.INNER_CLASS_SEPARATOR_CHAR), ClassUtils.INNER_CLASS_SEPARATOR);
        assertEquals('$', ClassUtils.INNER_CLASS_SEPARATOR_CHAR);
    }

    @Test
    public void getAllInterfacesReturnsUnmodifiedSetOfUniqueEntriesForRunnableImpl() {
        List<Class<?>> list = ClassUtils.getAllInterfaces(PrivRunnable.class);
        assertEquals(Collections.singletonList(Runnable.class), list);
        Set<Class<?>> set = new HashSet<Class<?>>(list);
        assertEquals(1, set.size());
    }
}
