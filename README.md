# Fortalecimiento de suites de pruebas con LLM (Defects4J + Claude)

Prototipo y scripts del experimento del Hito 2 del curso *Tópicos Avanzados en Ingeniería de Software* (UNSA, grupo A.6).

El experimento compara cuatro estrategias para pedirle a Claude pruebas JUnit nuevas, y mide cuántos fallos reales de Defects4J detectan:

| Estrategia | Información adicional que recibe Claude |
|---|---|
| E1 | Ninguna (control) |
| E2 | Informe de cobertura (líneas y ramas que la suite no ejecuta) |
| E3 | Registro de ejecución; primero lista lo que las pruebas no verifican y luego genera pruebas |
| E4 | Javadoc de la clase; primero lista los requisitos sin prueba y luego genera pruebas |

E0 es la línea base: la suite original sin las pruebas disparadoras (detección cero por construcción).

> **Aviso importante.** El modo `--simulado` produce datos **ficticios** (columna `simulado = 1`). Sirve solo para probar que el flujo funciona. Esos datos **no deben usarse en el informe** y `analisis/analizar.py` los excluye por defecto.

## 1. Requisitos

- Windows 10/11 con **WSL2 (Ubuntu 22.04 o 24.04)**, o Linux/macOS. Defects4J no funciona en Windows nativo.
- Unos 15 GB libres y buena conexión (la instalación de Defects4J descarga los repositorios de los proyectos).
- Una clave de API de Anthropic (ver sección 3).

Trabaje **dentro del disco de Linux** (`/home/usuario/...`), no en `/mnt/c/...`: es mucho más rápido y evita problemas de permisos y saltos de línea.

### Instalar WSL2 (una sola vez, en PowerShell como administrador)

```powershell
wsl --install -d Ubuntu
```

Reinicie, abra "Ubuntu" y cree su usuario.

## 2. Instalación

