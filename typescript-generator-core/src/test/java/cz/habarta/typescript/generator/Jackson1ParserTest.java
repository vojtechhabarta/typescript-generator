
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.*;
import java.util.logging.Logger;
import org.junit.Assert;
import org.junit.Test;


public class Jackson1ParserTest {

    @Test
    public void test() {
        final Jackson1Parser jacksonParser = new Jackson1Parser(Logger.getGlobal(), new Settings());
        final Class<?> bean = DummyBean.class;
        final Model model = jacksonParser.parseModel(bean);
        Assert.assertTrue(model.getBeans().size() > 0);
        final BeanModel beanModel = model.getBeans().get(0);
        System.out.println("beanModel: " + beanModel);
        Assert.assertEquals("DummyBean", beanModel.getName());
        Assert.assertTrue(beanModel.getProperties().size() > 0);
        Assert.assertEquals("firstProperty", beanModel.getProperties().get(0).getName());
    }

}
