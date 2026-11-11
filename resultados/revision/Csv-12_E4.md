# Csv-12 · E4 · repetición 1

- Clase: `org.apache.commons.csv.CSVFormat`  ·  cobertura inicial 74.03 % (media)
- Pruebas finales: 30; detectoras (pasan en f, fallan en b): 1
- Cambio de cobertura: líneas 1.63 pp, ramas 0.0 pp

## Corrección del fallo (diff b → f)

```diff
diff -ru b/org/apache/commons/csv/CSVFormat.java f/org/apache/commons/csv/CSVFormat.java
--- b/org/apache/commons/csv/CSVFormat.java	2026-11-10 19:08:06.001987621 -0500
+++ f/org/apache/commons/csv/CSVFormat.java	2026-11-10 19:08:08.684393396 -0500
@@ -216,7 +216,7 @@
      * Note: this is currently like {@link #RFC4180} plus {@link #withAllowMissingColumnNames(boolean) withAllowMissingColumnNames(true)}.
      * </p>
      */
-    public static final CSVFormat EXCEL = DEFAULT.withIgnoreEmptyLines(false);
+    public static final CSVFormat EXCEL = DEFAULT.withIgnoreEmptyLines(false).withAllowMissingColumnNames(true);
 
     /**
      * Tab-delimited format.
```

## Prueba detectora: `testExcelAllowsMissingColumnNames`

```java
    @Test
    public void testExcelAllowsMissingColumnNames() {
        assertTrue(CSVFormat.EXCEL.getAllowMissingColumnNames());
    }
```

Mensaje de fallo en la versión con fallo (b):

```
junit.framework.AssertionFailedError
	at org.junit.Assert.fail(Assert.java:86)
	at org.junit.Assert.assertTrue(Assert.java:41)
	at org.junit.Assert.assertTrue(Assert.java:52)
	at org.apache.commons.csv.CSVFormatLLMTest.testExcelAllowsMissingColumnNames(CSVFormatLLMTest.java:89)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
```

## Veredicto del revisor

- [x] La prueba falla en b por el defecto que corrige el parche
- [ ] Falla por otro motivo (inestable, dependiente del entorno, otra diferencia)
