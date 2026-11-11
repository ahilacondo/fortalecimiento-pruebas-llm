# -*- coding: utf-8 -*-
"""Revisión manual de las detecciones (Tabla 19 del informe).

Para cada corrida que detectó el fallo:
  1. vuelve a ejecutar las pruebas finales de la corrida en la versión con fallo (b) y en la corregida (f),
  2. identifica la(s) prueba(s) que pasan en f y fallan en b (la detección),
  3. genera una ficha en Markdown con la corrección del fallo (diff b -> f), el código de la prueba
     detectora y el mensaje de fallo en b, para que una persona confirme que la prueba falla por el
     defecto real y no por otra causa (amenaza de constructo de la Tabla 30).

Uso (dentro de WSL, con Defects4J y el entorno virtual activos):
    python analisis/revisar.py --proyecto Codec --registros resultados/Codec/registros
Salida: resultados/revision/<caso>_<estrategia>.md y resultados/revision/detecciones_<proyecto>.csv
"""
from __future__ import annotations

import argparse
import csv
import re
import subprocess
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(RAIZ))

from prototipo import javautil                       # noqa: E402
from prototipo.backend import Caso                   # noqa: E402
from prototipo.config import Config                  # noqa: E402
from prototipo.d4j import D4J                        # noqa: E402


def extraer_metodo(src: str, nombre: str) -> str:
    m = re.search(r"void\s+" + re.escape(nombre) + r"\s*\(", src)
    if not m:
        return f"// no se encontró el método {nombre}"
    abre = src.find("{", m.end())
    fin = javautil._cerrar_llave(src, abre)
    ini = javautil._inicio_con_anotaciones(src, src.rfind("\n", 0, m.start()) + 1)
    return src[ini:fin].rstrip()


def diff_fix(d4j: D4J, caso: Caso, prep) -> str:
    db, df = Path(prep.extra["dir_b"]), Path(prep.extra["dir_f"])
    rel = d4j.exportar(db, "dir.src.classes")
    p = subprocess.run(["diff", "-ru", str(db / rel), str(df / rel)], capture_output=True, text=True)
    texto = p.stdout.replace(str(db / rel), "b").replace(str(df / rel), "f")
    return texto[:6000] + ("\n... (recortado)" if len(texto) > 6000 else "")


def mensajes_fallo_b(d4j: D4J, prep) -> dict:
    """Mensaje de fallo por prueba en la versión con fallo (de los archivos que deja defects4j test)."""
    out = {}
    d = Path(prep.extra["dir_b"])
    arch = d / "failing_tests"
    if not arch.exists():
        return out
    texto = arch.read_text(errors="replace")
    for bloque in re.split(r"^--- ", texto, flags=re.M):
        if not bloque.strip():
            continue
        cab, _, resto = bloque.partition("\n")
        nombre = cab.strip().split("::")[-1]
        out[nombre] = "\n".join(resto.splitlines()[:6])
    return out


def main() -> None:
    ap = argparse.ArgumentParser(description="Fichas de revisión manual de las detecciones.")
    ap.add_argument("--entrada", default=str(RAIZ / "resultados" / "ejecuciones.csv"))
    ap.add_argument("--proyecto", required=True)
    ap.add_argument("--registros", required=True, help="carpeta registros/ de ese proyecto")
    ap.add_argument("--salida", default=str(RAIZ / "resultados" / "revision"))
    a = ap.parse_args()

    filas = [r for r in csv.DictReader(open(a.entrada, newline="", encoding="utf-8"))
             if r["proyecto"] == a.proyecto and r["detecta_fallo"] == "1"]
    if not filas:
        print("No hay detecciones para", a.proyecto)
        return
    salida = Path(a.salida)
    salida.mkdir(parents=True, exist_ok=True)
    cfg = Config()
    d4j = D4J(cfg)
    resumen = []
    por_caso: dict = {}
    for r in filas:
        por_caso.setdefault((r["fallo"], r["clase"]), []).append(r)

    for (fallo, clase), corridas in por_caso.items():
        caso = Caso(a.proyecto, int(fallo), clase)
        print(f"== {caso.id}: preparando…", flush=True)
        prep = d4j.preparar(caso)
        fix = diff_fix(d4j, caso, prep)
        for r in corridas:
            carpeta = Path(a.registros) / f"{caso.id}_{r['estrategia']}_r{r['repeticion']}"
            codigo = (carpeta / "pruebas_finales.java").read_text(encoding="utf-8")
            metodos = javautil.listar_metodos_prueba(codigo)
            d4j.escribir_prueba(prep, "b", codigo)
            okb, errb = d4j.compilar(prep, "b")
            res_b = d4j.ejecutar(prep, "b", metodos)
            fallos_b = mensajes_fallo_b(d4j, prep)
            d4j.escribir_prueba(prep, "f", codigo)
            okf, _ = d4j.compilar(prep, "f")
            res_f = d4j.ejecutar(prep, "f", metodos)
            det = [m for m in metodos if res_f[m] and not res_b[m]]
            print(f"   {caso.id} {r['estrategia']}: detectoras={det}", flush=True)
            ficha = [f"# {caso.id} · {r['estrategia']} · repetición {r['repeticion']}",
                     "", f"- Clase: `{clase}`  ·  cobertura inicial {r['cobertura_inicial']} % ({r['grupo']})",
                     f"- Pruebas finales: {len(metodos)}; detectoras (pasan en f, fallan en b): {len(det)}",
                     f"- Cambio de cobertura: líneas {r['delta_cobertura_lineas']} pp, ramas {r['delta_cobertura_ramas']} pp",
                     "", "## Corrección del fallo (diff b → f)", "", "```diff", fix.strip() or "(sin diferencias en src)", "```", ""]
            for m in det:
                ficha += [f"## Prueba detectora: `{m}`", "", "```java", extraer_metodo(codigo, m), "```", "",
                          "Mensaje de fallo en la versión con fallo (b):", "", "```",
                          fallos_b.get(m, "(no disponible)"), "```", ""]
            ficha += ["## Veredicto del revisor", "", "- [ ] La prueba falla en b por el defecto que corrige el parche",
                      "- [ ] Falla por otro motivo (inestable, dependiente del entorno, otra diferencia)", ""]
            (salida / f"{caso.id}_{r['estrategia']}.md").write_text("\n".join(ficha), encoding="utf-8")
            resumen.append(dict(caso=caso.id, estrategia=r["estrategia"], pruebas_detectoras=";".join(det),
                                n_detectoras=len(det), veredicto="", comentario=""))
            d4j.limpiar(prep)
    with open(salida / f"detecciones_{a.proyecto}.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(resumen[0].keys()))
        w.writeheader(); w.writerows(resumen)
    print("Fichas en", salida)


if __name__ == "__main__":
    main()
