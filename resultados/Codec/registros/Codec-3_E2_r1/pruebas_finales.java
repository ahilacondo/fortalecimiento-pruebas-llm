package org.apache.commons.codec.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.codec.EncoderException;
import org.junit.Test;

/**
 * Pruebas adicionales para {@link DoubleMetaphone} que usan únicamente la API pública.
 */
public class DoubleMetaphoneLLMTest {

    // ---------------------------------------------------------------
    // Entrada nula, vacía o en blanco
    // ---------------------------------------------------------------

    @Test
    public void nullInputAlternateReturnsNull() {
        DoubleMetaphone dm = new DoubleMetaphone();
        assertNull(dm.doubleMetaphone(null, true));
    }

    @Test
    public void whitespaceOnlyAlternateReturnsNull() {
        DoubleMetaphone dm = new DoubleMetaphone();
        assertNull(dm.doubleMetaphone("  \t ", true));
    }

    @Test
    public void encodeStringNullReturnsNull() {
        DoubleMetaphone dm = new DoubleMetaphone();
        assertNull(dm.encode((String) null));
    }

    // ---------------------------------------------------------------
    // Normalización de la entrada
    // ---------------------------------------------------------------

    @Test
    public void leadingAndTrailingWhitespaceIsTrimmed() {
        DoubleMetaphone dm = new DoubleMetaphone();
        assertEquals("TSTN", dm.doubleMetaphone("  testing\t"));
    }

    @Test
    public void inputIsCaseInsensitive() {
        DoubleMetaphone dm = new DoubleMetaphone();
        String upper = dm.doubleMetaphone("TESTING");
        assertEquals(upper, dm.doubleMetaphone("testing"));
        assertEquals(upper, dm.doubleMetaphone("TeStInG"));
        assertEquals("TSTN", upper);
    }

    @Test
    public void cedillaIsEncodedAsS() {
        DoubleMetaphone dm = new DoubleMetaphone();
        assertEquals("S", dm.doubleMetaphone("\u00E7"));
    }

    @Test
    public void nWithTildeIsEncodedAsN() {
        DoubleMetaphone dm = new DoubleMetaphone();
        assertEquals("N", dm.doubleMetaphone("\u00F1"));
    }

    // ---------------------------------------------------------------
    // Interfaz StringEncoder
    // ---------------------------------------------------------------

    @Test
    public void encodeObjectReturnsSameAsDoubleMetaphone() throws EncoderException {
        DoubleMetaphone dm = new DoubleMetaphone();
        Object encoded = dm.encode((Object) "Thumb");
        assertEquals("0M", encoded);
        assertEquals(dm.doubleMetaphone("Thumb"), encoded);
    }

    @Test(expected = EncoderException.class)
    public void encodeObjectNonStringThrows() throws EncoderException {
        new DoubleMetaphone().encode((Object) Integer.valueOf(42));
    }

    // ---------------------------------------------------------------
    // Límite de longitud del código
    // ---------------------------------------------------------------

    @Test
    public void defaultMaxCodeLenIsFour() {
        assertEquals(4, new DoubleMetaphone().getMaxCodeLen());
    }

    @Test
    public void setMaxCodeLenOneTruncatesToSingleCharacter() {
        DoubleMetaphone dm = new DoubleMetaphone();
        dm.setMaxCodeLen(1);
        assertEquals(1, dm.getMaxCodeLen());
        assertEquals("T", dm.doubleMetaphone("testing", false));
        assertEquals("T", dm.doubleMetaphone("testing", true));
    }

    @Test
    public void setMaxCodeLenTwoTruncatesPrimaryAndAlternate() {
        DoubleMetaphone dm = new DoubleMetaphone();
        dm.setMaxCodeLen(2);
        assertEquals("JM", dm.doubleMetaphone("jumped", false));
        assertEquals("AM", dm.doubleMetaphone("jumped", true));
    }

    @Test
    public void setMaxCodeLenZeroProducesEmptyCode() {
        DoubleMetaphone dm = new DoubleMetaphone();
        dm.setMaxCodeLen(0);
        assertEquals("", dm.doubleMetaphone("abc", false));
    }

    @Test
    public void maxCodeLenIsRespectedForLongWords() {
        DoubleMetaphone dm = new DoubleMetaphone();
        String[] words = {
            "antidisestablishmentarianism", "encyclopedia", "intelligent",
            "Schmidt", "Kuczewski", "philosophy"
        };
        for (String w : words) {
            String primary = dm.doubleMetaphone(w, false);
            String alternate = dm.doubleMetaphone(w, true);
            assertNotNull(primary);
            assertNotNull(alternate);
            assertTrue("primary demasiado largo para " + w, primary.length() <= 4);
            assertTrue("alternate demasiado largo para " + w, alternate.length() <= 4);
        }
    }

    @Test
    public void maxCodeLenChangeDoesNotAffectOtherInstances() {
        DoubleMetaphone a = new DoubleMetaphone();
        DoubleMetaphone b = new DoubleMetaphone();
        a.setMaxCodeLen(1);
        assertEquals(4, b.getMaxCodeLen());
        assertEquals("JMPT", b.doubleMetaphone("jumped", false));
    }

    // ---------------------------------------------------------------
    // Ramas de codificación de letras concretas
    // ---------------------------------------------------------------

    @Test
    public void initialVowelEncodesAsA() {
        DoubleMetaphone dm = new DoubleMetaphone();
        // 'A' inicial -> 'A'; 'L' -> 'L'; 'C' antes de 'E' -> 'S'
        assertEquals("ALS", dm.doubleMetaphone("Alice", false));
        assertEquals("ALS", dm.doubleMetaphone("Alice", true));
    }

