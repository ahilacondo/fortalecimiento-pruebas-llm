# -*- coding: utf-8 -*-
"""Análisis ampliado de los resultados (complementa a analizar.py; no lo reemplaza).

Genera, a partir de resultados/ejecuciones.csv:
  - tablas A1 a A6 (CSV) y un resumen en Markdown,
  - figuras: mapa de resultados por caso, desenlace de las corridas, detección por proyecto
    y relación costo-detección.

Uso:
    python analisis/ampliado.py
Salida: resultados/analisis_ampliado/
Los costos usan los precios de lista de Claude Haiku 5.5 que cita el informe (0,10 y 0,50 USD por
millón de tokens de entrada y salida).
"""
from __future__ import annotations

import argparse
from itertools import combinations
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from matplotlib.patches import Patch
from scipy import stats

RAIZ = Path(__file__).resolve().parent.parent
PRECIO_ENTRADA, PRECIO_SALIDA = 0.10 / 1e6, 0.50 / 1e6

NOMBRES = {"E1": "E1 Sin guía", "E2": "E2 Cobertura", "E3": "E3 Análisis de pruebas", "E4": "E4 Documentación"}
# Paleta categórica de referencia (blue, orange, aqua, yellow), validada para pares adyacentes.
COL_E = {"E1": "#2a78d6", "E2": "#eb6834", "E3": "#1baf7a", "E4": "#eda100"}
INK, INK2, GRID, SUP = "#0b0b0b", "#52514e", "#dcdad4", "#fcfcfb"
DESENLACES = [("Detectó el fallo", "#2a78d6"),
              ("Pasa en ambas versiones", "#c9c7bf"),
              ("No compila", "#eb6834"),
              ("Sin respuesta (tope de tokens)", "#52514e")]
GRUPO_ORDEN = ["baja", "media", "alta"]

plt.rcParams.update({"font.family": "DejaVu Sans", "axes.edgecolor": GRID, "axes.labelcolor": INK2,
                     "xtick.color": INK2, "ytick.color": INK2, "text.color": INK,
                     "figure.facecolor": SUP, "axes.facecolor": SUP, "savefig.facecolor": SUP})


def desenlace(r) -> str:
    if r.detecta_fallo == 1:
        return DESENLACES[0][0]
    if r.causa_sin_deteccion == "limite_o_error":
        return DESENLACES[3][0]
    if r.causa_sin_deteccion == "no_compila":
        return DESENLACES[2][0]
    return DESENLACES[1][0]


def holm(p: list[float]) -> list[float]:
    orden = np.argsort(p)
    ajust = np.empty(len(p))
    previo = 0.0
    for k, i in enumerate(orden):
        previo = max(previo, (len(p) - k) * p[i])
        ajust[i] = min(1.0, previo)
    return list(ajust)


def cochran_q(m: np.ndarray) -> tuple[float, float]:
    """m: casos x estrategias (0/1)."""
    k = m.shape[1]
    col, fila = m.sum(0), m.sum(1)
    t = m.sum()
    den = k * t - (fila ** 2).sum()
    if den == 0:
        return 0.0, 1.0
    q = (k - 1) * (k * (col ** 2).sum() - t ** 2) / den
    return float(q), float(stats.chi2.sf(q, k - 1))


def mcnemar_exacto(a: np.ndarray, b: np.ndarray) -> tuple[int, int, float]:
    n10 = int(((a == 1) & (b == 0)).sum())
    n01 = int(((a == 0) & (b == 1)).sum())
    if n10 + n01 == 0:
        return n10, n01, 1.0
    return n10, n01, float(stats.binomtest(n10, n10 + n01, 0.5).pvalue)


def limpiar_eje(ax):
    for s in ("top", "right"):
        ax.spines[s].set_visible(False)
    ax.tick_params(length=0)


