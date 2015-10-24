
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.logging.Logger;
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
        System.out.println("beanModel: " + beanModel);
        Assert.assertEquals("DummyBean", beanModel.getBeanClass().getSimpleName());
        Assert.assertTrue(beanModel.getProperties().size() > 0);
        Assert.assertEquals("firstProperty", beanModel.getProperties().get(0).getName());
    }

    @Test
    public void testChangedNameProperty() {
        final Jackson2Parser jacksonParser = getJackson2Parser();
        final Class<?> bean = DummyBeanJackson2.class;
        final Model model = jacksonParser.parseModel(bean);
        Assert.assertTrue(model.getBeans().size() > 0);
        final BeanModel beanModel = model.getBeans().get(0);
        System.out.println("beanModel: " + beanModel);
        Assert.assertEquals("DummyBeanJackson2", beanModel.getBeanClass().getSimpleName());
        Assert.assertTrue(beanModel.getProperties().size() > 0);
        Assert.assertEquals("changedNameProperty", beanModel.getProperties().get(0).getName());
    }

    static Jackson2Parser getJackson2Parser() {
        final Logger logger = Logger.getGlobal();
        final Settings settings = new Settings();
        return new Jackson2Parser(logger, settings, new ModelCompiler(logger, settings));
    }

    public static class DummyBeanJackson2 {

        @JsonProperty("changedNameProperty")
        public String _changed_name_property;

    }

}
