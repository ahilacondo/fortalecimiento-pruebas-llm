# Análisis ampliado

Corridas: 160 · casos: 40 · repetición única.

## A1. Casos con fallo detectado por proyecto (de 10 por proyecto)

| proyecto      |   E1 |   E2 |   E3 |   E4 |
|:--------------|-----:|-----:|-----:|-----:|
| Lang          |    1 |    1 |    1 |    1 |
| Cli           |    0 |    0 |    1 |    0 |
| Codec         |    3 |    2 |    2 |    3 |
| Csv           |    0 |    0 |    0 |    2 |
| Total (de 40) |    4 |    3 |    4 |    6 |

## A2. Desenlace de las corridas por estrategia

| estrategia   |   Detectó el fallo |   Pasa en ambas versiones |   No compila |   Sin respuesta (tope de tokens) |
|:-------------|-------------------:|--------------------------:|-------------:|---------------------------------:|
| E1           |                  4 |                        34 |            2 |                                0 |
| E2           |                  3 |                        33 |            2 |                                2 |
| E3           |                  4 |                        35 |            0 |                                1 |
| E4           |                  6 |                        31 |            1 |                                2 |

## A3. Costo y eficiencia (precios de lista de Haiku 5.5)

| estrategia   |   casos_detectados |   pruebas_generadas_media |   tokens_por_caso |   llamadas_por_caso |   segundos_por_caso |   usd_por_caso |   usd_total |   usd_por_deteccion |   detecciones_por_100_pruebas |
|:-------------|-------------------:|--------------------------:|------------------:|--------------------:|--------------------:|---------------:|------------:|--------------------:|------------------------------:|
| E1           |                  4 |                    34     |           39158.4 |               1.425 |              69.854 |          0.01  |       0.391 |               0.098 |                         0.294 |
| E2           |                  3 |                    25.05  |           37213.6 |               1.375 |              74.639 |          0.009 |       0.377 |               0.126 |                         0.299 |
| E3           |                  4 |                    20.4   |           61869.3 |               2.25  |              81.105 |          0.012 |       0.499 |               0.125 |                         0.49  |
| E4           |                  6 |                    18.075 |           64970.6 |               2.3   |              81.849 |          0.013 |       0.504 |               0.084 |                         0.83  |

Costo total medido de las 160 corridas: US$ 1.77.

## A4. Concordancia entre estrategias

|   n_estrategias_que_detectan |   casos |
|-----------------------------:|--------:|
|                            0 |      31 |
|                            1 |       4 |
|                            2 |       3 |
|                            3 |       1 |
|                            4 |       1 |

- Casos detectados por al menos una estrategia: **9 de 40**.
- Detectados por las cuatro: 1.
- Detectados por una sola estrategia: {'E1': 0, 'E2': 0, 'E3': 1, 'E4': 3}.
- Ninguna estrategia detecta: 31 casos.

## A5. Contrastes para resultados de 0 o 1

Prueba Q de Cochran (4 estrategias, 40 casos): Q = 2.111, p = 0.5497.

McNemar exacto por pares con corrección de Holm:

| contraste      |   solo_primera |   solo_segunda |      p |   p_holm |
|:---------------|---------------:|---------------:|-------:|---------:|
| E1 frente a E2 |              2 |              1 | 1      |        1 |
| E1 frente a E3 |              1 |              1 | 1      |        1 |
| E1 frente a E4 |              2 |              4 | 0.6875 |        1 |
| E2 frente a E3 |              2 |              3 | 1      |        1 |
| E2 frente a E4 |              0 |              3 | 0.25   |        1 |
| E3 frente a E4 |              3 |              5 | 0.7266 |        1 |

## A6. Casos con fallo detectado por grupo de cobertura inicial

| grupo   |   casos | E1      | E2      | E3      | E4      |
|:--------|--------:|:--------|:--------|:--------|:--------|
| baja    |       3 | 1 de 3  | 1 de 3  | 0 de 3  | 2 de 3  |
| media   |      23 | 2 de 23 | 1 de 23 | 2 de 23 | 3 de 23 |
| alta    |      14 | 1 de 14 | 1 de 14 | 2 de 14 | 1 de 14 |

## A7. Cobertura ganada y pruebas generadas según si la corrida detecta

|            |   delta_cobertura_lineas |   delta_cobertura_ramas |   pruebas_generadas |
|:-----------|-------------------------:|------------------------:|--------------------:|
| No detecta |                     7.92 |                    9.9  |               24.82 |
| Detecta    |                    12.59 |                   18.67 |               30.35 |
