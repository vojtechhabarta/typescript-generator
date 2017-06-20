
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.junit.Assert;
import org.junit.Test;

public class ImmutablesTest {

    @Test
    public void testImmutables() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Shape.class));
        final String expected = (
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


    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = Square.class, name = "square"),
        @JsonSubTypes.Type(value = Rectangle.class, name = "rectangle"),
        @JsonSubTypes.Type(value = Circle.class, name = "circle"),
    })
    public static interface Shape {
    }

    public static class Square implements Shape {
        public double size;
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableRectangle.class)
    @JsonDeserialize(as = ImmutableRectangle.class)
    public static abstract class Rectangle implements Shape {
        public abstract double width();
        public abstract double height();

        public static Rectangle.Builder builder() {
            return new Rectangle.Builder();
        }

        public static final class Builder extends ImmutableRectangle.Builder {}
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableCircle.class)
    @JsonDeserialize(as = ImmutableCircle.class)
    public static interface Circle extends Shape {
        double radius();

        final class Builder extends ImmutableCircle.Builder {}
    }

}
