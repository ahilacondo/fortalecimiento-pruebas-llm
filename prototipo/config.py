# -*- coding: utf-8 -*-
"""Parámetros del experimento (Tabla de parámetros del informe, sección 3.5).

Todos se pueden cambiar con variables de entorno o con el archivo .env.
Los valores usados en cada ejecución se guardan en el registro de resultados.
"""
from __future__ import annotations

import os
from dataclasses import dataclass, field, asdict
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent


def _cargar_env() -> None:
    """Lee el archivo .env (clave=valor) sin dependencias externas."""
    ruta = RAIZ / ".env"
    if not ruta.exists():
        return
    for linea in ruta.read_text(encoding="utf-8").splitlines():
        linea = linea.strip()
        if not linea or linea.startswith("#") or "=" not in linea:
            continue
        k, v = linea.split("=", 1)
        os.environ.setdefault(k.strip(), v.strip().strip('"').strip("'"))


_cargar_env()


def _num(nombre: str, defecto: float) -> float:
    return float(os.getenv(nombre, defecto))


@dataclass
class Config:
    # --- LLM
    modelo: str = field(default_factory=lambda: os.getenv("CLAUDE_MODEL", "claude-sonnet-5-5"))
    temperatura: float = field(default_factory=lambda: _num("TEMPERATURA", 0.7))
    max_tokens: int = field(default_factory=lambda: int(_num("MAX_TOKENS", 4096)))
    # --- diseño
    semilla: int = 2026
    repeticiones: int = 3
    max_correcciones: int = 2          # correcciones por errores de compilación
    ejecuciones_estabilidad: int = 3
    max_pruebas_fase1: int = 8         # elementos de la lista de E3 y E4
    # --- grupos de cobertura inicial (promedio de líneas y ramas, en %)
    umbral_baja: float = 60.0
    umbral_alta: float = 90.0
    # --- límites de ejecución
    timeout_suite_s: int = 300         # criterio de selección: < 5 minutos
    timeout_compilar_s: int = 600
    # --- rutas
    defects4j_home: str = field(default_factory=lambda: os.getenv("D4J_HOME", str(Path.home() / "defects4j")))
    trabajo: str = field(default_factory=lambda: os.getenv("D4J_TRABAJO", str(Path.home() / "d4j_trabajo")))
    resultados: str = str(RAIZ / "resultados")

    def parametros(self) -> dict:
        d = asdict(self)
        for k in ("defects4j_home", "trabajo", "resultados"):
            d.pop(k)
        return d


def grupo_cobertura(cob: float, cfg: Config) -> str:
    if cob < cfg.umbral_baja:
        return "baja"
    if cob >= cfg.umbral_alta:
        return "alta"
    return "media"
