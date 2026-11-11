# -*- coding: utf-8 -*-
"""Resumen de la evaluación de legibilidad (D5).

Lee las planillas de los evaluadores (resultados/legibilidad/planilla_evaluador_*.csv) y la clave
que relaciona cada prueba con su estrategia, y calcula el promedio por estrategia y el acuerdo
entre evaluadores.

Uso:
    python analisis/legibilidad.py
Salida: resultados/legibilidad/resumen_legibilidad.csv y un resumen por pantalla.
"""
from __future__ import annotations

import csv
import glob
import statistics
from pathlib import Path

CARPETA = Path(__file__).resolve().parent.parent / "resultados" / "legibilidad"


def leer(ruta):
    with open(ruta, encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f))


def main():
    clave = {f["id"]: f["estrategia"] for f in leer(CARPETA / "clave_NO_ABRIR.csv")}
    planillas = sorted(glob.glob(str(CARPETA / "planilla_evaluador_*.csv")))
    notas = {}                                   # evaluador -> {id: puntaje}
    for p in planillas:
        ev = Path(p).stem.replace("planilla_", "")
        notas[ev] = {f["id"]: int(f["puntaje_1_a_5"]) for f in leer(p) if f["puntaje_1_a_5"].strip()}
    if not notas:
        raise SystemExit("No hay planillas con puntajes.")

    filas = [["estrategia", "evaluador", "n_pruebas", "promedio", "desviacion"]]
    for e in sorted(set(clave.values())):
        todas = []
        for ev, d in notas.items():
            v = [n for i, n in d.items() if clave.get(i) == e]
            todas += v
            filas.append([e, ev, len(v), round(statistics.mean(v), 2), round(statistics.pstdev(v), 2)])
        filas.append([e, "promedio_de_evaluadores", len(todas), round(statistics.mean(todas), 2),
                      round(statistics.pstdev(todas), 2)])
    with open(CARPETA / "resumen_legibilidad.csv", "w", encoding="utf-8", newline="") as f:
        csv.writer(f).writerows(filas)
    for f in filas:
        print(*f, sep="\t")

    evs = list(notas)
    if len(evs) >= 2:
        comunes = set(notas[evs[0]]) & set(notas[evs[1]])
        dif = [abs(notas[evs[0]][i] - notas[evs[1]][i]) for i in comunes]
        print(f"\nAcuerdo {evs[0]} vs {evs[1]} ({len(dif)} pruebas): exacto {sum(d == 0 for d in dif) / len(dif):.0%}, "
              f"diferencia de 1 punto o menos {sum(d <= 1 for d in dif) / len(dif):.0%}")


if __name__ == "__main__":
    main()
