# Guía para llenar el informe del Hito 2 con los resultados finales

Documento de trabajo para quien pase los resultados al Word (`A.6_HITO2_...docx`). Sigue **la estructura que ya tiene el informe** (la del índice): no se agregan secciones nuevas, solo se actualizan las que existen. Todas las cifras salen de `resultados/ejecuciones.csv` (160 corridas, 40 casos, 1 repetición, Claude Haiku 5.5) y de las tablas generadas por `analisis/analizar.py` y `analisis/ampliado.py`. Si cambia algún dato, vuelvan a correr los dos scripts y actualicen este texto.

## 0. Antes de empezar: tres arreglos de estructura

1. **Falta el apartado 5.1 en el cuerpo.** El índice dice *5.1 Resultados de la fase 1: revisión de la literatura* y *5.2 Resultados del experimento*, pero el cuerpo solo tiene «5.1 Resultados del experimento». Cambien ese título a **5.2 Resultados del experimento** y agreguen antes un **5.1** con el texto de la sección 1 de esta guía.
2. **Figuras repetidas en el Anexo F.** Hay dos «Figura 12», dos «Figura 13» y dos «Figura 14» (las de Parsifal y las de Scopus). Renumérenlas en secuencia.
3. **Las secciones 5.3, 5.4 y 5.5** ya tienen el número correcto. Dentro de 5.4 hay referencias a «sección 5.3» y «Tabla 29»: se mantienen.

Al terminar, en Word: *Referencias → Actualizar tabla* para el índice y leer el documento completo.

## 1. Sección 5.1 (nueva): Resultados de la fase 1

Un párrafo corto que remite a la sección 2, sin datos nuevos:

> La revisión focalizada de la literatura (sección 2) siguió el protocolo registrado en Parsifal, con búsquedas en Scopus y Web of Science. Se analizaron 11 estudios, todos publicados entre 2024 y 2026. Los estudios ubican las debilidades de una suite de pruebas de seis maneras distintas y ninguna domina. Como cada estudio usa su propio banco de pruebas, métricas y línea base, no es posible afirmar cuál es la más efectiva. Ese vacío motiva el experimento que se presenta en la sección 5.2.

## 2. Sección 5.2 Resultados del experimento

### 2.1 Párrafo de apertura (reemplaza el actual)

> Las tablas siguientes presentan los resultados de la ejecución del experimento con 40 casos (10 de cada proyecto: Lang, Cli, Codec y Csv) y 4 estrategias (160 corridas, una repetición por combinación; ver limitaciones en la sección 5.4). Cada tabla indica la métrica y el contraste estadístico que le corresponden según la sección 3.

### 2.2 Tabla 25. Detección de fallos reales por estrategia
Fuente: `resultados/analisis_base/tabla_r1_deteccion.csv`.

| Estrategia | Casos con fallo detectado | Tasa media ± DE | IC 95 % | Pruebas nuevas por caso (mediana) |
|---|---|---|---|---|
| E1 Sin guía | 4 de 40 | 0,100 ± 0,304 | [0,025; 0,200] | 30,5 |
| E2 Cobertura | 3 de 40 | 0,075 ± 0,267 | [0,000; 0,175] | 21,5 |
| E3 Análisis de código y pruebas | 4 de 40 | 0,100 ± 0,304 | [0,025; 0,200] | 17,0 |
| E4 Documentación | 6 de 40 | 0,150 ± 0,362 | [0,050; 0,275] | 17,0 |

**Figura 9** (reemplaza la actual): `resultados/analisis_ampliado/fig_informe_09_deteccion.png`, ancho 446 pt. Pie: *Figura 9. Casos con fallo detectado por estrategia, con intervalo de confianza de 95 % (a) y por proyecto (b)*. Cambiar la frase que la introduce a «… con su intervalo de confianza y su desglose por proyecto».

### 2.3 Tabla 26. Tasa de detección por cobertura inicial
Fuente: `resultados/analisis_base/tabla_r2_cobertura.csv`.

