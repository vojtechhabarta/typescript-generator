
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.BeanModel;
import cz.habarta.typescript.generator.parser.Jackson1Parser;
import cz.habarta.typescript.generator.parser.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class Jackson1ParserTest {

    @Test
    public void test() {
        final Jackson1Parser jacksonParser = getJackson1Parser();
        final Class<?> bean = DummyBean.class;
        final Model model = jacksonParser.parseModel(bean);
        Assertions.assertTrue(model.getBeans().size() > 0);
        final BeanModel beanModel = model.getBeans().get(0);
        Assertions.assertEquals("DummyBean", beanModel.getOrigin().getSimpleName());
        Assertions.assertTrue(beanModel.getProperties().size() > 0);
        Assertions.assertEquals("firstProperty", beanModel.getProperties().get(0).getName());
    }

    private static Jackson1Parser getJackson1Parser() {
        final Settings settings = new Settings();
        return new Jackson1Parser(settings, new DefaultTypeProcessor());
    }

}
