# Muestra de pruebas para calificar la legibilidad (D5)

Califique cada prueba de 1 a 5 con la escala del Anexo D. **No abra `clave_NO_ABRIR.csv` hasta terminar.**

| Puntaje | Criterio |
|---|---|
| 5 | Nombres descriptivos, una idea por prueba, aserciones claras; se podría integrar sin cambios |
| 4 | Se entiende con facilidad; requiere cambios menores de nombres o formato |
| 3 | Se entiende con esfuerzo; las aserciones o el propósito no son del todo claros |
| 2 | Difícil de entender; mezcla varias ideas o usa valores sin explicación |
| 1 | No se entiende qué verifica |

## P01
Clase probada: `org.apache.commons.csv.CSVRecord`

```java
    @Test
    public void testIsMappedIsCaseSensitive() {
        final CSVRecord record = new CSVRecord(VALUES, headerOf("first", "second", "third"), null, 0);
        assertFalse(record.isMapped("First"));
        assertTrue(record.isMapped("first"));
    }
```

## P02
Clase probada: `org.apache.commons.lang3.math.NumberUtils`

```java
    @Test
    public void testCreateNumberLongSuffix() {
        Number n = NumberUtils.createNumber("12L");
        assertTrue(n instanceof Long);
        assertEquals(Long.valueOf(12L), n);
        assertEquals(Long.valueOf(-12L), NumberUtils.createNumber("-12l"));
    }
```

## P03
Clase probada: `org.apache.commons.lang.Entities`

```java
    @Test
    public void testUnescapeLoneAmpersandAndNoSemicolonAreKept() throws Exception {
        assertEquals("&", new Entities().unescape("&"));
        assertEquals("a & b", new Entities().unescape("a & b"));
        assertEquals("&foo", new Entities().unescape("&foo"));
        assertEquals("&", unescapeViaWriter(new Entities(), "&"));
        assertEquals("&foo", unescapeViaWriter(new Entities(), "&foo"));
    }
```

## P04
Clase probada: `org.apache.commons.lang3.time.FastDateFormat`

```java
    @Test
    public void twelveHourClockInAfternoonOrMorning() {
        // 01:46:40 -> "1:46 AM"
        FastDateFormat f = FastDateFormat.getInstance("h:mm a", UTC, Locale.US);
        assertEquals("1:46 AM", f.format(SAMPLE));
    }
```

## P05
Clase probada: `org.apache.commons.cli.Options`

```java
    /**
     * Requisito 7: hasOption ignora hasta dos guiones iniciales en el nombre consultado.
     */
    @Test
    public void testHasOptionIgnoresLeadingHyphens()
    {
        Options options = new Options();
        options.addOption("v", "verbose", false, "verbose output");

        assertTrue("hasOption debe aceptar nombre corto sin guiones", options.hasOption("v"));
        assertTrue("hasOption debe aceptar nombre corto con un guion", options.hasOption("-v"));
        assertTrue("hasOption debe aceptar nombre corto con dos guiones", options.hasOption("--v"));
        assertTrue("hasOption debe aceptar nombre largo sin guiones", options.hasOption("verbose"));
        assertTrue("hasOption debe aceptar nombre largo con un guion", options.hasOption("-verbose"));
        assertTrue("hasOption debe aceptar nombre largo con dos guiones", options.hasOption("--verbose"));
    }
```

## P06
Clase probada: `org.apache.commons.codec.binary.Base64`

```java
    // Suposición: el cuerpo original no estaba disponible. '-' pertenece al alfabeto URL-safe.
    @Test(expected = IllegalArgumentException.class)
    public void testLineSeparatorWithUrlSafeCharacterIsRejected() {
        new Base64(16, new byte[] {'-'});
    }
```

## P07
Clase probada: `org.apache.commons.csv.CSVFormat`

```java
    @Test
    public void testPostgresqlCsvConfiguration() {
        final CSVFormat f = CSVFormat.POSTGRESQL_CSV;
        assertEquals(',', f.getDelimiter());
        assertEquals(Character.valueOf('"'), f.getQuoteCharacter());
        assertEquals(Character.valueOf('"'), f.getEscapeCharacter());
        assertTrue(f.isEscapeCharacterSet());
        assertTrue(f.isNullStringSet());
        assertEquals("", f.getNullString());
        assertEquals(QuoteMode.ALL_NON_NULL, f.getQuoteMode());
        assertEquals("\n", f.getRecordSeparator());
    }
```

## P08
Clase probada: `org.apache.commons.codec.language.Soundex`

```java
    @Test
    public void testEncodeEmptyStringIsNotDefaultZeroCode() {
        final String result = new Soundex().encode("");
        Assert.assertNotEquals("0000", result);
        Assert.assertEquals(0, result.length());
    }
```

## P09
Clase probada: `org.apache.commons.lang3.StringUtils`