| Estrategia | Baja (< 60 %) | Media (60 a 90 %) | Alta (≥ 90 %) |
|---|---|---|---|
| N.º de casos del grupo | 3 | 23 | 14 |
| E1 Sin guía | 0,333 | 0,087 | 0,071 |
| E2 Cobertura | 0,333 | 0,043 | 0,071 |
| E3 Análisis de código y pruebas | 0,000 | 0,087 | 0,143 |
| E4 Documentación | 0,667 | 0,130 | 0,071 |

### 2.4 Tabla 27. Validez, costo y legibilidad
Fuente: `resultados/analisis_base/tabla_r3_validez_costo.csv`. La columna de legibilidad queda «—» si no se evalúa.

| Estrategia | Compilan (%) | Estables (%) | Falsos positivos (%) | Llamadas por caso | Tokens por caso | Tiempo (s) | Legibilidad (1 a 5) |
|---|---|---|---|---|---|---|---|
| E1 | 95,0 | 100,0 | 1,5 | 1,4 | 39 158 | 69,9 | — |
| E2 | 90,0 | 100,0 | 2,0 | 1,4 | 37 214 | 74,6 | — |
| E3 | 100,0 | 100,0 | 3,4 | 2,3 | 63 191 | 81,3 | — |
| E4 | 97,5 | 100,0 | 4,5 | 2,4 | 68 412 | 82,7 | — |

**Figura 10** (reemplaza la actual): `resultados/analisis_ampliado/fig_informe_10_cobertura_y_costo.png`, ancho 446 pt. Pie: *Figura 10. Aumento de cobertura de la clase objetivo (a) y costo en tokens frente a casos detectados (b), según la estrategia*.

### 2.5 Tabla 28. Contrastes estadísticos
Fuente: `resultados/analisis_base/tabla_r4_contrastes.csv`. Cambiar «n = 20» por **n = 40** y «n = 7» por **n = 14** en la columna de interpretación.

| Contraste | Estadístico | p ajustado (Holm) | Â12 | Interpretación |
|---|---|---|---|---|
| Friedman: E1 a E4 | 2,111 | — | — | no se detectan diferencias |
| Wilcoxon: E2 frente a E1 | 2,0 | 1,0000 | 0,487 | no significativa; efecto despreciable (n = 40) |
| Wilcoxon: E3 frente a E1 | 1,5 | 1,0000 | 0,500 | no significativa; efecto despreciable (n = 40) |
| Wilcoxon: E4 frente a E1 | 7,0 | 1,0000 | 0,525 | no significativa; efecto despreciable (n = 40) |
| Wilcoxon: E3 frente a E2 | 6,0 | 1,0000 | 0,512 | no significativa; efecto despreciable (n = 40) |
| Wilcoxon: E4 frente a E2 | 0,0 | 0,5829 | 0,537 | no significativa; efecto despreciable (n = 40) |
| Cobertura alta: E3 frente a E2 | 2,0 | 1,0000 | 0,536 | no significativa; efecto despreciable (n = 14) |
| Cobertura alta: E4 frente a E2 | — | 1,0000 | 0,500 | no significativa; efecto despreciable (n = 14) |

## 3. Sección 5.3 Análisis de los casos sin mejora

### 3.1 Tabla 29. Causas de los casos sin detección (31 casos)
Fuente: `resultados/analisis_base/tabla_r5_causas.csv`.

| Causa | N.º de casos |
|---|---|
| No compila | 1 |
| Pruebas inestables | 0 |
| Falsos positivos | 0 |
| Sin diferencia | 30 |
| Límite de intentos o costo | 0 |

### 3.2 Párrafo bajo la Figura 11 (reemplaza al actual)
La tabla cuenta casos en los que *ninguna* estrategia detectó. Para no esconder los problemas por corrida, el texto debe dar la cifra completa:

