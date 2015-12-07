package cz.habarta.typescript.generator;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.junit.*;

public class StyleConfigurationTest {

    @Test
    public void testOutputWithCustomStyle() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Settings settings = new Settings();
        settings.addTypeNamePrefix = "I";
        settings.sortDeclarations = true;
        settings.noFileComment = true;

        List<Class<?>> classList = new ArrayList<>();
        classList.add(A.class);

        String expected = ""    +
""                              + settings.newline +
"    export interface IA {"     + settings.newline +
"        b: IB;"                + settings.newline +
"        x: number;"            + settings.newline +
"    }"                         + settings.newline +
""                              + settings.newline +
"    export interface IB {"     + settings.newline +
"        s: string;"            + settings.newline +
"    }"                         + settings.newline +
"";
        new TypeScriptGenerator(settings).generateEmbeddableTypeScript(classList, output, true, 1);

        assertEquals(expected, new String(output.toByteArray()));
        assertEquals("IA", TypeScriptGenerator.createTypeProcessorContext(settings).processType(A.class).getTsType().toString());
    }

    public static class A {
        public int getX() {
            return -1;
        }
        public B getB() {
            return null;
        }
    }

    public static class B {
        public String getS() {
            return null;
        }
    }
}
