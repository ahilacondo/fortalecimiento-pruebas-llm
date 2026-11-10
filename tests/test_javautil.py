# -*- coding: utf-8 -*-
from prototipo import javautil

SRC = '''package org.apache.commons.lang3;

import org.junit.Test;
import static org.junit.Assert.*;

public class StringUtilsTest {
    private static final String LLAVE = "{ no es un bloque }";

    @Test
    public void testA() {
        String s = "}";   // llave dentro de cadena
        assertEquals("}", s);
    }

    /** Prueba que expone el fallo. */
    @Test(expected = IllegalArgumentException.class)
    public void testB() {
        if (true) { throw new IllegalArgumentException(); }
    }

    public void testC() throws Exception {
        assertTrue(true);
    }

    private void ayuda() { }
}
'''


def test_listar_metodos():
    assert javautil.listar_metodos_prueba(SRC) == ["testA", "testB", "testC"]


def test_eliminar_metodo_con_anotacion_y_javadoc():
    nuevo, faltan = javautil.eliminar_metodos(SRC, ["testB"])
    assert faltan == []
    assert "testB" not in nuevo and "Prueba que expone el fallo" not in nuevo
    assert "testA" in nuevo and "testC" in nuevo and "ayuda" in nuevo
    assert javautil.listar_metodos_prueba(nuevo) == ["testA", "testC"]


def test_eliminar_metodo_inexistente():
    nuevo, faltan = javautil.eliminar_metodos(SRC, ["noExiste"])
    assert faltan == ["noExiste"] and nuevo == SRC


def test_extraer_codigo_java():
    t = "Aquí va:\n```java\nclass A {}\n```\nfin"
    assert javautil.extraer_codigo_java(t).strip() == "class A {}"


def test_paquete_y_clase():
    assert javautil.paquete(SRC) == "org.apache.commons.lang3"
    assert javautil.nombre_clase_publica(SRC) == "StringUtilsTest"


def test_javadoc():
    src = "/** Suma dos números. */\npublic int suma(int a, int b) { return a + b; }\n"
    assert "Suma dos números" in javautil.extraer_javadoc(src)
