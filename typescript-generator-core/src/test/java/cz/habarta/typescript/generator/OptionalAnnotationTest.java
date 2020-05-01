package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.BeanModel;
import cz.habarta.typescript.generator.parser.Jackson1Parser;
import cz.habarta.typescript.generator.parser.Jackson2Parser;
import cz.habarta.typescript.generator.parser.Model;
import cz.habarta.typescript.generator.parser.ModelParser;
import cz.habarta.typescript.generator.parser.PropertyModel;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
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

    @Test
    public void testJavaxNullableWithJackson1() {
        testJavaxNullableUsingTypeScriptGenerator(JsonLibrary.jackson1);
    }

    @Test
    public void testJavaxNullableWithJackson2() {
        testJavaxNullableUsingTypeScriptGenerator(JsonLibrary.jackson2);
    }

    private void testJavaxNullableUsingTypeScriptGenerator(JsonLibrary jsonLibrary) {
        Settings settings = TestUtils.settings();
        settings.jsonLibrary = jsonLibrary;
        settings.optionalAnnotations.add(javax.annotation.Nullable.class);
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(BeanWithJavaxNullable.class));
        Assert.assertTrue(output.contains("property1?: string;"));
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

    static class BeanWithJavaxNullable {
        @javax.annotation.Nullable
        public String property1;
    }

    @Test
    public void testNullableTypeAnnotation() {
        Settings settings = TestUtils.settings();
        settings.optionalAnnotations.add(NullableType.class);
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(BeanWithNullableType.class));
        Assert.assertTrue(output.contains("property1?: string;"));
        Assert.assertTrue(output.contains("property2?: string;"));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    public @interface NullableType {
    }

    private static class BeanWithNullableType {
        @NullableType
        public String property1;

        @NullableType
        public String getProperty2() {
            return null;
        }
    }

    @Test
    public void testAnnotatedPrivateField() {
        final Settings settings = TestUtils.settings();
        settings.optionalAnnotations.add(TypescriptOptional.class);
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(SearchDTO.class));
        Assert.assertTrue(output.contains("selectedId?: number;"));
    }

    public class SearchDTO {

        private Integer year;

        @TypescriptOptional
        private Long selectedId;

        public Integer getYear() {
            return year;
        }

        public Long getSelectedId() {
            return selectedId;
        }

    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface TypescriptOptional {
    }

    @Test
    public void testOptionalAndRequiredProperty() {
        {
            final Settings settings = TestUtils.settings();
            settings.optionalAnnotations = Arrays.asList();
            settings.requiredAnnotations = Arrays.asList();
            final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithMarkedField.class));
            Assert.assertTrue(output.contains("a: string;"));
            Assert.assertTrue(output.contains("b: string;"));
        }
        {
            final Settings settings = TestUtils.settings();
            settings.optionalAnnotations = Arrays.asList(MarkerAnnotation.class);
            settings.requiredAnnotations = Arrays.asList();
            final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithMarkedField.class));
            Assert.assertTrue(output.contains("a: string;"));
            Assert.assertTrue(output.contains("b?: string;"));
        }
        {
            final Settings settings = TestUtils.settings();
            settings.optionalAnnotations = Arrays.asList();
            settings.requiredAnnotations = Arrays.asList(MarkerAnnotation.class);
            final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithMarkedField.class));
            Assert.assertTrue(output.contains("a?: string;"));
            Assert.assertTrue(output.contains("b: string;"));
        }
        try {
            final Settings settings = TestUtils.settings();
            settings.optionalAnnotations = Arrays.asList(MarkerAnnotation.class);
            settings.requiredAnnotations = Arrays.asList(MarkerAnnotation.class);
            new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithMarkedField.class));
            Assert.fail();
        } catch (Exception e) {
            // expected - optionalAnnotations and requiredAnnotations cannot be used together
        }
    }

    public class ClassWithMarkedField {
        public String a;
        @MarkerAnnotation public String b;
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface MarkerAnnotation {
    }

}
