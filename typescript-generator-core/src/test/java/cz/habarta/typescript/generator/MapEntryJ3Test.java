
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonFormat;
import cz.habarta.typescript.generator.util.Utils;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;


/**
 * Jackson 3 version of Map.Entry tests.
 * <p>
 * Jackson 3 always serializes {@code Map.Entry} in NATURAL (map-like) format, regardless of
 * {@code @JsonFormat} annotations or config overrides. The TypeScript generator matches this behavior:
 * all {@code Map.Entry} subclasses are emitted as {@code { [index: string]: V }} map types.
 * <p>
 * See {@link MapEntryJ2Test} for Jackson 2 behavior where {@code @JsonFormat(shape = OBJECT)} and
 * config overrides correctly switch entries to key/value object format.
 */
public class MapEntryJ3Test {

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
    public void testDefaultShapes() {
        final ObjectMapper objectMapper = Utils.getObjectMapper();
        final ClassWithEntries classWithEntries = new ClassWithEntries();
        final String json = objectMapper.writeValueAsString(classWithEntries);
        // Jackson 3: @JsonFormat(shape=OBJECT) on Entry2 is ignored during serialization —
        // all three entries serialize in NATURAL (map-like) format
        final String expectedJson = (""
            + "{\n"
            + "  'name': 'ClassWithEntries',\n"
            + "  'entry1': {\n"
            + "    'MyBean instance': 'NNN'\n"
            + "  },\n"
            + "  'entry2': {\n"
            + "    'MyBean instance': 'OOO'\n"
            + "  },\n"
            + "  'entry3': {\n"
            + "    'MyBean instance': 'EEE'\n"
            + "  }\n"
            + "}")
            .replace("'", "\"");
        Assertions.assertEquals(expectedJson, json);

        final Settings settings = TestUtils.settings();
        settings.setExcludeFilter(
            Arrays.asList(Serializable.class.getName(), AbstractMap.SimpleEntry.class.getName()),
            null);
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithEntries.class));
        // All entries are emitted as map types — Jackson 3 always uses NATURAL format
        Assertions.assertTrue(output.contains("entry1: { [index: string]: string }"));
        Assertions.assertTrue(output.contains("entry2: { [index: string]: string }"));
        Assertions.assertFalse(output.contains("interface Entry2"));
        Assertions.assertTrue(output.contains("entry3: { [index: string]: string }"));
    }

    /**
     * Jackson 3: shape config overrides on Map.Entry subclasses have no effect.
     * <p>
     * Unlike Jackson 2, {@code withConfigOverride(Entry1.class, OBJECT)} and
     * {@code withConfigOverride(Entry2.class, NATURAL)} do not change serialization —
     * all entries still serialize in NATURAL format. The TS output is identical to
     * {@link #testDefaultShapes()} — all entries are map types.
     * <p>
     * See {@link MapEntryJ2Test#testOverriddenShapes()} for Jackson 2 where overrides work.
     */
    @Test
    public void testOverriddenShapes() {
        final ObjectMapper objectMapper = Utils.getObjectMapper().rebuild()
            .withConfigOverride(Entry1.class, co -> co.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.OBJECT)))
            .withConfigOverride(Entry2.class, co -> co.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.NATURAL)))
            .build();
        final ClassWithEntries classWithEntries = new ClassWithEntries();
        final String json = objectMapper.writeValueAsString(classWithEntries);
        // Jackson 3: config overrides are ignored — identical output to testDefaultShapes
        final String expectedJson = (""
            + "{\n"
            + "  'name': 'ClassWithEntries',\n"
            + "  'entry1': {\n"
            + "    'MyBean instance': 'NNN'\n"
            + "  },\n"
            + "  'entry2': {\n"
            + "    'MyBean instance': 'OOO'\n"
            + "  },\n"
            + "  'entry3': {\n"
            + "    'MyBean instance': 'EEE'\n"
            + "  }\n"
            + "}")
            .replace("'", "\"");
        Assertions.assertEquals(expectedJson, json);

        final Settings settings = TestUtils.settings();
        settings.jackson3Configuration = new Jackson3ConfigurationResolved();
        settings.jackson3Configuration.shapeConfigOverrides = new LinkedHashMap<>();
        settings.jackson3Configuration.shapeConfigOverrides.put(Entry1.class, JsonFormat.Shape.OBJECT);
        settings.jackson3Configuration.shapeConfigOverrides.put(Entry2.class, JsonFormat.Shape.NATURAL);
        settings.setExcludeFilter(
            Arrays.asList(Serializable.class.getName(), AbstractMap.SimpleEntry.class.getName()),
            null);
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithEntries.class));
        // All entries are map types — config overrides have no effect in Jackson 3
        Assertions.assertTrue(output.contains("entry1: { [index: string]: string }"));
        Assertions.assertTrue(output.contains("entry2: { [index: string]: string }"));
        Assertions.assertFalse(output.contains("interface Entry2"));
        Assertions.assertTrue(output.contains("entry3: { [index: string]: string }"));
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
    public void testListOfMapEntry() {
        final ObjectMapper objectMapper = Utils.getObjectMapper();
        final ClassWithListOfEntries classWithListOfEntries = new ClassWithListOfEntries();
        final String json = objectMapper.writeValueAsString(classWithListOfEntries);
        // Jackson 3: Entry2 (shape=OBJECT) is ignored — both entries serialize as NATURAL maps
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
            + "      'key1': 'value1'\n"
            + "    },\n"
            + "    {\n"
            + "      'key2': 'value2'\n"
            + "    }\n"
            + "  ]\n"
            + "}")
            .replace("'", "\"");
        Assertions.assertEquals(expectedJson, json);

        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithListOfEntries.class));
        Assertions.assertTrue(output.contains("entries1: { [index: string]: string }[]"));
        Assertions.assertTrue(output.contains("entries2: { [index: string]: string }[]"));
        Assertions.assertFalse(output.contains("interface Entry2"));
    }

}
