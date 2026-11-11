package org.apache.commons.lang3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

/**
 * Pruebas adicionales para {@link SystemUtils}, usando solo su API pública.
 */
public class SystemUtilsLLMTest {

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    @Test
    public void testPublicConstructorCreatesNonNullInstance() {
        SystemUtils utils = new SystemUtils();
        assertNotNull(utils);
    }

    // ------------------------------------------------------------------
    // Directorios como File
    // ------------------------------------------------------------------

    @Test
    public void testGetJavaHomeMatchesJavaHomeProperty() {
        File expected = new File(System.getProperty("java.home"));
        assertEquals(expected.getPath(), SystemUtils.getJavaHome().getPath());
    }

    @Test
    public void testGetJavaIoTmpDirMatchesTmpDirProperty() {
        File expected = new File(System.getProperty("java.io.tmpdir"));
        assertEquals(expected.getPath(), SystemUtils.getJavaIoTmpDir().getPath());
    }

    @Test
    public void testGetUserDirMatchesUserDirProperty() {
        File expected = new File(System.getProperty("user.dir"));
        assertEquals(expected.getPath(), SystemUtils.getUserDir().getPath());
    }

    @Test
    public void testGetUserHomeMatchesUserHomeProperty() {
        File expected = new File(System.getProperty("user.home"));
        assertEquals(expected.getPath(), SystemUtils.getUserHome().getPath());
    }

    @Test
    public void testGettersReturnNewFileInstancesOnEachCall() {
        assertNotSame(SystemUtils.getUserHome(), SystemUtils.getUserHome());
        assertNotSame(SystemUtils.getUserDir(), SystemUtils.getUserDir());
    }

    @Test
    public void testGetUserDirIsDirectory() {
        assertTrue(SystemUtils.getUserDir().isDirectory());
    }

    // ------------------------------------------------------------------
    // Constantes de cadena frente a propiedades del sistema
    // ------------------------------------------------------------------

    @Test
    public void testSeparatorConstantsMatchSystemProperties() {
        assertEquals(System.getProperty("file.separator"), SystemUtils.FILE_SEPARATOR);
        assertEquals(System.getProperty("path.separator"), SystemUtils.PATH_SEPARATOR);
        assertEquals(System.getProperty("line.separator"), SystemUtils.LINE_SEPARATOR);
    }

    @Test
    public void testUserHomeConstantMatchesProperty() {
        assertEquals(System.getProperty("user.home"), SystemUtils.USER_HOME);
        assertEquals(new File(SystemUtils.USER_HOME), SystemUtils.getUserHome());
    }

    @Test
    public void testUserDirConstantMatchesProperty() {
        assertEquals(System.getProperty("user.dir"), SystemUtils.USER_DIR);
    }

    @Test
    public void testOsNameAndOsArchConstantsMatchProperties() {
        assertEquals(System.getProperty("os.name"), SystemUtils.OS_NAME);
        assertEquals(System.getProperty("os.arch"), SystemUtils.OS_ARCH);
        assertEquals(System.getProperty("os.version"), SystemUtils.OS_VERSION);
    }

    @Test
    public void testUserCountryFallsBackToUserRegion() {
        String country = System.getProperty("user.country");
        String expected = country != null ? country : System.getProperty("user.region");
        assertEquals(expected, SystemUtils.USER_COUNTRY);
    }

    // ------------------------------------------------------------------
    // Java AWT headless
    // ------------------------------------------------------------------

    @Test
    public void testIsJavaAwtHeadlessAgreesWithConstant() {
        boolean expected = SystemUtils.JAVA_AWT_HEADLESS != null
                && SystemUtils.JAVA_AWT_HEADLESS.equals(Boolean.TRUE.toString());
        assertEquals(expected, SystemUtils.isJavaAwtHeadless());
    }

    @Test
    public void testIsJavaAwtHeadlessFalseWhenConstantIsNull() {
        if (SystemUtils.JAVA_AWT_HEADLESS == null) {
            assertFalse(SystemUtils.isJavaAwtHeadless());
        }
    }

