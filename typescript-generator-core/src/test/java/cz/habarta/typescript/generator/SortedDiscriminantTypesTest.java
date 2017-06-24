
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;


public class SortedDiscriminantTypesTest {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes({
        @JsonSubTypes.Type(Square.class),
        @JsonSubTypes.Type(Rectangle.class),
        @JsonSubTypes.Type(Circle.class),
    })
    private interface Shape {
    }

    private interface Quadrilateral extends Shape {
    }

    @JsonTypeName("square")
    private static class Square implements Quadrilateral {
        public double size;
    }

    @JsonTypeName("rectangle")
    private static class Rectangle implements Quadrilateral {
        public double width;
        public double height;
    }

    @JsonTypeName("circle")
    private static class Circle implements Shape {
        public double radius;
    }

    @Test
    public void testSortedDiscriminantTypes() {
        final Settings settings = TestUtils.settings();
        settings.sortDiscriminantTypes = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Shape.class));
        System.out.println(output);
        final String expected = (
                "\n" +
                "interface Shape {\n" +
                "    kind: 'circle' | 'rectangle' | 'square';\n" +
                "}\n" +
                "\n" +
                "interface Square extends Quadrilateral {\n" +
                "    kind: 'square';\n" +
                "    size: number;\n" +
                "}\n" +
                "\n" +
                "interface Rectangle extends Quadrilateral {\n" +
                "    kind: 'rectangle';\n" +
                "    width: number;\n" +
                "    height: number;\n" +
                "}\n" +
                "\n" +
                "interface Circle extends Shape {\n" +
                "    kind: 'circle';\n" +
                "    radius: number;\n" +
                "}\n" +
                "\n" +
                "interface Quadrilateral extends Shape {\n" +
                "    kind: 'rectangle' | 'square';\n" +
                "}\n" +
                "\n" +
                "type ShapeUnion = Square | Rectangle | Circle;\n" +
                ""
                ).replace('\'', '"');
        Assert.assertEquals(expected, output);
    }

}
