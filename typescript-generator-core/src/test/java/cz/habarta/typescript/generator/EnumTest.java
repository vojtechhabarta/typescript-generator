
package cz.habarta.typescript.generator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class EnumTest {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        final String actual = new TypeScriptGenerator(settings).generateTypeScript(Input.from(AClass.class));
        final String expected = "\n" +
                        "interface AClass {\n" +
                        "    direction: Direction;\n" +
                        "}\n" +
                        "\n" +
                        "declare enum Direction {North, East, South, West}\n";
        assertEquals(expected, actual);
    }

    @Test
    public void testSingleEnum() {
        final Settings settings = TestUtils.settings();
        final String actual = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Direction.class));
        final String expected = "\n" +
                        "declare enum Direction {North, East, South, West}\n";
        System.out.println(actual);
        assertEquals(expected, actual);
    }

    @Test
    public void testSingleEnumAsModule() throws Exception {
        Settings settings = TestUtils.settings();
        settings.outputKind = TypeScriptOutputKind.module;
        settings.outputFileType = TypeScriptFileType.implementationFile;
        final String actual = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Direction.class));
        final String expected = "\n" +
                        "export enum Direction {North, East, South, West}\n";
        System.out.println(actual);
        assertEquals(expected, actual);
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

}