```bash
cd ~
git clone https://github.com/USUARIO/fortalecimiento-pruebas-llm.git
cd fortalecimiento-pruebas-llm

# 2.1 Java 11, Defects4J y dependencias (tarda; descarga los repositorios de los proyectos)
bash scripts/instalar_defects4j.sh
source ~/.bashrc

# 2.2 Entorno de Python
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

Compruebe que Defects4J usa Java 11:

```bash
java -version          # debe decir 11.x
defects4j info -p Lang | head
```

## 3. Clave de la API de Claude

1. Entre a <https://console.anthropic.com> e inicie sesión (puede ser con su correo).
2. En **Billing** (o *Plans & Billing*) agregue saldo. La API se paga aparte de la suscripción de claude.ai. Para el piloto bastan pocos dólares.
3. En **API Keys**, pulse *Create Key*, póngale un nombre (por ejemplo `tais-hito2`) y **copie la clave en ese momento**: no se vuelve a mostrar.
4. Cree su archivo de configuración y pegue la clave:

```bash
cp .env.example .env
nano .env            # ANTHROPIC_API_KEY=sk-ant-...   y ajuste D4J_HOME / D4J_TRABAJO con su usuario
```

`.env` está en `.gitignore`: **nunca suba la clave a GitHub**. Si se filtra, bórrela en la consola y cree otra.

Parámetros que se registran en cada corrida: `CLAUDE_MODEL` (por defecto `claude-sonnet-5-5`), `TEMPERATURA` (0.7), `MAX_TOKENS` (4096), semilla 2026, 3 repeticiones, máximo 2 correcciones, 3 ejecuciones de estabilidad.

## 4. Pruebas del propio prototipo (sin API ni Defects4J)

```bash
pytest -q
```

Las pruebas de `tests/test_d4j_falso.py` necesitan dos JAR (JUnit y Hamcrest). Defects4J los trae:

```bash
export JUNIT_JAR=$(find ~/defects4j -name 'junit-4*.jar' | head -1)
export HAMCREST_JAR=$(find ~/defects4j -name 'hamcrest-core*.jar' | head -1)
pytest -q
```

## 5. Ejecución del experimento

Todos los comandos se ejecutan desde la raíz del repositorio, con el entorno virtual activo (`source .venv/bin/activate`).

### Paso 1. Seleccionar los 40 casos (módulo 1; no usa la API)

```bash
python -m prototipo.selector
```

Para cada proyecto (Lang, Cli, Codec, Csv) toma los fallos activos cuyo cambio afecta a una sola clase de producción, los ordena con la semilla 2026 y acepta los que cumplen los criterios de la sección 3.5 del informe. Genera:

- `casos/muestra_piloto.csv`: los casos aceptados (esta es la lista del Anexo C).
- `casos/descartados.csv`: los casos rechazados y el motivo.

Si algún proyecto no llega a 10 casos válidos, el script lo avisa; en ese caso se documenta en el informe.

### Paso 2. Piloto de costo y funcionamiento (1 caso por proyecto, 1 repetición)

```bash
python -m prototipo.ejecutar --casos casos/muestra_piloto.csv --piloto
```

Son 4 casos × 4 estrategias = 16 corridas. Revise `resultados/ejecuciones.csv` y las carpetas de `resultados/registros/` (instrucciones enviadas, respuestas de Claude, errores de compilación). Con los tokens registrados estime el costo del experimento completo.

### Paso 3. Experimento completo (40 casos × 4 estrategias × 3 repeticiones = 480 corridas)

```bash
python -m prototipo.ejecutar --casos casos/muestra_piloto.csv
```

El script **retoma** donde quedó: si se corta, vuelva a ejecutar el mismo comando y omite las corridas ya registradas. Cada corrida usa como máximo 1 llamada de generación, 1 de análisis previo (E3 y E4) y 2 de corrección (máximo estimado: 1 680 llamadas).

Opciones útiles: `--estrategias E1 E3`, `--repeticiones 1`, `--max-casos 5`, `--salida otro.csv`.

### Paso 4. Análisis estadístico

```bash
python analisis/analizar.py
```

Genera en `analisis/salida/`: `tabla_r1_deteccion`, `tabla_r2_cobertura`, `tabla_r3_validez_costo`, `tabla_r4_contrastes`, `tabla_r5_causas` (CSV), `anexo_c_casos.csv`, `resumen.md`, `fig_deteccion.png` y `fig_cobertura.png`. Esas tablas son las que se copian a la sección 5.2 y al Anexo C del informe.

### Modo simulado (solo para probar el flujo)

```bash
python -m prototipo.ejecutar --casos casos/ejemplo_simulado.csv --simulado
python analisis/analizar.py --entrada resultados/ejecuciones_simuladas.csv --incluir-simulado
```

## 6. Definición de las métricas

| Métrica | Cómo se calcula |
|---|---|
| D1 Detección | Una corrida detecta el fallo si alguna prueba estable **pasa en la versión corregida y falla en la versión con fallo** |
| D2 Cobertura | Cobertura de la clase objetivo (promedio de líneas y ramas) con la suite ampliada menos la inicial |
| D3 Validez | Pruebas que compilan, pruebas estables (3 ejecuciones iguales) y falsos positivos (fallan en la versión corregida) |
| D4 Costo | Llamadas, tokens y segundos por corrida |
| D5 Legibilidad | Evaluación manual (Anexo D del informe); no la calcula el código |
| Grupo de cobertura | Sobre la cobertura inicial: baja < 60 %, media 60–90 %, alta ≥ 90 % |

## 7. Estructura

```
README.md              esta guía
ESPECIFICACION.md      qué hace cada módulo y qué decisiones se tomaron
casos/                 lista de casos y descartados (la genera el selector)
prototipo/             código de los ocho módulos (ver ESPECIFICACION.md)
instrucciones/         plantillas de instrucciones por estrategia (E1 a E4)
resultados/            CSV de corridas y registros de cada corrida (instrucciones, respuestas, errores)
analisis/              script estadístico y gráficos
tests/                 pruebas del prototipo (incluye un Defects4J falso para probar sin instalarlo)
scripts/               instalador de Defects4J para WSL2/Ubuntu
```

## 8. Problemas frecuentes

- **`defects4j: command not found`**: ejecute `source ~/.bashrc` o abra una terminal nueva.
- **Versión de Java incorrecta**: Defects4J necesita Java 11. Use `sudo update-alternatives --config java`.
- **`ANTHROPIC_API_KEY` vacía o error 401**: revise `.env`; no deje espacios ni comillas alrededor de la clave.
- **Error 429 o de saldo**: el cliente reintenta hasta 5 veces con espera; si persiste, revise el saldo en la consola y relance el mismo comando (retoma).
- **El modelo rechaza `temperature`**: el cliente reintenta sin ese parámetro y lo avisa en pantalla. Si ocurre, anótelo en el informe (sección 3.5), porque la temperatura del CSV ya no se aplicó.
- **Un caso queda descartado**: es normal; el selector lo registra en `casos/descartados.csv` con el motivo y pasa al siguiente.
