# Especificación del prototipo

Documento breve que fija **qué hace** cada módulo y **qué decisiones** se tomaron, antes de leer el código. Corresponde a la sección 4.2 del informe del Hito 2.

## 1. Objetivo

Dado un fallo real de Defects4J y su suite de pruebas **sin** las pruebas disparadoras, pedir a Claude pruebas JUnit nuevas con cuatro estrategias de información (E1 a E4) y medir si esas pruebas detectan el fallo.

## 2. Módulos

| # | Módulo del informe | Archivo | Entrada → Salida |
|---|---|---|---|
| 1 | Selector de casos | `prototipo/selector.py` | Fallos activos de Defects4J → `casos/muestra_piloto.csv`, `casos/descartados.csv` |
| 2 | Preparador de suite | `prototipo/d4j.py` (`preparar`), `prototipo/javautil.py` | Caso → proyectos `b` y `f` listos, suite sin pruebas disparadoras |
| 3 | Medidor de cobertura | `prototipo/cobertura.py`, `prototipo/d4j.py` | Suite original → cobertura inicial y grupo (baja/media/alta) |
| 4 | Constructor de contexto | `prototipo/contexto.py`, `instrucciones/*.txt` | Caso + estrategia → instrucción para Claude |
| 5 | Cliente LLM | `prototipo/llm.py` | Instrucción → respuesta de Claude (y tokens) |
| 6 | Validador | `prototipo/ejecutar.py` | Archivo de pruebas → pruebas que compilan y son estables |
| 7 | Evaluador | `prototipo/ejecutar.py`, `prototipo/d4j.py` | Pruebas estables → detección, falsos positivos, delta de cobertura |
| 8 | Registro de resultados | `prototipo/registro.py` | Fila de resultados → `resultados/ejecuciones.csv` y `resultados/registros/` |

Apoyo: `prototipo/config.py` (parámetros y `.env`), `prototipo/backend.py` (interfaz común del banco de pruebas), `prototipo/simulador.py` (backend ficticio para probar el flujo), `analisis/analizar.py` (estadística y gráficos).

## 3. Flujo de una corrida (caso, estrategia, repetición)

1. **Preparar el caso** (una vez por caso): extraer `b` (con fallo) y `f` (corregida); quitar de `<Clase>Test` los métodos disparadores; compilar ambas; ejecutar la suite original como suite externa para obtener cobertura inicial y comprobar que no falla.
2. **Fase 1 (solo E3 y E4)**: una llamada pide la lista de comportamientos (E3) o requisitos del Javadoc (E4) sin prueba; se limita a 8 elementos.
3. **Generación**: una llamada con la plantilla común más el bloque de la estrategia.
4. **Compilación con correcciones**: si no compila, se devuelven a Claude **solo los errores del compilador**; máximo 2 correcciones.
5. **Estabilidad**: las pruebas que compilan se ejecutan 3 veces sobre `b`; se descartan las que no dan el mismo resultado.
6. **Evaluación**: se ejecutan en `f` y en `b`. Una prueba **detecta** si pasa en `f` y falla en `b`. Una prueba que falla en `f` es un **falso positivo** y se excluye.
7. **Cobertura final**: suite original + pruebas estables; el delta se calcula sobre la clase modificada.
8. **Causa de no detección** (si no hubo detección): `no_compila`, `pruebas_inestables`, `falsos_positivos`, `sin_diferencia` o `limite_o_error`.
9. **Registro**: fila en CSV y archivos de la corrida (instrucciones, respuestas, errores, pruebas finales).

## 4. Decisiones de diseño

| Decisión | Motivo |
|---|---|
| Las correcciones usan solo errores de **compilación**, nunca el resultado de ejecutar en `b` o `f` | Si se mostrara a Claude que una prueba falla en `b`, se le estaría diciendo dónde está el fallo y se invalidaría D1 |
| Una sola plantilla de instrucciones; solo cambia el bloque por estrategia | Las diferencias de resultado deben deberse a la información, no al estilo de la instrucción |
| Se usan **suites externas** de Defects4J (`defects4j test -s`) | `-t` solo acepta `Clase::método`; con suites externas se ejecutan las pruebas nuevas sin modificar el proyecto |
| Los métodos disparadores se quitan por texto (con control de llaves, cadenas y comentarios) | Garantiza que la línea base E0 detecte cero por construcción |
| Grupo de cobertura según el **promedio de líneas y ramas** de la clase | Una sola cifra por caso para formar bloques: baja < 60 %, media 60–90 %, alta ≥ 90 % |
| Muestreo con semilla fija (`2026-<proyecto>`) | Reproducibilidad de la lista de casos |
| El registro es un CSV que se reanuda | Un corte de red o de saldo no obliga a repetir lo ya ejecutado |
| Los datos simulados llevan `simulado = 1` y el análisis los excluye | Evita que datos ficticios entren al informe |

## 5. Fuera de alcance

Mutación de código, ajuste fino de modelos, otros lenguajes distintos de Java, y la medición de legibilidad (D5), que se hace a mano según el Anexo D del informe.

## 6. Criterios de aceptación del prototipo

- `pytest -q` pasa sin Defects4J ni API.
- `python -m prototipo.selector` produce 40 casos (o documenta en `descartados.csv` por qué no).
- El piloto (`--piloto`) completa las 16 corridas y deja registros auditables.
- `python analisis/analizar.py` genera las tablas R1 a R5 y los dos gráficos a partir de `resultados/ejecuciones.csv`.
