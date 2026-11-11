# Codec-18 · E1 · repetición 1

- Clase: `org.apache.commons.codec.binary.StringUtils`  ·  cobertura inicial 79.81 % (media)
- Pruebas finales: 40; detectoras (pasan en f, fallan en b): 1
- Cambio de cobertura: líneas 15.38 pp, ramas 20.0 pp

## Corrección del fallo (diff b → f)

```diff
diff -ru b/org/apache/commons/codec/binary/StringUtils.java f/org/apache/commons/codec/binary/StringUtils.java
--- b/org/apache/commons/codec/binary/StringUtils.java	2026-11-10 19:02:30.631991980 -0500
+++ f/org/apache/commons/codec/binary/StringUtils.java	2026-11-10 19:02:31.074260888 -0500
@@ -78,7 +78,7 @@
         if (cs1 instanceof String && cs2 instanceof String) {
             return cs1.equals(cs2);
         }
-        return CharSequenceUtils.regionMatches(cs1, false, 0, cs2, 0, Math.max(cs1.length(), cs2.length()));
+        return cs1.length() == cs2.length() && CharSequenceUtils.regionMatches(cs1, false, 0, cs2, 0, cs1.length());
     }
 
     /**
```

## Prueba detectora: `testEqualsStringBuilderPrefixIsNotEqual`

```java
    @Test
    public void testEqualsStringBuilderPrefixIsNotEqual() {
        Assert.assertFalse(StringUtils.equals(new StringBuilder("abc"), "abcd"));
        Assert.assertFalse(StringUtils.equals("abcd", new StringBuilder("abc")));
    }
```

Mensaje de fallo en la versión con fallo (b):

```
java.lang.StringIndexOutOfBoundsException: index 3,length 3
	at java.base/java.lang.String.checkIndex(String.java:3278)
	at java.base/java.lang.AbstractStringBuilder.charAt(AbstractStringBuilder.java:307)
	at java.base/java.lang.StringBuilder.charAt(StringBuilder.java:89)
	at org.apache.commons.codec.binary.CharSequenceUtils.regionMatches(CharSequenceUtils.java:59)
	at org.apache.commons.codec.binary.StringUtils.equals(StringUtils.java:81)
```

## Veredicto del revisor

- [x] La prueba falla en b por el defecto que corrige el parche
- [ ] Falla por otro motivo (inestable, dependiente del entorno, otra diferencia)
