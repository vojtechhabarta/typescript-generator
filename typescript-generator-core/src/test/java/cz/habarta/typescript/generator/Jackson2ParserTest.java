
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.logging.Logger;
import org.junit.Assert;
import org.junit.Test;


public class Jackson2ParserTest {

    @Test
    public void test() {
        final Jackson2Parser jacksonParser = new Jackson2Parser(Logger.getGlobal(), new Settings());
        final Class<?> bean = DummyBean.class;
        final Model model = jacksonParser.parseModel(bean);
        Assert.assertTrue(model.getBeans().size() > 0);
        final BeanModel beanModel = model.getBeans().get(0);
        System.out.println("beanModel: " + beanModel);
        Assert.assertEquals("DummyBean", beanModel.getName());
        Assert.assertTrue(beanModel.getProperties().size() > 0);
        Assert.assertEquals("firstProperty", beanModel.getProperties().get(0).getName());
    }

    @Test
    public void testChangedNameProperty() {
        final Jackson2Parser jacksonParser = new Jackson2Parser(Logger.getGlobal(), new Settings());
        final Class<?> bean = DummyBeanJackson2.class;
        final Model model = jacksonParser.parseModel(bean);
        Assert.assertTrue(model.getBeans().size() > 0);
        final BeanModel beanModel = model.getBeans().get(0);
        System.out.println("beanModel: " + beanModel);
        Assert.assertEquals("DummyBeanJackson2", beanModel.getName());
        Assert.assertTrue(beanModel.getProperties().size() > 0);
        Assert.assertEquals("changedNameProperty", beanModel.getProperties().get(0).getName());
    }

    public static class DummyBeanJackson2 {

        @JsonProperty("changedNameProperty")
        public String _changed_name_property;

    }

}
