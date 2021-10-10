
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.PathTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class PathTemplateTest {

    @Test
    public void test() {
        Assertions.assertEquals(0, PathTemplate.parse("").getParts().size());
        Assertions.assertEquals(1, PathTemplate.parse("a").getParts().size());
        Assertions.assertEquals(1, PathTemplate.parse("{x}").getParts().size());
        Assertions.assertEquals(1, PathTemplate.parse("{x:.+}").getParts().size());
        Assertions.assertEquals(1, PathTemplate.parse("{ x : .+ }").getParts().size());
        Assertions.assertEquals(2, PathTemplate.parse("a{x}").getParts().size());
        Assertions.assertEquals(2, PathTemplate.parse("{x}a").getParts().size());
        Assertions.assertEquals(2, PathTemplate.parse("{x}{y}").getParts().size());
        Assertions.assertEquals(3, PathTemplate.parse("a{x}a").getParts().size());
        Assertions.assertEquals(3, PathTemplate.parse("{x}a{y}").getParts().size());

        Assertions.assertEquals("${x}a${y}", PathTemplate.parse("{ x : .+ }a{y}").format("${", "}", false));
        Assertions.assertEquals("{x:.+}a{y}", PathTemplate.parse("{ x : .+ }a{y}").format("{", "}", true));
    }

}