> La Figura 11 muestra el resultado de cada una de las 160 corridas, caso por caso y estrategia por estrategia. De las 160, 17 detectaron el fallo (10,6 %); 136 produjeron pruebas válidas que pasan en ambas versiones y no ejercitan el comportamiento del fallo («sin diferencia», 85,0 %); 5 no compilaron tras las dos correcciones permitidas; y 2 terminaron sin respuesta (Cli-38 con E2 y Csv-14 con E2) porque el modelo agotó el máximo de tokens, incluso al subirlo a 20.000 y repetirlas. La detección se concentra en pocos casos: solo 9 de los 40 fueron detectados por alguna estrategia.

**Figura 11** (reemplaza la actual): `resultados/analisis_ampliado/fig_A_mapa_de_resultados.png`, ancho 400 pt. Pie: *Figura 11. Resultado de cada corrida por caso y estrategia (✔ detectó el fallo; C no compila; T sin respuesta por tope de tokens)*.

## 4. Sección 5.4 Discusión

Mantengan los cuatro criterios de interpretación del inicio. Reemplacen los párrafos PE1 a PE4, «Cobertura» y «Limitaciones» por los siguientes, y agreguen los cuatro párrafos nuevos **después de PE4 y antes de «Cobertura»**.

**PE1.** El fallo real se detectó en 4 de 40 casos con E1, 3 con E2, 4 con E3 y 6 con E4 (Tabla 25 y Figura 9). La detección es baja con las cuatro estrategias: en 85 % de las corridas las pruebas generadas pasan tanto en la versión corregida como en la versión con el fallo (Figura 11), y en 31 de los 40 casos ninguna estrategia detectó el fallo (Tabla 29). Claude genera muchas pruebas válidas, pero casi ninguna ejercita el comportamiento defectuoso. E4 es la estrategia con más casos detectados (6 de 40, frente a 3 o 4 de las otras), pero los intervalos de confianza de 95 % de las cuatro estrategias se traslapan (Figura 9a).

**PE2.** La prueba de Friedman no detecta diferencias entre las estrategias (estadístico = 2,111, p = 0,550), y ninguna comparación por pares es significativa tras la corrección de Holm (menor p ajustado = 0,583; Tabla 28). Como la detección es un resultado de 0 o 1 por caso, con la prueba Q de Cochran se obtiene el mismo resultado (Q = 2,111; p = 0,550), y con McNemar exacto por pares y corrección de Holm todos los p ajustados valen 1,000. Con tan pocos casos detectados la prueba tiene poca potencia: estos datos no muestran ventaja de ninguna estrategia, pero tampoco bastan para afirmar que sean equivalentes.

**PE3.** En el grupo de cobertura alta (14 casos) la tasa de detección fue 0,071 con E1, 0,071 con E2, 0,143 con E3 y 0,071 con E4 (Tabla 26). E2 no supera a E1, lo cual es compatible con que la guía por cobertura pierda su señal cuando la suite ya ejecuta casi todo el código, pero con 14 casos y una o dos detecciones por estrategia no se puede afirmar; los contrastes de ese grupo (Tabla 28) tampoco son significativos. En el grupo de cobertura baja (3 casos) E4 detectó 2 y E1 y E2 uno cada una, y en el grupo medio (23 casos) las tasas fueron de 0,043 a 0,130; con tan pocos casos por grupo estas cifras son solo descriptivas.

**PE4.** Las pruebas generadas son en su mayoría válidas: compilan en 90 a 100 % de las corridas según la estrategia, 100 % de las que compilan son estables y entre 1,5 y 4,5 % fallan en la versión corregida (falsos positivos; Tabla 27). El costo difiere más que la detección: E3 y E4 consumen 1,6 y 1,7 veces los tokens de E1 por la fase previa (63.191 y 68.412 frente a 39.158 por caso). El costo medido de las 160 corridas finales fue de unos 1,8 dólares, a precios de lista de Claude Haiku 5.5 (0,10 dólares por millón de tokens de entrada y 0,50 de salida). La legibilidad (D5) no se evaluó por falta de tiempo; su columna en la Tabla 27 queda sin datos.

