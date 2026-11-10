# -*- coding: utf-8 -*-
"""Orquestador: módulos 4 a 8 para cada caso, estrategia y repetición.

Uso (ver README.md):
    python -m prototipo.ejecutar --casos casos/muestra_piloto.csv --piloto
    python -m prototipo.ejecutar --casos casos/muestra_piloto.csv --repeticiones 3
    python -m prototipo.ejecutar --simulado --casos casos/ejemplo_simulado.csv      # sin Defects4J ni API
"""
from __future__ import annotations

import argparse
import csv
import json
import time
import traceback
from pathlib import Path
from typing import Dict, List, Optional

from . import contexto, javautil
from .backend import Backend, Caso, CasoInvalido, Preparado
from .config import Config, grupo_cobertura
from .registro import Registro

CAUSAS = ("no_compila", "pruebas_inestables", "falsos_positivos", "sin_diferencia", "limite_o_error")


def leer_casos(ruta: Path) -> List[Caso]:
    with open(ruta, newline="", encoding="utf-8") as f:
        return [Caso(r["proyecto"], int(r["fallo"]), r["clase"]) for r in csv.DictReader(f)]


def _extraer_lista(texto: str, maximo: int) -> str:
    lineas = [l.strip() for l in texto.splitlines() if l.strip()[:2].rstrip(".)").isdigit() or l.strip().startswith("-")]
    return "\n".join(lineas[:maximo]) or texto.strip()[:1500]


