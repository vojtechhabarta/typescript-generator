
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.*;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.habarta.typescript.generator.parser.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.xml.bind.annotation.XmlElement;
import org.junit.Assert;
import org.junit.Test;


public class Jackson2ParserTest {

    @Test
    public void test() {
        final Jackson2Parser jacksonParser = getJackson2Parser();
        final Class<?> bean = DummyBean.class;
        final Model model = jacksonParser.parseModel(bean);
        Assert.assertTrue(model.getBeans().size() > 0);
        final BeanModel beanModel = model.getBeans().get(0);
        Assert.assertEquals("DummyBean", beanModel.getOrigin().getSimpleName());
        Assert.assertTrue(beanModel.getProperties().size() > 0);
        Assert.assertEquals("firstProperty", beanModel.getProperties().get(0).getName());
    }

    @Test
    public void testChangedNameProperty() {
        final Jackson2Parser jacksonParser = getJackson2Parser();
        final Model model = jacksonParser.parseModel(DummyBeanJackson2.class);
        Assert.assertTrue(model.getBeans().size() > 0);
        final BeanModel beanModel = model.getBeans().get(0);
        Assert.assertEquals("DummyBeanJackson2", beanModel.getOrigin().getSimpleName());
        Assert.assertTrue(beanModel.getProperties().size() > 0);
        Assert.assertEquals("changedNameProperty", beanModel.getProperties().get(0).getName());
    }

    @Test
    public void testConflictingJsonTypeInfoProperty() {
        final Jackson2Parser jacksonParser = getJackson2Parser();
        final Model model = jacksonParser.parseModel(InheritedClass.class);
        Assert.assertTrue(model.getBeans().size() > 0);
        final BeanModel beanModel = model.getBeans().get(0);
        Assert.assertEquals(1, beanModel.getProperties().size());
    }

    @Test
    public void testTaggedUnion() {
        final Jackson2Parser jacksonParser = getJackson2Parser();
        final Model model = jacksonParser.parseModel(SubTypeDiscriminatedByName1.class);
        Assert.assertEquals(5, model.getBeans().size());
        final BeanModel bean0 = model.getBean(ParentWithNameDiscriminant.class);
        final BeanModel bean1 = model.getBean(SubTypeDiscriminatedByName1.class);
        final BeanModel bean2 = model.getBean(SubTypeDiscriminatedByName2.class);
        final BeanModel bean3 = model.getBean(SubTypeDiscriminatedByName3.class);
        final BeanModel bean4 = model.getBean(SubTypeDiscriminatedByName4.class);
        Assert.assertEquals(4, bean0.getTaggedUnionClasses().size());
        Assert.assertNull(bean1.getTaggedUnionClasses());
        Assert.assertNull(bean2.getTaggedUnionClasses());
        Assert.assertNull(bean3.getTaggedUnionClasses());
        Assert.assertEquals("kind", bean0.getDiscriminantProperty());
        Assert.assertEquals("explicit-name1", bean1.getDiscriminantLiteral());
        Assert.assertEquals("SubType2", bean2.getDiscriminantLiteral());
        Assert.assertEquals("Jackson2ParserTest$SubTypeDiscriminatedByName3", bean3.getDiscriminantLiteral());
        Assert.assertEquals("Jackson2ParserTest$SubTypeDiscriminatedByName4", bean4.getDiscriminantLiteral());
    }

    @Test
    public void testMethodsInInterface() {
        final Jackson2Parser jacksonParser = getJackson2Parser(settings -> settings.emitMethodsInBeans = true);
        final Model model = jacksonParser.parseModel(InterfaceWithMethodsAndBeanProperties.class);
        assertMethodsInInterface(model);

        // Now let's test w/ the setting disabled
        final Jackson2Parser parserNoMethods = getJackson2Parser(settings -> settings.emitMethodsInBeans = false);
        final Model modelNoMethods = parserNoMethods.parseModel(InterfaceWithMethodsAndBeanProperties.class);

        Assert.assertEquals(0, modelNoMethods.getBeans().get(0).getMethods().size());
    }

