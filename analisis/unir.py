# -*- coding: utf-8 -*-
"""Une los CSV de resultados por proyecto en resultados/ejecuciones.csv.

Orden de lectura: Lang y Cli (respaldo_parcial), Codec y Csv. Después se aplican los reintentos de
resultados/reintento3/: una fila reemplaza a la original solo si la original terminó sin respuesta
(`limite_o_error`) y el reintento sí produjo pruebas. Las filas reemplazadas se guardan en
resultados/intentos_reemplazados.csv para auditoría.

Uso:  python analisis/unir.py
"""
from pathlib import Path

import pandas as pd

R = Path(__file__).resolve().parent.parent / "resultados"
base = [R / "respaldo_parcial" / "ejecuciones_Lang.csv", R / "respaldo_parcial" / "ejecuciones_Cli.csv",
        R / "Codec" / "ejecuciones_Codec.csv", R / "Csv" / "ejecuciones_Csv.csv"]
d = pd.concat([pd.read_csv(f) for f in base], ignore_index=True)
clave = ["caso", "estrategia", "repeticion"]
reemplazadas = []
for f in sorted((R / "reintento3").glob("ejecuciones_*.csv")):
    for _, nueva in pd.read_csv(f).iterrows():
        m = (d.caso == nueva.caso) & (d.estrategia == nueva.estrategia) & (d.repeticion == nueva.repeticion)
        if m.sum() != 1:
            print("AVISO: no hay fila única para", nueva.caso, nueva.estrategia)
            continue
        vieja = d[m].iloc[0]
        if vieja.causa_sin_deteccion == "limite_o_error" and nueva.causa_sin_deteccion != "limite_o_error":
            reemplazadas.append(vieja)
            d.loc[m, :] = [nueva[c] for c in d.columns]
            print("Reemplazada:", nueva.caso, nueva.estrategia, "->", nueva.causa_sin_deteccion or "ok")
        else:
            print("Se conserva la fila original:", nueva.caso, nueva.estrategia)
assert not d.duplicated(clave).any()
d.to_csv(R / "ejecuciones.csv", index=False)
if reemplazadas:
    pd.DataFrame(reemplazadas).to_csv(R / "intentos_reemplazados.csv", index=False)
print(len(d), "corridas;", int((d.causa_sin_deteccion == "limite_o_error").sum()), "sin respuesta")
