package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.DefaultTypeProcessor;
import cz.habarta.typescript.generator.DummyBean;
import cz.habarta.typescript.generator.Input;
import cz.habarta.typescript.generator.JsonLibrary;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TestUtils;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GsonParserTest {

    private static class DummyBeanGson {
        @SuppressWarnings("unused")
        private int privateField;
    }

    @Test
    public void test() {
        final GsonParser gsonParser = getGsonParser();
        final Class<?> bean = DummyBean.class;
        final Model model = gsonParser.parseModel(bean);
        Assert.assertTrue(model.getBeans().size() > 0);
        final BeanModel beanModel = model.getBeans().get(0);
        Assert.assertEquals("DummyBean", beanModel.getOrigin().getSimpleName());
        Assert.assertTrue(beanModel.getProperties().size() > 0);
        Assert.assertEquals("firstProperty", beanModel.getProperties().get(0).getName());
    }

    @Test
    public void testPrivateFieldGenerated() {
        final Settings settings = TestUtils.settings();
        settings.jsonLibrary = JsonLibrary.gson;
        final String output = generate(settings, DummyBeanGson.class);
        assertTrue(output, output.contains("privateField"));
    }

    private String generate(final Settings settings, Class<DummyBeanGson> cls) {
        return new TypeScriptGenerator(settings).generateTypeScript(Input.from(cls));
    }

    static GsonParser getGsonParser() {
        final Settings settings = new Settings();
        return new GsonParser(settings, new DefaultTypeProcessor());
    }
}
