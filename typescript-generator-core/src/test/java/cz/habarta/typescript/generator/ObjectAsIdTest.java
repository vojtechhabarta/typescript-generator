
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import cz.habarta.typescript.generator.util.Utils;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;


public class ObjectAsIdTest {

    @Test
    public void testJackson() throws JsonProcessingException {
        final TestObjectA testObjectA = new TestObjectA();
        final TestObjectB testObjectB = new TestObjectB();
        final TestObjectC<String> testObjectC = new TestObjectC<>("valueC");
        final TestObjectD testObjectD = new TestObjectD();
        final TestObjectE testObjectE = new TestObjectE();
        final Wrapper wrapper = new Wrapper();
        wrapper.testObjectA1 = testObjectA;
        wrapper.testObjectA2 = testObjectA;
        wrapper.testObjectB1 = testObjectB;
        wrapper.testObjectB2 = testObjectB;
        wrapper.testObjectC1 = testObjectC;
        wrapper.testObjectC2 = testObjectC;
        wrapper.testObjectD1 = testObjectD;
        wrapper.testObjectD2 = testObjectD;
        wrapper.testObjectE1 = testObjectE;
        wrapper.testObjectE2 = testObjectE;
        wrapper.testObjectE3 = testObjectE;
        final ObjectMapper objectMapper = Utils.getObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final String json = objectMapper.writeValueAsString(wrapper);
        Assert.assertTrue(json.contains("\"testObjectA1\": \"id1\""));
        Assert.assertTrue(json.contains("\"testObjectA2\": \"id1\""));
        Assert.assertTrue(json.contains("\"testObjectB1\": {"));
        Assert.assertTrue(json.contains("\"testObjectB2\": \"id2\""));
        Assert.assertTrue(json.contains("\"testObjectC1\": {"));
        Assert.assertTrue(json.contains("\"testObjectC2\": \"id3\""));
        Assert.assertTrue(json.contains("\"testObjectD1\": \"id4\""));
        Assert.assertTrue(json.contains("\"testObjectD2\": \"id4\""));
        Assert.assertTrue(json.contains("\"testObjectE1\": \"id5\""));
        Assert.assertTrue(json.contains("\"testObjectE2\": {"));
        Assert.assertTrue(json.contains("\"testObjectE3\": {"));
    }

    @Test
    public void testJacksonLists() throws JsonProcessingException {
        final TestObjectA testObjectA = new TestObjectA();
        final TestObjectB testObjectB = new TestObjectB();
        final TestObjectC<String> testObjectC = new TestObjectC<>("valueC");
        final TestObjectD testObjectD = new TestObjectD();
        final TestObjectE testObjectE = new TestObjectE();
        final WrapperWithLists wrapper = new WrapperWithLists();
        wrapper.listOfTestObjectA = Arrays.asList(testObjectA, testObjectA);
        wrapper.listOfTestObjectB = Arrays.asList(testObjectB, testObjectB);
        wrapper.listOfTestObjectC = Arrays.asList(testObjectC, testObjectC);
        wrapper.listOfTestObjectD = Arrays.asList(testObjectD, testObjectD);
        wrapper.listOfTestObjectE = Arrays.asList(testObjectE, testObjectE);
        final ObjectMapper objectMapper = Utils.getObjectMapper();
        objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
        final String json = objectMapper.writeValueAsString(wrapper);
        Assert.assertTrue(json.contains("\"listOfTestObjectA\":[\"id1\""));
        Assert.assertTrue(json.contains("\"listOfTestObjectB\":[{"));
        Assert.assertTrue(json.contains("\"listOfTestObjectC\":[{"));
        Assert.assertTrue(json.contains("\"listOfTestObjectD\":[\"id4\""));
        Assert.assertTrue(json.contains("\"listOfTestObjectE\":[\"id5\""));
    }

