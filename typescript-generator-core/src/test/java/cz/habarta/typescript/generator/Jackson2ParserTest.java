
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.*;
import com.fasterxml.jackson.annotation.*;
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
        Assert.assertEquals("DummyBean", beanModel.getBeanClass().getSimpleName());
        Assert.assertTrue(beanModel.getProperties().size() > 0);
        Assert.assertEquals("firstProperty", beanModel.getProperties().get(0).getName());
    }

    @Test
    public void testChangedNameProperty() {
        final Jackson2Parser jacksonParser = getJackson2Parser();
        final Model model = jacksonParser.parseModel(DummyBeanJackson2.class);
        Assert.assertTrue(model.getBeans().size() > 0);
        final BeanModel beanModel = model.getBeans().get(0);
        Assert.assertEquals("DummyBeanJackson2", beanModel.getBeanClass().getSimpleName());
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

}
