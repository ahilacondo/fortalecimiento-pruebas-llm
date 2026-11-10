# -*- coding: utf-8 -*-
"""Módulo 5: cliente LLM. Real (API de Anthropic) y simulado (sin red)."""
from __future__ import annotations

import hashlib
import random
import time
from dataclasses import dataclass
from typing import List

from .config import Config


@dataclass
class Respuesta:
    texto: str
    tokens_entrada: int
    tokens_salida: int
    segundos: float


class ClienteClaude:
    """Llama a la API de Mensajes de Anthropic. Requiere la variable ANTHROPIC_API_KEY."""
    simulado = False

    def __init__(self, cfg: Config):
        try:
            import anthropic  # noqa: WPS433
        except ImportError as e:  # pragma: no cover
            raise SystemExit("Falta el paquete 'anthropic'. Ejecute: pip install -r requirements.txt") from e
        self._anthropic = anthropic
        self.cfg = cfg
        self.cliente = anthropic.Anthropic(max_retries=5)   # reintenta límites de uso y errores 5xx
        self._usar_temperatura = True

    def generar(self, sistema: str, usuario: str) -> Respuesta:
        t0 = time.time()
        args = dict(model=self.cfg.modelo, max_tokens=self.cfg.max_tokens, system=sistema,
                    messages=[{"role": "user", "content": usuario}])
        if self._usar_temperatura:
            args["temperature"] = self.cfg.temperatura
        try:
            r = self.cliente.messages.create(**args)
        except (self._anthropic.BadRequestError, TypeError) as e:
            # Algunas versiones del SDK o modelos no aceptan 'temperature'; se reintenta sin ese parámetro.
            if "temperature" in str(e).lower() and self._usar_temperatura:
                print("AVISO: no se puede enviar 'temperature'; se continúa sin ese parámetro "
                      "(temperatura por defecto del modelo). Anótelo en el informe.")
                self._usar_temperatura = False
                self.cfg.temperatura = None      # el registro de parámetros refleja lo que realmente se usó
                args.pop("temperature", None)
                r = self.cliente.messages.create(**args)
            else:
                raise
        texto = "".join(b.text for b in r.content if getattr(b, "type", "") == "text")
        if not texto.strip():
            # p. ej. el modelo agotó max_tokens razonando y no escribió código: no es un fallo de las pruebas
            raise RuntimeError(f"respuesta sin texto (stop_reason={getattr(r, 'stop_reason', '?')}, "
                               f"tokens_salida={r.usage.output_tokens}); aumente MAX_TOKENS en .env")
        return Respuesta(texto, r.usage.input_tokens, r.usage.output_tokens, time.time() - t0)


class ClienteSimulado:
    """Respuestas ficticias para probar el flujo. Nunca llama a la red."""
    simulado = True

    def __init__(self, cfg: Config, semilla: str = ""):
        self.cfg = cfg
        self.semilla = semilla
        self._n = 0

    def generar(self, sistema: str, usuario: str) -> Respuesta:
        self._n += 1
        h = int(hashlib.md5((self.semilla + usuario[:300] + str(len(usuario)) + str(self._n)).encode()).hexdigest(), 16)
        rnd = random.Random(h)
        estr = "E1"
        for e in ("E2", "E3", "E4"):
            if f"BLOQUE {e}" in usuario or f"estrategia: {e}" in usuario:
                estr = e
        if "FASE 1" in usuario:
            n = rnd.randint(3, 6)
            texto = "\n".join(f"{i + 1}. comportamiento {i + 1} sin verificar" for i in range(n))
        else:
            n = rnd.randint(3, 7)
            cuerpo = "\n".join(f"    @Test\n    public void prueba{i + 1}() {{ assertTrue(true); }}\n" for i in range(n))
            texto = ("```java\npackage simulado;\nimport org.junit.Test;\nimport static org.junit.Assert.*;\n"
                     f"// estrategia: {estr}\npublic class ClaseLLMTest {{\n{cuerpo}}}\n```")
        return Respuesta(texto, len(usuario) // 4, len(texto) // 4, 0.01)
