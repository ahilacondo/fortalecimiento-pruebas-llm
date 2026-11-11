# Codec-4 · E1 · repetición 1

- Clase: `org.apache.commons.codec.binary.Base64`  ·  cobertura inicial 91.94 % (alta)
- Pruebas finales: 35; detectoras (pasan en f, fallan en b): 1
- Cambio de cobertura: líneas 0.0 pp, ramas 0.0 pp

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

## Prueba detectora: `testEncodeToStringWithInstance`

```java
    @Test
    public void testEncodeToStringWithInstance() {
        Base64 b64 = new Base64();
        assertEquals("SGVsbG8=", b64.encodeToString(ascii("Hello")));
    }
```

Mensaje de fallo en la versión con fallo (b):

```
junit.framework.AssertionFailedError: expected:<SGVsbG8=[]> but was:<SGVsbG8=[
]>
	at org.junit.Assert.assertEquals(Assert.java:115)
	at org.junit.Assert.assertEquals(Assert.java:144)
	at org.apache.commons.codec.binary.Base64LLMTest.testEncodeToStringWithInstance(Base64LLMTest.java:82)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

## Veredicto del revisor

- [ ] La prueba falla en b por el defecto que corrige el parche
- [ ] Falla por otro motivo (inestable, dependiente del entorno, otra diferencia)