    // ------------------------------------------------------------------
    // Versión de Java: constantes y comparaciones
    // ------------------------------------------------------------------

    @Test
    public void testJavaVersionTrimmedIsSuffixOfJavaVersion() {
        if (SystemUtils.JAVA_VERSION == null) {
            assertNull(SystemUtils.JAVA_VERSION_TRIMMED);
        } else {
            assertNotNull(SystemUtils.JAVA_VERSION_TRIMMED);
            assertTrue(SystemUtils.JAVA_VERSION.endsWith(SystemUtils.JAVA_VERSION_TRIMMED));
            char first = SystemUtils.JAVA_VERSION_TRIMMED.charAt(0);
            assertTrue(first >= '0' && first <= '9');
        }
    }

    @Test
    public void testJavaVersionIntIsZeroOnlyWhenVersionIsNull() {
        if (SystemUtils.JAVA_VERSION == null) {
            assertEquals(0, SystemUtils.JAVA_VERSION_INT);
            assertEquals(0f, SystemUtils.JAVA_VERSION_FLOAT, 0f);
        } else {
            assertTrue(SystemUtils.JAVA_VERSION_INT > 0);
            assertTrue(SystemUtils.JAVA_VERSION_FLOAT > 0f);
        }
    }

    @Test
    public void testIsJavaVersionAtLeastFloatIncludesCurrentVersion() {
        assertTrue(SystemUtils.isJavaVersionAtLeast(SystemUtils.JAVA_VERSION_FLOAT));
    }

    @Test
    public void testIsJavaVersionAtLeastFloatRejectsNextHigherValue() {
        float next = Math.nextUp(SystemUtils.JAVA_VERSION_FLOAT);
        assertFalse(SystemUtils.isJavaVersionAtLeast(next));
    }

    @Test
    public void testIsJavaVersionAtLeastFloatExtremes() {
        assertTrue(SystemUtils.isJavaVersionAtLeast(0f));
        assertTrue(SystemUtils.isJavaVersionAtLeast(-1f));
        assertFalse(SystemUtils.isJavaVersionAtLeast(Float.MAX_VALUE));
    }

    @Test
    public void testIsJavaVersionAtLeastIntIncludesCurrentVersion() {
        assertTrue(SystemUtils.isJavaVersionAtLeast(SystemUtils.JAVA_VERSION_INT));
    }

    @Test
    public void testIsJavaVersionAtLeastIntRejectsNextHigherValue() {
        assertFalse(SystemUtils.isJavaVersionAtLeast(SystemUtils.JAVA_VERSION_INT + 1));
    }

    @Test
    public void testIsJavaVersionAtLeastIntExtremes() {
        assertTrue(SystemUtils.isJavaVersionAtLeast(0));
        assertFalse(SystemUtils.isJavaVersionAtLeast(Integer.MAX_VALUE));
    }

    @Test
    public void testJavaVersionFlagsConsistentWithTrimmedVersion() {
        String v = SystemUtils.JAVA_VERSION_TRIMMED;
        assertEquals(v != null && v.startsWith("1.1"), SystemUtils.IS_JAVA_1_1);
        assertEquals(v != null && v.startsWith("1.2"), SystemUtils.IS_JAVA_1_2);
        assertEquals(v != null && v.startsWith("1.3"), SystemUtils.IS_JAVA_1_3);
        assertEquals(v != null && v.startsWith("1.4"), SystemUtils.IS_JAVA_1_4);
        assertEquals(v != null && v.startsWith("1.5"), SystemUtils.IS_JAVA_1_5);
        assertEquals(v != null && v.startsWith("1.6"), SystemUtils.IS_JAVA_1_6);
        assertEquals(v != null && v.startsWith("1.7"), SystemUtils.IS_JAVA_1_7);
    }

