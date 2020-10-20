package cz.habarta.typescript.generator;

import java.util.HashMap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MapExtensionTest {

    @Test
    public void testOrder1() {
        final Settings settings = TestUtils.settings();
        settings.sortDeclarations = true;
        String expected = "" +
""                                      + settings.newline +
"interface A {"                         + settings.newline +
"    mapExt?: { [index: any]: any };"   + settings.newline +
"}"                                     + settings.newline +
""                                      + settings.newline +
"";
        final String actualA = new TypeScriptGenerator(settings).generateTypeScript(Input.from(A.class));
        final String actualB = new TypeScriptGenerator(settings).generateTypeScript(Input.from(B.class));

        assertEquals(expected, actualA);
        assertEquals(expected, actualB);
    }

    public static class A {
        public MapExtension mapExt;
    }

    public static class B {
        public MapExtension<String> mapExt;
    }

    public static class MapExtension<T> extends HashMap<T, Long> {}
}
