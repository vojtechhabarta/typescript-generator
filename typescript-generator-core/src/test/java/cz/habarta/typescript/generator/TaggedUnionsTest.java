
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;


public class TaggedUnionsTest {

    private static class Geometry {
        public List<Shape> shapes;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes({
        @JsonSubTypes.Type(Square.class),
        @JsonSubTypes.Type(Rectangle.class),
        @JsonSubTypes.Type(Circle.class),
    })
    private static class Shape {
    }

    @JsonTypeName("square")
    private static class Square extends Shape {
        public double size;
    }

    @JsonTypeName("rectangle")
    private static class Rectangle extends Shape {
        public double width;
        public double height;
    }

    @JsonTypeName("circle")
    private static class Circle extends Shape {
        public double radius;
    }


    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes({
        @JsonSubTypes.Type(CSquare2.class),
        @JsonSubTypes.Type(CRectangle2.class),
        @JsonSubTypes.Type(CCircle2.class),
    })
    private static interface IShape2 {
    }

    private static interface IQuadrilateral2 extends IShape2 {
    }

    @JsonTypeName("square")
    private static class CSquare2 implements IQuadrilateral2 {
        public double size;
    }

    @JsonTypeName("rectangle")
    private static class CRectangle2 implements IQuadrilateral2 {
        public double width;
        public double height;
    }

    @JsonTypeName("circle")
    private static class CCircle2 implements IShape2 {
        public double radius;
    }

    @Test
    public void testTaggedUnions() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Geometry.class));
        final String expected = (
                "\n" +
                "interface Geometry {\n" +
                "    shapes: ShapeUnion[];\n" +
                "}\n" +
                "\n" +
                "interface Shape {\n" +
                "    kind: 'square' | 'rectangle' | 'circle';\n" +
                "}\n" +
                "\n" +
                "interface Square extends Shape {\n" +
                "    kind: 'square';\n" +
                "    size: number;\n" +
                "}\n" +
                "\n" +
                "interface Rectangle extends Shape {\n" +
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
                "type ShapeUnion = Square | Rectangle | Circle;\n" +
                ""
                ).replace('\'', '"');
        Assert.assertEquals(expected, output);
    }

    @Test
    public void testTaggedUnionsWithInterfaces() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(IShape2.class));
        final String expected = (
                "\n" +
                "interface IShape2 {\n" +
                "    kind: 'circle' | 'square' | 'rectangle';\n" +
                "}\n" +
                "\n" +
                "interface CSquare2 extends IQuadrilateral2 {\n" +
                "    kind: 'square';\n" +
                "    size: number;\n" +
                "}\n" +
                "\n" +
                "interface CRectangle2 extends IQuadrilateral2 {\n" +
                "    kind: 'rectangle';\n" +
                "    width: number;\n" +
                "    height: number;\n" +
                "}\n" +
                "\n" +
                "interface CCircle2 extends IShape2 {\n" +
                "    kind: 'circle';\n" +
                "    radius: number;\n" +
                "}\n" +
                "\n" +
                "interface IQuadrilateral2 extends IShape2 {\n" +
                "}\n" +
                "\n" +
                "type IShape2Union = CSquare2 | CRectangle2 | CCircle2;\n" +
                ""
                ).replace('\'', '"');
        Assert.assertEquals(expected, output);
    }

    @Test
    public void testTaggedUnionsDisabled() {
        final Settings settings = TestUtils.settings();
        settings.disableTaggedUnions = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Geometry.class));
        final String expected = (
                "\n" +
                "interface Geometry {\n" +
                "    shapes: Shape[];\n" +
                "}\n" +
                "\n" +
                "interface Shape {\n" +
                "    kind: 'square' | 'rectangle' | 'circle';\n" +
                "}\n" +
                "\n" +
                "interface Square extends Shape {\n" +
                "    kind: 'square';\n" +
                "    size: number;\n" +
                "}\n" +
                "\n" +
                "interface Rectangle extends Shape {\n" +
                "    kind: 'rectangle';\n" +
                "    width: number;\n" +
                "    height: number;\n" +
                "}\n" +
                "\n" +
                "interface Circle extends Shape {\n" +
                "    kind: 'circle';\n" +
                "    radius: number;\n" +
                "}\n" +
                ""
                ).replace('\'', '"');
        Assert.assertEquals(expected, output);
    }

}
