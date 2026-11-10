# -*- coding: utf-8 -*-
"""Análisis estadístico (secciones 3.5 y 5.2 del informe).

Lee resultados/ejecuciones.csv y genera, en analisis/salida/, las tablas del informe (CSV y Markdown)
y dos gráficos. Los datos simulados se excluyen a menos que se use --incluir-simulado.

    python analisis/analizar.py
    python analisis/analizar.py --entrada resultados/ejecuciones_simuladas.csv --incluir-simulado   # solo para probar
"""
from __future__ import annotations

import argparse
import itertools
from pathlib import Path

import numpy as np
import pandas as pd
from scipy import stats

ESTRATEGIAS = ["E1", "E2", "E3", "E4"]
NOMBRES = {"E1": "E1 Sin guía", "E2": "E2 Cobertura", "E3": "E3 Análisis de código y pruebas", "E4": "E4 Documentación"}
GRUPOS = ["baja", "media", "alta"]
CAUSAS = {"no_compila": "No compila", "pruebas_inestables": "Pruebas inestables",
          "falsos_positivos": "Falsos positivos", "sin_diferencia": "Sin diferencia",
          "limite_o_error": "Límite de intentos o costo"}


def vargha_delaney(x, y) -> float:
    """Â12: probabilidad de que un valor de x supere a uno de y (empates cuentan la mitad)."""
    x, y = np.asarray(x, float), np.asarray(y, float)
    if len(x) == 0 or len(y) == 0:
        return float("nan")
    mayor = (x[:, None] > y[None, :]).sum()
    igual = (x[:, None] == y[None, :]).sum()
    return float((mayor + 0.5 * igual) / (len(x) * len(y)))


def magnitud(a12: float) -> str:
    if np.isnan(a12):
        return "—"
    d = abs(a12 - 0.5)
    return "despreciable" if d < 0.06 else "pequeño" if d < 0.14 else "mediano" if d < 0.21 else "grande"


def ic_bootstrap(v, n=10000, semilla=2026, nivel=95):
    v = np.asarray(v, float)
    if len(v) == 0:
        return (float("nan"), float("nan"))
    rng = np.random.default_rng(semilla)
    medias = rng.choice(v, size=(n, len(v)), replace=True).mean(axis=1)
    a = (100 - nivel) / 2
    return (float(np.percentile(medias, a)), float(np.percentile(medias, 100 - a)))


def holm(pvalores):
    """Corrección de Holm. Devuelve los p ajustados en el orden original."""
    p = np.asarray(pvalores, float)
    orden = np.argsort(p)
    m = len(p)
    aj = np.empty(m)
    maximo = 0.0
    for rango, i in enumerate(orden):
        val = min(1.0, (m - rango) * p[i])
        maximo = max(maximo, val)
        aj[i] = maximo
    return aj


def wilcoxon_par(x, y):
    d = np.asarray(x, float) - np.asarray(y, float)
    if np.allclose(d, 0):
        return float("nan"), 1.0
    r = stats.wilcoxon(x, y, zero_method="wilcox", alternative="two-sided")
    return float(r.statistic), float(r.pvalue)


def pct(x, d=1):
    return "—" if pd.isna(x) else f"{x:.{d}f}"


def tasas_por_caso(df: pd.DataFrame) -> pd.DataFrame:
    """Filas = casos; columnas = estrategias; valor = proporción de repeticiones que detectan el fallo."""
    t = df.groupby(["caso", "estrategia"])["detecta_fallo"].mean().unstack("estrategia")
    return t.reindex(columns=ESTRATEGIAS)