def fig_mapa(d: pd.DataFrame, salida: Path):
    proyectos = ["Lang", "Cli", "Codec", "Csv"]
    fig, axes = plt.subplots(2, 2, figsize=(10.5, 9.6), gridspec_kw={"wspace": 0.8, "hspace": 0.3})
    axes = axes.ravel()
    cat = {n: i for i, (n, _) in enumerate(DESENLACES)}
    simbolo = {DESENLACES[0][0]: "✔", DESENLACES[1][0]: "", DESENLACES[2][0]: "C", DESENLACES[3][0]: "T"}
    for ax, p in zip(axes, proyectos):
        x = d[d.proyecto == p]
        casos = (x[["caso", "fallo", "clase", "cobertura_inicial", "grupo"]].drop_duplicates()
                 .sort_values("fallo"))
        for i, (_, c) in enumerate(casos.iterrows()):
            for j, e in enumerate(NOMBRES):
                f = x[(x.caso == c.caso) & (x.estrategia == e)].iloc[0]
                des = desenlace(f)
                color = dict(DESENLACES)[des]
                ax.add_patch(plt.Rectangle((j + 0.04, i + 0.06), 0.92, 0.88, color=color, lw=0))
                if simbolo[des]:
                    ax.text(j + 0.5, i + 0.52, simbolo[des], ha="center", va="center", fontsize=11,
                            color="white", fontweight="bold")
        ax.set_xlim(0, 4); ax.set_ylim(len(casos), 0)
        ax.set_xticks([j + 0.5 for j in range(4)]); ax.set_xticklabels(list(NOMBRES), fontsize=9)
        ax.xaxis.tick_top()
        ax.set_yticks([i + 0.5 for i in range(len(casos))])
        ax.set_yticklabels([f"{c.fallo} · {c.clase.rsplit('.', 1)[-1]} ({c.cobertura_inicial:.0f} %)"
                            for _, c in casos.iterrows()], fontsize=8.5)
        ax.set_title(p, fontsize=12, fontweight="bold", pad=24, loc="left")
        for s in ax.spines.values():
            s.set_visible(False)
        ax.tick_params(length=0)
    handles = [Patch(color=c, label=n) for n, c in DESENLACES]
    fig.legend(handles=handles, loc="lower center", ncol=2, frameon=False, fontsize=10, bbox_to_anchor=(0.5, 0.0))
    fig.subplots_adjust(bottom=0.1)
    fig.savefig(salida, dpi=200, bbox_inches="tight")
    plt.close(fig)


def fig_desenlace(d: pd.DataFrame, salida: Path):
    t = d.groupby(["estrategia", "desenlace"]).size().unstack(fill_value=0)
    fig, ax = plt.subplots(figsize=(9, 3.6))
    ests = list(NOMBRES)[::-1]
    for i, e in enumerate(ests):
        izq = 0
        for n, c in DESENLACES:
            v = int(t.loc[e].get(n, 0)) if e in t.index else 0
            if v:
                ax.barh(i, v - 0.12, left=izq + 0.06, color=c, height=0.62)
                if v >= 3:
                    ax.text(izq + v / 2, i, str(v), ha="center", va="center", fontsize=10,
                            color="white" if c != "#c9c7bf" else INK, fontweight="bold")
                izq += v
    ax.set_yticks(range(len(ests))); ax.set_yticklabels([NOMBRES[e] for e in ests])
    ax.set_xlim(0, 40); ax.set_xlabel("Corridas (de 40 por estrategia)")
    ax.xaxis.grid(True, color=GRID, lw=0.6); ax.set_axisbelow(True)
    limpiar_eje(ax)
    ax.legend(handles=[Patch(color=c, label=n) for n, c in DESENLACES], loc="upper center",
              bbox_to_anchor=(0.45, -0.22), ncol=2, frameon=False, fontsize=9)
    ax.set_title("Qué pasó en las corridas de cada estrategia", loc="left", fontsize=12, fontweight="bold")
    fig.savefig(salida, dpi=200, bbox_inches="tight")
    plt.close(fig)


def fig_proyecto(d: pd.DataFrame, salida: Path):
    proyectos = ["Lang", "Cli", "Codec", "Csv"]
    fig, ax = plt.subplots(figsize=(9, 4))
    ancho = 0.19
    for j, e in enumerate(NOMBRES):
        vals = [int(d[(d.proyecto == p) & (d.estrategia == e)].detecta_fallo.sum()) for p in proyectos]
        xs = np.arange(len(proyectos)) + (j - 1.5) * (ancho + 0.02)
        ax.bar(xs, vals, ancho, color=COL_E[e], label=NOMBRES[e])
        for x, v in zip(xs, vals):
            ax.text(x, v + 0.12, str(v), ha="center", va="bottom", fontsize=9, color=INK2)
    ax.set_xticks(range(len(proyectos))); ax.set_xticklabels(proyectos)
    ax.set_ylabel("Casos con fallo detectado (de 10)"); ax.set_ylim(0, 5)
    ax.yaxis.grid(True, color=GRID, lw=0.6); ax.set_axisbelow(True)
    limpiar_eje(ax)
    ax.legend(frameon=False, ncol=4, loc="upper center", bbox_to_anchor=(0.5, -0.1), fontsize=9)
    ax.set_title("Detección por proyecto y estrategia", loc="left", fontsize=12, fontweight="bold")
    fig.savefig(salida, dpi=200, bbox_inches="tight")
    plt.close(fig)


