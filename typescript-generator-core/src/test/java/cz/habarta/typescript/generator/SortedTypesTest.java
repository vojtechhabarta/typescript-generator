package cz.habarta.typescript.generator;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.junit.*;

public class SortedTypesTest {

    @Test
    public void testOrder1() {
        List<Class<?>> list = new ArrayList<>();
        list.add(A.class);
        list.add(B.class);
        assertCorrectOrder(list);
    }

    @Test
    public void testOrder2() {
        List<Class<?>> list = new ArrayList<>();
        list.add(B.class);
        list.add(A.class);
        assertCorrectOrder(list);
    }

    public void assertCorrectOrder(List<Class<?>> list) {
        Settings settings = new Settings();
        settings.sortDeclarations = true;
        String expected = "" +
""                           + settings.newline +
"interface A {"              + settings.newline +
"    x: number;"             + settings.newline +
"    y: number;"             + settings.newline +
"}"                          + settings.newline +
""                           + settings.newline +
"interface B {"              + settings.newline +
"    x: number;"             + settings.newline +
"}"                          + settings.newline +
"";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TypeScriptGenerator.generateTypeScript(list, settings, out);

        assertEquals(expected, new String(out.toByteArray()));
    }

    public static class A {
        public int getY() {
            return -1;
        }
        public int getX() {
            return -1;
        }
    }

    public static class B {
        public int getX() {
            return -1;
        }
    }
}