```java
    @Test
    public void testStartsEndsWith() {
        assertTrue(StringUtils.startsWith("abc", "ab"));
        assertTrue(StringUtils.startsWithIgnoreCase("ABC", "ab"));
        assertTrue(StringUtils.endsWith("abc", "bc"));
        assertFalse(StringUtils.endsWith("abc", null));
    }
```

## P10
Clase probada: `org.apache.commons.csv.CSVPrinter`

```java
    @Test(expected = IllegalArgumentException.class)
    public void testDelimiterEqualToQuoteCharIsRejected() throws Exception {
        new CSVPrinter(new StringWriter(), CSVFormat.DEFAULT.withDelimiter('"'));
    }
```

## P11
Clase probada: `org.apache.commons.cli.OptionGroup`

```java
    @Test
    public void testAddOptionReturnsSameGroupInstance()
    {
        OptionGroup group = new OptionGroup();

        OptionGroup returned = group.addOption(shortOpt("a", "alpha"));

        assertSame("addOption debe devolver el mismo grupo (fluent)", group, returned);
    }
```

## P12
Clase probada: `org.apache.commons.codec.binary.Base32`

```java
    @Test
    public void testSeparatorContainingAlphabetCharacterThrows() {
        try {
            new Base32(10, bytes("A"));
            fail("Se esperaba IllegalArgumentException por separador con carácter del alfabeto");
        } catch (final IllegalArgumentException expected) {
            assertTrue(expected.getMessage() != null);
        }
    }
```

## P13
Clase probada: `org.apache.commons.cli.Options`

```java
    @Test
    public void testAddOptionGroupMarksMembersNotRequired()
    {
        Options opts = new Options();
        Option a = new Option("a", "alpha", false, "alpha");
        a.setRequired(true);
        OptionGroup group = new OptionGroup();
        group.addOption(a);
        group.setRequired(true);

        opts.addOptionGroup(group);

        assertFalse("option inside a group cannot be required on its own", a.isRequired());
        assertTrue("option must still be registered", opts.hasOption("a"));
    }
```

## P14
Clase probada: `org.apache.commons.lang.ClassUtils`

```java
    @Test
    public void getAllSuperclasses_object_returnsEmptyList() {
        List<Class<?>> list = ClassUtils.getAllSuperclasses(Object.class);
        assertNotNull(list);
        assertTrue("Object no tiene superclases", list.isEmpty());
    }
```

## P15
Clase probada: `org.apache.commons.lang3.time.FastDateParser`

```java
    @Test
    public void testTwoDigitYearOfCurrentYearMapsToCurrentYear() throws ParseException {
        int thisYear = Calendar.getInstance(UTC, US).get(Calendar.YEAR);
        String yy = String.format("%02d", thisYear % 100);
        FastDateParser parser = new FastDateParser("yy-MM-dd", UTC, US);
        Calendar parsed = Calendar.getInstance(UTC, US);
        parsed.setTime(parser.parse(yy + "-01-01"));
        assertEquals(thisYear, parsed.get(Calendar.YEAR));
    }
```

## P16
Clase probada: `org.apache.commons.codec.binary.StringUtils`

```java
    @Test
    public void testUtf16LeEncodingOfEAcute() {
        // "é" (U+00E9) en UTF-16LE es E9 00
        Assert.assertArrayEquals(new byte[] {(byte) 0xE9, 0x00}, StringUtils.getBytesUtf16Le("\u00e9"));
    }
```

## P17
Clase probada: `org.apache.commons.lang3.builder.HashCodeBuilder`

```java
    @Test
    public void excludedFieldNameOnlyInSuperclassLeavesSubclassFieldsIntact() {
        ShadowLeaf leaf = new ShadowLeaf();
        // "other" se excluye; quedan code=2 de ShadowLeaf y code=5 de ShadowBase.
        int expected = (17 * 37 + 2) * 37 + 5;
        assertEquals(expected,
                HashCodeBuilder.reflectionHashCode(17, 37, leaf, false, null, new String[] {"other"}));
    }
```

## P18
Clase probada: `org.apache.commons.codec.binary.Base64`

```java
    @Test
    public void testSingleByteFBEncodings() {
        assertEquals("+w==", str(Base64.encodeBase64(new byte[] {(byte) 0xfb})));
        assertEquals("-w", str(Base64.encodeBase64URLSafe(new byte[] {(byte) 0xfb})));
    }
```

## P19
Clase probada: `org.apache.commons.csv.CSVFormat`

```java
    @Test
    public void informixUnloadCsvUsesDoubleQuote() {
        assertEquals(Character.valueOf('"'), CSVFormat.INFORMIX_UNLOAD_CSV.getQuoteCharacter());
    }
```

## P20
Clase probada: `org.apache.commons.cli.PosixParser`

```java
    @Test
    public void testDoubleDashStopsOptionProcessing() throws Exception
    {
        CommandLine cl = parser.parse(options, new String[] { "--", "-a" });

        assertFalse("-a no debe procesarse tras '--'", cl.hasOption("a"));
        assertEquals(Arrays.asList("-a"), cl.getArgList());
    }
```