def ejecutar_corrida(backend: Backend, llm, prep: Preparado, estrategia: str, rep: int, cfg: Config,
                     carpeta_registros: Path) -> Dict:
    """Una corrida = un caso, una estrategia y una repetición. Devuelve el registro (fila del CSV)."""
    caso = prep.caso
    t0 = time.time()
    llamadas = tin = tout = 0
    artefactos: Dict[str, str] = {}
    fila: Dict = dict(caso=caso.id, proyecto=caso.proyecto, fallo=caso.fallo, clase=caso.clase,
                      cobertura_inicial=round(prep.cobertura_inicial.pct_promedio, 2),
                      grupo=grupo_cobertura(prep.cobertura_inicial.pct_promedio, cfg),
                      estrategia=estrategia, repeticion=rep, pruebas_generadas=0, compilan=0, estables=0,
                      detecta_fallo=0, pruebas_detectoras=0, falsos_positivos=0,
                      delta_cobertura_lineas=0.0, delta_cobertura_ramas=0.0, causa_sin_deteccion="",
                      error="")
    try:
        sis = contexto.sistema()
        lista: Optional[str] = None

        # --- fase previa de E3 y E4
        if estrategia in contexto.CON_FASE_PREVIA:
            p1 = contexto.prompt_fase1(estrategia, prep, cfg)
            r1 = llm.generar(sis, p1)
            llamadas += 1; tin += r1.tokens_entrada; tout += r1.tokens_salida
            lista = _extraer_lista(r1.texto, cfg.max_pruebas_fase1)
            artefactos["fase1_prompt.txt"] = p1
            artefactos["fase1_respuesta.txt"] = r1.texto

        # --- generación
        p2 = contexto.prompt_generacion(estrategia, prep, lista)
        r2 = llm.generar(sis, p2)
        llamadas += 1; tin += r2.tokens_entrada; tout += r2.tokens_salida
        artefactos["generacion_prompt.txt"] = p2
        artefactos["generacion_respuesta.txt"] = r2.texto
        codigo = _normalizar(javautil.extraer_codigo_java(r2.texto), prep)

        # --- módulo 6: compilación con hasta N correcciones
        compila = False
        errores = ""
        for intento in range(cfg.max_correcciones + 1):
            backend.escribir_prueba(prep, "b", codigo)
            compila, errores = backend.compilar(prep, "b")
            if compila:
                break
            if intento == cfg.max_correcciones:
                break
            pc = contexto.prompt_correccion(prep, codigo, errores)
            rc = llm.generar(sis, pc)
            llamadas += 1; tin += rc.tokens_entrada; tout += rc.tokens_salida
            artefactos[f"correccion{intento + 1}_prompt.txt"] = pc
            artefactos[f"correccion{intento + 1}_respuesta.txt"] = rc.texto
            codigo = _normalizar(javautil.extraer_codigo_java(rc.texto), prep)
        metodos = javautil.listar_metodos_prueba(codigo)
        fila["pruebas_generadas"] = len(metodos)
        if not compila or not metodos:
            fila["causa_sin_deteccion"] = "no_compila"
            artefactos["errores_compilacion.txt"] = errores
        else:
            fila["compilan"] = len(metodos)
            # --- estabilidad: se ejecuta N veces en la versión con fallo
            resultados = [backend.ejecutar(prep, "b", metodos) for _ in range(cfg.ejecuciones_estabilidad)]
            estables = [m for m in metodos if len({r[m] for r in resultados}) == 1]
            inestables = [m for m in metodos if m not in estables]
            if inestables:
                codigo, _ = javautil.eliminar_metodos(codigo, inestables)
                backend.escribir_prueba(prep, "b", codigo)
                ok, err = backend.compilar(prep, "b")
                if not ok:
                    estables = []
                    artefactos["errores_compilacion.txt"] = err
            fila["estables"] = len(estables)
            artefactos["pruebas_finales.java"] = codigo
            if not estables:
                fila["causa_sin_deteccion"] = "pruebas_inestables"
            else:
                res_b = {m: resultados[0][m] for m in estables}
                # --- módulo 7: evaluación en la versión corregida
                backend.escribir_prueba(prep, "f", codigo)
                okf, errf = backend.compilar(prep, "f")
                if not okf:
                    fila["falsos_positivos"] = len(estables)
                    fila["causa_sin_deteccion"] = "falsos_positivos"
                    artefactos["errores_compilacion_corregida.txt"] = errf
                else:
                    res_f = backend.ejecutar(prep, "f", estables)
                    detectoras = [m for m in estables if res_f[m] and not res_b[m]]
                    falsos = [m for m in estables if not res_f[m]]
                    fila["pruebas_detectoras"] = len(detectoras)
                    fila["detecta_fallo"] = 1 if detectoras else 0
                    fila["falsos_positivos"] = len(falsos)
                    if not detectoras:
                        fila["causa_sin_deteccion"] = "falsos_positivos" if len(falsos) == len(estables) else "sin_diferencia"
                    # cobertura conjunta (suite original + pruebas conservadas)
                    conj = backend.cobertura_conjunta(prep)
                    ini = prep.cobertura_inicial
                    fila["delta_cobertura_lineas"] = round(conj.pct_lineas - ini.pct_lineas, 2)
                    fila["delta_cobertura_ramas"] = round(conj.pct_ramas - ini.pct_ramas, 2)
    except Exception as e:   # error del entorno o de la API: se registra y se sigue con el resto
        fila["causa_sin_deteccion"] = "limite_o_error"
        fila["error"] = f"{type(e).__name__}: {e}"[:300]
        artefactos["traza_error.txt"] = traceback.format_exc()
    finally:
        try:
            backend.limpiar(prep)
        except Exception:
            pass

    fila.update(llamadas=llamadas, tokens_entrada=tin, tokens_salida=tout,
                segundos=round(time.time() - t0, 2), modelo=cfg.modelo,
                parametros=json.dumps(cfg.parametros(), ensure_ascii=False), semilla=cfg.semilla,
                simulado=1 if backend.simulado else 0)
    _guardar_artefactos(carpeta_registros / f"{caso.id}_{estrategia}_r{rep}", artefactos)
    return fila


def _normalizar(codigo: str, prep: Preparado) -> str:
    """Fuerza el nombre de la clase y el paquete esperados (la clase generada se llama <Clase>LLMTest)."""
    esperado = prep.test_nuevo.rsplit(".", 1)[-1]
    actual = javautil.nombre_clase_publica(codigo)
    if actual and actual != esperado:
        codigo = javautil.renombrar_clase(codigo, actual, esperado)
    pkg = prep.caso.paquete
    if pkg and javautil.paquete(codigo) != pkg:
        import re
        if javautil.paquete(codigo):
            codigo = re.sub(r"^\s*package\s+[\w.]+\s*;", f"package {pkg};", codigo, count=1, flags=re.M)
        else:
            codigo = f"package {pkg};\n\n" + codigo
    return codigo