def fig_costo(c: pd.DataFrame, salida: Path):
    fig, ax = plt.subplots(figsize=(7.2, 4.4))
    for e, r in c.iterrows():
        ax.scatter(r["tokens_por_caso"] / 1000, r["casos_detectados"], s=190, color=COL_E[e],
                   edgecolor=SUP, linewidth=2, zorder=3)
        ax.annotate(NOMBRES[e], (r["tokens_por_caso"] / 1000, r["casos_detectados"]),
                    textcoords="offset points", xytext=(0, 14), ha="center", fontsize=9, color=INK)
    ax.set_xlabel("Tokens por caso (miles)"); ax.set_ylabel("Casos con fallo detectado (de 40)")
    ax.set_xlim(30, 72); ax.set_ylim(0, 8)
    ax.grid(True, color=GRID, lw=0.6); ax.set_axisbelow(True)
    limpiar_eje(ax)
    ax.set_title("Costo en tokens frente a casos detectados", loc="left", fontsize=12, fontweight="bold")
    fig.savefig(salida, dpi=200, bbox_inches="tight")
    plt.close(fig)


def _ic_base() -> dict:
    """IC 95 % de analizar.py (tabla_r1), para que la figura coincida con la Tabla 25 del informe."""
    ruta = RAIZ / "resultados" / "analisis_base" / "tabla_r1_deteccion.csv"
    if not ruta.exists():
        ruta = RAIZ / "analisis" / "salida" / "tabla_r1_deteccion.csv"
    t = pd.read_csv(ruta)
    ic = {}
    for _, r in t.iterrows():
        lo, hi = r["IC 95 %"].strip("[]").split(";")
        ic[r.iloc[0][:2]] = (float(lo), float(hi))
    return ic


def fig_informe9(d: pd.DataFrame, salida: Path):
    """Figura 9 del informe: detección por estrategia con IC 95 % (a) y por proyecto (b)."""
    ests = list(NOMBRES)
    n = d.caso.nunique()
    ic = _ic_base()
    fig, (a1, a2) = plt.subplots(1, 2, figsize=(10.5, 4.2), gridspec_kw={"width_ratios": [1, 1.25], "wspace": 0.28})
    tasa = [d[d.estrategia == e].detecta_fallo.mean() for e in ests]
    for i, e in enumerate(ests):
        a1.bar(i, tasa[i], 0.62, color=COL_E[e])
        lo, hi = ic[e]
        a1.errorbar(i, tasa[i], yerr=[[tasa[i] - lo], [hi - tasa[i]]], color=INK, capsize=5, lw=1.4)
        a1.text(i, hi + 0.02, f"{int(round(tasa[i] * n))} de {n}", ha="center", fontsize=9, color=INK2)
    a1.set_xticks(range(4)); a1.set_xticklabels(ests); a1.set_ylim(0, 0.5)
    a1.set_ylabel("Tasa de detección (casos detectados / 40)")
    a1.set_title("(a) Por estrategia, con IC de 95 %", loc="left", fontsize=10.5, fontweight="bold")
    proyectos = ["Lang", "Cli", "Codec", "Csv"]
    ancho = 0.19
    for j, e in enumerate(ests):
        vals = [int(d[(d.proyecto == p) & (d.estrategia == e)].detecta_fallo.sum()) for p in proyectos]
        xs = np.arange(4) + (j - 1.5) * (ancho + 0.02)
        a2.bar(xs, vals, ancho, color=COL_E[e], label=NOMBRES[e])
        for x, v in zip(xs, vals):
            a2.text(x, v + 0.08, str(v), ha="center", va="bottom", fontsize=8.5, color=INK2)
    a2.set_xticks(range(4)); a2.set_xticklabels(proyectos); a2.set_ylim(0, 4.2)
    a2.set_ylabel("Casos detectados (de 10)")
    a2.set_title("(b) Por proyecto", loc="left", fontsize=10.5, fontweight="bold")
    a2.legend(frameon=False, ncol=2, loc="upper left", fontsize=8.5)
    for ax in (a1, a2):
        ax.yaxis.grid(True, color=GRID, lw=0.6); ax.set_axisbelow(True); limpiar_eje(ax)
    fig.savefig(salida, dpi=200, bbox_inches="tight")
    plt.close(fig)


