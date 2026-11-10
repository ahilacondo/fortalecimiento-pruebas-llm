# -*- coding: utf-8 -*-
"""Implementación real: módulos 2, 3 y 7 sobre los comandos de línea de Defects4J.

Las pruebas (la suite original sin las disparadoras y las que genera el LLM) se ejecutan como *suites
externas* de Defects4J: `defects4j test -s <archivo>.tar.bz2` y `defects4j coverage -s <archivo> -i <clases>`.
Se usa ese mecanismo porque la opción `-t` de Defects4J solo admite un método (`Clase::método`).

Requiere Linux (o WSL2 en Windows), Java 11, Perl y Defects4J instalado y inicializado.
Ver README.md y scripts/instalar_defects4j.sh.
"""
from __future__ import annotations

import csv
import io
import os
import re
import shutil
import subprocess
import tarfile
import time
from pathlib import Path
from typing import Dict, List, Optional, Tuple

from . import javautil
from .backend import Backend, Caso, CasoInvalido, Preparado
from .cobertura import Cobertura, leer_xml
from .config import Config


class ErrorD4J(RuntimeError):
    pass


class D4J(Backend):
    simulado = False

    def __init__(self, cfg: Config):
        self.cfg = cfg
        self.home = Path(cfg.defects4j_home)
        self.bin = self.home / "framework" / "bin" / "defects4j"
        self.trabajo = Path(cfg.trabajo)
        self.trabajo.mkdir(parents=True, exist_ok=True)
        self._cache: Dict[Tuple[str, str], dict] = {}
        if not self.bin.exists():
            raise ErrorD4J(f"No se encontró {self.bin}. Defina D4J_HOME o instale Defects4J (README.md).")

    # ------------------------------------------------------------------ comandos
    def _run(self, args: List[str], cwd: Optional[Path] = None, timeout: int = 600) -> Tuple[int, str]:
        env = dict(os.environ)
        env["PATH"] = f"{self.bin.parent}:{env.get('PATH', '')}"
        try:
            p = subprocess.run([str(self.bin)] + args, cwd=str(cwd) if cwd else None, env=env,
                               capture_output=True, text=True, timeout=timeout)
            return p.returncode, (p.stdout or "") + (p.stderr or "")
        except subprocess.TimeoutExpired as e:
            salida = e.stdout.decode(errors="ignore") if isinstance(e.stdout, bytes) else (e.stdout or "")
            return 124, salida + "\n[TIMEOUT]"

    def ids_activos(self, proyecto: str) -> List[int]:
        rc, out = self._run(["bids", "-p", proyecto])
        if rc != 0:
            raise ErrorD4J(out)
        return [int(x) for x in out.split() if x.strip().isdigit()]

    def consulta(self, proyecto: str, campos: str) -> List[List[str]]:
        rc, out = self._run(["query", "-p", proyecto, "-q", campos])
        if rc != 0:
            raise ErrorD4J(out)
        return list(csv.reader(io.StringIO(out)))

    def exportar(self, dir_: Path, prop: str, varias: bool = False) -> str:
        """Valor de una propiedad del proyecto. Se lee solo la salida estándar (los avisos de Perl van a stderr).
        Con varias=True devuelve todas las líneas (p. ej. tests.trigger); si no, la última línea."""
        env = dict(os.environ)
        env["PATH"] = f"{self.bin.parent}:{env.get('PATH', '')}"
        p = subprocess.run([str(self.bin), "export", "-p", prop, "-w", str(dir_)], env=env,
                           capture_output=True, text=True, timeout=120)
        if p.returncode != 0:
            raise ErrorD4J(f"export {prop}: {(p.stdout or '') + (p.stderr or '')}")
        lineas = [l.strip() for l in (p.stdout or "").splitlines() if l.strip()]
        if not lineas:
            raise ErrorD4J(f"export {prop}: sin salida")
        return "\n".join(lineas) if varias else lineas[-1]

    def checkout(self, proyecto: str, fallo: int, version: str, destino: Path) -> None:
        if destino.exists():
            shutil.rmtree(destino)
        rc, out = self._run(["checkout", "-p", proyecto, "-v", f"{fallo}{version}", "-w", str(destino)], timeout=900)
        if rc != 0:
            raise ErrorD4J(f"checkout {proyecto}-{fallo}{version}: {out}")

    # ------------------------------------------------------------------ rutas y archivos
    def _dirs(self, caso: Caso) -> Tuple[Path, Path]:
        return self.trabajo / f"{caso.proyecto}_{caso.fallo}_b", self.trabajo / f"{caso.proyecto}_{caso.fallo}_f"

    def _dir(self, prep: Preparado, version: str) -> Path:
        return Path(prep.extra["dir_" + version])

    def _archivo_suite(self, dir_: Path, caso: Caso, version: str, etiqueta: str,
                       nombre_clase: str, codigo: str) -> Path:
        """Empaqueta una clase de prueba como suite externa: <Proy>-<id><b|f>-<etiqueta>.1.tar.bz2"""
        carpeta = dir_ / ".llm"
        carpeta.mkdir(exist_ok=True)
        ruta = carpeta / f"{caso.proyecto}-{caso.fallo}{version}-{etiqueta}.1.tar.bz2"
        interno = nombre_clase.replace(".", "/") + ".java"
        datos = codigo.encode("utf-8")
        with tarfile.open(ruta, "w:bz2") as tar:
            info = tarfile.TarInfo(interno)
            info.size = len(datos)
            info.mtime = int(time.time())
            tar.addfile(info, io.BytesIO(datos))
        return ruta

    @staticmethod
    def _fallos(dir_: Path) -> Optional[List[str]]:
        """Pruebas que fallan según el archivo failing_tests. Una clase sin método (error al cargar) se marca con '*'."""
        f = dir_ / "failing_tests"
        if not f.exists():
            return None
        out: List[str] = []
        for l in f.read_text(encoding="utf-8", errors="replace").splitlines():
            m = re.match(r"--- ([^:\s]+)(?:::(\S+))?", l)
            if m:
                out.append(m.group(2) or "*")
        return out

    # ------------------------------------------------------------------ ejecución de suites externas
    def _test_suite(self, dir_: Path, archivo: Path, timeout: int) -> dict:
        """Compila y ejecuta una suite externa. Devuelve {'estado': 'ok'|'no_compila'|'timeout'|'error', 'fallan', 'salida'}"""
        (dir_ / "failing_tests").unlink(missing_ok=True)
        rc, out = self._run(["test", "-w", str(dir_), "-s", str(archivo)], timeout=timeout)
        if rc == 124:
            return {"estado": "timeout", "fallan": [], "salida": out}
        if rc != 0:
            if "Cannot compile" in out or "error:" in out:
                errores = [l for l in out.splitlines()
                           if ("error:" in l or "cannot find symbol" in l or l.strip().startswith(("symbol:", "location:")))]
                return {"estado": "no_compila", "fallan": [], "salida": "\n".join(errores[:40])[:4000] or out[-3000:]}
            return {"estado": "error", "fallan": [], "salida": out[-3000:]}
        return {"estado": "ok", "fallan": self._fallos(dir_) or [], "salida": out}

    def _coverage_suite(self, dir_: Path, clase: str, archivo: Path) -> Tuple[Cobertura, Optional[List[str]], str]:
        (dir_ / "instrumentar.txt").write_text(clase + "\n", encoding="utf-8")
        (dir_ / "coverage.xml").unlink(missing_ok=True)
        (dir_ / "failing_tests").unlink(missing_ok=True)
        rc, out = self._run(["coverage", "-w", str(dir_), "-s", str(archivo), "-i", str(dir_ / "instrumentar.txt")],
                            timeout=self.cfg.timeout_suite_s * 2)
        xml = dir_ / "coverage.xml"
        cob = leer_xml(xml, clase) if xml.exists() else Cobertura()
        return cob, self._fallos(dir_), out

    # ------------------------------------------------------------------ módulos 2 y 3
    def preparar(self, caso: Caso) -> Preparado:
        dir_b, dir_f = self._dirs(caso)
        listo = dir_b / ".preparado"
        if not (listo.exists() and dir_f.exists()):
            self.checkout(caso.proyecto, caso.fallo, "b", dir_b)
            self.checkout(caso.proyecto, caso.fallo, "f", dir_f)

        src_dir = self.exportar(dir_b, "dir.src.classes")
        test_dir = self.exportar(dir_b, "dir.src.tests")
        ruta_clase = dir_b / src_dir / (caso.clase.replace(".", "/") + ".java")
        test_orig = caso.clase + "Test"
        ruta_test = dir_b / test_dir / (test_orig.replace(".", "/") + ".java")
        if not ruta_clase.exists():
            raise CasoInvalido(f"no existe el archivo de la clase: {ruta_clase}")
        if not ruta_test.exists():
            raise CasoInvalido(f"no existe la clase de prueba {caso.clase_simple}Test")

        # copia de seguridad de la prueba original y retiro de las pruebas disparadoras
        copia = ruta_test.with_suffix(".java.orig")
        if not copia.exists():
            shutil.copy(ruta_test, copia)
        texto = copia.read_text(encoding="utf-8", errors="replace")
        disparadoras = [l.strip() for l in self.exportar(dir_b, "tests.trigger", varias=True).splitlines() if "::" in l]
        propias = [t.split("::", 1)[1] for t in disparadoras if t.split("::", 1)[0] == test_orig]
        texto2, faltan = javautil.eliminar_metodos(texto, propias)
        if faltan:
            raise CasoInvalido(f"no se pudo retirar la prueba disparadora: {', '.join(faltan)}")
        ruta_test.write_text(texto2, encoding="utf-8")

        rc, out = self._run(["compile", "-w", str(dir_b)], timeout=self.cfg.timeout_compilar_s)
        if rc != 0:
            raise CasoInvalido("la versión con fallo no compila: " + out[-200:].replace("\n", " "))

        # la versión corregida también debe estar compilada: las suites externas se compilan contra ella
        rcf, outf = self._run(["compile", "-w", str(dir_f)], timeout=self.cfg.timeout_compilar_s)
        if rcf != 0:
            raise CasoInvalido("la versión corregida no compila: " + outf[-200:].replace("\n", " "))

        # suite de partida como suite externa: mide la cobertura inicial y comprueba que pasa
        arch = self._archivo_suite(dir_b, caso, "b", "orig", test_orig, texto2)
        t0 = time.time()
        cob, fallan, salida = self._coverage_suite(dir_b, caso.clase, arch)
        dur = time.time() - t0
        if "[TIMEOUT]" in salida:
            raise CasoInvalido("la suite de la clase supera el tiempo máximo")
        if fallan is None or cob.lineas_total == 0:
            raise CasoInvalido("no se pudo ejecutar la suite o medir la cobertura de la clase")
        if fallan:
            raise CasoInvalido(f"la suite de partida falla sin las disparadoras ({len(fallan)} pruebas)")
        listo.write_text("ok", encoding="utf-8")

        metodos = javautil.listar_metodos_prueba(texto2)
        registro = (f"Clase de prueba: {test_orig}\nPruebas ejecutadas: {len(metodos)}; fallos: 0; "
                    f"tiempo total: {dur:.1f} s\n" + "\n".join(f"- {m}: pasa" for m in metodos))
        codigo_clase = ruta_clase.read_text(encoding="utf-8", errors="replace")
        return Preparado(caso=caso, codigo_clase=codigo_clase, codigo_test=texto2,
                         javadoc=javautil.extraer_javadoc(codigo_clase), registro_ejecucion=registro,
                         cobertura_inicial=cob, test_original=test_orig,
                         test_nuevo=f"{caso.clase}LLMTest",
                         extra={"dir_b": str(dir_b), "dir_f": str(dir_f), "test_dir": test_dir,
                                "duracion_suite_s": round(dur, 1)})

    # ------------------------------------------------------------------ interfaz del backend
    def escribir_prueba(self, prep: Preparado, version: str, codigo: str) -> None:
        d = self._dir(prep, version)
        arch = self._archivo_suite(d, prep.caso, version, "llm", prep.test_nuevo, codigo)
        prep.extra["archivo_" + version] = str(arch)
        self._cache.pop((prep.caso.id, version), None)

    def compilar(self, prep: Preparado, version: str) -> Tuple[bool, str]:
        """Compila y ejecuta la suite generada. La primera ejecución se guarda para `ejecutar`."""
        d = self._dir(prep, version)
        r = self._test_suite(d, Path(prep.extra["archivo_" + version]), self.cfg.timeout_suite_s)
        if r["estado"] == "ok":
            self._cache[(prep.caso.id, version)] = r
            return True, ""
        if r["estado"] == "timeout":
            return True, ""      # compiló pero tardó demasiado: se tratará como fallo en `ejecutar`
        return False, r["salida"]

    def ejecutar(self, prep: Preparado, version: str, metodos: List[str]) -> Dict[str, bool]:
        r = self._cache.pop((prep.caso.id, version), None)
        if r is None:
            r = self._test_suite(self._dir(prep, version), Path(prep.extra["archivo_" + version]),
                                 self.cfg.timeout_suite_s)
        if r["estado"] != "ok":
            return {m: False for m in metodos}
        fallan = set(r["fallan"])
        if "*" in fallan:                      # error al cargar la clase: todas fallan
            return {m: False for m in metodos}
        return {m: (m not in fallan) for m in metodos}

    def cobertura_conjunta(self, prep: Preparado) -> Cobertura:
        d = self._dir(prep, "b")
        c2, _, _ = self._coverage_suite(d, prep.caso.clase, Path(prep.extra["archivo_b"]))
        return prep.cobertura_inicial.combinar(c2)

    def limpiar(self, prep: Preparado) -> None:
        for v in ("b", "f"):
            d = self._dir(prep, v)
            for f in (d / ".llm").glob("*-llm.1.tar.bz2"):
                f.unlink(missing_ok=True)
            shutil.rmtree(d / ".test_suite", ignore_errors=True)
            self._cache.pop((prep.caso.id, v), None)