def _guardar_artefactos(carpeta: Path, artefactos: Dict[str, str]) -> None:
    carpeta.mkdir(parents=True, exist_ok=True)
    for nombre, texto in artefactos.items():
        (carpeta / nombre).write_text(texto, encoding="utf-8")


def main(argv: Optional[List[str]] = None) -> None:
    ap = argparse.ArgumentParser(description="Ejecuta el experimento (módulos 4 a 8).")
    ap.add_argument("--casos", required=True, help="CSV con columnas proyecto, fallo, clase")
    ap.add_argument("--estrategias", nargs="+", default=list(contexto.ESTRATEGIAS), choices=contexto.ESTRATEGIAS)
    ap.add_argument("--repeticiones", type=int, default=None)
    ap.add_argument("--max-casos", type=int, default=None, help="usa solo los primeros N casos")
    ap.add_argument("--piloto", action="store_true", help="un caso por proyecto, 1 repetición")
    ap.add_argument("--simulado", action="store_true", help="sin Defects4J ni API (solo para probar el flujo)")
    ap.add_argument("--salida", default=None, help="CSV de resultados (por defecto resultados/ejecuciones.csv)")
    a = ap.parse_args(argv)

    cfg = Config()
    if a.repeticiones:
        cfg.repeticiones = a.repeticiones
    casos = leer_casos(Path(a.casos))
    if a.piloto:
        vistos, unicos = set(), []
        for c in casos:
            if c.proyecto not in vistos:
                vistos.add(c.proyecto); unicos.append(c)
        casos = unicos
        cfg.repeticiones = a.repeticiones or 1
    if a.max_casos:
        casos = casos[:a.max_casos]

    if a.simulado:
        from .simulador import Simulado
        from .llm import ClienteSimulado
        backend, llm = Simulado(cfg), ClienteSimulado(cfg)
        salida = Path(a.salida or Path(cfg.resultados) / "ejecuciones_simuladas.csv")
    else:
        from .d4j import D4J
        from .llm import ClienteClaude
        backend, llm = D4J(cfg), ClienteClaude(cfg)
        salida = Path(a.salida or Path(cfg.resultados) / "ejecuciones.csv")
    reg = Registro(salida)
    carpeta = salida.parent / ("registros_simulados" if a.simulado else "registros")

    total = len(casos) * len(a.estrategias) * cfg.repeticiones
    hechos = 0
    print(f"Casos: {len(casos)}; estrategias: {a.estrategias}; repeticiones: {cfg.repeticiones}; "
          f"corridas: {total}; modelo: {cfg.modelo}{' (SIMULADO)' if a.simulado else ''}")
    for caso in casos:
        pendientes = [(e, r) for e in a.estrategias for r in range(1, cfg.repeticiones + 1)
                      if not reg.existe(caso.id, e, r)]
        if not pendientes:
            hechos += len(a.estrategias) * cfg.repeticiones
            continue
        try:
            prep = backend.preparar(caso)
        except CasoInvalido as e:
            print(f"[{caso.id}] descartado: {e}")
            continue
        except Exception as e:
            print(f"[{caso.id}] error al preparar: {e}")
            continue
        for e, r in pendientes:
            fila = ejecutar_corrida(backend, llm, prep, e, r, cfg, carpeta)
            reg.agregar(fila)
            hechos += 1
            print(f"[{hechos}/{total}] {caso.id} {e} r{r}: detecta={fila['detecta_fallo']} "
                  f"estables={fila['estables']}/{fila['pruebas_generadas']} llamadas={fila['llamadas']} "
                  f"{fila['causa_sin_deteccion'] or 'ok'}")
    print(f"Resultados en {salida}")


if __name__ == "__main__":
    main()
