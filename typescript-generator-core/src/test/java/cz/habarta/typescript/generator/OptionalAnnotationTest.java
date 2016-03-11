package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.*;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.junit.Assert;
import org.junit.Test;


public class OptionalAnnotationTest {

    @Test
    public void testJackson1OptionalAnnotation() {
        Settings settings = new Settings();
        settings.optionalAnnotations.add(Nullable.class);
        ModelParser parser = new Jackson1Parser(settings, new DefaultTypeProcessor());
        testModel(parser.parseModel(Jackson1Bean.class), true);
    }

    @Test
    public void testJackson1NoAnnotation() {
        Settings settings = new Settings();
        ModelParser parser = new Jackson1Parser(settings, new DefaultTypeProcessor());
        testModel(parser.parseModel(Jackson1Bean.class), false);
    }

    @Test
    public void testJackson2OptionalAnnotation() {
        Settings settings = new Settings();
        settings.optionalAnnotations.add(Nullable.class);
        ModelParser parser = new Jackson2Parser(settings, new DefaultTypeProcessor());
        testModel(parser.parseModel(Jackson2Bean.class), true);
    }

    @Test
    public void testJackson2NoAnnotation() {
        Settings settings = new Settings();
        ModelParser parser = new Jackson2Parser(settings, new DefaultTypeProcessor());
        testModel(parser.parseModel(Jackson2Bean.class), false);
    }

    private void testModel(Model model, boolean optional) {
        Assert.assertEquals(1, model.getBeans().size());
        BeanModel beanModel = model.getBeans().get(0);
        Assert.assertEquals(2, beanModel.getProperties().size());
        for (PropertyModel propertyModel : beanModel.getProperties()) {
            Assert.assertEquals(optional, propertyModel.isOptional());
        }
    }

    @org.codehaus.jackson.annotate.JacksonAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    static @interface Nullable {
        // marker
    }

    static class Jackson1Bean {
        @Nullable
        @org.codehaus.jackson.annotate.JsonProperty
        private String fieldProperty;

        @Nullable
        @org.codehaus.jackson.annotate.JsonProperty
        public String getMethodProperty() {
            return fieldProperty;
        }
    }

    static class Jackson2Bean {
        @Nullable
        @com.fasterxml.jackson.annotation.JsonProperty
        private String fieldProperty;

        @Nullable
        @com.fasterxml.jackson.annotation.JsonProperty
        public String getMethodProperty() {
            return fieldProperty;
        }
    }
}
