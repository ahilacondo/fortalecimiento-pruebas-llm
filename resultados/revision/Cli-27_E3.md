# Cli-27 · E3 · repetición 1

- Clase: `org.apache.commons.cli.OptionGroup`  ·  cobertura inicial 92.89 % (alta)
- Pruebas finales: 25; detectoras (pasan en f, fallan en b): 1
- Cambio de cobertura: líneas 5.88 pp, ramas 8.33 pp

## Corrección del fallo (diff b → f)

```diff
diff -ru b/org/apache/commons/cli/OptionGroup.java f/org/apache/commons/cli/OptionGroup.java
--- b/org/apache/commons/cli/OptionGroup.java	2026-11-10 21:53:46.356071020 -0500
+++ f/org/apache/commons/cli/OptionGroup.java	2026-11-10 21:53:47.696070750 -0500
@@ -95,9 +95,9 @@
         // if no option has already been selected or the 
         // same option is being reselected then set the
         // selected member variable
-        if (selected == null || selected.equals(option.getOpt()))
+        if (selected == null || selected.equals(option.getKey()))
         {
-            selected = option.getOpt();
+            selected = option.getKey();
         }
         else
         {
```

## Prueba detectora: `reselectingSameOptionByLongNameDoesNotThrow`

```java
    @Test
    public void reselectingSameOptionByLongNameDoesNotThrow() throws Exception {
        OptionGroup group = new OptionGroup();
        Option importOpt = new Option(null, "import", false, "import data");
        group.addOption(importOpt);

        group.setSelected(importOpt);
        group.setSelected(importOpt);

        assertEquals("import", group.getSelected());
    }
```

Mensaje de fallo en la versión con fallo (b):

```
junit.framework.AssertionFailedError: expected:<import> but was:<null>
	at org.junit.Assert.fail(Assert.java:88)
	at org.junit.Assert.failNotEquals(Assert.java:834)
	at org.junit.Assert.assertEquals(Assert.java:118)
	at org.junit.Assert.assertEquals(Assert.java:144)
	at org.apache.commons.cli.OptionGroupLLMTest.reselectingSameOptionByLongNameDoesNotThrow(OptionGroupLLMTest.java:83)
```

## Veredicto del revisor

- [x] La prueba falla en b por el defecto que corrige el parche
- [ ] Falla por otro motivo (inestable, dependiente del entorno, otra diferencia)