    static void assertMethodsInInterface(Model model) {
        Assert.assertEquals(1, model.getBeans().size());
        BeanModel beanModel = model.getBeans().get(0);

        // Expect "enabled" and "name"
        Assert.assertEquals(2, beanModel.getProperties().size());
        PropertyModel propertyModel = beanModel.getProperties().get(1);
        Assert.assertEquals("enabled", propertyModel.getName());
        propertyModel = beanModel.getProperties().get(0);
        Assert.assertEquals("name", propertyModel.getName());

        Assert.assertEquals(1, beanModel.getMethods().size());
        MethodModel methodModel = beanModel.getMethods().get(0);
        Assert.assertEquals("callMeMaybe", methodModel.getName());
        Assert.assertEquals(2, methodModel.getParameters().size());
        Assert.assertEquals("java.util.List<java.lang.String>", methodModel.getParameters().get(0).getType().getTypeName());
        Assert.assertEquals("arg0", methodModel.getParameters().get(0).getName());
        Assert.assertEquals("arg1", methodModel.getParameters().get(1).getName());
    }

    static Jackson2Parser getJackson2Parser() {
        return getJackson2Parser(settings -> {});
    }

    static Jackson2Parser getJackson2Parser(Consumer<Settings> settingsModifier) {
        final Settings settings = new Settings();
        settingsModifier.accept(settings);
        return new Jackson2Parser(settings, new DefaultTypeProcessor());
    }

    public static class DummyBeanJackson2 {

        @JsonProperty("changedNameProperty")
        public String _changed_name_property;

    }

    public interface InterfaceWithMethodsAndBeanProperties {
        boolean isEnabled();

        String getName();
        void setName(String name);

        DummyBeanJackson2 callMeMaybe(List<String> sources, Map<String, Object> metadata);
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    public class InheritedClass {
        public String type;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = SubTypeDiscriminatedByName1.class, name = "SubType1"), // value from @JsonTypeName is used
        @JsonSubTypes.Type(value = SubTypeDiscriminatedByName2.class, name = "SubType2"),
        @JsonSubTypes.Type(value = SubTypeDiscriminatedByName3.class),
        @JsonSubTypes.Type(value = SubTypeDiscriminatedByName4.class),
    })
    private static interface ParentWithNameDiscriminant {
    }

    @JsonTypeName("explicit-name1")
    private static class SubTypeDiscriminatedByName1 implements ParentWithNameDiscriminant {
    }
    private static class SubTypeDiscriminatedByName2 implements ParentWithNameDiscriminant {
    }
    @JsonTypeName(/* Default should be the simplename of the class */)
    private static class SubTypeDiscriminatedByName3 implements ParentWithNameDiscriminant {
    }
    private static class SubTypeDiscriminatedByName4 implements ParentWithNameDiscriminant {
    }

    public static void main(String[] args) throws JsonProcessingException {
        System.out.println(new ObjectMapper().writeValueAsString(new SubTypeDiscriminatedByName1()));
        System.out.println(new ObjectMapper().writeValueAsString(new SubTypeDiscriminatedByName2()));
        System.out.println(new ObjectMapper().writeValueAsString(new SubTypeDiscriminatedByName3()));
        System.out.println(new ObjectMapper().writeValueAsString(new SubTypeDiscriminatedByName4()));
    }

    @Test
    public void testOptionalJsonProperty() {
        final Settings settings = TestUtils.settings();
        settings.optionalProperties = OptionalProperties.useLibraryDefinition;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithOptionals.class));
        Assert.assertTrue(output.contains("oname1?: string"));
        Assert.assertTrue(output.contains("oname2?: string"));
        Assert.assertTrue(output.contains("jname1?: string"));
        Assert.assertTrue(output.contains("jname2?: string"));
        Assert.assertTrue(output.contains("jname3: string"));
        Assert.assertTrue(output.contains("jname4: string"));
        Assert.assertTrue(output.contains("xname1?: string"));
        Assert.assertTrue(output.contains("xname2?: string"));
        Assert.assertTrue(output.contains("xname3?: string"));
        Assert.assertTrue(output.contains("xname4?: string"));
    }

    @Test
    public void testOptionalXmlElement() {
        final Settings settings = TestUtils.settings();
        settings.jsonLibrary = JsonLibrary.jaxb;
        settings.optionalProperties = OptionalProperties.useLibraryDefinition;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithOptionals.class));
        Assert.assertTrue(output.contains("oname1?: string"));
        Assert.assertTrue(output.contains("oname2?: string"));
        Assert.assertTrue(output.contains("jname1?: string"));
        Assert.assertTrue(output.contains("jname2?: string"));
        Assert.assertTrue(output.contains("jname3?: string"));
        Assert.assertTrue(output.contains("jname4?: string"));
        Assert.assertTrue(output.contains("xname1?: string"));
        Assert.assertTrue(output.contains("xname2?: string"));
        Assert.assertTrue(output.contains("xname3: string"));
        Assert.assertTrue(output.contains("xname4: string"));
    }

    public static class ClassWithOptionals {
        public String oname1;
        public Optional<String> oname2;

        @JsonProperty
        public String jname1;
        @JsonProperty(required = false)
        public String jname2;
        @JsonProperty(required = true)
        public String jname3;
        private String jname4;
        @JsonProperty(required = true)
        public String getJname4() {
            return jname4;
        }

        @XmlElement
        public String xname1;
        @XmlElement(required = false)
        public String xname2;
        @XmlElement(required = true)
        public String xname3;
        private String xname4;
        @XmlElement(required = true)
        public String getXname4() {
            return xname4;
        }
    }

    @Test
    public void testStandardEnumValue() {
        testEnumByType(TestEnums.StandardEnum.class, "A", "B", "C");
    }

    @Test
    public void testStringPropertyEnumValue() {
        testEnumByType(TestEnums.StringPropertyValuedEnum.class, "_A", "_B", "_C");
    }

    @Test
    public void testNumberPropertyEnumValue() {
        testEnumByType(TestEnums.NumberPropertyValuedEnum.class, 0, 1, 2);
    }

    @Test
    public void testJsonNumberFieldValuedEnum() {
        testEnumByType(TestEnums.NumberFieldValuedEnum.class, 1, 2, 3);
    }

    @Test
    public void testJsonNumberMethodValuedEnum() {
        testEnumByType(TestEnums.NumberMethodValuedEnum.class, 1, 2, 3);
    }

    @Test
    public void testMethodEnumValue() {
        testEnumByType(TestEnums.GeneralMethodValuedEnum.class, "_A", "_B", "_C");
    }

    @Test
    public void testToStringEnumValue() {
        testEnumByType(TestEnums.ToStringValuedEnum.class, "_A", "_B", "_C");
    }

    @Test
    public void testJsonPropertyEnumValue() {
        testEnumByType(TestEnums.JsonPropertyValuedEnum.class, "_A", "_B", "_C");
    }

    private void testEnumByType(Class<? extends Enum<?>> type, Object... expectedValues) {
        final Jackson2Parser jacksonParser = getJackson2Parser();
        final Model model = jacksonParser.parseModel(type);
        Assert.assertEquals(1, model.getEnums().size());
        final EnumModel enumModel = model.getEnums().get(0);
        Assert.assertEquals(expectedValues.length, enumModel.getMembers().size());
        for (int i = 0; i < expectedValues.length; i++) {
            Assert.assertEquals(expectedValues[i], enumModel.getMembers().get(i).getEnumValue());
        }
    }

    @Test
    public void testIgnoredProperty() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithIgnoredProperty.class));
        Assert.assertTrue(output.contains("name1: string"));
        Assert.assertTrue(!output.contains("name2: string"));
    }

    private static class ClassWithIgnoredProperty {
        public String name1;
        @JsonIgnore
        public String name2;
    }

