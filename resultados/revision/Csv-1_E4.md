# Csv-1 · E4 · repetición 1

- Clase: `org.apache.commons.csv.ExtendedBufferedReader`  ·  cobertura inicial 69.16 % (media)
- Pruebas finales: 24; detectoras (pasan en f, fallan en b): 1
- Cambio de cobertura: líneas 10.81 pp, ramas 31.82 pp

## Corrección del fallo (diff b → f)

```diff
diff -ru b/org/apache/commons/csv/ExtendedBufferedReader.java f/org/apache/commons/csv/ExtendedBufferedReader.java
--- b/org/apache/commons/csv/ExtendedBufferedReader.java	2026-11-10 18:58:15.443234188 -0500
+++ f/org/apache/commons/csv/ExtendedBufferedReader.java	2026-11-10 18:58:16.954424046 -0500
@@ -55,7 +55,7 @@
     @Override
     public int read() throws IOException {
         int current = super.read();
-        if (current == '\n') {
+        if (current == '\r' || (current == '\n' && lastChar != '\r')) {
             lineCounter++;
         }
         lastChar = current;
```

## Prueba detectora: `readIncrementsLineCounterOnCarriageReturn`

```java
     * Requisito 5 (CR): según la documentación, read() debe contar el CR como
     * terminador de línea. La implementación actual solo cuenta '\n' en read(),
     * por lo que esta prueba falla con el código actual (relacionado con CSV-75).
     */
    @Test
    public void readIncrementsLineCounterOnCarriageReturn() throws Exception {
        ExtendedBufferedReader br = getBufferedReader("a\rb");
        assertEquals('a', br.read());
        assertEquals(0, br.getLineNumber());
        assertEquals('\r', br.read());
        assertEquals(1, br.getLineNumber());
        assertEquals('b', br.read());
        assertEquals(1, br.getLineNumber());
    }
```

Mensaje de fallo en la versión con fallo (b):

```
junit.framework.AssertionFailedError: expected:<1> but was:<0>
	at org.junit.Assert.fail(Assert.java:88)
	at org.junit.Assert.failNotEquals(Assert.java:834)
	at org.junit.Assert.assertEquals(Assert.java:645)
	at org.junit.Assert.assertEquals(Assert.java:631)
	at org.apache.commons.csv.ExtendedBufferedReaderLLMTest.readIncrementsLineCounterOnCarriageReturn(ExtendedBufferedReaderLLMTest.java:137)
```

## Veredicto del revisor

- [x] La prueba falla en b por el defecto que corrige el parche
- [ ] Falla por otro motivo (inestable, dependiente del entorno, otra diferencia)
