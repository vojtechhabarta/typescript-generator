
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.*;
import org.junit.Assert;
import org.junit.Test;


public class Jackson1ParserTest {

    @Test
    public void test() {
        final Jackson1Parser jacksonParser = getJackson1Parser();
        final Class<?> bean = DummyBean.class;
        final Model model = jacksonParser.parseModel(bean);
        Assert.assertTrue(model.getBeans().size() > 0);
        final BeanModel beanModel = model.getBeans().get(0);
        Assert.assertEquals("DummyBean", beanModel.getOrigin().getSimpleName());
        Assert.assertTrue(beanModel.getProperties().size() > 0);
        Assert.assertEquals("firstProperty", beanModel.getProperties().get(0).getName());
    }

    private static Jackson1Parser getJackson1Parser() {
        final Settings settings = new Settings();
        return new Jackson1Parser(settings, new DefaultTypeProcessor());
    }

}
