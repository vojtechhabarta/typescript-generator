package cz.habarta.typescript.generator;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

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
        final Settings settings = TestUtils.settings();
        settings.sortDeclarations = true;
        String expected = "" +
""                           + settings.newline +
"interface A {"              + settings.newline +
"    w: string;"             + settings.newline +
"    x: number;"             + settings.newline +
"    yyy: number;"           + settings.newline +
"    z: boolean;"            + settings.newline +
"}"                          + settings.newline +
""                           + settings.newline +
"interface B {"              + settings.newline +
"    a: boolean;"            + settings.newline +
"    x: number;"             + settings.newline +
"}"                          + settings.newline +
"";
        final String actual = new TypeScriptGenerator(settings).generateTypeScript(Input.from(classes));

        assertEquals(expected, actual);
    }

    public static class A {
        public boolean getZ() { return false; }
        public int getYYY() {
            return -1;
        }
        public String getW() { return "Hello World!"; }
        public int getX() {
            return -1;
        }
    }

    public static class B {
        public int getX() {
            return -1;
        }
        public boolean getA() { return false; }
    }
}
