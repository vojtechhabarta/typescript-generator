
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.habarta.typescript.generator.util.Utils;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class MapEntryTest {

    public static class ClassWithEntries {
        public String name = "ClassWithEntries";
        public Entry1<MyBean, String> entry1 = new Entry1<>(new MyBean("nnn"), "NNN");
        public Entry2<MyBean, String> entry2 = new Entry2<>(new MyBean("ooo"), "OOO");
        public Map.Entry<MyBean, String> entry3 = new AbstractMap.SimpleEntry<>(new MyBean("eee"), "EEE");
    }

    @JsonFormat(shape = JsonFormat.Shape.NATURAL)
    public static class Entry1<K, V> extends AbstractMap.SimpleEntry<K, V> {
        private static final long serialVersionUID = 1L;
        public Entry1(K key, V value) {
            super(key, value);
        }
    }

    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    public static class Entry2<K, V> extends AbstractMap.SimpleEntry<K, V> {
        private static final long serialVersionUID = 1L;
        public Entry2(K key, V value) {
            super(key, value);
        }
    }

    public static class MyBean {
        public String f0;
        public boolean f1 = true;

        public MyBean(String f0) {
            this.f0 = f0;
        }

        @Override
        public String toString() {
            return "MyBean instance";
        }
    }

    @Test
    public void testDefaultShapes() throws Exception {
        final ObjectMapper objectMapper = Utils.getObjectMapper();
        final ClassWithEntries classWithEntries = new ClassWithEntries();
        final String json = objectMapper.writeValueAsString(classWithEntries);
        final String expectedJson = (""
                + "{\n"
                + "  'entry1': {\n"
                + "    'MyBean instance': 'NNN'\n"
                + "  },\n"
                + "  'entry2': {\n"
                + "    'key': {\n"
                + "      'f0': 'ooo',\n"
                + "      'f1': true\n"
                + "    },\n"
                + "    'value': 'OOO'\n"
                + "  },\n"
                + "  'entry3': {\n"
                + "    'MyBean instance': 'EEE'\n"
                + "  },\n"
                + "  'name': 'ClassWithEntries'\n"
                + "}")
                .replace("'", "\"");
        Assert.assertEquals(expectedJson, json);

        final Settings settings = TestUtils.settings();
        settings.setExcludeFilter(
                Arrays.asList(Serializable.class.getName(), AbstractMap.SimpleEntry.class.getName()),
                null);
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithEntries.class));
        Assert.assertTrue(output.contains("entry1: { [index: string]: string }"));
        Assert.assertTrue(output.contains("entry2: Entry2<MyBean, string>"));
        Assert.assertTrue(output.contains(""
                + "interface Entry2<K, V> {\n"
                + "    key: K;\n"
                + "    value: V;\n"
                + "}"));
        Assert.assertTrue(output.contains("entry3: { [index: string]: string }"));
    }

    @Test
    public void testOverriddenShapes() throws Exception {
        final ObjectMapper objectMapper = Utils.getObjectMapper();
        objectMapper.configOverride(Entry1.class).setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.OBJECT));
        objectMapper.configOverride(Entry2.class).setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.NATURAL));
        final ClassWithEntries classWithEntries = new ClassWithEntries();
        final String json = objectMapper.writeValueAsString(classWithEntries);
        final String expectedJson = (""
                + "{\n"
                + "  'entry1': {\n"
                + "    'key': {\n"
                + "      'f0': 'nnn',\n"
                + "      'f1': true\n"
                + "    },\n"
                + "    'value': 'NNN'\n"
                + "  },\n"
                + "  'entry2': {\n"
                + "    'MyBean instance': 'OOO'\n"
                + "  },\n"
                + "  'entry3': {\n"
                + "    'MyBean instance': 'EEE'\n"
                + "  },\n"
                + "  'name': 'ClassWithEntries'\n"
                + "}")
                .replace("'", "\"");
        Assert.assertEquals(expectedJson, json);

        final Settings settings = TestUtils.settings();
        settings.jackson2Configuration = new Jackson2ConfigurationResolved();
        settings.jackson2Configuration.shapeConfigOverrides = new LinkedHashMap<>();
        settings.jackson2Configuration.shapeConfigOverrides.put(Entry1.class, JsonFormat.Shape.OBJECT);
        settings.jackson2Configuration.shapeConfigOverrides.put(Entry2.class, JsonFormat.Shape.NATURAL);
        settings.setExcludeFilter(
                Arrays.asList(Serializable.class.getName(), AbstractMap.SimpleEntry.class.getName()),
                null);
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithEntries.class));
        Assert.assertTrue(output.contains("entry1: Entry1<MyBean, string>"));
        Assert.assertTrue(output.contains("entry2: { [index: string]: string }"));
        Assert.assertTrue(output.contains(""
                + "interface Entry1<K, V> {\n"
                + "    key: K;\n"
                + "    value: V;\n"
                + "}"));
        Assert.assertTrue(output.contains("entry3: { [index: string]: string }"));
    }

    public static class ClassWithListOfEntries {
        public List<Entry1<String, String>> entries1 = Arrays.asList(
                new Entry1<>("key1", "value1"),
                new Entry1<>("key2", "value2"));
        public List<Entry2<String, String>> entries2 = Arrays.asList(
                new Entry2<>("key1", "value1"),
                new Entry2<>("key2", "value2"));
    }

    @Test
    public void testListOfMapEntry() throws Exception {
        final ObjectMapper objectMapper = Utils.getObjectMapper();
        final ClassWithListOfEntries classWithListOfEntries = new ClassWithListOfEntries();
        final String json = objectMapper.writeValueAsString(classWithListOfEntries);
        final String expectedJson = (""
                + "{\n"
                + "  'entries1': [\n"
                + "    {\n"
                + "      'key1': 'value1'\n"
                + "    },\n"
                + "    {\n"
                + "      'key2': 'value2'\n"
                + "    }\n"
                + "  ],\n"
                + "  'entries2': [\n"
                + "    {\n"
                + "      'key': 'key1',\n"
                + "      'value': 'value1'\n"
                + "    },\n"
                + "    {\n"
                + "      'key': 'key2',\n"
                + "      'value': 'value2'\n"
                + "    }\n"
                + "  ]\n"
                + "}")
                .replace("'", "\"");
        Assert.assertEquals(expectedJson, json);

        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithListOfEntries.class));
        Assert.assertTrue(output.contains("entries1: { [index: string]: string }[]"));
        Assert.assertTrue(output.contains("entries2: Entry2<string, string>[]"));
    }

}