    @Test
    public void testAtMostOneLegacyJavaVersionFlagIsTrue() {
        int count = 0;
        if (SystemUtils.IS_JAVA_1_1) count++;
        if (SystemUtils.IS_JAVA_1_2) count++;
        if (SystemUtils.IS_JAVA_1_3) count++;
        if (SystemUtils.IS_JAVA_1_4) count++;
        if (SystemUtils.IS_JAVA_1_5) count++;
        if (SystemUtils.IS_JAVA_1_6) count++;
        if (SystemUtils.IS_JAVA_1_7) count++;
        assertTrue("Como máximo una bandera de versión puede ser true", count <= 1);
    }

    // ------------------------------------------------------------------
    // Sistema operativo
    // ------------------------------------------------------------------

    @Test
    public void testOsFlagsAllFalseWhenOsNameIsNull() {
        if (SystemUtils.OS_NAME == null) {
            assertFalse(SystemUtils.IS_OS_AIX);
            assertFalse(SystemUtils.IS_OS_HP_UX);
            assertFalse(SystemUtils.IS_OS_IRIX);
            assertFalse(SystemUtils.IS_OS_LINUX);
            assertFalse(SystemUtils.IS_OS_MAC);
            assertFalse(SystemUtils.IS_OS_MAC_OSX);
            assertFalse(SystemUtils.IS_OS_OS2);
            assertFalse(SystemUtils.IS_OS_SOLARIS);
            assertFalse(SystemUtils.IS_OS_SUN_OS);
            assertFalse(SystemUtils.IS_OS_WINDOWS);
            assertFalse(SystemUtils.IS_OS_UNIX);
        }
    }

    @Test
    public void testOsUnixFlagIsCompositionOfUnixFamily() {
        boolean expected = SystemUtils.IS_OS_AIX || SystemUtils.IS_OS_HP_UX || SystemUtils.IS_OS_IRIX
                || SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC_OSX || SystemUtils.IS_OS_SOLARIS
                || SystemUtils.IS_OS_SUN_OS;
        assertEquals(expected, SystemUtils.IS_OS_UNIX);
    }

    @Test
    public void testOsWindowsVersionFlagsImplyWindows() {
        if (SystemUtils.IS_OS_WINDOWS_2000 || SystemUtils.IS_OS_WINDOWS_95 || SystemUtils.IS_OS_WINDOWS_98
                || SystemUtils.IS_OS_WINDOWS_ME || SystemUtils.IS_OS_WINDOWS_NT || SystemUtils.IS_OS_WINDOWS_XP
                || SystemUtils.IS_OS_WINDOWS_VISTA || SystemUtils.IS_OS_WINDOWS_7) {
            assertTrue(SystemUtils.IS_OS_WINDOWS);
            assertFalse(SystemUtils.IS_OS_UNIX);
        }
    }

    @Test
    public void testOsWindowsFlagConsistentWithOsName() {
        String name = SystemUtils.OS_NAME;
        boolean expected = name != null && name.startsWith("Windows");
        assertEquals(expected, SystemUtils.IS_OS_WINDOWS);
    }

    @Test
    public void testOsLinuxFlagConsistentWithOsName() {
        String name = SystemUtils.OS_NAME;
        boolean expected = name != null && (name.startsWith("Linux") || name.startsWith("LINUX"));
        assertEquals(expected, SystemUtils.IS_OS_LINUX);
    }

    @Test
    public void testOsMacOsXImpliesMac() {
        String name = SystemUtils.OS_NAME;
        boolean expectedOsX = name != null && name.startsWith("Mac OS X");
        assertEquals(expectedOsX, SystemUtils.IS_OS_MAC_OSX);
        if (SystemUtils.IS_OS_MAC_OSX) {
            assertTrue(SystemUtils.IS_OS_MAC);
            assertTrue(SystemUtils.IS_OS_UNIX);
        }
    }

    @Test
    public void testOsSolarisAndSunOsFlagsConsistentWithOsName() {
        String name = SystemUtils.OS_NAME;
        assertEquals(name != null && name.startsWith("Solaris"), SystemUtils.IS_OS_SOLARIS);
        assertEquals(name != null && name.startsWith("SunOS"), SystemUtils.IS_OS_SUN_OS);
    }
}
