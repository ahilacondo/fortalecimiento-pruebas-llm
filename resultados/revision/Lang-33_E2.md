# Lang-33 · E2 · repetición 1

- Clase: `org.apache.commons.lang3.ClassUtils`  ·  cobertura inicial 91.52 % (alta)
- Pruebas finales: 21; detectoras (pasan en f, fallan en b): 1
- Cambio de cobertura: líneas 3.15 pp, ramas 2.78 pp

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

## Prueba detectora: `toClassReturnsClassesOfElementsAndNullForNullElements`

```java
    @Test
    public void toClassReturnsClassesOfElementsAndNullForNullElements() {
        assertArrayEquals(new Class<?>[] { String.class, Integer.class },
                ClassUtils.toClass(new Object[] { "a", 1 }));
        assertArrayEquals(new Class<?>[] { null, String.class },
                ClassUtils.toClass(new Object[] { null, "a" }));
    }
```

Mensaje de fallo en la versión con fallo (b):

```
java.lang.NullPointerException
	at org.apache.commons.lang3.ClassUtils.toClass(ClassUtils.java:910)
	at org.apache.commons.lang3.ClassUtilsLLMTest.toClassReturnsClassesOfElementsAndNullForNullElements(ClassUtilsLLMTest.java:203)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
```

## Veredicto del revisor

- [x] La prueba falla en b por el defecto que corrige el parche
- [ ] Falla por otro motivo (inestable, dependiente del entorno, otra diferencia)
