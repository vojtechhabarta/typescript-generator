
package cz.habarta.typescript.generator;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unused")
public class GenericCustomTypeMappingsTest {

    @Test
    public void testListWrapper() {
        final Settings settings = TestUtils.settings();
        settings.customTypeNaming = Collections.singletonMap(ListWrapper1.class.getName(), "ListWrapper");
        settings.customTypeMappings = Collections.singletonMap(ListWrapper2.class.getName() + "<T>", "ListWrapper<T>");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Class1.class));
        Assertions.assertTrue(output.contains("list1: ListWrapper<string>"));
        Assertions.assertTrue(output.contains("list2: ListWrapper<number>"));
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
        Assertions.assertTrue(output.contains("someMap: Map<string, any>"));
        Assertions.assertTrue(output.contains("dateMap: Map<string, DateAsString>"));
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
        Assertions.assertTrue(output.contains("id: string;"));
        Assertions.assertTrue(!output.contains("IdRepresentation"));
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
        testInvalid(Generic2.class.getName(), "string");
        testInvalid(Generic2.class.getName() + "<T>", "string");
    }

    private static void testInvalid(String javaName, String tsName) {
        try {
            final Settings settings = TestUtils.settings();
            settings.customTypeMappings = Collections.singletonMap(javaName, tsName);
            new TypeScriptGenerator(settings).generateTypeScript(Input.from());
            Assertions.fail();
        } catch (RuntimeException e) {
            // expected
        }
    }

    @Test
    public void testGenerics() {
        final Settings settings = TestUtils.settings();
        settings.customTypeMappings = Collections.singletonMap("cz.habarta.typescript.generator.GenericCustomTypeMappingsTest$Generic2<T1, T2>", "Test<T2, T1>");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Usage.class));
        Assertions.assertTrue(output.contains("generic: Test<number, string>"));
    }

    @Test
    public void testUnwrap() {
        final Settings settings = TestUtils.settings();
        settings.customTypeMappings = Collections.singletonMap("cz.habarta.typescript.generator.GenericCustomTypeMappingsTest$Generic2<T1, T2>", "T2");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Usage.class));
        Assertions.assertTrue(output.contains("generic: number"));
    }

    @Test
    public void testMapStringString() {
        final Settings settings = TestUtils.settings();
        settings.customTypeMappings = Collections.singletonMap("cz.habarta.typescript.generator.GenericCustomTypeMappingsTest$NonGeneric", "Map<string, string>");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(NonGenericUsage.class));
        Assertions.assertTrue(output.contains("nonGeneric: Map<string, string>"));
    }

    private static class NonGeneric {}
    private static class NonGenericUsage {
        public NonGeneric nonGeneric;
    }
    private static class Generic2<T1, T2> {}
    private static class Usage {
        public Generic2<String, Integer> generic;
    }

    @Test
    public void testAlternativeSyntax() {
        final Settings settings = TestUtils.settings();
        settings.customTypeMappings = Collections.singletonMap("cz.habarta.typescript.generator.GenericCustomTypeMappingsTest$Generic2[T1, T2]", "Test[T2, T1]");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Usage.class));
        Assertions.assertTrue(output.contains("generic: Test<number, string>"));
    }

    @Test
    public void testAlternativeSyntaxWithArray() {
        final Settings settings = TestUtils.settings();
        settings.customTypeMappings = Collections.singletonMap("cz.habarta.typescript.generator.GenericCustomTypeMappingsTest$Generic2[T1, T2]", "string[]");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Usage.class));
        Assertions.assertTrue(output.contains("generic: string[]"));
    }

    private static class BinaryData {
        public byte[] data;
        public byte[][] dataArray;
        public List<byte[]> dataList;
        public long[] specialData;
    }

    @Test
    public void byteArrayAsString() {
        final Settings settings = TestUtils.settings();
        settings.customTypeMappings.put("byte[]", "string");
        settings.customTypeMappings.put("byte[][]", "DifferentString[]");
        settings.customTypeMappings.put("long[]", "SpecialString");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(BinaryData.class));
        Assertions.assertTrue(output.contains("data: string"), output);
        Assertions.assertTrue(output.contains("dataArray: DifferentString[]"), output);
        Assertions.assertTrue(output.contains("dataList: string[]"), output);
        Assertions.assertTrue(output.contains("specialData: SpecialString"), output);
    }

    @Test
    public void testGenericSuperType() {
        final Settings settings = TestUtils.settings();
        settings.mapDate = DateMapping.asString;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Class3.class));
        Assertions.assertTrue(output.contains("Interface<DateAsString>"));
        Assertions.assertTrue(output.contains("interfaceValue: DateAsString;"));
        Assertions.assertTrue(output.contains("AbstractClass<DateAsString>"));
        Assertions.assertTrue(output.contains("abstractValue: DateAsString;"));
    }

    private interface Interface<T> {
        T getInterfaceValue();
    }

    private static abstract class AbstractClass<T> {
        public abstract T getAbstractValue();
    }

    private static abstract class Class3 extends AbstractClass<Date> implements Interface<Date>{
    }


}
