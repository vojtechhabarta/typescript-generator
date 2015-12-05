package cz.habarta.typescript.generator;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.junit.*;

import cz.habarta.typescript.generator.TypeProcessor.*;

public class GenericsTest {

    @Test
    public void testGeneric() throws NoSuchMethodException, SecurityException {
        List<Class<?>> classes = new ArrayList<>();
        classes.add(A.class);
        Settings settings = new Settings();
        settings.addTypeNamePrefix = "I";
        settings.sortDeclarations = true;
        settings.noFileComment = true;

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        TypeScriptGenerator.generateEmbeddableTypeScript(classes, settings, output, true, 0);
        String actual = new String(output.toByteArray()).trim();
        String expected = ""                     +
"export interface IA<U, V> {"                    + settings.newline +
"    x: IA<string, string>;"                     + settings.newline +
"    y: IA<IA<string, IB>, string[]>;"           + settings.newline +
"    z: IA<{ [index: string]: V }, number[]>;"   + settings.newline +
"}"                                              + settings.newline +
""                                               + settings.newline +
"export interface IB {"                          + settings.newline +
"}";
        assertEquals(expected, actual);
        Context context = TypeScriptGenerator.createTypeProcessorContext(settings);
        assertEquals("IA<string, string>", context.processType(A.class.getMethod("getX").getGenericReturnType()).getTsType().toString());
        assertEquals("IA<IA<string, IB>, string[]>", context.processType(A.class.getMethod("getY").getGenericReturnType()).getTsType().toString());
        assertEquals("IA<{ [index: string]: V }, number[]>", context.processType(A.class.getMethod("getZ").getGenericReturnType()).getTsType().toString());
    }

    class A<U,V> {
        public A<String, String> getX() {
            return null;
        }

        public A<A<String, B>, List<String>> getY() {
            return null;
        }

        public A<Map<String, V>, Set<Integer>> getZ() {
            return null;
        }
    }

    class B {

    }
}
