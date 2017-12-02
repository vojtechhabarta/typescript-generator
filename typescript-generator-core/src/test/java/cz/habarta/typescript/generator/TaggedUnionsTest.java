
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes({
        @JsonSubTypes.Type(IRectangle3.class),
        @JsonSubTypes.Type(ICircle3.class),
    })
    interface IShape3 {
    }
    
    interface IQuadrilateral3 extends IShape3 {
    }
    
    interface INamedShape3 extends IShape3 {
        String getName();
    }
    
    interface INamedQuadrilateral3 extends INamedShape3, IQuadrilateral3 {
    }
    
    @JsonTypeName("rectangle")
    interface IRectangle3 extends INamedQuadrilateral3 {
    }
    
    @JsonTypeName("circle")
    interface ICircle3 extends INamedShape3 {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes({
        @JsonSubTypes.Type(DiamondB1.class),
        @JsonSubTypes.Type(DiamondB2.class),
        @JsonSubTypes.Type(DiamondC.class),
    })
    private static interface DiamondA {
        public String getA();
    }

    @JsonTypeName("b1")
    private static interface DiamondB1 extends DiamondA {
        public String getB1();
    }

    @JsonTypeName("b2")
    private static interface DiamondB2 extends DiamondA {
        public String getB2();
    }

    @JsonTypeName("c")
    private static interface DiamondC extends DiamondB1, DiamondB2 {
        public String getC();
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
    @JsonSubTypes({
        @JsonSubTypes.Type(DieselCar.class),
        @JsonSubTypes.Type(ElectricCar.class),
    })
    private static class Car {
        public String name;
    }

    private static class DieselCar extends Car {
        public double fuelTankCapacityInLiters;
    }

    private static class ElectricCar extends Car {
        public double batteryCapacityInKWh;
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
                "    kind: 'square' | 'rectangle';\n" +
                "}\n" +
                "\n" +
                "type IShape2Union = CSquare2 | CRectangle2 | CCircle2;\n" +
                ""
                ).replace('\'', '"');
        Assert.assertEquals(expected, output);
    }

    @Test
    public void testTaggedUnionsWithOverlappingInterfaces() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(IShape3.class));
        final String expected = (
                "\n" +
                "interface IShape3 {\n" +
                "    kind: 'circle' | 'rectangle';\n" +
                "}\n" +
                "\n" +
                "interface IRectangle3 extends INamedQuadrilateral3 {\n" +
                "    kind: 'rectangle';\n" +
                "}\n" +
                "\n" +
                "interface ICircle3 extends INamedShape3 {\n" +
                "    kind: 'circle';\n" +
                "}\n" +
                "\n" +
                "interface INamedQuadrilateral3 extends INamedShape3, IQuadrilateral3 {\n" +
                "    kind: 'rectangle';\n" +
                "}\n" +
                "\n" +
                "interface INamedShape3 extends IShape3 {\n" +
                "    kind: 'circle' | 'rectangle';\n" +
                "    name: string;\n" +
                "}\n" +
                "\n" +
                "interface IQuadrilateral3 extends IShape3 {\n" +
                "    kind: 'rectangle';\n" +
                "}\n" +
                "\n" +
                "type IShape3Union = IRectangle3 | ICircle3;\n"
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

    @Test
    public void testTaggedUnionsWithDiamond() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(DiamondA.class));
        final String expected = (
                "\n" +
                "interface DiamondA {\n" +
                "    kind: 'b1' | 'c' | 'b2';\n" +
                "    a: string;\n" +
                "}\n" +
                "\n" +
                "interface DiamondB1 extends DiamondA {\n" +
                "    kind: 'b1' | 'c';\n" +
                "    b1: string;\n" +
                "}\n" +
                "\n" +
                "interface DiamondB2 extends DiamondA {\n" +
                "    kind: 'b2' | 'c';\n" +
                "    b2: string;\n" +
                "}\n" +
                "\n" +
                "interface DiamondC extends DiamondB1, DiamondB2 {\n" +
                "    kind: 'c';\n" +
                "    c: string;\n" +
                "}\n" +
                "\n" +
                "type DiamondAUnion = DiamondB1 | DiamondB2 | DiamondC;\n" +
                ""
                ).replace('\'', '"');
        Assert.assertEquals(expected, output);
    }

    @Test
    public void testIdClass() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Car.class));
        final String expected = (
                "\n" +
                "interface Car {\n" +
                "    '@class': 'cz.habarta.typescript.generator.TaggedUnionsTest$DieselCar' | 'cz.habarta.typescript.generator.TaggedUnionsTest$ElectricCar';\n" +
                "    name: string;\n" +
                "}\n" +
                "\n" +
                "interface DieselCar extends Car {\n" +
                "    '@class': 'cz.habarta.typescript.generator.TaggedUnionsTest$DieselCar';\n" +
                "    fuelTankCapacityInLiters: number;\n" +
                "}\n" +
                "\n" +
                "interface ElectricCar extends Car {\n" +
                "    '@class': 'cz.habarta.typescript.generator.TaggedUnionsTest$ElectricCar';\n" +
                "    batteryCapacityInKWh: number;\n" +
                "}\n" +
                "\n" +
                "type CarUnion = DieselCar | ElectricCar;\n" +
                ""
                ).replace('\'', '"');
        Assert.assertEquals(expected, output);
    }

    public static void main(String[] args) throws Exception {
        final ElectricCar electricCar = new ElectricCar();
        electricCar.name = "Tesla";
        electricCar.batteryCapacityInKWh = 75;  // kWh
        System.out.println(new ObjectMapper().writeValueAsString(electricCar));
    }

}