    @Test
    public void silentKnStartIsSkipped() {
        DoubleMetaphone dm = new DoubleMetaphone();
        assertEquals("NT", dm.doubleMetaphone("knight", false));
    }

    @Test
    public void knightAndNightAreEqual() {
        DoubleMetaphone dm = new DoubleMetaphone();
        assertTrue(dm.isDoubleMetaphoneEqual("Knight", "Night"));
        assertTrue(dm.isDoubleMetaphoneEqual("Knight", "Night", true));
    }

    @Test
    public void initialXEncodesAsSWithAlternateR() {
        DoubleMetaphone dm = new DoubleMetaphone();
        // 'X' inicial -> 'S'; 'V' -> 'F'; 'R' final tiene alternativa
        assertEquals("SF", dm.doubleMetaphone("Xavier", false));
        assertEquals("SFR", dm.doubleMetaphone("Xavier", true));
    }

    @Test
    public void initialCaesarSpecialCase() {
        DoubleMetaphone dm = new DoubleMetaphone();
        assertEquals("SSR", dm.doubleMetaphone("Caesar", false));
    }

    @Test
    public void initialChOfGreekRootEncodesAsK() {
        DoubleMetaphone dm = new DoubleMetaphone();
        assertEquals("KMST", dm.doubleMetaphone("chemistry", false));
    }

    @Test
    public void initialThomasEncodesTWithoutTh() {
        DoubleMetaphone dm = new DoubleMetaphone();
        assertEquals("TMS", dm.doubleMetaphone("Thomas", false));
        assertEquals("TMS", dm.doubleMetaphone("Thomas", true));
    }

    @Test
    public void thInGeneralHasPrimaryZeroAndAlternateT() {
        DoubleMetaphone dm = new DoubleMetaphone();
        // "TH" general -> primario '0', alternativo 'T'; "MB" final -> una sola 'M'
        assertEquals("0M", dm.doubleMetaphone("thumb", false));
        assertEquals("TM", dm.doubleMetaphone("thumb", true));
    }

    @Test
    public void queenStartsWithK() {
        DoubleMetaphone dm = new DoubleMetaphone();
        assertEquals("KN", dm.doubleMetaphone("queen", false));
    }

    @Test
    public void phStartsAsF() {
        DoubleMetaphone dm = new DoubleMetaphone();
        assertEquals("FN", dm.doubleMetaphone("phone", false));
    }

    @Test
    public void doubleFCollapsesToSingleF() {
        DoubleMetaphone dm = new DoubleMetaphone();
        assertEquals("AF", dm.doubleMetaphone("Off", false));
    }

    @Test
    public void doubleBCollapsesToSingleP() {
        DoubleMetaphone dm = new DoubleMetaphone();
        assertEquals("P", dm.doubleMetaphone("BB", false));
    }

    @Test
    public void initialSchoolEncodesSkWithSchlesingerRule() {
        DoubleMetaphone dm = new DoubleMetaphone();
        assertEquals("SKL", dm.doubleMetaphone("School", false));
    }

    @Test
    public void initialZEncodesAsS() {
        DoubleMetaphone dm = new DoubleMetaphone();
        assertEquals("S", dm.doubleMetaphone("Zoe", false));
    }

    // ---------------------------------------------------------------
    // Igualdad de codificaciones
    // ---------------------------------------------------------------

    @Test
    public void differentWordsWithDifferentCodesAreNotEqual() {
        DoubleMetaphone dm = new DoubleMetaphone();
        // "Dog" -> "TK", "Cat" -> "KT"
        assertFalse(dm.isDoubleMetaphoneEqual("Dog", "Cat"));
        assertFalse(dm.isDoubleMetaphoneEqual("Dog", "Cat", true));
    }

    @Test
    public void equalityIsSymmetric() {
        DoubleMetaphone dm = new DoubleMetaphone();
        assertEquals(dm.isDoubleMetaphoneEqual("Knight", "Night"),
                     dm.isDoubleMetaphoneEqual("Night", "Knight"));
        assertEquals(dm.isDoubleMetaphoneEqual("Dog", "Cat"),
                     dm.isDoubleMetaphoneEqual("Cat", "Dog"));
    }

    @Test
    public void equalityIsCaseInsensitive() {
        DoubleMetaphone dm = new DoubleMetaphone();
        assertTrue(dm.isDoubleMetaphoneEqual("KNIGHT", "night"));
        assertTrue(dm.isDoubleMetaphoneEqual("knight", "NiGhT", true));
    }

    // ---------------------------------------------------------------
    // Determinismo
    // ---------------------------------------------------------------

    @Test
    public void encodingIsDeterministicAcrossInstances() {
        DoubleMetaphone a = new DoubleMetaphone();
        DoubleMetaphone b = new DoubleMetaphone();
        String[] words = { "testing", "Schmidt", "Thomas", "Xavier", "chemistry" };
        for (String w : words) {
            assertEquals(a.doubleMetaphone(w, false), b.doubleMetaphone(w, false));
            assertEquals(a.doubleMetaphone(w, true), b.doubleMetaphone(w, true));
        }
    }

    @Test
    public void repeatedEncodingOfSameInstanceIsStable() {
        DoubleMetaphone dm = new DoubleMetaphone();
        String first = dm.doubleMetaphone("antidisestablishmentarianism", true);
        for (int i = 0; i < 5; i++) {
            assertEquals(first, dm.doubleMetaphone("antidisestablishmentarianism", true));
        }
    }
}