**Párrafos nuevos (cuerpo de la discusión):**

**Concentración y complementariedad.** Solo 9 de los 40 casos fueron detectados por al menos una estrategia y 31 por ninguna. Codec concentra 10 de las 17 corridas que detectaron (4 de sus 10 casos); Lang aporta 2 casos, Csv 2 y Cli 1 (Figura 9b). Las estrategias detectan casos distintos: solo Codec-18 fue detectado por las cuatro, un caso por tres y tres por dos; E4 es la única que detectó tres casos (Codec-17, Csv-1 y Csv-12) y E3 uno (Cli-27), mientras que todo caso detectado por E1 o E2 fue detectado también por otra estrategia. Esto sugiere que las estrategias se complementan, aunque con una sola repetición no se puede separar la complementariedad del azar de la generación.

**Revisión manual de las detecciones.** Se revisaron a mano las 12 corridas detectoras de Codec y Csv (Codec-4, 16, 17 y 18; Csv-1 y 12). En cada una se volvió a ejecutar la prueba detectora, se comparó con la corrección del defecto y se leyó su mensaje de fallo. Las 12 fallan en la versión con el fallo por el defecto que corrige el parche, y ninguna por otra causa. Los defectos son de seis tipos: un constructor sin argumentos que activaba la división en líneas y agregaba un separador al codificar en Base64 (Codec-4); una comparación de secuencias de caracteres de distinta longitud que lanzaba una excepción de índice (Codec-18); una entrada de más en la tabla del alfabeto hexadecimal de Base32 que aceptaba la letra «W» (Codec-16); una entrada nula que lanzaba `NullPointerException` en lugar de devolver `null` (Codec-17); un contador de líneas que ignoraba el retorno de carro (Csv-1); y una constante de formato (`CSVFormat.EXCEL`) que no activaba una opción que su propio Javadoc afirmaba (Csv-12). Las fichas están en la carpeta `resultados/revision` del repositorio. *(Si se suben los registros de Lang y Cli, revisar también Lang-33, Lang-63 y Cli-27 con `analisis/revisar.py` y ajustar este párrafo; mientras tanto, esas 5 corridas quedan declaradas como pendientes.)*

**Qué aporta la documentación (E4).** Dos de las tres detecciones exclusivas de E4 (Csv-1 y Csv-12) comparten un patrón: la prueba contrasta el código con lo que dice su documentación. En Csv-12 el Javadoc de `CSVFormat.EXCEL` indica que el formato equivale a RFC4180 más `withAllowMissingColumnNames(true)`, pero el código no activaba esa opción, y la prueba de E4 verifica lo que el Javadoc afirma. En Csv-1 el comentario de la propia prueba generada señala que, según la documentación, `read()` debe contar el retorno de carro como fin de línea y que la implementación solo cuenta el salto de línea. E1 y E2, que no ven el Javadoc, no detectaron ninguno de los dos casos. Como es posible que el modelo conociera estos fallos por su entrenamiento (amenaza de contaminación, sección 5.5), este patrón se interpreta como un indicio y no como una prueba.

**Eficiencia y costo.** E3 y E4 generan menos pruebas pero más dirigidas: 21,3 y 19,1 pruebas por corrida en promedio frente a 34,0 de E1 y 25,1 de E2. E4 detecta 0,79 casos por cada 100 pruebas generadas, frente a 0,29 de E1, 0,30 de E2 y 0,47 de E3. Por costo, E4 es la más barata por caso detectado (unos 0,09 dólares frente a 0,10 de E1 y 0,13 de E2 y de E3). Con 3 a 6 detecciones por estrategia estas diferencias son descriptivas. Entre las corridas que produjeron pruebas, las que detectaron ganaron más cobertura (12,6 puntos de líneas y 18,7 de ramas, frente a 7,8 y 9,8 en las que no); esto refleja en parte que las clases con cobertura baja (Codec-16 y Codec-17) tienen más margen de mejora, y no implica que la cobertura cause la detección.

