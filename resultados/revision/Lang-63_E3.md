# Lang-63 · E3 · repetición 1

- Clase: `org.apache.commons.lang.time.DurationFormatUtils`  ·  cobertura inicial 89.51 % (media)
- Pruebas finales: 19; detectoras (pasan en f, fallan en b): 2
- Cambio de cobertura: líneas 3.97 pp, ramas 3.7 pp

## Corrección del fallo (diff b → f)

```diff
diff -ru b/org/apache/commons/lang/time/DurationFormatUtils.java f/org/apache/commons/lang/time/DurationFormatUtils.java
--- b/org/apache/commons/lang/time/DurationFormatUtils.java	2026-11-10 21:51:59.500115080 -0500
+++ f/org/apache/commons/lang/time/DurationFormatUtils.java	2026-11-10 21:52:03.254456567 -0500
@@ -303,25 +303,20 @@
             days -= 1;
         }
         while (days < 0) {
-            days += 31;
+            end.add(Calendar.MONTH, -1);
+            days += end.getActualMaximum(Calendar.DAY_OF_MONTH);
 //days += 31; // TODO: Need tests to show this is bad and the new code is good.
 // HEN: It's a tricky subject. Jan 15th to March 10th. If I count days-first it is 
 // 1 month and 26 days, but if I count month-first then it is 1 month and 23 days.
 // Also it's contextual - if asked for no M in the format then I should probably 
 // be doing no calculating here.
             months -= 1;
+            end.add(Calendar.MONTH, 1);
         }
         while (months < 0) {
             months += 12;
             years -= 1;
         }
-        milliseconds -= reduceAndCorrect(start, end, Calendar.MILLISECOND, milliseconds);
-        seconds -= reduceAndCorrect(start, end, Calendar.SECOND, seconds);
-        minutes -= reduceAndCorrect(start, end, Calendar.MINUTE, minutes);
-        hours -= reduceAndCorrect(start, end, Calendar.HOUR_OF_DAY, hours);
-        days -= reduceAndCorrect(start, end, Calendar.DAY_OF_MONTH, days);
-        months -= reduceAndCorrect(start, end, Calendar.MONTH, months);
-        years -= reduceAndCorrect(start, end, Calendar.YEAR, years);
 
         // This next block of code adds in values that 
         // aren't requested. This allows the user to ask for the 
@@ -429,18 +424,6 @@
         }
         return buffer.toString();
     }
-    static int reduceAndCorrect(Calendar start, Calendar end, int field, int difference) {
-        end.add( field, -1 * difference );
-        int endValue = end.get(field);
-        int startValue = start.get(field);
-        if (endValue < startValue) {
-            int newdiff = startValue - endValue;
-            end.add( field, newdiff );
-            return newdiff;
-        } else {
-            return 0;
-        }
-    }
 
     static final Object y = "y";
     static final Object M = "M";
```

## Prueba detectora: `testFormatPeriodDefaultOverloadMatchesExplicitDefaultTimeZone`

```java
    @Test
    public void testFormatPeriodDefaultOverloadMatchesExplicitDefaultTimeZone() {
        Calendar start = Calendar.getInstance();
        start.clear();
        start.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
        Calendar end = Calendar.getInstance();
        end.clear();
        end.set(2000, Calendar.MARCH, 1, 0, 0, 0);

        long s = start.getTimeInMillis();
        long e = end.getTimeInMillis();

        // 60 días entre 1 de enero y 1 de marzo de 2000 (año bisiesto)
        String viaDefaultOverload = DurationFormatUtils.formatPeriod(s, e, "d");
        String viaExplicit = DurationFormatUtils.formatPeriod(s, e, "d", true, TimeZone.getDefault());
        assertEquals(viaExplicit, viaDefaultOverload);
        assertEquals("60", viaDefaultOverload);
    }
```

Mensaje de fallo en la versión con fallo (b):

```
junit.framework.AssertionFailedError: expected:<[6]0> but was:<[]0>
	at org.junit.Assert.assertEquals(Assert.java:115)
	at org.junit.Assert.assertEquals(Assert.java:144)
	at org.apache.commons.lang.time.DurationFormatUtilsLLMTest.testFormatPeriodDefaultOverloadMatchesExplicitDefaultTimeZone(DurationFormatUtilsLLMTest.java:133)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
```

## Prueba detectora: `testFormatPeriodExactly28DaysUsesCalendarPath`

```java
    @Test
    public void testFormatPeriodExactly28DaysUsesCalendarPath() {
        long start = 0L;
        long end = start + 28L * MILLIS_PER_DAY;
        // Con 28 días exactos se usa el cálculo calendario (no formatDuration, que daría "28").
        // Resultado observado: 28 (diferencia de día del mes) + 28 (diferencia de día del año).
        assertEquals("56", DurationFormatUtils.formatPeriod(start, end, "d", true, utc()));
        assertFalse("En la frontera no se debe delegar en formatDuration",
                "28".equals(DurationFormatUtils.formatPeriod(start, end, "d", true, utc())));
    }
```

Mensaje de fallo en la versión con fallo (b):

```
junit.framework.AssertionFailedError: expected:<[56]> but was:<[28]>
	at org.junit.Assert.assertEquals(Assert.java:115)
	at org.junit.Assert.assertEquals(Assert.java:144)
	at org.apache.commons.lang.time.DurationFormatUtilsLLMTest.testFormatPeriodExactly28DaysUsesCalendarPath(DurationFormatUtilsLLMTest.java:167)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
```

## Veredicto del revisor

- [ ] La prueba falla en b por el defecto que corrige el parche
- [ ] Falla por otro motivo (inestable, dependiente del entorno, otra diferencia)
