# Resultados del análisis

Casos: 40; corridas: 160

## Tabla r1. Detección de fallos reales por estrategia
| Estrategia                      | Casos con fallo detectado   | Tasa media ± DE   | IC 95 %        |   Pruebas nuevas por caso (mediana) |
|:--------------------------------|:----------------------------|:------------------|:---------------|------------------------------------:|
| E1 Sin guía                     | 4 de 40                     | 0.100 ± 0.304     | [0.025; 0.200] |                                30.5 |
| E2 Cobertura                    | 3 de 40                     | 0.075 ± 0.267     | [0.000; 0.175] |                                21.5 |
| E3 Análisis de código y pruebas | 4 de 40                     | 0.100 ± 0.304     | [0.025; 0.200] |                                17   |
| E4 Documentación                | 6 de 40                     | 0.150 ± 0.362     | [0.050; 0.275] |                                17   |

## Tabla r2. Tasa de detección por cobertura inicial
| Estrategia                      |   Baja (< 60 %) |   Media (60 a 90 %) |   Alta (≥ 90 %) |
|:--------------------------------|----------------:|--------------------:|----------------:|
| N.º de casos del grupo          |           3     |              23     |          14     |
| E1 Sin guía                     |           0.333 |               0.087 |           0.071 |
| E2 Cobertura                    |           0.333 |               0.043 |           0.071 |
| E3 Análisis de código y pruebas |           0     |               0.087 |           0.143 |
| E4 Documentación                |           0.667 |               0.13  |           0.071 |

## Tabla r3. Validez, costo y cambio de cobertura (la legibilidad se agrega a mano, Anexo D)
| Estrategia   |   Compilan (%) |   Estables (%) |   Falsos positivos (%) |   Llamadas por caso |   Tokens por caso |   Tiempo (s) |   Δ cobertura líneas (pp) |   Δ cobertura ramas (pp) |
|:-------------|---------------:|---------------:|-----------------------:|--------------------:|------------------:|-------------:|--------------------------:|-------------------------:|
| E1           |           95   |            100 |                    1.5 |                 1.4 |             39158 |         69.9 |                      7.92 |                    10.47 |
| E2           |           90   |            100 |                    2   |                 1.4 |             37214 |         74.6 |                      7.74 |                    10.43 |
| E3           |          100   |            100 |                    3.4 |                 2.3 |             63191 |         81.3 |                      8.82 |                    11.41 |
| E4           |           97.5 |            100 |                    4.5 |                 2.4 |             68412 |         82.7 |                      7.31 |                     8.85 |

## Tabla r4. Contrastes estadísticos (Holm sobre los 7 contrastes de Wilcoxon)
| Contraste                      | Estadístico   |      p | p ajustado (Holm)   | Â12   | Interpretación                                 |
|:-------------------------------|:--------------|-------:|:--------------------|:------|:-----------------------------------------------|
| Friedman: E1 a E4              | 2.111         | 0.5497 | —                   | —     | no se detectan diferencias                     |
| Wilcoxon: E2 frente a E1       | 2.0           | 0.5637 | 1.0000              | 0.487 | no significativa; efecto despreciable (n = 40) |
| Wilcoxon: E3 frente a E1       | 1.5           | 1      | 1.0000              | 0.500 | no significativa; efecto despreciable (n = 40) |
| Wilcoxon: E4 frente a E1       | 7.0           | 0.4142 | 1.0000              | 0.525 | no significativa; efecto despreciable (n = 40) |
| Wilcoxon: E3 frente a E2       | 6.0           | 0.6547 | 1.0000              | 0.512 | no significativa; efecto despreciable (n = 40) |
| Wilcoxon: E4 frente a E2       | 0.0           | 0.0833 | 0.5829              | 0.537 | no significativa; efecto despreciable (n = 40) |
| Cobertura alta: E3 frente a E2 | 2.0           | 0.5637 | 1.0000              | 0.536 | no significativa; efecto despreciable (n = 14) |
| Cobertura alta: E4 frente a E2 | —             | 1      | 1.0000              | 0.500 | no significativa; efecto despreciable (n = 14) |

## Tabla r5. Causas en los 31 casos donde ninguna estrategia detecta el fallo
| Causa                      |   N.º de casos |
|:---------------------------|---------------:|
| No compila                 |              1 |
| Pruebas inestables         |              0 |
| Falsos positivos           |              0 |
| Sin diferencia             |             30 |
| Límite de intentos o costo |              0 |
