# Lang-33 · E4 · repetición 1

- Clase: `org.apache.commons.lang3.ClassUtils`  ·  cobertura inicial 91.52 % (alta)
- Pruebas finales: 12; detectoras (pasan en f, fallan en b): 1
- Cambio de cobertura: líneas 3.85 pp, ramas 2.78 pp

## Corrección del fallo (diff b → f)

```diff
diff -ru b/org/apache/commons/lang3/ClassUtils.java f/org/apache/commons/lang3/ClassUtils.java
--- b/org/apache/commons/lang3/ClassUtils.java	2026-11-10 21:52:40.984081939 -0500
+++ f/org/apache/commons/lang3/ClassUtils.java	2026-11-10 21:52:44.688082178 -0500
@@ -907,7 +907,7 @@
         }
         Class<?>[] classes = new Class[array.length];
         for (int i = 0; i < array.length; i++) {
-            classes[i] = array[i].getClass();
+            classes[i] = array[i] == null ? null : array[i].getClass();
         }
         return classes;
     }
```

## Prueba detectora: `testToClass_ElementoNulo_InsertaNull`

```java
    // ------------------------------------------------------------------
    // Requisito 1: toClass inserta null para elementos nulos
    // ------------------------------------------------------------------
    @Test
    public void testToClass_ElementoNulo_InsertaNull() {
        Object[] input = {"texto", null, Integer.valueOf(3)};
        Class<?>[] result = ClassUtils.toClass(input);

        assertNotNull("El resultado no debe ser null para un array no nulo", result);
        assertEquals("La longitud debe coincidir con la entrada", 3, result.length);
        assertEquals(String.class, result[0]);
        assertNull("El elemento nulo debe convertirse en null", result[1]);
        assertEquals(Integer.class, result[2]);
    }
```

Mensaje de fallo en la versión con fallo (b):

```
java.lang.NullPointerException
	at org.apache.commons.lang3.ClassUtils.toClass(ClassUtils.java:910)
	at org.apache.commons.lang3.ClassUtilsLLMTest.testToClass_ElementoNulo_InsertaNull(ClassUtilsLLMTest.java:64)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
```

## Veredicto del revisor

- [x] La prueba falla en b por el defecto que corrige el parche
- [ ] Falla por otro motivo (inestable, dependiente del entorno, otra diferencia)
