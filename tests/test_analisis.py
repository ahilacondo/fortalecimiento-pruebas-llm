# -*- coding: utf-8 -*-
import importlib.util
from pathlib import Path

spec = importlib.util.spec_from_file_location("analizar", Path(__file__).resolve().parent.parent / "analisis" / "analizar.py")
an = importlib.util.module_from_spec(spec); spec.loader.exec_module(an)


def test_vargha_delaney():
    assert an.vargha_delaney([3, 4, 5], [1, 2, 3]) > 0.9
    assert an.vargha_delaney([1, 1], [1, 1]) == 0.5


def test_holm():
    aj = an.holm([0.01, 0.04, 0.03])
    assert [round(x, 2) for x in aj] == [0.03, 0.06, 0.06]


def test_ic_bootstrap_contiene_media():
    lo, hi = an.ic_bootstrap([0, 1, 1, 0, 1, 1, 0, 0])
    assert lo <= 0.5 <= hi
