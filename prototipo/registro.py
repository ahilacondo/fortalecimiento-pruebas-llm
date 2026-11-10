# -*- coding: utf-8 -*-
"""Módulo 8: registro de resultados (CSV con una fila por caso, estrategia y repetición)."""
from __future__ import annotations

import csv
from pathlib import Path
from typing import Dict, Set, Tuple

CAMPOS = ["caso", "proyecto", "fallo", "clase", "cobertura_inicial", "grupo", "estrategia", "repeticion",
          "pruebas_generadas", "compilan", "estables", "detecta_fallo", "pruebas_detectoras", "falsos_positivos",
          "delta_cobertura_lineas", "delta_cobertura_ramas", "causa_sin_deteccion", "error",
          "llamadas", "tokens_entrada", "tokens_salida", "segundos",
          "modelo", "parametros", "semilla", "simulado"]


class Registro:
    def __init__(self, ruta: Path):
        self.ruta = Path(ruta)
        self.ruta.parent.mkdir(parents=True, exist_ok=True)
        self._claves: Set[Tuple[str, str, int]] = set()
        if self.ruta.exists():
            with open(self.ruta, newline="", encoding="utf-8") as f:
                for r in csv.DictReader(f):
                    self._claves.add((r["caso"], r["estrategia"], int(r["repeticion"])))
        else:
            with open(self.ruta, "w", newline="", encoding="utf-8") as f:
                csv.DictWriter(f, fieldnames=CAMPOS).writeheader()

    def existe(self, caso: str, estrategia: str, rep: int) -> bool:
        return (caso, estrategia, rep) in self._claves

    def agregar(self, fila: Dict) -> None:
        with open(self.ruta, "a", newline="", encoding="utf-8") as f:
            csv.DictWriter(f, fieldnames=CAMPOS, extrasaction="ignore").writerow(fila)
        self._claves.add((fila["caso"], fila["estrategia"], int(fila["repeticion"])))
