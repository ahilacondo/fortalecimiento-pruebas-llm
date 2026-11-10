# -*- coding: utf-8 -*-
"""Backend simulado: NO usa Defects4J. Sirve para probar el flujo y los scripts de análisis.

Los datos que produce son ficticios (columna `simulado` = 1) y NO deben usarse en el informe.
"""
from __future__ import annotations

import hashlib
import random
from typing import Dict, List, Tuple

from . import javautil
from .backend import Backend, Caso, Preparado
from .cobertura import Cobertura
from .config import Config

# Probabilidad ficticia de que una prueba detecte el fallo, solo para generar datos de prueba.
_P_DETECTA = {"E1": 0.10, "E2": 0.11, "E3": 0.20, "E4": 0.19}


def _rnd(*partes) -> random.Random:
    h = hashlib.md5("|".join(map(str, partes)).encode()).hexdigest()
    return random.Random(int(h, 16))


class Simulado(Backend):
    simulado = True

    def __init__(self, cfg: Config):
        self.cfg = cfg
        self._estado: Dict[str, dict] = {}
        self._llamadas = 0

    def preparar(self, caso: Caso) -> Preparado:
        r = _rnd("prep", caso.id)
        lineas = 80
        cob = Cobertura()
        objetivo = r.choice([45, 55, 70, 80, 92, 96])
        for n in range(1, lineas + 1):
            hits = 1 if r.random() * 100 < objetivo else 0
            cob.lineas[n] = (hits, 0, 0)
        codigo = f"package {caso.paquete};\n\npublic class {caso.clase_simple} {{ /** Suma. */ public int f(int a) {{ return a; }} }}\n"
        test = f"package {caso.paquete};\n\npublic class {caso.clase_simple}Test {{\n  @Test public void testF() {{ }}\n}}\n"
        prep = Preparado(caso=caso, codigo_clase=codigo, codigo_test=test,
                         javadoc=javautil.extraer_javadoc(codigo), registro_ejecucion="Pruebas ejecutadas: 1; fallos: 0",
                         cobertura_inicial=cob, test_original=caso.clase + "Test", test_nuevo=caso.clase + "LLMTest")
        return prep

    def escribir_prueba(self, prep: Preparado, version: str, codigo: str) -> None:
        self._estado[prep.caso.id] = {"codigo": codigo, "n": len(self._estado) + 1}

    def compilar(self, prep: Preparado, version: str) -> Tuple[bool, str]:
        cod = self._estado[prep.caso.id]["codigo"]
        r = _rnd("comp", cod)
        ok = r.random() > 0.15
        return ok, "" if ok else "error: cannot find symbol (simulado)"

    def ejecutar(self, prep: Preparado, version: str, metodos: List[str]) -> Dict[str, bool]:
        cod = self._estado[prep.caso.id]["codigo"]
        # estrategia deducida del texto de la respuesta simulada no está disponible aquí:
        # se usa una probabilidad que depende del grupo de cobertura para dar datos variados.
        import re
        m_est = re.search(r"estrategia: (E\d)", cod)
        estr = m_est.group(1) if m_est else "E1"
        grupo_alto = prep.cobertura_inicial.pct_promedio >= self.cfg.umbral_alta
        p = _P_DETECTA[estr] * (0.4 if (estr == "E2" and grupo_alto) else 1.0)
        dificultad = _rnd("dif", prep.caso.id).random()          # algunos casos casi nunca se detectan
        p *= 0.0 if dificultad < 0.3 else 1.5
        out: Dict[str, bool] = {}
        self._llamadas += 1
        for m in metodos:
            r = _rnd("run", prep.caso.id, cod, m)
            if version == "b" and _rnd("ruido", self._llamadas, m).random() < 0.02:
                out[m] = not (r.random() > p)              # ruido: prueba inestable
                continue
            if version == "b":
                out[m] = r.random() > p                  # falla en la versión con fallo si "detecta"
            else:
                out[m] = r.random() > 0.08               # algunas fallan en la corregida (falsos positivos)
        return out

    def cobertura_conjunta(self, prep: Preparado) -> Cobertura:
        cod = self._estado[prep.caso.id]["codigo"]
        r = _rnd("cob", cod)
        nueva = Cobertura()
        for n, (h, c, t) in prep.cobertura_inicial.lineas.items():
            nueva.lineas[n] = (1 if h > 0 or r.random() < 0.15 else 0, c, t)
        return nueva

    def limpiar(self, prep: Preparado) -> None:
        self._estado.pop(prep.caso.id, None)
