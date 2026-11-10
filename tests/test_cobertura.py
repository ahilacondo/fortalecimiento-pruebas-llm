# -*- coding: utf-8 -*-
from prototipo.cobertura import leer_xml

XML = '''<?xml version="1.0"?>
<coverage><packages><package name="p"><classes>
<class name="p.A" filename="p/A.java"><lines>
 <line number="1" hits="2" branch="false"/>
 <line number="2" hits="0" branch="false"/>
 <line number="3" hits="1" branch="true" condition-coverage="50% (1/2)"/>
</lines></class>
<class name="p.A$Interna" filename="p/A.java"><lines>
 <line number="10" hits="1" branch="false"/>
</lines></class>
<class name="p.B" filename="p/B.java"><lines><line number="1" hits="1" branch="false"/></lines></class>
</classes></package></packages></coverage>'''


def test_leer_y_metricas(tmp_path):
    r = tmp_path / "coverage.xml"
    r.write_text(XML, encoding="utf-8")
    c = leer_xml(r, "p.A")
    assert c.lineas_total == 4 and c.lineas_cubiertas == 3
    assert c.ramas_total == 2 and c.ramas_cubiertas == 1
    assert round(c.pct_lineas, 1) == 75.0 and round(c.pct_ramas, 1) == 50.0
    assert "2" in c.informe_texto() and "línea 3" in c.informe_texto()


def test_combinar(tmp_path):
    r = tmp_path / "coverage.xml"
    r.write_text(XML, encoding="utf-8")
    a = leer_xml(r, "p.A")
    b = leer_xml(r, "p.A")
    b.lineas[2] = (5, 0, 0)
    c = a.combinar(b)
    assert c.lineas_cubiertas == 4