def main():
    ap = argparse.ArgumentParser()
    raiz = Path(__file__).resolve().parent.parent
    ap.add_argument("--entrada", default=str(raiz / "resultados" / "ejecuciones.csv"))
    ap.add_argument("--salida", default=str(raiz / "analisis" / "salida"))
    ap.add_argument("--incluir-simulado", action="store_true")
    a = ap.parse_args()

    df = pd.read_csv(a.entrada)
    if not a.incluir_simulado:
        df = df[df["simulado"] == 0]
    if df.empty:
        raise SystemExit("No hay corridas reales en el archivo de entrada.")
    salida = Path(a.salida)
    salida.mkdir(parents=True, exist_ok=True)
    marca = "\n> DATOS SIMULADOS: no usar en el informe.\n" if (df["simulado"] == 1).any() else ""

    casos = df.drop_duplicates("caso")[["caso", "grupo", "cobertura_inicial"]].set_index("caso")
    tasas = tasas_por_caso(df)
    n_casos = len(tasas)
    md = ["# Resultados del análisis", marca, f"Casos: {n_casos}; corridas: {len(df)}\n"]

    # ---------------- Tabla 1: detección por estrategia (D1)
    filas = []
    for e in ESTRATEGIAS:
        v = tasas[e].dropna()
        lo, hi = ic_bootstrap(v)
        sub = df[df["estrategia"] == e]
        filas.append({"Estrategia": NOMBRES[e], "Casos con fallo detectado": f"{int((v > 0).sum())} de {len(v)}",
                      "Tasa media ± DE": f"{v.mean():.3f} ± {v.std(ddof=1) if len(v) > 1 else float('nan'):.3f}",
                      "IC 95 %": f"[{lo:.3f}; {hi:.3f}]",
                      "Pruebas nuevas por caso (mediana)": f"{sub.groupby('caso')['estables'].median().median():.1f}"})
    t1 = pd.DataFrame(filas)
    t1.to_csv(salida / "tabla_r1_deteccion.csv", index=False, encoding="utf-8")
    md += ["## Tabla r1. Detección de fallos reales por estrategia", t1.to_markdown(index=False), ""]

    # ---------------- Tabla 2: por cobertura inicial (PE3)
    filas = [{"Estrategia": "N.º de casos del grupo", **{g: int((casos["grupo"] == g).sum()) for g in GRUPOS}}]
    for e in ESTRATEGIAS:
        fila = {"Estrategia": NOMBRES[e]}
        for g in GRUPOS:
            ids = casos.index[casos["grupo"] == g]
            v = tasas.loc[tasas.index.isin(ids), e].dropna()
            fila[g] = f"{v.mean():.3f}" if len(v) else "—"
        filas.append(fila)
    t2 = pd.DataFrame(filas).rename(columns={"baja": "Baja (< 60 %)", "media": "Media (60 a 90 %)", "alta": "Alta (≥ 90 %)"})
    t2.to_csv(salida / "tabla_r2_cobertura.csv", index=False, encoding="utf-8")
    md += ["## Tabla r2. Tasa de detección por cobertura inicial", t2.to_markdown(index=False), ""]

    # ---------------- Tabla 3: validez, costo (D2 a D4)
    filas = []
    for e in ESTRATEGIAS:
        s = df[df["estrategia"] == e]
        compilan = 100 * (s["compilan"] > 0).mean()
        estables = 100 * s["estables"].sum() / s["compilan"].sum() if s["compilan"].sum() else float("nan")
        fp = 100 * s["falsos_positivos"].sum() / s["estables"].sum() if s["estables"].sum() else float("nan")
        por_caso = s.groupby("caso")
        filas.append({"Estrategia": e, "Compilan (%)": pct(compilan), "Estables (%)": pct(estables),
                      "Falsos positivos (%)": pct(fp),
                      "Llamadas por caso": pct(por_caso["llamadas"].mean().mean(), 1),
                      "Tokens por caso": f"{(por_caso['tokens_entrada'].mean() + por_caso['tokens_salida'].mean()).mean():.0f}",
                      "Tiempo (s)": pct(por_caso["segundos"].mean().mean(), 1),
                      "Δ cobertura líneas (pp)": pct(s["delta_cobertura_lineas"].mean(), 2),
                      "Δ cobertura ramas (pp)": pct(s["delta_cobertura_ramas"].mean(), 2)})
    t3 = pd.DataFrame(filas)
    t3.to_csv(salida / "tabla_r3_validez_costo.csv", index=False, encoding="utf-8")
    md += ["## Tabla r3. Validez, costo y cambio de cobertura (la legibilidad se agrega a mano, Anexo D)",
           t3.to_markdown(index=False), ""]

    # ---------------- Tabla 4: contrastes (PE2 y PE3)
    contrastes = []
    completos = tasas.dropna()
    if len(completos) >= 3:
        try:
            ch = stats.friedmanchisquare(*[completos[e] for e in ESTRATEGIAS])
            fried = (float(ch.statistic), float(ch.pvalue))
        except ValueError:
            fried = (float("nan"), 1.0)
    else:
        fried = (float("nan"), float("nan"))
    pares = [("E2", "E1"), ("E3", "E1"), ("E4", "E1"), ("E3", "E2"), ("E4", "E2")]
    pruebas = []
    for x, y in pares:
        par = tasas[[x, y]].dropna()
        w, p = wilcoxon_par(par[x], par[y]) if len(par) else (float("nan"), float("nan"))
        pruebas.append((f"Wilcoxon: {x} frente a {y}", w, p, vargha_delaney(par[x], par[y]), len(par)))
    alta_ids = casos.index[casos["grupo"] == "alta"]
    for x, y in (("E3", "E2"), ("E4", "E2")):
        par = tasas.loc[tasas.index.isin(alta_ids), [x, y]].dropna()
        w, p = wilcoxon_par(par[x], par[y]) if len(par) else (float("nan"), float("nan"))
        pruebas.append((f"Cobertura alta: {x} frente a {y}", w, p, vargha_delaney(par[x], par[y]), len(par)))
    validos = [i for i, t in enumerate(pruebas) if not np.isnan(t[2])]
    aj = holm([pruebas[i][2] for i in validos]) if validos else []
    ajustado = {i: aj[k] for k, i in enumerate(validos)}
    contrastes.append({"Contraste": "Friedman: E1 a E4", "Estadístico": pct(fried[0], 3),
                       "p": pct(fried[1], 4), "p ajustado (Holm)": "—", "Â12": "—",
                       "Interpretación": "hay diferencias entre estrategias" if fried[1] < 0.05 else "no se detectan diferencias"})
    for i, (nombre, w, p, a12, n) in enumerate(pruebas):
        pa = ajustado.get(i, float("nan"))
        interp = ("sin datos" if np.isnan(pa) else
                  f"{'significativa' if pa < 0.05 else 'no significativa'}; efecto {magnitud(a12)} (n = {n})")
        contrastes.append({"Contraste": nombre, "Estadístico": pct(w, 1), "p": pct(p, 4),
                           "p ajustado (Holm)": pct(pa, 4), "Â12": pct(a12, 3), "Interpretación": interp})
    t4 = pd.DataFrame(contrastes)
    t4.to_csv(salida / "tabla_r4_contrastes.csv", index=False, encoding="utf-8")
    md += ["## Tabla r4. Contrastes estadísticos (Holm sobre los 7 contrastes de Wilcoxon)", t4.to_markdown(index=False), ""]

    # ---------------- Tabla 5: causas en casos sin detección
    sin = tasas.index[(tasas.fillna(0).max(axis=1) == 0)]
    filas = []
    causas_caso = {}
    for c in sin:
        s = df[(df["caso"] == c) & (df["causa_sin_deteccion"].fillna("") != "")]
        causas_caso[c] = s["causa_sin_deteccion"].mode().iloc[0] if len(s) else "sin_diferencia"
    for k, nombre in CAUSAS.items():
        filas.append({"Causa": nombre, "N.º de casos": sum(1 for v in causas_caso.values() if v == k)})
    t5 = pd.DataFrame(filas)
    t5.to_csv(salida / "tabla_r5_causas.csv", index=False, encoding="utf-8")
    md += [f"## Tabla r5. Causas en los {len(sin)} casos donde ninguna estrategia detecta el fallo",
           t5.to_markdown(index=False), ""]

    # ---------------- Anexo C: casos con su cobertura
    anexo = casos.reset_index().merge(df.drop_duplicates("caso")[["caso", "proyecto", "fallo", "clase"]], on="caso")
    anexo = anexo[["proyecto", "fallo", "clase", "cobertura_inicial", "grupo"]]
    anexo.to_csv(salida / "anexo_c_casos.csv", index=False, encoding="utf-8")

    (salida / "resumen.md").write_text("\n".join(md), encoding="utf-8")

    # ---------------- gráficos
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    fig, ax = plt.subplots(figsize=(5.5, 3.2))
    medias = [tasas[e].mean() for e in ESTRATEGIAS]
    err = [[m - ic_bootstrap(tasas[e].dropna())[0] for m, e in zip(medias, ESTRATEGIAS)],
           [ic_bootstrap(tasas[e].dropna())[1] - m for m, e in zip(medias, ESTRATEGIAS)]]
    ax.bar(ESTRATEGIAS, medias, yerr=err, capsize=4, color="#6fa8dc", edgecolor="#4a6a8a")
    ax.set_ylabel("Tasa de detección (media por caso)")
    ax.set_ylim(0, 1)
    for s in ("top", "right"):
        ax.spines[s].set_visible(False)
    fig.tight_layout(); fig.savefig(salida / "fig_deteccion.png", dpi=200); plt.close(fig)

    fig, ax = plt.subplots(figsize=(6, 3.2))
    ancho = 0.2
    for i, e in enumerate(ESTRATEGIAS):
        vals = []
        for g in GRUPOS:
            ids = casos.index[casos["grupo"] == g]
            v = tasas.loc[tasas.index.isin(ids), e].dropna()
            vals.append(v.mean() if len(v) else 0)
        ax.bar([j + i * ancho for j in range(3)], vals, ancho, label=e)
    ax.set_xticks([j + 1.5 * ancho for j in range(3)]); ax.set_xticklabels(["Baja", "Media", "Alta"])
    ax.set_xlabel("Cobertura inicial"); ax.set_ylabel("Tasa de detección"); ax.set_ylim(0, 1); ax.legend(fontsize=8)
    for s in ("top", "right"):
        ax.spines[s].set_visible(False)
    fig.tight_layout(); fig.savefig(salida / "fig_cobertura.png", dpi=200); plt.close(fig)

    print(f"Análisis listo en {salida}")
    print((salida / "resumen.md").read_text(encoding="utf-8")[:3000])


if __name__ == "__main__":
    main()
