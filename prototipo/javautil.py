# -*- coding: utf-8 -*-
"""Utilidades para manipular código Java como texto (sin dependencias externas)."""
from __future__ import annotations

import re
from typing import Iterable, List, Tuple

_DECL_TEST = re.compile(r"(?:public\s+|protected\s+|private\s+)?(?:static\s+)?void\s+(\w+)\s*\(\s*\)")


def _saltar_literal(s: str, i: int) -> int:
    """Si en s[i] empieza un comentario, cadena o carácter, devuelve el índice posterior; si no, i."""
    if s.startswith("//", i):
        j = s.find("\n", i)
        return len(s) if j < 0 else j
    if s.startswith("/*", i):
        j = s.find("*/", i + 2)
        return len(s) if j < 0 else j + 2
    if s.startswith('"""', i):  # bloque de texto
        j = s.find('"""', i + 3)
        return len(s) if j < 0 else j + 3
    if s[i] in "\"'":
        q = s[i]
        j = i + 1
        while j < len(s) and s[j] != q:
            j += 2 if s[j] == "\\" else 1
        return j + 1
    return i


def _cerrar_llave(s: str, abre: int) -> int:
    """Dado el índice de '{', devuelve el índice posterior a su '}' correspondiente (o -1)."""
    prof = 0
    i = abre
    while i < len(s):
        j = _saltar_literal(s, i)
        if j != i:
            i = j
            continue
        if s[i] == "{":
            prof += 1
        elif s[i] == "}":
            prof -= 1
            if prof == 0:
                return i + 1
        i += 1
    return -1


def listar_metodos_prueba(src: str) -> List[str]:
    """Nombres de métodos de prueba: anotados con @Test o con nombre test* (JUnit 3)."""
    nombres: List[str] = []
    for m in _DECL_TEST.finditer(src):
        nombre = m.group(1)
        previo = src[max(0, m.start() - 200):m.start()]
        # anotaciones inmediatamente anteriores (sin pasar por otro método)
        previo = previo.split("}")[-1]
        if "@Test" in previo or nombre.startswith("test"):
            if nombre not in nombres:
                nombres.append(nombre)
    return nombres


def _es_prefijo(linea: str) -> bool:
    """True si la línea solo contiene anotaciones o comentarios que preceden a un método."""
    if linea.startswith("@"):
        return "void" not in linea and not linea.endswith((";", "{", "}"))
    return linea.startswith(("/**", "*", "*/", "//"))


def _inicio_con_anotaciones(src: str, pos: int) -> int:
    """Retrocede desde el inicio de la línea de la declaración para incluir anotaciones y Javadoc previos."""
    while pos > 0:
        ant_fin = pos - 1                      # el '\n' de la línea anterior
        ant_ini = src.rfind("\n", 0, ant_fin) + 1
        if _es_prefijo(src[ant_ini:ant_fin].strip()):
            pos = ant_ini
        else:
            break
    return pos


def eliminar_metodos(src: str, nombres: Iterable[str]) -> Tuple[str, List[str]]:
    """Elimina los métodos indicados. Devuelve (código nuevo, nombres que no se encontraron)."""
    faltan: List[str] = []
    for nombre in nombres:
        patron = re.compile(r"void\s+" + re.escape(nombre) + r"\s*\(")
        m = patron.search(src)
        if not m:
            faltan.append(nombre)
            continue
        abre = src.find("{", m.end())
        if abre < 0:
            faltan.append(nombre)
            continue
        fin = _cerrar_llave(src, abre)
        if fin < 0:
            faltan.append(nombre)
            continue
        # inicio de la línea de la declaración
        ini_linea = src.rfind("\n", 0, m.start()) + 1
        ini = _inicio_con_anotaciones(src, ini_linea)
        src = src[:ini] + src[fin:].lstrip("\n")
    return src, faltan


def extraer_codigo_java(texto: str) -> str:
    """Devuelve el contenido del primer bloque ```java ... ``` (o el texto completo si no hay bloque)."""
    m = re.search(r"```(?:java)?\s*\n(.*?)```", texto, flags=re.S)
    if m:
        return m.group(1).strip() + "\n"
    return texto.strip() + "\n"


def paquete(src: str) -> str:
    m = re.search(r"^\s*package\s+([\w.]+)\s*;", src, flags=re.M)
    return m.group(1) if m else ""


def nombre_clase_publica(src: str) -> str:
    m = re.search(r"public\s+(?:final\s+|abstract\s+)?class\s+(\w+)", src)
    return m.group(1) if m else ""


def renombrar_clase(src: str, viejo: str, nuevo: str) -> str:
    return re.sub(r"\b" + re.escape(viejo) + r"\b", nuevo, src)


def extraer_javadoc(src: str, limite: int = 12000) -> str:
    """Javadoc de la clase y de sus miembros, cada bloque seguido de la firma que documenta."""
    partes: List[str] = []
    for m in re.finditer(r"/\*\*(.*?)\*/\s*((?:@\w+(?:\([^)]*\))?\s*)*)([^;{=]+)[;{=]", src, flags=re.S):
        texto = re.sub(r"^\s*\*\s?", "", m.group(1), flags=re.M).strip()
        firma = " ".join(m.group(3).split())
        if not texto:
            continue
        partes.append(f"{firma}\n    {texto}".rstrip())
    salida = "\n\n".join(partes)
    return salida[:limite]


def resumir_pruebas(src: str) -> str:
    """Lista de métodos de prueba de la suite (registro de ejecución de E3)."""
    return "\n".join(f"- {n}" for n in listar_metodos_prueba(src))