**Cobertura.** (reemplazar solo las cifras) Todas las estrategias aumentan la cobertura de la clase objetivo entre 7,3 y 8,8 puntos porcentuales en líneas y entre 8,9 y 11,4 en ramas (Figura 10), pero ese aumento no se traduce en detección. Cubrir más líneas no implica ejercitar el comportamiento defectuoso.

**Limitaciones de esta ejecución.** (reemplazar el tercero y el quinto) *Tercero*, 2 de las 160 corridas (1,3 %) terminaron sin respuesta porque el modelo agotó el máximo de tokens, incluso al subirlo a 20.000 y repetirlas. De las 8 corridas de Codec y Csv que agotaron los 16.000 tokens, 7 se recuperaron al repetirlas con 20.000 (5 en el primer reintento y 2 en el segundo); de las 2 de Lang y Cli que lo agotaron incluso con 20.000, una se recuperó en un reintento adicional. Las dos restantes (Cli-38 con E2 y Csv-14 con E2) se cuentan como no detección. *Quinto*, la revisión manual cubrió las 12 corridas detectoras de Codec y Csv pero no las 5 de Lang y Cli, y la evaluación de legibilidad no se realizó. Mantener el primero (una repetición), el segundo (Haiku 5.5 y temperatura por defecto) y el cuarto (Hamcrest en Cli).

## 5. Sección 6.2 Conclusiones del experimento

Reemplazar solo las cifras de estas cinco frases:

1. «… la detección de fallos reales fue baja con las cuatro estrategias: de **3 a 6 de 40 casos**, según la estrategia (PE1), y E4 fue la que más detectó.»
2. «… Friedman **p = 0,550**; menor p ajustado por pares = **0,583**.»
3. «El grupo de cobertura alta tiene **14 casos** …»
4. «… compilan en **90 a 100 %** …, pero E3 y E4 cuestan entre 1,6 y **1,7** veces más tokens que E1 (PE4).»
5. En «Lo que no se logró»: «… completar las tres repeticiones, la revisión manual de las detecciones **de Lang y Cli** y la medición de legibilidad.»

## 6. Otras secciones que dependen de estos datos

| Dónde | Qué cambiar |
|---|---|
| **3.6, texto de la Tabla 20 (Recursos)** | «… se usaron **298 llamadas en 160 corridas**; el costo medido fue de unos **1,8 dólares**, a precios de lista de Claude Haiku 5.5.» |
| **Anexo A** | Reemplazar «[URL del repositorio por completar]» por `https://github.com/ahilacondo/fortalecimiento-pruebas-llm` |
| **Anexo C, párrafo inicial** | «Lista final de los 40 casos (10 por proyecto), seleccionados con semilla fija 2026 entre el 6 y el 9 de noviembre. La cobertura inicial es el promedio de líneas y ramas de la clase modificada; grupos: baja (< 60 %), media (60 a 90 %) y alta (≥ 90 %).» |
| **Anexo C, Tabla 31** | Agregar las 20 filas siguientes (la tabla tiene hoy 20, de Lang y Cli) |
| **Tabla 7** | Sin cambios: ya dice 16.000 tokens (20.000 al repetir) y temperatura por defecto |
| **4.3, «Estado a la fecha»** | Sin cambios |

Filas para la **Tabla 31** (copiar de `resultados/analisis_base/anexo_c_casos.csv`, que tiene los valores sin redondear):