//    public static void main(String[] args) throws JsonProcessingException {
//        final ObjectMapper objectMapper = new ObjectMapper();
//        final ClassWithIgnoredProperty instance = new ClassWithIgnoredProperty();
//        instance.name1 = "xxx";
//        instance.name2 = "xxx";
//        System.out.println(objectMapper.writeValueAsString(instance));
//    }

    @Test
    public void testVisibilityConfiguration() {
        {
            final Settings settings = TestUtils.settings();
            final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithDifferentMemberVisibilities.class));
            Assert.assertTrue(!output.contains("property1: string"));
            Assert.assertTrue(output.contains("property2: string"));
        }
        {
            final Settings settings = TestUtils.settings();
            settings.jackson2Configuration = new Jackson2ConfigurationResolved();
            settings.jackson2Configuration.setVisibility(ANY, NONE, NONE, NONE, NONE);
            final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithDifferentMemberVisibilities.class));
            Assert.assertTrue(output.contains("property1: string"));
            Assert.assertTrue(!output.contains("property2: string"));
        }
    }

    private static class ClassWithDifferentMemberVisibilities {
        private String property1;
        public String getProperty2() {
            return null;
        }
    }

    @Test
    public void testJsonNode() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithJsonNode.class));
        Assert.assertTrue(output.contains("node: any"));
        Assert.assertTrue(output.contains("nodes: any[]"));
    }

    private static class ClassWithJsonNode {
        public JsonNode node;
        public List<JsonNode> nodes;
    }

}
