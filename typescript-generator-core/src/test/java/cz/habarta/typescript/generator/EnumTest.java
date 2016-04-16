
package cz.habarta.typescript.generator;

import static org.junit.Assert.*;
import org.junit.Test;


public class EnumTest {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        final String actual = new TypeScriptGenerator(settings).generateTypeScript(Input.from(AClass.class));
        final String expected =
                "\n" +
                "interface AClass {\n" +
                "    direction: Direction;\n" +
                "}\n" +
                "\n" +
                "type Direction = 'North' | 'East' | 'South' | 'West';\n"
                .replace("'", "\"");
        assertEquals(expected, actual);
    }

    @Test
    public void testSingleEnum() {
        final Settings settings = TestUtils.settings();
        final String actual = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Direction.class));
        final String expected =
                "\n" +
                "type Direction = 'North' | 'East' | 'South' | 'West';\n"
                .replace("'", "\"");
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