| N.º | Proyecto | Fallo | Clase | Cobertura inicial | Grupo |
|---|---|---|---|---|---|
| 21 | Codec | 9 | Base64 | 89,0 % | media |
| 22 | Codec | 6 | Base64InputStream | 100,0 % | alta |
| 23 | Codec | 4 | Base64 | 91,9 % | alta |
| 24 | Codec | 18 | StringUtils | 79,8 % | media |
| 25 | Codec | 15 | Soundex | 94,1 % | alta |
| 26 | Codec | 3 | DoubleMetaphone | 83,7 % | media |
| 27 | Codec | 2 | Base64 | 84,0 % | media |
| 28 | Codec | 16 | Base32 | 58,5 % | baja |
| 29 | Codec | 17 | StringUtils | 49,6 % | baja |
| 30 | Codec | 10 | Caverphone | 100,0 % | alta |
| 31 | Csv | 10 | CSVPrinter | 93,1 % | alta |
| 32 | Csv | 11 | CSVParser | 88,9 % | media |
| 33 | Csv | 2 | CSVRecord | 78,7 % | media |
| 34 | Csv | 1 | ExtendedBufferedReader | 69,2 % | media |
| 35 | Csv | 5 | CSVPrinter | 93,7 % | alta |
| 36 | Csv | 12 | CSVFormat | 74,0 % | media |
| 37 | Csv | 15 | CSVFormat | 72,7 % | media |
| 38 | Csv | 16 | CSVParser | 90,5 % | alta |
| 39 | Csv | 6 | CSVRecord | 87,3 % | media |
| 40 | Csv | 14 | CSVFormat | 60,6 % | media |

## 7. Cómo presentar los resultados (recomendaciones de forma)

- **Empiecen por el hallazgo, no por la tabla.** El primer párrafo de 5.2 debe decir en una línea que la detección es baja (4 a 6 de 40 casos) y que no hay diferencias significativas. Después van las tablas.
- **Muestren dónde se concentra la detección.** La Figura 11 (mapa por caso) y el párrafo de concentración hacen que el resultado se lea como un análisis y no como una lista de porcentajes: 9 de 40 casos, casi todos en Codec, y estrategias que se complementan.
- **Usen la revisión manual como evidencia de calidad.** Que las 12 detecciones revisadas sean válidas responde a la amenaza de constructo de la Tabla 30. Citen las fichas de `resultados/revision/`.
- **No afirmen más de lo que dicen los datos.** Con 1 repetición y 17 corridas detectoras no se puede decir que E4 sea mejor: se puede decir que detectó más casos (6 de 40) y que los intervalos se traslapan. Eviten «mejor», «supera» o «demuestra».
- **Declaren los problemas con cifras.** 2 corridas sin respuesta (1,3 %), 5 que no compilan, repeticiones únicas, temperatura por defecto, Hamcrest en Cli. Un informe que los declara es más creíble que uno que los omite.
- **Ancho de figuras.** 446 pt para las de dos paneles (9 y 10) y 400 pt para el mapa (11). Texto de las figuras en español y mismo estilo de pie «Fuente: Elaboración propia con los resultados del experimento».

## 8. Pendientes y responsables sugeridos

| Pendiente | Quién | Cómo |
|---|---|---|
| Subir `registros/` de Lang y Cli (al menos las 5 corridas detectoras: Lang-63 E1 y E3, Lang-33 E2 y E4, Cli-27 E3) | Quien corrió Lang y Cli | `git pull`, copiar a `resultados/Lang/registros` y `resultados/Cli/registros`, commit y push |
| Revisar a mano esas 5 detecciones | Cualquiera, después de lo anterior | `python analisis/revisar.py --proyecto Lang --registros resultados/Lang/registros` (y Cli) |
| Evaluar legibilidad (D5) | 2 o 3 personas | Escala del Anexo D sobre una muestra de pruebas; completar la columna de la Tabla 27 |
| Pasar tablas, figuras y textos al Word | Quien tenga el `.docx` | Secciones 1 a 6 de esta guía |
| Arreglar figuras 12 a 14 repetidas del Anexo F y actualizar el índice | Quien edite el Word | Sección 0 |
| Lectura final completa | Todo el equipo | Antes de entregar |
