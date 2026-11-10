# -*- coding: utf-8 -*-
"""Módulo 4: constructor de contexto. Arma lo que recibe el LLM según la estrategia.

La plantilla es la misma para las cuatro estrategias; solo cambia el bloque (Anexo B del informe).
"""
from __future__ import annotations

from pathlib import Path
from string import Template
from typing import Optional, Tuple

from .backend import Preparado
from .config import RAIZ, Config

DIR = RAIZ / "instrucciones"
ESTRATEGIAS = ("E1", "E2", "E3", "E4")
CON_FASE_PREVIA = ("E3", "E4")


def _plantilla(nombre: str) -> Template:
    return Template((DIR / nombre).read_text(encoding="utf-8"))


def sistema() -> str:
    return (DIR / "sistema.txt").read_text(encoding="utf-8").strip()


def _comunes(prep: Preparado) -> dict:
    c = prep.caso
    return dict(clase=c.clase, clase_simple=c.clase_simple, paquete=c.paquete,
                clase_test=prep.test_nuevo.rsplit(".", 1)[-1], test_original=prep.test_original,
                codigo_clase=prep.codigo_clase.rstrip(), codigo_test=prep.codigo_test.rstrip())


def prompt_fase1(estrategia: str, prep: Preparado, cfg: Config) -> str:
    assert estrategia in CON_FASE_PREVIA
    datos = _comunes(prep)
    datos.update(max=cfg.max_pruebas_fase1, registro=prep.registro_ejecucion,
                 javadoc=prep.javadoc or "(la clase no tiene documentación Javadoc)")
    return _plantilla(f"fase1_{estrategia.lower()}.txt").substitute(datos)


def prompt_generacion(estrategia: str, prep: Preparado, lista: Optional[str] = None) -> str:
    datos = _comunes(prep)
    if estrategia == "E1":
        bloque = ""
    elif estrategia == "E2":
        bloque = _plantilla("bloque_e2.txt").substitute(informe=prep.cobertura_inicial.informe_texto())
    elif estrategia == "E3":
        bloque = _plantilla("bloque_e3.txt").substitute(registro=prep.registro_ejecucion, lista=lista or "")
    elif estrategia == "E4":
        bloque = _plantilla("bloque_e4.txt").substitute(
            javadoc=prep.javadoc or "(la clase no tiene documentación Javadoc)", lista=lista or "")
    else:
        raise ValueError(f"Estrategia desconocida: {estrategia}")
    datos["bloque"] = bloque
    return _plantilla("base.txt").substitute(datos)


def prompt_correccion(prep: Preparado, codigo_previo: str, errores: str) -> str:
    datos = _comunes(prep)
    return _plantilla("correccion.txt").substitute(paquete=datos["paquete"], clase_test=datos["clase_test"],
                                                   codigo_previo=codigo_previo.rstrip(), errores=errores)
