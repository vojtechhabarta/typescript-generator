
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import cz.habarta.typescript.generator.util.Utils;
import org.junit.Assert;
import org.junit.Test;


public class ObjectAsIdTest {

    @Test
    public void testJackson() throws JsonProcessingException {
        final TestObjectA testObjectA = new TestObjectA();
        final TestObjectB testObjectB = new TestObjectB();
        final TestObjectC<String> testObjectC = new TestObjectC<>("valueC");
        final Wrapper wrapper = new Wrapper();
        wrapper.testObjectA1 = testObjectA;
        wrapper.testObjectA2 = testObjectA;
        wrapper.testObjectB1 = testObjectB;
        wrapper.testObjectB2 = testObjectB;
        wrapper.testObjectC1 = testObjectC;
        wrapper.testObjectC2 = testObjectC;
        final ObjectMapper objectMapper = Utils.getObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final String json = objectMapper.writeValueAsString(wrapper);
        Assert.assertTrue(json.contains("\"testObjectA1\": \"id1\""));
        Assert.assertTrue(json.contains("\"testObjectA2\": \"id1\""));
        Assert.assertTrue(json.contains("\"testObjectB1\": {"));
        Assert.assertTrue(json.contains("\"testObjectB2\": \"id2\""));
        Assert.assertTrue(json.contains("\"testObjectC1\": {"));
        Assert.assertTrue(json.contains("\"testObjectC2\": \"id2\""));
    }

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Wrapper.class));
        Assert.assertTrue(output.contains("testObjectA1: string"));
        Assert.assertTrue(output.contains("testObjectB1: TestObjectB | string"));
        Assert.assertTrue(output.contains("testObjectC1: TestObjectC<string> | string"));
        Assert.assertTrue(!output.contains("interface TestObjectA"));
        Assert.assertTrue(output.contains("interface TestObjectB"));
        Assert.assertTrue(output.contains("interface TestObjectC<T>"));
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "@@@id")
    @JsonIdentityReference(alwaysAsId = true)
    private static class TestObjectA {

        @JsonProperty("@@@id")
        public String myIdentification = "id1";

        public String myProperty = "valueA";
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "@@@id")
    private static class TestObjectB {

        @JsonProperty("@@@id")
        public String myIdentification = "id2";

        public String myProperty = "valueB";
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "@@@id")
    private static class TestObjectC<T> {

        @JsonProperty("@@@id")
        public String myIdentification = "id2";

        public T myProperty;

        public TestObjectC(T myProperty) {
            this.myProperty = myProperty;
        }
    }

    private static class Wrapper {
        public TestObjectA testObjectA1;
        public TestObjectA testObjectA2;
        public TestObjectB testObjectB1;
        public TestObjectB testObjectB2;
        public TestObjectC<String> testObjectC1;
        public TestObjectC<String> testObjectC2;
    }

}
