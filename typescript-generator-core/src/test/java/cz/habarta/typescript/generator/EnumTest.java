
package cz.habarta.typescript.generator;

import static org.junit.Assert.*;
import org.junit.Test;


public class EnumTest {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        final String actual = new TypeScriptGenerator(settings).generateTypeScript(Input.from(AClass.class));
        final String expected = (
                "\n" +
                "interface AClass {\n" +
                "    direction: Direction;\n" +
                "}\n" +
                "\n" +
                "type Direction = 'North' | 'East' | 'South' | 'West';\n"
                ).replace("'", "\"");
        assertEquals(expected, actual);
    }

    @Test
    public void testRichEnum() {
        final Settings settings = TestUtils.settings();
        final String actual = new TypeScriptGenerator(settings).generateTypeScript(Input.from(AnotherClass.class));
        final String expected = ("\n"
                + "interface AnotherClass {\n"
                + "    richEnum: RichEnum;\n"
                + "}\n"
                + "\n"
                + "type RichEnum = 'A' | 'B';\n").replace("'", "\"");
        assertEquals(expected, actual);
    }

    @Test
    public void testSingleEnum() {
        final Settings settings = TestUtils.settings();
        final String actual = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Direction.class));
        final String expected = (
                "\n" +
                "type Direction = 'North' | 'East' | 'South' | 'West';\n"
                ).replace("'", "\"");
        assertEquals(expected, actual);
    }

    @Test
    public void inlineEnumTest() {
        final Settings settings = TestUtils.settings();
        settings.quotes = "'";
        settings.experimentalInlineEnums = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(AClass.class));
        final String expected =
                "\n" +
                "interface AClass {\n" +
                "    direction: 'North' | 'East' | 'South' | 'West';\n" +
                "}\n";
        assertEquals(expected, output);
    }

    @Test
    public void enumValueModelTest() {
        final Settings settings = TestUtils.settings();
        settings.enumValueModel = true;
        settings.addEnumValueSuffix = "Value";
        final String actual = new TypeScriptGenerator(settings).generateTypeScript(Input.from(AClass.class));
        final String expected = ("\n"
                + "interface AClass {\n"
                + "    direction: Direction;\n"
                + "}\n"
                + "\n"
                + "interface DirectionValue {\n"
                + "}\n"
                + "\n"
                + "type Direction = 'North' | 'East' | 'South' | 'West';\n").replace("'", "\"");
        assertEquals(expected, actual);
    }

    @Test
    public void richEnumValueModelTest() {
        final Settings settings = TestUtils.settings();
        settings.enumValueModel = true;
        settings.addEnumValueSuffix = "Value";
        final String actual = new TypeScriptGenerator(settings).generateTypeScript(Input.from(AnotherClass.class));
        final String expected = ("\n"
                + "interface AnotherClass {\n"
                + "    richEnum: RichEnum;\n"
                + "}\n"
                + "\n"
                + "interface RichEnumValue {\n"
                + "    name: string;\n"
                + "    value: number;\n"
                + "    directions: Direction[];\n"
                + "}\n"
                + "\n"
                + "type RichEnum = 'A' | 'B';\n").replace("'", "\"");
        assertEquals(expected, actual);
    }

    private static class AClass {
        public Direction direction;
    }

    private static class AnotherClass {

        public RichEnum richEnum;
    }

    enum Direction {
        North,
        East,
        South,
        West
    }

    enum RichEnum {
        A("aaa", 12, Direction.North),
        B("bqsd", 54, Direction.North, Direction.West);
        String name;
        int value;
        Direction[] directions;

        private RichEnum(String name, int value, Direction... directions) {
            this.name = name;
            this.value = value;
            this.directions = directions;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }

        public Direction[] getDirections() {
            return directions;
        }

    }

}
