# Codec-16 · E1 · repetición 1

- Clase: `org.apache.commons.codec.binary.Base32`  ·  cobertura inicial 58.53 % (baja)
- Pruebas finales: 38; detectoras (pasan en f, fallan en b): 1
- Cambio de cobertura: líneas 27.71 pp, ramas 38.67 pp

## Corrección del fallo (diff b → f)

```diff
diff -ru b/org/apache/commons/codec/binary/Base32.java f/org/apache/commons/codec/binary/Base32.java
--- b/org/apache/commons/codec/binary/Base32.java	2026-11-10 19:24:08.717446384 -0500
+++ f/org/apache/commons/codec/binary/Base32.java	2026-11-10 19:24:09.112584446 -0500
@@ -96,7 +96,7 @@
             -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 20-2f
              0,  1,  2,  3,  4,  5,  6,  7,  8,  9, -1, -1, -1, -1, -1, -1, // 30-3f 2-7
             -1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, // 40-4f A-O
-            25, 26, 27, 28, 29, 30, 31, 32,
+            25, 26, 27, 28, 29, 30, 31,                                     // 50-57 P-V
     };
 
     /**
```

## Prueba detectora: `testHexIsInAlphabetRange`

```java
    @Test
    public void testHexIsInAlphabetRange() {
        final Base32 codec = new Base32(true);
        assertTrue(codec.isInAlphabet(octet('0')));
        assertTrue(codec.isInAlphabet(octet('9')));
        assertTrue(codec.isInAlphabet(octet('A')));
        assertTrue(codec.isInAlphabet(octet('V')));
        assertFalse(codec.isInAlphabet(octet('W')));
        assertFalse(codec.isInAlphabet(octet('Z')));
    }
```

Mensaje de fallo en la versión con fallo (b):

```
junit.framework.AssertionFailedError
	at org.junit.Assert.fail(Assert.java:86)
	at org.junit.Assert.assertTrue(Assert.java:41)
	at org.junit.Assert.assertFalse(Assert.java:64)
	at org.junit.Assert.assertFalse(Assert.java:74)
	at org.apache.commons.codec.binary.Base32LLMTest.testHexIsInAlphabetRange(Base32LLMTest.java:121)
```

## Veredicto del revisor

- [ ] La prueba falla en b por el defecto que corrige el parche
- [ ] Falla por otro motivo (inestable, dependiente del entorno, otra diferencia)
