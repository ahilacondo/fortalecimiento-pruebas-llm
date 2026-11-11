# Lang-63 · E1 · repetición 1

- Clase: `org.apache.commons.lang.time.DurationFormatUtils`  ·  cobertura inicial 89.51 % (media)
- Pruebas finales: 22; detectoras (pasan en f, fallan en b): 1
- Cambio de cobertura: líneas 3.97 pp, ramas 4.44 pp

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

## Prueba detectora: `testFormatPeriodAcrossMonthWithTimeFields`

```java
    @Test
    public void testFormatPeriodAcrossMonthWithTimeFields() {
        // 1970-01-01 00:00:00.000 -> 1970-02-01 12:30:45.250 (UTC)
        long start = utcMillis(1970, 0, 1, 0, 0, 0, 0);
        long end = utcMillis(1970, 1, 1, 12, 30, 45, 250);
        assertEquals("31 days 12:30:45.250",
                DurationFormatUtils.formatPeriod(start, end, "d' days 'H:mm:ss.SSS", true, UTC));
    }
```

Mensaje de fallo en la versión con fallo (b):

```
junit.framework.AssertionFailedError: expected:<[31] days 12:30:45.250> but was:<[0] days 12:30:45.250>
	at org.junit.Assert.assertEquals(Assert.java:115)
	at org.junit.Assert.assertEquals(Assert.java:144)
	at org.apache.commons.lang.time.DurationFormatUtilsLLMTest.testFormatPeriodAcrossMonthWithTimeFields(DurationFormatUtilsLLMTest.java:159)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
```

## Veredicto del revisor

- [x] La prueba falla en b por el defecto que corrige el parche
- [ ] Falla por otro motivo (inestable, dependiente del entorno, otra diferencia)
