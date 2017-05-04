
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import static org.junit.Assert.*;
import org.junit.Test;


public class EnumTest {

    @Test
    public void testEnumAsUnion() {
        final Settings settings = TestUtils.settings();
//        settings.mapEnum = EnumMapping.asUnion;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(AClass.class));
        final String expected = (
                "\n" +
                "interface AClass {\n" +
                "    direction: Direction;\n" +
                "}\n" +
                "\n" +
                "type Direction = 'North' | 'East' | 'South' | 'West';\n"
                ).replace("'", "\"");
        assertEquals(expected, output);
    }

    @Test
    public void testSingleEnum() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Direction.class));
        final String expected = (
                "\n" +
                "type Direction = 'North' | 'East' | 'South' | 'West';\n"
                ).replace("'", "\"");
        assertEquals(expected, output);
    }

    @Test
    public void testEnumAsInlineUnion() {
        final Settings settings = TestUtils.settings();
        settings.quotes = "'";
        settings.mapEnum = EnumMapping.asInlineUnion;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(AClass.class));
        final String expected =
                "\n" +
                "interface AClass {\n" +
                "    direction: 'North' | 'East' | 'South' | 'West';\n" +
                "}\n";
        assertEquals(expected, output);
    }

    @Test
    public void testEnumAsNumberBasedEnum() {
        final Settings settings = TestUtils.settings();
        settings.mapEnum = EnumMapping.asNumberBasedEnum;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(AClass.class));
        final String expected = (
                "\n" +
                "interface AClass {\n" +
                "    direction: Direction;\n" +
                "}\n" +
                "\n" +
                "declare const enum Direction {\n" +
                "    North,\n" +
                "    East,\n" +
                "    South,\n" +
                "    West,\n" +
                "}\n"
                ).replace("'", "\"");
        assertEquals(expected, output);
    }

    @Test
    public void testEnumWithJsonPropertyAnnotations() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(SideWithJsonPropertyAnnotations.class));
        final String expected = (
                "\n" +
                "type SideWithJsonPropertyAnnotations = 'left-side' | 'right-side';\n"
                ).replace("'", "\"");
        assertEquals(expected, output);
    }

    @Test
    public void testEnumWithJsonValueAnnotation() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(SideWithJsonValueAnnotations.class));
        final String expected = (
                "\n" +
                "type SideWithJsonValueAnnotations = 'left-side' | 'right-side';\n"
                ).replace("'", "\"");
        assertEquals(expected, output);
    }

    @Test
    public void testEmptyEnum() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(EmptyEnum.class));
        final String expected = (
                "\n" +
                "type EmptyEnum = never;\n"
                ).replace("'", "\"");
        assertEquals(expected, output);
    }

    @Test
    public void testExcludeObjectEnum() {
        final Settings settings = TestUtils.settings();
        settings.setExcludeFilter(Arrays.asList(StatusType.class.getName()), Arrays.<String>asList());
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithObjectEnum.class, StatusType.class));
        assertTrue(!output.contains("StatusType"));
    }

    @Test
    public void testObjectEnum() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(StatusType.class));
        System.out.println(output);
        final String expected = "" +
                "interface StatusType {\n" +
                "    code: number;\n" +
                "    label: string;\n" +
                "}";
        assertEquals(expected.trim(), output.trim());
    }

    private static class AClass {
        public Direction direction;
    }

    enum Direction {
        North,
        East, 
        South,
        West
    }

    enum SideWithJsonPropertyAnnotations {
        @JsonProperty("left-side")
        Left,
        @JsonProperty("right-side")
        Right
    }

    enum SideWithJsonValueAnnotations {
        @JsonProperty("@JsonProperty ignored since @JsonValue has higher precedence")
        Left("left-side"),
        @JsonProperty("@JsonProperty ignored since @JsonValue has higher precedence")
        Right("right-side");

        private final String jsonValue;

        private SideWithJsonValueAnnotations(String jsonValue) {
            this.jsonValue = jsonValue;
        }

        @JsonValue
        public Object getJsonValue() {
            return jsonValue;
        }
    }

    enum EmptyEnum {
    }

    static class ClassWithObjectEnum {
        public StatusType status;
        public List<Map<String, StatusType>> statuses;
    }

    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    public enum StatusType {
        GOOD(0, "Good"),
        FULL(1, "Full");

        private final int code;
        private final String label;

        private StatusType(int code, String label) {
            this.label = label;
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }
    }

}
