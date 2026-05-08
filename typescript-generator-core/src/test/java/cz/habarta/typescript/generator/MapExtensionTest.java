
package cz.habarta.typescript.generator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SuppressWarnings("rawtypes")
public class MapExtensionTest {

    @Test
    public void testOrder1() {
        final Settings settings = TestUtils.settings();
        settings.sortDeclarations = true;
        String expectedA = "" +
            "\n" +
            "interface A {\n" +
            "    mapExt: { [index: string]: any };\n" +
            "}\n";
        String expectedB = "" +
            "\n" +
            "interface B {\n" +
            "    mapExt: { [index: string]: number };\n" +
            "}\n";
        final String actualA = new TypeScriptGenerator(settings).generateTypeScript(Input.from(A.class));
        final String actualB = new TypeScriptGenerator(settings).generateTypeScript(Input.from(B.class));

        assertEquals(expectedA, actualA);
        assertEquals(expectedB, actualB);
    }

    @SuppressWarnings("NullAway.Init")
    public static class A {
        public MapExtension mapExt;
    }

    @SuppressWarnings("NullAway.Init")
    public static class B {
        public MapExtension<String> mapExt;
    }

    public static class MapExtension<T> extends HashMap<T, Long> {
    }

    @Test
    public void testStringList() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(C.class));
        Assertions.assertTrue(output.contains("stringList: string[];"));
    }

    public static interface StringList extends List<String> {
    }

    @SuppressWarnings("NullAway.Init")
    public static class C {
        public StringList stringList;
    }

    @Test
    public void testStringKeyMapNumberValue() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(D.class));
        Assertions.assertTrue(output.contains("stringKeyMap: { [index: string]: number };"));
    }

    @SuppressWarnings("NullAway.Init")
    public static class D {
        public StringKeyMap<Number> stringKeyMap;
    }

    @Test
    public void testStringKeyMapGenericValue() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(E.class));
        Assertions.assertTrue(output.contains("stringKeyMap: { [index: string]: T };"));
    }

    @SuppressWarnings("NullAway.Init")
    public static class E<T> {
        public StringKeyMap<T> stringKeyMap;
    }

    public static interface StringKeyMap<T> extends Map<String, T> {
    }

}
