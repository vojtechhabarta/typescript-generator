
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import cz.habarta.typescript.generator.util.StandardJsonPrettyPrinter;
import org.junit.Assert;
import org.junit.Test;


public class JsonUnwrappedTest {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Person.class));
        final String expected = "\n"
                + "interface Person {\n"
                + "    AageA: number;\n"
                + "    AfirstA: string;\n"
                + "    AlastA: string;\n"
                + "    A_first2A: string;\n"
                + "    A_last2A: string;\n"
                + "    Aname3A: Name;\n"
                + "    BageB: number;\n"
                + "    BfirstB: string;\n"
                + "    BlastB: string;\n"
                + "    B_first2B: string;\n"
                + "    B_last2B: string;\n"
                + "    Bname3B: Name;\n"
                + "}\n"
                + "\n"
                + "interface Parent {\n"
                + "    age: number;\n"
                + "    first: string;\n"
                + "    last: string;\n"
                + "    _first2: string;\n"
                + "    _last2: string;\n"
                + "    name3: Name;\n"
                + "}\n"
                + "\n"
                + "interface Name {\n"
                + "    first: string;\n"
                + "    last: string;\n"
                + "}\n"
                + "";
        Assert.assertEquals(expected.trim(), output.trim());
    }

    @Test
    public void testPrivateField() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Person2.class));
        final String expected = "\n"
                + "interface Person2 {\n"
                + "    first: string;\n"
                + "    last: string;\n"
                + "}\n"
                + "\n"
                + "interface Name {\n"
                + "    first: string;\n"
                + "    last: string;\n"
                + "}\n"
                + "";
        Assert.assertEquals(expected.trim(), output.trim());
    }

    public static class Person {
        @JsonUnwrapped(prefix = "A", suffix = "A")
        public Parent parentA;
        @JsonUnwrapped(prefix = "B", suffix = "B")
        public Parent parentB;
    }

    public static class Parent {
        public int age;
        @JsonUnwrapped
        public Name name;
        @JsonUnwrapped(prefix = "_", suffix = "2")
        public Name name2;
        @JsonUnwrapped(enabled = false)
        public Name name3;
    }

    public static class Name {
        public String first, last;
    }

    public static class Person2 {
        @JsonUnwrapped
        private Name name;

        public Name getName() {
            return name;
        }
    }

    public static void main(String[] args) throws Exception {
        final Parent parent = new Parent();
        parent.age = 18;
        parent.name = new Name();
        parent.name.first = "Joey";
        parent.name.last = "Sixpack";
        parent.name2 = new Name();
        parent.name2.first = "Joey";
        parent.name2.last = "Sixpack";
        parent.name3 = new Name();
        parent.name3.first = "Joey";
        parent.name3.last = "Sixpack";
        final Person person = new Person();
        person.parentA = parent;
        person.parentB = parent;
        final Person2 person2 = new Person2();
        person2.name = parent.name;
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.setDefaultPrettyPrinter(new StandardJsonPrettyPrinter());
        System.out.println(objectMapper.writeValueAsString(person));
        System.out.println(objectMapper.writeValueAsString(person2));
    }

}