    @Test
    public void testJacksonNestedMaps() throws JsonProcessingException {
        final TestObjectA testObjectA = new TestObjectA();
        final TestObjectB testObjectB = new TestObjectB();
        final TestObjectC<String> testObjectC = new TestObjectC<>("valueC");
        final TestObjectD testObjectD = new TestObjectD();
        final TestObjectE testObjectE = new TestObjectE();
        final WrapperWithNestedMaps wrapper = new WrapperWithNestedMaps();
        wrapper.listOfMapOfTestObjectA = Arrays.asList(generateMap(testObjectA, testObjectA));
        wrapper.listOfMapOfTestObjectB = Arrays.asList(generateMap(testObjectB, testObjectB));
        wrapper.listOfMapOfTestObjectC = Arrays.asList(generateMap(testObjectC, testObjectC));
        wrapper.listOfMapOfTestObjectD = Arrays.asList(generateMap(testObjectD, testObjectD));
        wrapper.listOfMapOfTestObjectE = Arrays.asList(generateMap(testObjectE, testObjectE));
        final ObjectMapper objectMapper = Utils.getObjectMapper();
        objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
        final String json = objectMapper.writeValueAsString(wrapper);
        Assert.assertTrue(json.contains("\"listOfMapOfTestObjectA\":[{\"k1\":\"id1\""));
        Assert.assertTrue(json.contains("\"listOfMapOfTestObjectB\":[{\"k1\":{"));
        Assert.assertTrue(json.contains("\"listOfMapOfTestObjectC\":[{\"k1\":{"));
        Assert.assertTrue(json.contains("\"listOfMapOfTestObjectD\":[{\"k1\":\"id4\""));
        Assert.assertTrue(json.contains("\"listOfMapOfTestObjectE\":[{\"k1\":\"id5\""));
    }

