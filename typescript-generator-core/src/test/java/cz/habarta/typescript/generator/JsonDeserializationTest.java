
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import cz.habarta.typescript.generator.util.Utils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import org.junit.Test;


public class JsonDeserializationTest {

    @Test
    public void test() throws IOException {
        final Settings settings = TestUtils.settings();
        settings.outputKind = TypeScriptOutputKind.module;
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asClasses;
        settings.experimentalJsonDeserialization = true;
        final File actualFile = new File("target/JsonDeserializationTest-actual.ts");
        new TypeScriptGenerator(settings).generateTypeScript(Input.from(User.class), Output.to(actualFile));
        final List<String> actualLines = Files.readAllLines(actualFile.toPath());
        final List<String> expectedLines = Utils.readLines(getClass().getResourceAsStream("JsonDeserializationTest-expected.ts"));

        int contentLines = 0;
        int foundLines = 0;
        final List<String> notFoundLines = new ArrayList<>();
        for (String expectedLine : expectedLines) {
            if (!expectedLine.isEmpty() || !expectedLine.trim().equals("}")) {
                contentLines++;
                if (actualLines.contains(expectedLine)) {
                    foundLines++;
                } else {
                    notFoundLines.add(expectedLine);
                }
            }
        }
        System.out.println(String.format("Number of correctly generated content lines: %d/%d (%d%%).", foundLines, contentLines, 100 * foundLines / contentLines));
        System.out.println("Following lines were not generated:");
        for (String notFoundLine : notFoundLines) {
            System.out.println(notFoundLine);
        }
    }

    private static class User {
        public String name;
        public Authentication authentication;
        public boolean childAccount;
        public int age;
        public Address address;
        public List<Address> addresses;
        public Map<String, Address> taggedAddresses;
        public Map<String, List<Address>> groupedAddresses;
        public List<Map<String, Address>> listOfTaggedAddresses;
        public PagedList<Order, Authentication> orders;
        public List<PagedList<Order, Authentication>> allOrders;
        public Shape shape;
        public List<Shape> shapes;
    }

    public enum Authentication {
        Password, Token, Fingerprint, Voice
    }

    public static class Address {
        public String street;
        public String city;
    }

    public static class PagedList<T, A> {
        public int page;
        public List<T> items;
        public A additionalInfo;
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
