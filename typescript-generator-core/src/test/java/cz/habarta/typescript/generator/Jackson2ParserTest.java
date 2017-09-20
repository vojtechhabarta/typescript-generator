
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    static Jackson2Parser getJackson2Parser() {
        final Settings settings = new Settings();
        return new Jackson2Parser(settings, new DefaultTypeProcessor());
    }

    public static class DummyBeanJackson2 {

        @JsonProperty("changedNameProperty")
        public String _changed_name_property;

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

}
