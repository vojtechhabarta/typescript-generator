
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import cz.habarta.typescript.generator.util.Utils;
import java.io.File;
import java.util.*;
import org.junit.Assert;
import org.junit.Test;


public class CopyConstructorsTest {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        settings.outputKind = TypeScriptOutputKind.module;
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asClasses;
        new TypeScriptGenerator(settings).generateTypeScript(Input.from(User.class), Output.to(new File("target/CopyConstructorsTest-actual.ts")));
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(User.class));
        final String expected = Utils.readString(getClass().getResourceAsStream("CopyConstructorsTest-expected.ts"));
        Assert.assertEquals(expected.trim(), output.trim());
    }

    private static class User {
        public String name;
        public boolean childAccount;
        public int age;
        public Address address;
        public List<Address> addresses;
        public Map<String, Address> taggedAddresses;
        public Map<String, List<Address>> groupedAddresses;
        public List<Map<String, Address>> listOfTaggedAddresses;
        public PagedList<Order> orders;
        public List<PagedList<Order>> allOrders;
        public Shape shape;
        public List<Shape> shapes;
    }

    public static class Address {
        public String street;
        public String city;
    }

    public static class PagedList<T> {
        public int page;
        public List<T> items;
    }

    public static class Order {
        public String id;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes({
        @JsonSubTypes.Type(Square.class),
        @JsonSubTypes.Type(Rectangle.class),
        @JsonSubTypes.Type(Circle.class),
    })
    private static class Shape {
        public ShapeMetadata metadata;
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

    private static class ShapeMetadata {
        public String group;
    }
}
