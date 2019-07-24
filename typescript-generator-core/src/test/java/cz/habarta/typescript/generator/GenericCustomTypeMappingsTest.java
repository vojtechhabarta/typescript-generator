
package cz.habarta.typescript.generator;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;


public class GenericCustomTypeMappingsTest {

    @Test
    public void testListWrapper() {
        final Settings settings = TestUtils.settings();
        settings.customTypeNaming = Collections.singletonMap(ListWrapper1.class.getName(), "ListWrapper");
        settings.customTypeMappings = Collections.singletonMap(ListWrapper2.class.getName() + "<T>", "ListWrapper<T>");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Class1.class));
        Assert.assertTrue(output.contains("list1: ListWrapper<string>"));
        Assert.assertTrue(output.contains("list2: ListWrapper<number>"));
    }

    private static class Class1 {
        public ListWrapper1<String> list1;
        public ListWrapper2<Number> list2;
    }

    private static class ListWrapper1<T> {
        public List<T> values;
    }

    private static class ListWrapper2<T> {
        public List<T> values;
    }

    @Test
    public void testMap() {
        final Settings settings = TestUtils.settings();
        settings.customTypeMappings = Collections.singletonMap("java.util.Map<K, V>", "Map<K, V>");
        settings.mapDate = DateMapping.asString;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Class2.class));
        Assert.assertTrue(output.contains("someMap: Map<string, any>"));
        Assert.assertTrue(output.contains("dateMap: Map<string, DateAsString>"));
    }

    private static class Class2 {
        public Map<String, Object> someMap;
        public Map<String, Date> dateMap;
    }

    @Test
    public void testGenericMappingToString() {
        final Settings settings = TestUtils.settings();
        settings.customTypeMappings = Collections.singletonMap("cz.habarta.typescript.generator.GenericCustomTypeMappingsTest$IdRepresentation<T>", "string");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(MyEntityRepresentation.class));
        Assert.assertTrue(output.contains("id: string;"));
        Assert.assertTrue(!output.contains("IdRepresentation"));
    }

    private static class MyEntityRepresentation {
        public IdRepresentation<MyEntityRepresentation> id;
    }

    private static class IdRepresentation<T> {
        public String id;
    }

    @Test
    public void testInvalidGenerics() {
        testInvalid("NonExisting", "string");
        testInvalid(NonGeneric.class.getName() + "<T>", "string");
        testInvalid(NonGeneric.class.getName(), "string<T>");
        testInvalid(Generic2.class.getName(), "string");
        testInvalid(Generic2.class.getName() + "<T>", "string");
        testInvalid(Generic2.class.getName() + "<T1, T2>", "string<T>");
    }

    private static void testInvalid(String javaName, String tsName) {
        try {
            final Settings settings = TestUtils.settings();
            settings.customTypeMappings = Collections.singletonMap(javaName, tsName);
            new TypeScriptGenerator(settings).generateTypeScript(Input.from());
            Assert.fail();
        } catch (RuntimeException e) {
            // expected
        }
    }

    @Test
    public void testGenerics() {
        final Settings settings = TestUtils.settings();
        settings.customTypeMappings = Collections.singletonMap("cz.habarta.typescript.generator.GenericCustomTypeMappingsTest$Generic2<T1, T2>", "Test<T2, T1>");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Usage.class));
        Assert.assertTrue(output.contains("generic: Test<number, string>"));
    }

    @Test
    public void testUnwrap() {
        final Settings settings = TestUtils.settings();
        settings.customTypeMappings = Collections.singletonMap("cz.habarta.typescript.generator.GenericCustomTypeMappingsTest$Generic2<T1, T2>", "T2");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Usage.class));
        Assert.assertTrue(output.contains("generic: number"));
    }

    private static class NonGeneric {}
    private static class Generic2<T1, T2> {}
    private static class Usage {
        public Generic2<String, Integer> generic;
    }

    @Test
    public void testAlternativeSyntax() {
        final Settings settings = TestUtils.settings();
        settings.customTypeMappings = Collections.singletonMap("cz.habarta.typescript.generator.GenericCustomTypeMappingsTest$Generic2[T1, T2]", "Test[T2, T1]");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Usage.class));
        Assert.assertTrue(output.contains("generic: Test<number, string>"));
    }

}