def fig_informe10(d: pd.DataFrame, c: pd.DataFrame, salida: Path):
    """Figura 10 del informe: cobertura ganada (a) y costo frente a detección (b)."""
    ests = list(NOMBRES)
    fig, (a1, a2) = plt.subplots(1, 2, figsize=(10.5, 4.2), gridspec_kw={"wspace": 0.28})
    g = d.groupby("estrategia")      # las 160 corridas, igual que la Tabla 27 de analizar.py
    lin = g.delta_cobertura_lineas.mean().reindex(ests)
    ram = g.delta_cobertura_ramas.mean().reindex(ests)
    x = np.arange(4)
    a1.bar(x - 0.2, lin.values, 0.36, color="#2a78d6", label="Líneas")
    a1.bar(x + 0.2, ram.values, 0.36, color="#eda100", label="Ramas")
    for xi, v in zip(x - 0.2, lin.values):
        a1.text(xi, v + 0.2, f"{v:.1f}", ha="center", fontsize=8.5, color=INK2)
    for xi, v in zip(x + 0.2, ram.values):
        a1.text(xi, v + 0.2, f"{v:.1f}", ha="center", fontsize=8.5, color=INK2)
    a1.set_xticks(x); a1.set_xticklabels([e for e in ests]); a1.set_ylim(0, 14)
    a1.set_ylabel("Aumento de cobertura (puntos porcentuales)")
    a1.legend(frameon=False, fontsize=9, loc="upper left")
    a1.set_title("(a) Cobertura ganada en la clase objetivo", loc="left", fontsize=10.5, fontweight="bold")
    for e, r in c.iterrows():
        a2.scatter(r["tokens_por_caso"] / 1000, r["casos_detectados"], s=170, color=COL_E[e],
                   edgecolor=SUP, linewidth=2, zorder=3)
        a2.annotate(e, (r["tokens_por_caso"] / 1000, r["casos_detectados"]), textcoords="offset points",
                    xytext=(0, 13), ha="center", fontsize=10, color=INK, fontweight="bold")
    a2.set_xlabel("Tokens por caso (miles)"); a2.set_ylabel("Casos detectados (de 40)")
    a2.set_xlim(30, 72); a2.set_ylim(0, 8)
    a2.set_title("(b) Costo frente a detección", loc="left", fontsize=10.5, fontweight="bold")
    a2.grid(True, color=GRID, lw=0.6); a2.set_axisbelow(True)
    for ax in (a1, a2):
        limpiar_eje(ax)
    a1.yaxis.grid(True, color=GRID, lw=0.6); a1.set_axisbelow(True)
    fig.savefig(salida, dpi=200, bbox_inches="tight")
    plt.close(fig)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--entrada", default=str(RAIZ / "resultados" / "ejecuciones.csv"))
    ap.add_argument("--salida", default=str(RAIZ / "resultados" / "analisis_ampliado"))
    a = ap.parse_args()
    out = Path(a.salida)
    out.mkdir(parents=True, exist_ok=True)
    d = pd.read_csv(a.entrada)
    d = d[d.simulado == 0].copy()
    d["desenlace"] = d.apply(desenlace, axis=1)
    d["usd"] = d.tokens_entrada * PRECIO_ENTRADA + d.tokens_salida * PRECIO_SALIDA
    ests = list(NOMBRES)
    md = ["# Análisis ampliado", "", f"Corridas: {len(d)} · casos: {d.caso.nunique()} · repetición única.", ""]

    # A1 detección por proyecto
    a1 = (d.pivot_table(index="proyecto", columns="estrategia", values="detecta_fallo", aggfunc="sum")
          .reindex(["Lang", "Cli", "Codec", "Csv"])[ests].astype(int))
    a1.loc["Total (de 40)"] = a1.sum()
    a1.to_csv(out / "A1_deteccion_por_proyecto.csv")
    md += ["## A1. Casos con fallo detectado por proyecto (de 10 por proyecto)", "", a1.to_markdown(), ""]

    # A2 desenlace por estrategia
    a2 = d.groupby(["estrategia", "desenlace"]).size().unstack(fill_value=0)
    a2 = a2.reindex(columns=[n for n, _ in DESENLACES], fill_value=0).loc[ests]
    a2.to_csv(out / "A2_desenlace_por_estrategia.csv")
    md += ["## A2. Desenlace de las corridas por estrategia", "", a2.to_markdown(), ""]

    # A3 costo
    g = d.groupby("estrategia")
    a3 = pd.DataFrame({
        "casos_detectados": g.detecta_fallo.sum(),
        "pruebas_generadas_media": g.pruebas_generadas.mean(),
        "tokens_por_caso": (g.tokens_entrada.mean() + g.tokens_salida.mean()),
        "llamadas_por_caso": g.llamadas.mean(),
        "segundos_por_caso": g.segundos.mean(),
        "usd_por_caso": g.usd.mean(),
        "usd_total": g.usd.sum(),
    }).loc[ests]
    a3["usd_por_deteccion"] = a3.usd_total / a3.casos_detectados.replace(0, np.nan)
    a3["detecciones_por_100_pruebas"] = a3.casos_detectados / (g.pruebas_generadas.sum().loc[ests]) * 100
    a3.round(3).to_csv(out / "A3_costo_y_eficiencia.csv")
    md += ["## A3. Costo y eficiencia (precios de lista de Haiku 5.5)", "", a3.round(3).to_markdown(), "",
           f"Costo total medido de las {len(d)} corridas: US$ {d.usd.sum():.2f}.", ""]

    # A4 concordancia entre estrategias
    m = d.pivot_table(index="caso", columns="estrategia", values="detecta_fallo", aggfunc="max")[ests].astype(int)
    nest = m.sum(1)
    a4 = pd.DataFrame({"casos": nest.value_counts().sort_index()})
    a4.index.name = "n_estrategias_que_detectan"
    a4.to_csv(out / "A4_concordancia.csv")
    exclusivos = {e: int(((m[e] == 1) & (nest == 1)).sum()) for e in ests}
    md += ["## A4. Concordancia entre estrategias", "", a4.to_markdown(), "",
           f"- Casos detectados por al menos una estrategia: **{int((nest > 0).sum())} de {len(m)}**.",
           f"- Detectados por las cuatro: {int((nest == 4).sum())}.",
           f"- Detectados por una sola estrategia: {exclusivos}.",
           f"- Ninguna estrategia detecta: {int((nest == 0).sum())} casos.", ""]

    # A5 contrastes para datos binarios
    q, pq = cochran_q(m.values)
    pares = list(combinations(ests, 2))
    filas, ps = [], []
    for x, y in pares:
        n10, n01, p = mcnemar_exacto(m[x].values, m[y].values)
        filas.append(dict(contraste=f"{x} frente a {y}", solo_primera=n10, solo_segunda=n01, p=p)); ps.append(p)
    for f, pa in zip(filas, holm(ps)):
        f["p_holm"] = pa
    a5 = pd.DataFrame(filas)
    a5.round(4).to_csv(out / "A5_contrastes_binarios.csv", index=False)
    md += ["## A5. Contrastes para resultados de 0 o 1",
           "", f"Prueba Q de Cochran (4 estrategias, {len(m)} casos): Q = {q:.3f}, p = {pq:.4f}.", "",
           "McNemar exacto por pares con corrección de Holm:", "", a5.round(4).to_markdown(index=False), ""]

    # A6 detección por grupo de cobertura
    d["grupo"] = pd.Categorical(d.grupo, GRUPO_ORDEN, ordered=True)
    a6 = d.pivot_table(index="grupo", columns="estrategia", values="detecta_fallo", aggfunc=["sum", "count"],
                       observed=True)
    filas6 = []
    for gname in GRUPO_ORDEN:
        fila = {"grupo": gname, "casos": int(a6["count"].loc[gname].iloc[0])}
        for e in ests:
            fila[e] = f"{int(a6['sum'].loc[gname, e])} de {int(a6['count'].loc[gname, e])}"
        filas6.append(fila)
    a6 = pd.DataFrame(filas6)
    a6.to_csv(out / "A6_deteccion_por_grupo_cobertura.csv", index=False)
    md += ["## A6. Casos con fallo detectado por grupo de cobertura inicial", "", a6.to_markdown(index=False), ""]

    # cobertura ganada: corridas que detectan frente a las que no
    cob = d[d.desenlace.isin([DESENLACES[0][0], DESENLACES[1][0]])].groupby("detecta_fallo")[
        ["delta_cobertura_lineas", "delta_cobertura_ramas", "pruebas_generadas"]].mean().round(2)
    cob.index = ["No detecta", "Detecta"]
    cob.to_csv(out / "A7_cobertura_segun_deteccion.csv")
    md += ["## A7. Cobertura ganada y pruebas generadas según si la corrida detecta", "", cob.to_markdown(), ""]

    (out / "resumen_ampliado.md").write_text("\n".join(md), encoding="utf-8")
    fig_mapa(d, out / "fig_A_mapa_de_resultados.png")
    fig_desenlace(d, out / "fig_B_desenlace_por_estrategia.png")
    fig_proyecto(d, out / "fig_C_deteccion_por_proyecto.png")
    fig_costo(a3, out / "fig_D_costo_vs_deteccion.png")
    fig_informe9(d, out / "fig_informe_09_deteccion.png")
    fig_informe10(d, a3, out / "fig_informe_10_cobertura_y_costo.png")
    print("\n".join(md))
    print("Salida en", out)


if __name__ == "__main__":
    main()
