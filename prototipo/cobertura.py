# -*- coding: utf-8 -*-
"""Módulo 3 (parte): lectura del informe de cobertura Cobertura (coverage.xml de Defects4J)."""
from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Tuple

_CC = re.compile(r"\((\d+)/(\d+)\)")


@dataclass
class Cobertura:
    # línea -> (ejecuciones, condiciones cubiertas, condiciones totales)
    lineas: Dict[int, Tuple[int, int, int]] = field(default_factory=dict)

    @property
    def lineas_total(self) -> int:
        return len(self.lineas)

    @property
    def lineas_cubiertas(self) -> int:
        return sum(1 for h, _, _ in self.lineas.values() if h > 0)

    @property
    def ramas_total(self) -> int:
        return sum(t for _, _, t in self.lineas.values())

    @property
    def ramas_cubiertas(self) -> int:
        return sum(c for _, c, _ in self.lineas.values())

    @property
    def pct_lineas(self) -> float:
        return 100.0 * self.lineas_cubiertas / self.lineas_total if self.lineas_total else 0.0

    @property
    def pct_ramas(self) -> float:
        return 100.0 * self.ramas_cubiertas / self.ramas_total if self.ramas_total else self.pct_lineas

    @property
    def pct_promedio(self) -> float:
        """Cobertura usada para el bloqueo: promedio de líneas y ramas (solo líneas si no hay ramas)."""
        if not self.ramas_total:
            return self.pct_lineas
        return (self.pct_lineas + self.pct_ramas) / 2.0

    def combinar(self, otra: "Cobertura") -> "Cobertura":
        """Unión de dos informes: por línea, el mayor número de ejecuciones y de ramas cubiertas.
        Es una cota inferior de la cobertura conjunta de las dos suites."""
        nueva = Cobertura()
        for n in set(self.lineas) | set(otra.lineas):
            a = self.lineas.get(n, (0, 0, 0))
            b = otra.lineas.get(n, (0, 0, 0))
            nueva.lineas[n] = (max(a[0], b[0]), max(a[1], b[1]), max(a[2], b[2]))
        return nueva

    def informe_texto(self, max_items: int = 80) -> str:
        """Texto para E2: líneas que la suite no ejecuta y ramas parcialmente cubiertas."""
        sin = sorted(n for n, (h, _, _) in self.lineas.items() if h == 0)
        parc = sorted((n, c, t) for n, (h, c, t) in self.lineas.items() if h > 0 and t > 0 and c < t)
        out = [f"Cobertura de líneas: {self.pct_lineas:.1f} % ({self.lineas_cubiertas}/{self.lineas_total})",
               f"Cobertura de ramas: {self.pct_ramas:.1f} % ({self.ramas_cubiertas}/{self.ramas_total})"]
        out.append("Líneas que la suite no ejecuta: " + (_rangos(sin) if sin else "ninguna"))
        if parc:
            items = [f"línea {n} ({c}/{t} ramas)" for n, c, t in parc[:max_items]]
            out.append("Ramas parcialmente cubiertas: " + "; ".join(items))
        else:
            out.append("Ramas parcialmente cubiertas: ninguna")
        return "\n".join(out)


def _rangos(nums: List[int]) -> str:
    if not nums:
        return ""
    partes, ini, prev = [], nums[0], nums[0]
    for n in nums[1:]:
        if n == prev + 1:
            prev = n
            continue
        partes.append(f"{ini}" if ini == prev else f"{ini}-{prev}")
        ini = prev = n
    partes.append(f"{ini}" if ini == prev else f"{ini}-{prev}")
    return ", ".join(partes)


def leer_xml(ruta: Path, clase: str) -> Cobertura:
    """Lee coverage.xml y agrega la clase y sus clases internas (Clase$Interna)."""
    cob = Cobertura()
    arbol = ET.parse(str(ruta))
    for c in arbol.iter("class"):
        nombre = c.get("name", "")
        if nombre != clase and not nombre.startswith(clase + "$"):
            continue
        for ln in c.iter("line"):
            n = int(ln.get("number"))
            hits = int(ln.get("hits", "0"))
            cc, ct = 0, 0
            if ln.get("branch") == "true":
                m = _CC.search(ln.get("condition-coverage", ""))
                if m:
                    cc, ct = int(m.group(1)), int(m.group(2))
            previo = cob.lineas.get(n, (0, 0, 0))
            cob.lineas[n] = (max(previo[0], hits), max(previo[1], cc), max(previo[2], ct))
    return cob
