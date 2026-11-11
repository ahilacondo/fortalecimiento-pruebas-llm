# Codec-17 · E4 · repetición 1

- Clase: `org.apache.commons.codec.binary.StringUtils`  ·  cobertura inicial 49.62 % (baja)
- Pruebas finales: 34; detectoras (pasan en f, fallan en b): 1
- Cambio de cobertura: líneas 30.77 pp, ramas 65.0 pp

## Corrección del fallo (diff b → f)

```diff
diff -ru b/org/apache/commons/codec/binary/StringUtils.java f/org/apache/commons/codec/binary/StringUtils.java
--- b/org/apache/commons/codec/binary/StringUtils.java	2026-11-10 19:30:12.030749055 -0500
+++ f/org/apache/commons/codec/binary/StringUtils.java	2026-11-10 19:30:12.474748994 -0500
@@ -336,7 +336,7 @@
      * @since As of 1.7, throws {@link NullPointerException} instead of UnsupportedEncodingException
      */
     public static String newStringIso8859_1(final byte[] bytes) {
-        return new String(bytes, Charsets.ISO_8859_1);
+        return newString(bytes, Charsets.ISO_8859_1);
     }
 
     /**
```

## Prueba detectora: `testNewStringIso8859_1NullInputReturnsNull`

```java
    @Test
    public void testNewStringIso8859_1NullInputReturnsNull() {
        Assert.assertNull(StringUtils.newStringIso8859_1(null));
    }
```

Mensaje de fallo en la versión con fallo (b):

```
java.lang.NullPointerException
	at java.base/java.lang.String.<init>(String.java:561)
	at org.apache.commons.codec.binary.StringUtils.newStringIso8859_1(StringUtils.java:339)
	at org.apache.commons.codec.binary.StringUtilsLLMTest.testNewStringIso8859_1NullInputReturnsNull(StringUtilsLLMTest.java:203)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
```

## Veredicto del revisor

- [ ] La prueba falla en b por el defecto que corrige el parche
- [ ] Falla por otro motivo (inestable, dependiente del entorno, otra diferencia)
