
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;


public class JsonViewTest {

    public static void main(String[] args) throws Exception {
        final SomeClass parent1 = new SomeClass();
        parent1.id = 22;
        final SomeClass some1 = new SomeClass();
        some1.id = 13;
        some1.parentID = 22;
        some1.parent = parent1;
        some1.someProperty = "f";
        some1.anotherProperty = 10L;

        final ObjectMapper objectMapper1 = new ObjectMapper();
        objectMapper1.setConfig(objectMapper1.getSerializationConfig().withView(Views.REST.class));
        final String json1 = objectMapper1.writeValueAsString(some1);
        System.out.println(json1);

        final ObjectMapper objectMapper2 = new ObjectMapper();
        objectMapper2.setConfig(objectMapper2.getDeserializationConfig().withView(Views.REST.class));
        final String json2 = "{\"id\":13,\"parentID\":22,\"parent\":{\"id\":22,\"parentID\":null,\"parent\":null,\"someProperty\":null,\"anotherProperty\":null},\"someProperty\":\"f\",\"anotherProperty\":10}";
        final SomeClass some2 = objectMapper2.readValue(json2, SomeClass.class);
        System.out.println(some2.anotherProperty);
    }

    @Test
    public void test1() {
        final Settings settings = TestUtils.settings();
        settings.jackson2Configuration = new Jackson2ConfigurationResolved();
        settings.jackson2Configuration.view = Views.REST.class;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(SomeClass.class));
        assertOutput(output);
    }

    @Test
    public void test2() {
        final Settings settings = TestUtils.settings();
        settings.jackson2Configuration = new Jackson2ConfigurationResolved();
        settings.jackson2Configuration.view = Views.REST.class;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(SomeClassGetters.class));
        assertOutput(output);
    }

    private static void assertOutput(String output) {
        Assert.assertTrue(output.contains("id:"));
        Assert.assertTrue(output.contains("parentID:"));
        Assert.assertTrue(!output.contains("parent:"));
        Assert.assertTrue(output.contains("someProperty:"));
        Assert.assertTrue(!output.contains("anotherProperty:"));
    }

    public static class Views {

        public static class BaseConfig {
        }

        public static class REST {
        }

        public static class Exclude {
        }

    }

    public static class SomeClass {

        @JsonView({Views.BaseConfig.class, Views.REST.class})
        public Integer id;

        @JsonView({Views.BaseConfig.class, Views.REST.class})
        public Integer parentID;

        @JsonView(Views.Exclude.class)
        public SomeClass parent;

        @JsonView(Views.REST.class)
        public String someProperty;

        @JsonView(Views.BaseConfig.class)
        public Long anotherProperty;

    }

    public static class SomeClassGetters {

        private Integer id;
        private Integer parentID;
        private SomeClass parent;
        private String someProperty;
        private Long anotherProperty;

        @JsonView({Views.BaseConfig.class, Views.REST.class})
        public Integer getId() {
            return id;
        }

        @JsonView({Views.BaseConfig.class, Views.REST.class})
        public Integer getParentID() {
            return parentID;
        }

        @JsonView(Views.Exclude.class)
        public SomeClass getParent() {
            return parent;
        }

        @JsonView(Views.REST.class)
        public String getSomeProperty() {
            return someProperty;
        }

        @JsonView(Views.BaseConfig.class)
        public Long getAnotherProperty() {
            return anotherProperty;
        }

    }

}
