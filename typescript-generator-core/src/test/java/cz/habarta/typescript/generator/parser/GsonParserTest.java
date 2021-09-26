package cz.habarta.typescript.generator.parser;

import com.google.gson.annotations.SerializedName;
import cz.habarta.typescript.generator.DefaultTypeProcessor;
import cz.habarta.typescript.generator.DummyBean;
import cz.habarta.typescript.generator.GsonConfiguration;
import cz.habarta.typescript.generator.Input;
import cz.habarta.typescript.generator.JsonLibrary;
import cz.habarta.typescript.generator.OptionalProperties;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TestUtils;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unused")
public class GsonParserTest {

    private Settings settings;

    private static class DummyBeanGson {
        private int privateField;
    }

    @Before
    public void before() {
        settings = TestUtils.settings();
        settings.jsonLibrary = JsonLibrary.gson;
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
        beanModel.getProperties().sort((p1, p2) -> (p1.getName().compareTo(p2.getName())));
        Assert.assertEquals("booleanProperty", beanModel.getProperties().get(0).getName());
    }

    @Test
    public void testPrivateFieldGenerated() {
        final String output = generate(settings, DummyBeanGson.class);
        Assert.assertTrue(output, output.contains("privateField"));
    }

    private static class DummyBeanSerializedName {
        @SerializedName("bar")
        int foo;
    }

    @Test
    public void testSerializedName() {
        final String output = generate(settings, DummyBeanSerializedName.class);
        Assert.assertTrue(output, output.contains("bar"));
    }

    private String generate(final Settings settings, Class<?> cls) {
        return new TypeScriptGenerator(settings).generateTypeScript(Input.from(cls));
    }

    static GsonParser getGsonParser() {
        final Settings settings = new Settings();
        return new GsonParser(settings, new DefaultTypeProcessor());
    }

    private static class Demo {
        public static String THIS_FIELD_SHOULD_NOT_BE_INCLUDED = "test";
        public String thisShouldBeIncluded = "test";
    }

    @Test
    public void testStaticFieldNotIncluded() {
        final String output = generate(settings, Demo.class);
        Assert.assertTrue(!output.contains("THIS_FIELD_SHOULD_NOT_BE_INCLUDED"));
    }

    @Test
    public void testStaticFieldIncluded() {
        settings.gsonConfiguration = new GsonConfiguration();
        settings.gsonConfiguration.excludeFieldsWithModifiers = "transient";
        final String output = generate(settings, Demo.class);
        Assert.assertTrue(output.contains("THIS_FIELD_SHOULD_NOT_BE_INCLUDED"));
    }

    @Test
    public void testOptionalProperties_Default() {
        final String output = generate(settings, BeanWithOptionalProperty.class);
        System.out.println(output);
        Assert.assertTrue(output.contains("property1: string;"));
    }

    @Test
    public void testOptionalProperties_All() {
        settings.optionalProperties = OptionalProperties.all;
        final String output = generate(settings, BeanWithOptionalProperty.class);
        System.out.println(output);
        Assert.assertTrue(output.contains("property1?: string;"));
    }

    @Test
    public void testOptionalProperties_UseLibraryDefinition() {
        settings.optionalProperties = OptionalProperties.useLibraryDefinition;
        final String output = generate(settings, BeanWithOptionalProperty.class);
        System.out.println(output);
        Assert.assertTrue(output.contains("property1?: string;"));
    }

    @Test
    public void testOptionalProperties_UseSpecifiedAnnotations() {
        settings.optionalAnnotations = Arrays.asList(OptionalProperty.class);
        final String output = generate(settings, BeanWithOptionalProperty.class);
        System.out.println(output);
        Assert.assertTrue(output.contains("property1?: string;"));
    }

    private static class BeanWithOptionalProperty {
        @OptionalProperty
        private String property1;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface OptionalProperty {
    }

}
