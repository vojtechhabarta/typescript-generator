package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.Input;
import cz.habarta.typescript.generator.JsonLibrary;
import cz.habarta.typescript.generator.OptionalProperties;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TestUtils;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.OptionalInt;
import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JsonbParserTest {

    private Settings settings;

    @Before
    public void before() {
        settings = TestUtils.settings();
        settings.jsonLibrary = JsonLibrary.jsonb;
        settings.optionalProperties = OptionalProperties.useLibraryDefinition;
    }

    private static class OverridenPropertyName {
        @JsonbProperty("$foo")
        int foo;
    }

    private static class OverridenPropertyNameOnGetter {
        int foo;

        @JsonbProperty("$foo")
        public int getFoo() {
            return foo;
        }
    }

    public static class DirectName {
        public int foo;
    }

    public static class OptionalWithGetter {
        private int foo;

        public int getFoo() {
            return foo;
        }
    }

    public static class Required {
        public final int foo;

        @JsonbCreator
        public Required(final int foo) {
            this.foo = foo;
        }
    }

    public static class RequiredOptional {
        public final int foo;

        @JsonbCreator
        public RequiredOptional(final OptionalInt foo) {
            this.foo = foo.orElse(1);
        }
    }

    public static class RequiredWithGetter {
        private final int foo;

        @JsonbCreator
        public RequiredWithGetter(final int foo) {
            this.foo = foo;
        }

        public int getFoo() {
            return foo;
        }
    }

    public static class NillableConstructorParameter {
        private final int foo;

        @JsonbCreator // we agree it is a stupid case but generator must respect user choice
        public NillableConstructorParameter(@JsonbProperty(value = "foo", nillable = true) final int foo) {
            this.foo = foo;
        }

        public int getFoo() {
            return foo;
        }
    }

    public static class ObjecWithRequiredProperty {
        @RequiredAnnotation
        public String foo;
        public String bar;
    }

    public static class ObjectWithRequiredPropertyAndConstructor {
        public String foo;
        public String bar;

        @JsonbCreator
        public ObjectWithRequiredPropertyAndConstructor(@RequiredAnnotation final String foo, final String bar) {}
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface RequiredAnnotation {
    }

    public static class ListOfNullableElementsField {
        public List<@Nullable String> foos;
    }

    public static class ListOfNullableElementsGetter {
        public List<@Nullable String> getFoos() {
            return null;
        }
    }

    public static class ListOfNullableElementsConstructor {
        private List<String> foos;

        @JsonbCreator
        public ListOfNullableElementsConstructor(List<@Nullable String> foos) {
            this.foos = foos;
        }
    }

    @Test
    public void testNullabilityField() {
        settings.nullableAnnotations.add(Nullable.class);
        final String output = generate(settings, ListOfNullableElementsField.class);
        Assert.assertTrue(output, output.contains(" foos?: (string | null)[];"));
    }

    @Test
    public void testNullabilityGetter() {
        settings.nullableAnnotations.add(Nullable.class);
        final String output = generate(settings, ListOfNullableElementsGetter.class);
        Assert.assertTrue(output, output.contains(" foos?: (string | null)[];"));
    }

    @Test
    public void testNullabilityConstructor() {
        settings.nullableAnnotations.add(Nullable.class);
        final String output = generate(settings, ListOfNullableElementsConstructor.class);
        Assert.assertTrue(output, output.contains(" foos: (string | null)[];"));
    }

    @Test
    public void testRequiredPropertyMarkedByAnnotation() {
        settings.optionalProperties = OptionalProperties.useSpecifiedAnnotations;
        settings.requiredAnnotations.add(RequiredAnnotation.class);
        final String output = generate(settings, ObjecWithRequiredProperty.class);
        Assert.assertTrue(output, output.contains(" foo:"));
        Assert.assertTrue(output, output.contains(" bar?:"));
    }

    @Test
    public void testRequiredPropertyMarkedByAnnotationAndConstructorFactory() {
        settings.optionalProperties = OptionalProperties.useSpecifiedAnnotations;
        settings.requiredAnnotations.add(RequiredAnnotation.class);
        final String output = generate(settings, ObjectWithRequiredPropertyAndConstructor.class);
        Assert.assertTrue(output, output.contains(" foo:"));
        Assert.assertTrue(output, output.contains(" bar?:"));
    }

    @Test
    public void tesJsonbProperty() {
        final String output = generate(settings, OverridenPropertyName.class);
        Assert.assertTrue(output, output.contains(" $foo?:"));
    }

    @Test
    public void tesJsonbPropertyOnMethod() {
        final String output = generate(settings, OverridenPropertyNameOnGetter.class);
        Assert.assertTrue(output, output.contains(" $foo?:"));
        Assert.assertFalse(output, output.contains(" foo?:"));
    }
    @Test
    public void tesImplicitName() {
        final String output = generate(settings, DirectName.class);
        Assert.assertTrue(output, output.contains(" foo?:"));
    }
    @Test
    public void optionality() {
        {
            final String output = generate(settings, DirectName.class);
            Assert.assertTrue(output, output.contains(" foo?: number"));
        }
        {
            final String output = generate(settings, OptionalWithGetter.class);
            Assert.assertTrue(output, output.contains(" foo?: number"));
        }
        {
            final String output = generate(settings, Required.class);
            Assert.assertTrue(output, output.contains(" foo: number"));
        }
        {
            final String output = generate(settings, RequiredWithGetter.class);
            Assert.assertTrue(output, output.contains(" foo: number"));
        }
        {
            final String output = generate(settings, RequiredOptional.class);
            Assert.assertTrue(output, output.contains(" foo?: number"));
        }
        {
            final String output = generate(settings, NillableConstructorParameter.class);
            Assert.assertTrue(output, output.contains(" foo?: number"));
        }
    }

    private String generate(final Settings settings, Class<?> cls) {
        return new TypeScriptGenerator(settings).generateTypeScript(Input.from(cls));
    }
}
