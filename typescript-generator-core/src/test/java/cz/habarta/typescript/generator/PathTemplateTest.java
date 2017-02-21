
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.PathTemplate;
import org.junit.Assert;
import org.junit.Test;


public class PathTemplateTest {

    @Test
    public void test() {
        Assert.assertEquals(0, PathTemplate.parse("").getParts().size());
        Assert.assertEquals(1, PathTemplate.parse("a").getParts().size());
        Assert.assertEquals(1, PathTemplate.parse("{x}").getParts().size());
        Assert.assertEquals(1, PathTemplate.parse("{x:.+}").getParts().size());
        Assert.assertEquals(1, PathTemplate.parse("{ x : .+ }").getParts().size());
        Assert.assertEquals(2, PathTemplate.parse("a{x}").getParts().size());
        Assert.assertEquals(2, PathTemplate.parse("{x}a").getParts().size());
        Assert.assertEquals(2, PathTemplate.parse("{x}{y}").getParts().size());
        Assert.assertEquals(3, PathTemplate.parse("a{x}a").getParts().size());
        Assert.assertEquals(3, PathTemplate.parse("{x}a{y}").getParts().size());

        Assert.assertEquals("${x}a${y}", PathTemplate.parse("{ x : .+ }a{y}").format("${", "}", false));
        Assert.assertEquals("{x:.+}a{y}", PathTemplate.parse("{ x : .+ }a{y}").format("{", "}", true));
    }

}
