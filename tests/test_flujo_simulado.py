# -*- coding: utf-8 -*-
import csv
from pathlib import Path

from prototipo import ejecutar


def test_flujo_simulado(tmp_path):
    casos = tmp_path / "casos.csv"
    casos.write_text("proyecto,fallo,clase\nLang,1,org.apache.commons.lang3.A\nCli,2,org.apache.commons.cli.B\n", encoding="utf-8")
    salida = tmp_path / "res.csv"
    ejecutar.main(["--simulado", "--casos", str(casos), "--repeticiones", "2", "--salida", str(salida)])
    filas = list(csv.DictReader(open(salida, encoding="utf-8")))
    assert len(filas) == 2 * 4 * 2
    assert all(f["simulado"] == "1" for f in filas)
    # reanudación: una segunda ejecución no agrega filas
    ejecutar.main(["--simulado", "--casos", str(casos), "--repeticiones", "2", "--salida", str(salida)])
    assert len(list(csv.DictReader(open(salida, encoding="utf-8")))) == 16
    assert (tmp_path / "registros_simulados").exists()
