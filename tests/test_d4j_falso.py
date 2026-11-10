# -*- coding: utf-8 -*-
"""Prueba del código de conexión con Defects4J usando un Defects4J de juguete (tests/d4j_falso).

Verifica: retiro de la prueba disparadora, empaquetado de suites externas, compilación con corrección,
estabilidad, detección frente a la versión corregida y registro de resultados. NO prueba Defects4J real.
Se omite si no hay Java o los JAR de JUnit (variables JUNIT_JAR y HAMCREST_JAR).
"""
import os
import shutil
from pathlib import Path

import pytest

from prototipo import ejecutar
from prototipo.backend import Caso
from prototipo.config import Config
from prototipo.d4j import D4J
from prototipo.llm import Respuesta

JUNIT = os.environ.get("JUNIT_JAR", "")
HAMC = os.environ.get("HAMCREST_JAR", "")
pytestmark = pytest.mark.skipif(not (shutil.which("javac") and Path(JUNIT).exists() and Path(HAMC).exists()),
                                reason="faltan javac o los JAR de JUnit")

CLASE_B = "package pkg;\npublic class StringTools {\n  public static boolean esMayor(int edad) { return edad > 18; }\n}\n"
CLASE_F = CLASE_B.replace("edad > 18", "edad >= 18")
TEST = '''package pkg;
import org.junit.Test;
import static org.junit.Assert.*;
public class StringToolsTest {
    @Test public void testAdulto() { assertTrue(StringTools.esMayor(30)); }
    @Test public void testMenor() { assertFalse(StringTools.esMayor(5)); }
    /** Expone el fallo. */
    @Test public void testBug() { assertTrue(StringTools.esMayor(18)); }
}
'''


class LLMGuion:
    simulado = False

    def __init__(self, respuestas):
        self.r = list(respuestas)
        self.prompts = []

    def generar(self, sistema, usuario):
        self.prompts.append(usuario)
        return Respuesta(self.r.pop(0), 100, 50, 0.0)


def _datos(tmp):
    for v, clase in (("b", CLASE_B), ("f", CLASE_F)):
        base = tmp / "datos" / "Toy" / "1" / v
        (base / "src/main/pkg").mkdir(parents=True)
        (base / "src/test/pkg").mkdir(parents=True)
        (base / "src/main/pkg/StringTools.java").write_text(clase)
        (base / "src/test/pkg/StringToolsTest.java").write_text(TEST)
    d4j_home = tmp / "d4j"
    (d4j_home / "framework/bin").mkdir(parents=True)
    shutil.copy(Path(__file__).parent / "d4j_falso/defects4j", d4j_home / "framework/bin/defects4j")
    return d4j_home


def test_flujo_real_con_cli_de_juguete(tmp_path, monkeypatch):
    monkeypatch.setenv("FAKE_D4J_DATA", str(tmp_path / "datos"))
    d4j_home = _datos(tmp_path)
    cfg = Config()
    cfg.defects4j_home = str(d4j_home)
    cfg.trabajo = str(tmp_path / "trabajo")
    backend = D4J(cfg)

    prep = backend.preparar(Caso("Toy", 1, "pkg.StringTools"))
    assert "testBug" not in prep.codigo_test                     # se retiró la prueba disparadora
    assert "testAdulto" in prep.codigo_test
    assert prep.cobertura_inicial.lineas_total > 0

    malo = ("```java\npackage pkg;\nimport org.junit.Test;\nimport static org.junit.Assert.*;\n"
            "public class StringToolsLLMTest {\n @Test public void t1() { assertTrue(StringTools.noExiste(1)); }\n}\n```")
    bueno = ("```java\npackage pkg;\nimport org.junit.Test;\nimport static org.junit.Assert.*;\n"
             "public class StringToolsLLMTest {\n"
             " @Test public void limite() { assertTrue(StringTools.esMayor(18)); }\n"
             " @Test public void otro() { assertTrue(StringTools.esMayor(40)); }\n"
             " @Test public void incorrecta() { assertTrue(StringTools.esMayor(3)); }\n}\n```")
    llm = LLMGuion([malo, bueno])
    fila = ejecutar.ejecutar_corrida(backend, llm, prep, "E1", 1, cfg, tmp_path / "registros")

    assert fila["llamadas"] == 2                       # una generación y una corrección
    assert any("noExiste" in p or "cannot find symbol" in p for p in llm.prompts[1:])   # el error se devolvió al LLM
    assert fila["pruebas_generadas"] == 3 and fila["estables"] == 3
    assert fila["detecta_fallo"] == 1 and fila["pruebas_detectoras"] == 1    # `limite` pasa en f y falla en b
    assert fila["falsos_positivos"] == 1                                    # `incorrecta` falla también en f
    assert fila["causa_sin_deteccion"] == ""
    assert (tmp_path / "registros" / "Toy-1_E1_r1" / "generacion_prompt.txt").exists()
