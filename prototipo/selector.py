# -*- coding: utf-8 -*-
"""Módulo 1: selector de casos.

Para cada proyecto: toma los fallos activos de Defects4J cuyo cambio afecta a una sola clase de producción,
los ordena con una semilla fija y va aceptando los que cumplen los criterios de la sección 3.5
(clase de prueba <Clase>Test, versión con fallo compila, suite sin fallos salvo las disparadoras, < 5 min)
hasta completar N casos por proyecto.
"""
from __future__ import annotations

import argparse
import csv
import random
from pathlib import Path
from typing import List, Optional

from .backend import Caso, CasoInvalido
from .config import Config, RAIZ


def candidatos(d4j, proyecto: str) -> List[Caso]:
    activos = set(d4j.ids_activos(proyecto))
    filas = d4j.consulta(proyecto, "bug.id,classes.modified")
    out: List[Caso] = []
    for f in filas:
        if len(f) < 2 or not f[0].strip().isdigit():
            continue
        bug = int(f[0])
        clases = [c for c in f[1].replace('"', "").split(";") if c.strip()]
        if bug in activos and len(clases) == 1:
            out.append(Caso(proyecto, bug, clases[0].strip()))
    return out


def seleccionar(d4j, proyectos: List[str], por_proyecto: int, semilla: int, salida: Path,
                descartados: Path) -> None:
    aceptados: List[Caso] = []
    rechazos: List[List[str]] = []
    for p in proyectos:
        lista = candidatos(d4j, p)
        random.Random(f"{semilla}-{p}").shuffle(lista)
        print(f"{p}: {len(lista)} candidatos con una sola clase modificada")
        n = 0
        for caso in lista:
            if n >= por_proyecto:
                break
            try:
                d4j.preparar(caso)
            except CasoInvalido as e:
                rechazos.append([caso.id, caso.clase, str(e)])
                print(f"  - {caso.id}: descartado ({e})")
                continue
            except Exception as e:  # error del entorno
                rechazos.append([caso.id, caso.clase, f"error: {e}"])
                print(f"  - {caso.id}: error ({e})")
                continue
            aceptados.append(caso)
            n += 1
            print(f"  + {caso.id} {caso.clase} ({n}/{por_proyecto})")
        if n < por_proyecto:
            print(f"  AVISO: solo se completaron {n} de {por_proyecto} casos en {p}")
    salida.parent.mkdir(parents=True, exist_ok=True)
    with open(salida, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["proyecto", "fallo", "clase"])
        for c in aceptados:
            w.writerow([c.proyecto, c.fallo, c.clase])
    with open(descartados, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["caso", "clase", "motivo"])
        w.writerows(rechazos)
    print(f"Casos aceptados: {len(aceptados)} -> {salida}")


def main(argv: Optional[List[str]] = None) -> None:
    ap = argparse.ArgumentParser(description="Selecciona los casos del experimento (módulo 1).")
    ap.add_argument("--proyectos", nargs="+", default=["Lang", "Cli", "Codec", "Csv"])
    ap.add_argument("--por-proyecto", type=int, default=10)
    ap.add_argument("--semilla", type=int, default=2026)
    ap.add_argument("--salida", default=str(RAIZ / "casos" / "muestra_piloto.csv"))
    ap.add_argument("--descartados", default=str(RAIZ / "casos" / "descartados.csv"))
    a = ap.parse_args(argv)
    from .d4j import D4J
    seleccionar(D4J(Config()), a.proyectos, a.por_proyecto, a.semilla, Path(a.salida), Path(a.descartados))


if __name__ == "__main__":
    main()
