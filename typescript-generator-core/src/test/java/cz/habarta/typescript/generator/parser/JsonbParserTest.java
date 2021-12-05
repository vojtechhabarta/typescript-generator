package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.Input;
import cz.habarta.typescript.generator.JsonLibrary;
import cz.habarta.typescript.generator.OptionalProperties;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TestUtils;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JsonbParserTest {

    private Settings settings;

    @BeforeEach
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

    public static class PrimitiveObjectWithTheOtherObject {
        public char charVar1;
        public byte byteVar1;
        public short shortVar1;
        public int intVar1;
        public long longVar1;
        public float floatVar1;
        public double doubleVar1;
        public boolean booleanVar1;
        public String stringVar;
        public Character charVar2;
        public Byte byteVar2;
        public Short shortVar2;
        public Integer intVar2;
        public Long longVar2;
        public Float floatVar2;
        public Double doubleVar2;
        public Boolean booleanVar2;
        public UUID uuidVar;
        public Date dateVar;
        public Collection<String> collectionVar;
        public Map<String, String> mapVar;
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

    @SuppressWarnings("unused")
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
        Assertions.assertTrue(output.contains(" foos?: (string | null)[];"), output);
    }

    @Test
    public void testNullabilityGetter() {
        settings.nullableAnnotations.add(Nullable.class);
        final String output = generate(settings, ListOfNullableElementsGetter.class);
        Assertions.assertTrue(output.contains(" foos?: (string | null)[];"), output);
    }

    @Test
    public void testNullabilityConstructor() {
        settings.nullableAnnotations.add(Nullable.class);
        final String output = generate(settings, ListOfNullableElementsConstructor.class);
        Assertions.assertTrue(output.contains(" foos: (string | null)[];"), output);
    }

    @Test
    public void testRequiredPropertyMarkedByAnnotation() {
        settings.optionalProperties = OptionalProperties.useSpecifiedAnnotations;
        settings.requiredAnnotations.add(RequiredAnnotation.class);
        final String output = generate(settings, ObjecWithRequiredProperty.class);
        Assertions.assertTrue(output.contains(" foo:"), output);
        Assertions.assertTrue(output.contains(" bar?:"), output);
    }

    @Test
    public void testRequiredPropertyMarkedByAnnotationAndConstructorFactory() {
        settings.optionalProperties = OptionalProperties.useSpecifiedAnnotations;
        settings.requiredAnnotations.add(RequiredAnnotation.class);
        final String output = generate(settings, ObjectWithRequiredPropertyAndConstructor.class);
        Assertions.assertTrue(output.contains(" foo:"), output);
        Assertions.assertTrue(output.contains(" bar?:"), output);
    }

    @Test
    public void testRequiredPropertyMarkedByAnnotationWithPrimitivePropertiesRequired() {
        settings.optionalProperties = OptionalProperties.useSpecifiedAnnotations;
        settings.requiredAnnotations.add(RequiredAnnotation.class);
        settings.primitivePropertiesRequired = true;
        final String output = generate(settings, PrimitiveObjectWithTheOtherObject.class);
        Assertions.assertTrue(output.contains("charVar1: string;"));
        Assertions.assertTrue(output.contains("byteVar1: number;"));
        Assertions.assertTrue(output.contains("shortVar1: number;"));
        Assertions.assertTrue(output.contains("intVar1: number;"));
        Assertions.assertTrue(output.contains("longVar1: number;"));
        Assertions.assertTrue(output.contains("floatVar1: number;"));
        Assertions.assertTrue(output.contains("doubleVar1: number;"));
        Assertions.assertTrue(output.contains("booleanVar1: boolean;"));
        Assertions.assertTrue(output.contains("stringVar?: string;"));
        Assertions.assertTrue(output.contains("charVar2?: string;"));
        Assertions.assertTrue(output.contains("byteVar2?: number;"));
        Assertions.assertTrue(output.contains("shortVar2?: number;"));
        Assertions.assertTrue(output.contains("intVar2?: number;"));
        Assertions.assertTrue(output.contains("longVar2?: number;"));
        Assertions.assertTrue(output.contains("floatVar2?: number;"));
        Assertions.assertTrue(output.contains("doubleVar2?: number;"));
        Assertions.assertTrue(output.contains("booleanVar2?: boolean;"));
        Assertions.assertTrue(output.contains("uuidVar?: string;"));
        Assertions.assertTrue(output.contains("dateVar?: Date;"));
        Assertions.assertTrue(output.contains("collectionVar?: string[];"));
        Assertions.assertTrue(output.contains("mapVar?: { [index: string]: string };"));
    }

    @Test
    public void tesJsonbProperty() {
        final String output = generate(settings, OverridenPropertyName.class);
        Assertions.assertTrue(output.contains(" $foo?:"), output);
    }

    @Test
    public void tesJsonbPropertyOnMethod() {
        final String output = generate(settings, OverridenPropertyNameOnGetter.class);
        Assertions.assertTrue(output.contains(" $foo?:"), output);
        Assertions.assertFalse(output.contains(" foo?:"), output);
    }
    @Test
    public void tesImplicitName() {
        final String output = generate(settings, DirectName.class);
        Assertions.assertTrue(output.contains(" foo?:"), output);
    }
    @Test
    public void optionality() {
        {
            final String output = generate(settings, DirectName.class);
            Assertions.assertTrue(output.contains(" foo?: number"), output);
        }
        {
            final String output = generate(settings, OptionalWithGetter.class);
            Assertions.assertTrue(output.contains(" foo?: number"), output);
        }
        {
            final String output = generate(settings, Required.class);
            Assertions.assertTrue(output.contains(" foo: number"), output);
        }
        {
            final String output = generate(settings, RequiredWithGetter.class);
            Assertions.assertTrue(output.contains(" foo: number"), output);
        }
        {
            final String output = generate(settings, RequiredOptional.class);
            Assertions.assertTrue(output.contains(" foo?: number"), output);
        }
        {
            final String output = generate(settings, NillableConstructorParameter.class);
            Assertions.assertTrue(output.contains(" foo?: number"), output);
        }
    }

    private String generate(final Settings settings, Class<?> cls) {
        return new TypeScriptGenerator(settings).generateTypeScript(Input.from(cls));
    }
}
