# Guía para evaluar la legibilidad (D5)

Objetivo: calificar de 1 a 5 una muestra de 20 pruebas generadas por Claude (5 por estrategia) con la escala del Anexo D (Tabla 32) del informe, y completar la columna «Legibilidad» de la Tabla 27.

## Archivos

| Archivo | Para qué |
|---|---|
| `muestra_para_calificar.md` | Las 20 pruebas (P01 a P20), en orden aleatorio y **sin indicar la estrategia** |
| `planilla_evaluador_1.csv` | Planilla vacía; cada evaluador copia el archivo con su número (`planilla_evaluador_2.csv`, ...) |
| `clave_NO_ABRIR.csv` | Relaciona cada prueba con su estrategia. **No abrirla hasta terminar de calificar** |

La muestra se sorteó con la semilla 2026, entre corridas que compilaron: 5 casos distintos por estrategia y una prueba al azar de cada uno. Se excluyeron las corridas repetidas en `resultados/reintento3`.

## Pasos

1. Cada evaluador trabaja **por separado**, sin comentar las notas hasta terminar (2 o 3 personas).
2. Copiar `planilla_evaluador_1.csv` a `planilla_evaluador_2.csv` (y `_3`, si hay tercera persona).
3. Leer `muestra_para_calificar.md` y, para cada prueba, escribir en la planilla el puntaje de 1 a 5 y, si quiere, un comentario breve. Criterio: lo que dice la escala, mirando solo esa prueba (nombre, una idea por prueba, aserciones claras). No juzgue si detecta o no un fallo.
4. Ordenar: ~1 a 2 minutos por prueba, 30 a 40 minutos en total.
5. Cuando todos terminen: `git add resultados/legibilidad`, commit y push. Luego correr:

```bash
python analisis/legibilidad.py
```

Genera `resultados/legibilidad/resumen_legibilidad.csv` con el promedio por estrategia (y por evaluador) y muestra el acuerdo entre dos evaluadores.

## Cómo reportarlo en el informe

- **Tabla 27, columna «Legibilidad (1 a 5)»:** el promedio por estrategia de todos los evaluadores.
- **PE4:** reemplazar «La legibilidad (D5) no se evaluó por falta de tiempo…» por la cifra y por «muestra de 20 pruebas (5 por estrategia), evaluada por N personas con la escala del Anexo D».
- **Limitaciones:** con 5 pruebas por estrategia las diferencias son solo descriptivas; decirlo.
- **Quinto punto de limitaciones y conclusiones 6.2:** quitar «la medición de legibilidad» de lo no logrado.
- Mencionar el acuerdo entre evaluadores (porcentaje de pruebas con diferencia de 1 punto o menos).