    @SafeVarargs
    private static <V> Map<String, V> generateMap(V... values) {
        final AtomicInteger index = new AtomicInteger();
        return Stream.of(values).collect(Collectors.toMap(
                v -> "k" + index.incrementAndGet(),
                v -> v,
                (v1, v2) -> { throw new RuntimeException(); },
                LinkedHashMap::new
        ));
    }

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Wrapper.class));
        Assert.assertTrue(output.contains("testObjectA1: string"));
        Assert.assertTrue(output.contains("testObjectB1: TestObjectB | string"));
        Assert.assertTrue(output.contains("testObjectC1: TestObjectC<string> | string"));
        Assert.assertTrue(output.contains("testObjectD1: string"));
        Assert.assertTrue(output.contains("testObjectE1: string"));
        Assert.assertTrue(output.contains("testObjectE2: TestObjectE | string"));
        Assert.assertTrue(output.contains("testObjectE3: TestObjectE"));
        Assert.assertTrue(!output.contains("interface TestObjectA"));
        Assert.assertTrue(output.contains("interface TestObjectB"));
        Assert.assertTrue(output.contains("interface TestObjectC<T>"));
        Assert.assertTrue(!output.contains("interface TestObjectD"));
        Assert.assertTrue(output.contains("interface TestObjectE"));
    }

    @Test
    public void testLists() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(WrapperWithLists.class));
        Assert.assertTrue(output.contains("listOfTestObjectA: string[]"));
        Assert.assertTrue(output.contains("listOfTestObjectB: (TestObjectB | string)[]"));
        Assert.assertTrue(output.contains("listOfTestObjectC: (TestObjectC<string> | string)[]"));
        Assert.assertTrue(output.contains("listOfTestObjectD: string[]"));
        Assert.assertTrue(output.contains("listOfTestObjectE: string[]"));
        Assert.assertTrue(!output.contains("interface TestObjectA"));
        Assert.assertTrue(output.contains("interface TestObjectB"));
        Assert.assertTrue(output.contains("interface TestObjectC<T>"));
        Assert.assertTrue(!output.contains("interface TestObjectD"));
        Assert.assertTrue(!output.contains("interface TestObjectE"));
    }

    @Test
    public void testNestedMaps() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(WrapperWithNestedMaps.class));
        Assert.assertTrue(output.contains("listOfMapOfTestObjectA: { [index: string]: string }[]"));
        Assert.assertTrue(output.contains("listOfMapOfTestObjectB: { [index: string]: TestObjectB | string }[]"));
        Assert.assertTrue(output.contains("listOfMapOfTestObjectC: { [index: string]: TestObjectC<string> | string }[]"));
        Assert.assertTrue(output.contains("listOfMapOfTestObjectD: { [index: string]: string }[]"));
        Assert.assertTrue(output.contains("listOfMapOfTestObjectE: { [index: string]: string }[]"));
        Assert.assertTrue(!output.contains("interface TestObjectA"));
        Assert.assertTrue(output.contains("interface TestObjectB"));
        Assert.assertTrue(output.contains("interface TestObjectC<T>"));
        Assert.assertTrue(!output.contains("interface TestObjectD"));
        Assert.assertTrue(!output.contains("interface TestObjectE"));
    }


    @Test
    public void testGenerics() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(TestObjectWithGeneric.class));
        Assert.assertTrue(output.contains("objectReference: string"));
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
        public String myIdentification = "id3";

        public T myProperty;

        public TestObjectC(T myProperty) {
            this.myProperty = myProperty;
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "@@@id")
    private static class TestObjectD {

        @JsonProperty("@@@id")
        public String myIdentification = "id4";

        public String myProperty = "valueD";
    }


    private static class ObjectWithID {
        @JsonProperty("@@@id")
        public String myIdentification;

    }

    private static class TestObjectWithID extends ObjectWithID {
        public String myProperty = "valueE";

        public TestObjectWithID(String myIdentification) {
            this.myIdentification = myIdentification;
        }
    }


    private static class GenericWrapper<M extends ObjectWithID> {
        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "@@@id") @JsonIdentityReference(alwaysAsId = true)
        public M objectReference;


        public GenericWrapper(M objectReference) {
            this.objectReference = objectReference;
        }
    }


    private static class TestObjectWithGeneric {
        public GenericWrapper<ObjectWithID> genericTestObject;

        public TestObjectWithGeneric(GenericWrapper<ObjectWithID> genericTestObject) {
            this.genericTestObject = genericTestObject;
        }
    }




    private static class TestObjectE {

        @JsonProperty("@@@id")
        public String myIdentification = "id5";

        public String myProperty = "valueE";
    }

    private static class Wrapper {
        public TestObjectA testObjectA1;
        public TestObjectA testObjectA2;
        public TestObjectB testObjectB1;
        public TestObjectB testObjectB2;
        public TestObjectC<String> testObjectC1;
        public TestObjectC<String> testObjectC2;
        public @JsonIdentityReference(alwaysAsId = true) TestObjectD testObjectD1;
        public @JsonIdentityReference(alwaysAsId = true) TestObjectD testObjectD2;
        public @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "@@@id") @JsonIdentityReference(alwaysAsId = true) TestObjectE testObjectE1;
        public @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "@@@id") TestObjectE testObjectE2;
        public TestObjectE testObjectE3;
    }

    private static class WrapperWithLists {
        public List<TestObjectA> listOfTestObjectA;
        public List<TestObjectB> listOfTestObjectB;
        public List<TestObjectC<String>> listOfTestObjectC;
        public @JsonIdentityReference(alwaysAsId = true) List<TestObjectD> listOfTestObjectD;
        public @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "@@@id") @JsonIdentityReference(alwaysAsId = true) List<TestObjectE> listOfTestObjectE;
    }

    private static class WrapperWithNestedMaps {
        public List<Map<String, TestObjectA>> listOfMapOfTestObjectA;
        public List<Map<String, TestObjectB>> listOfMapOfTestObjectB;
        public List<Map<String, TestObjectC<String>>> listOfMapOfTestObjectC;
        public @JsonIdentityReference(alwaysAsId = true) List<Map<String, TestObjectD>> listOfMapOfTestObjectD;
        public @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "@@@id") @JsonIdentityReference(alwaysAsId = true) List<Map<String, TestObjectE>> listOfMapOfTestObjectE;
    }

}
