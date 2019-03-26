
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.Jackson2ParserTest.InterfaceWithMethodsAndBeanProperties;
import cz.habarta.typescript.generator.parser.*;
import java.util.function.Consumer;
import org.junit.Assert;
import org.junit.Test;


public class Jackson1ParserTest {

    @Test
    public void test() {
        final Jackson1Parser jacksonParser = getJackson1Parser(it -> {});
        final Class<?> bean = DummyBean.class;
        final Model model = jacksonParser.parseModel(bean);
        Assert.assertTrue(model.getBeans().size() > 0);
        final BeanModel beanModel = model.getBeans().get(0);
        Assert.assertEquals("DummyBean", beanModel.getOrigin().getSimpleName());
        Assert.assertTrue(beanModel.getProperties().size() > 0);
        Assert.assertEquals("firstProperty", beanModel.getProperties().get(0).getName());
    }

    @Test
    public void testEmitMethods() {
        final Jackson1Parser jacksonParser = getJackson1Parser(it -> it.emitMethodsInBeans = true);
        final Class<?> bean = InterfaceWithMethodsAndBeanProperties.class;
        final Model model = jacksonParser.parseModel(bean);

        Jackson2ParserTest.assertMethodsInInterface(model);

        final Jackson1Parser parserNoMethods = getJackson1Parser(it -> it.emitMethodsInBeans = false);
        final Model modelNoMethods = parserNoMethods.parseModel(bean);
        Assert.assertEquals(0, modelNoMethods.getBeans().get(0).getMethods().size());
    }

    private static Jackson1Parser getJackson1Parser(Consumer<Settings> settingsModifier) {
        final Settings settings = new Settings();
        settingsModifier.accept(settings);
        return new Jackson1Parser(settings, new DefaultTypeProcessor());
    }

}
