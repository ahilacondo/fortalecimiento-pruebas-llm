# Codec-4 · E3 · repetición 1

- Clase: `org.apache.commons.codec.binary.Base64`  ·  cobertura inicial 91.94 % (alta)
- Pruebas finales: 25; detectoras (pasan en f, fallan en b): 1
- Cambio de cobertura: líneas 0.0 pp, ramas 1.27 pp

## Corrección del fallo (diff b → f)

```diff
diff -ru b/org/apache/commons/codec/binary/Base64.java f/org/apache/commons/codec/binary/Base64.java
--- b/org/apache/commons/codec/binary/Base64.java	2026-11-10 18:55:46.240246529 -0500
+++ f/org/apache/commons/codec/binary/Base64.java	2026-11-10 18:55:46.593907183 -0500
@@ -222,7 +222,7 @@
      * </p>
      */
     public Base64() {
-        this(false);
+        this(0);
     }
 
     /**
```

## Prueba detectora: `testReusedInstanceEncodeThenDecodeRoundTrip`

```java
    /**
     * Codificar y luego decodificar con la misma instancia debe devolver los datos originales.
     */
    @Test
    public void testReusedInstanceEncodeThenDecodeRoundTrip() {
        Base64 b64 = new Base64();
        byte[] encoded = b64.encode(ascii("ab"));
        assertEquals("YWI=", str(encoded));
        assertEquals("ab", str(b64.decode(encoded)));
    }
```

Mensaje de fallo en la versión con fallo (b):

```
junit.framework.AssertionFailedError: expected:<YWI=[]> but was:<YWI=[
]>
	at org.junit.Assert.assertEquals(Assert.java:115)
	at org.junit.Assert.assertEquals(Assert.java:144)
	at org.apache.commons.codec.binary.Base64LLMTest.testReusedInstanceEncodeThenDecodeRoundTrip(Base64LLMTest.java:153)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

## Veredicto del revisor

- [x] La prueba falla en b por el defecto que corrige el parche
- [ ] Falla por otro motivo (inestable, dependiente del entorno, otra diferencia)
