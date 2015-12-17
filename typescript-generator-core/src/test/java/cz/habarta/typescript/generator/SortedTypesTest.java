package cz.habarta.typescript.generator;

import static org.junit.Assert.*;
import org.junit.*;

public class SortedTypesTest {

    @Test
    public void testOrder1() {
        assertCorrectOrder(A.class, B.class);
    }

    @Test
    public void testOrder2() {
        assertCorrectOrder(B.class, A.class);
    }

    public void assertCorrectOrder(Class<?>... classes) {
        Settings settings = new Settings();
        settings.sortDeclarations = true;
        settings.noFileComment = true;
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
        final String actual = new TypeScriptGenerator(settings).generateTypeScript(Input.from(classes));

        assertEquals(expected, actual);
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
