# -*- coding: utf-8 -*-
"""Interfaz común entre el orquestador y el entorno de ejecución.

Hay dos implementaciones: `D4J` (real, usa los comandos de Defects4J) y `Simulado`
(sin Defects4J ni API, solo para probar el flujo y los scripts de análisis).
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Dict, List, Tuple

from .cobertura import Cobertura


class CasoInvalido(Exception):
    """El caso no cumple los criterios de selección (se descarta y se registra el motivo)."""


@dataclass(frozen=True)
class Caso:
    proyecto: str
    fallo: int
    clase: str          # nombre completo, p. ej. org.apache.commons.lang3.StringUtils

    @property
    def id(self) -> str:
        return f"{self.proyecto}-{self.fallo}"

    @property
    def clase_simple(self) -> str:
        return self.clase.rsplit(".", 1)[-1]

    @property
    def paquete(self) -> str:
        return self.clase.rsplit(".", 1)[0] if "." in self.clase else ""


@dataclass
class Preparado:
    caso: Caso
    codigo_clase: str
    codigo_test: str                 # suite de partida (sin las pruebas disparadoras)
    javadoc: str
    registro_ejecucion: str          # lista de pruebas ejecutadas y su resultado
    cobertura_inicial: Cobertura
    test_original: str               # nombre completo de la clase de prueba original
    test_nuevo: str                  # nombre completo de la clase que se genera
    extra: dict = field(default_factory=dict)


class Backend(ABC):
    simulado = False

    @abstractmethod
    def preparar(self, caso: Caso) -> Preparado:
        """Módulos 2 y 3: descarga, retira pruebas disparadoras y mide la cobertura inicial."""

    @abstractmethod
    def escribir_prueba(self, prep: Preparado, version: str, codigo: str) -> None:
        """Escribe la clase generada en la versión 'b' (con fallo) o 'f' (corregida)."""

    @abstractmethod
    def compilar(self, prep: Preparado, version: str) -> Tuple[bool, str]:
        """Compila el proyecto con la clase generada. Devuelve (ok, mensajes de error)."""

    @abstractmethod
    def ejecutar(self, prep: Preparado, version: str, metodos: List[str]) -> Dict[str, bool]:
        """Ejecuta la clase generada. Devuelve método -> True si pasa."""

    @abstractmethod
    def cobertura_conjunta(self, prep: Preparado) -> Cobertura:
        """Cobertura de la clase con la suite original más la clase generada (versión 'b')."""

    @abstractmethod
    def limpiar(self, prep: Preparado) -> None:
        """Retira la clase generada de las dos versiones."""
