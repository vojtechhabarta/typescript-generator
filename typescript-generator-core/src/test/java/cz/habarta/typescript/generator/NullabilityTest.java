
package cz.habarta.typescript.generator;

import java.util.Date;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


@SuppressWarnings("unused")
public class NullabilityTest {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        settings.nullableAnnotations.add(Nullable.class);
        settings.nullabilityDefinition = NullabilityDefinition.undefinedInlineUnion;
        settings.sortDeclarations = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(A.class));
        final String expected = "" +
                "interface A<T> {\n" +
                "    data?: T;\n" +
                "    isNonNullableFlag: boolean;\n" +
                "    isNullableFlag?: boolean;\n" +
                "    nonNullableBoolean: boolean;\n" +
                "    nonNullableString: string;\n" +
                "    nullableArray?: (string | undefined)[];\n" +
                "    nullableBoolean?: boolean;\n" +
                "    nullableGenericArray?: (T | undefined)[];\n" +
                "    nullableGenericType?: T;\n" +
                "    nullableList?: (string | undefined)[];\n" +
                "    nullableString?: string;\n" +
                "    test: string;\n" +
                "    testNullable?: string;\n" +
                "}";
        Assertions.assertEquals(expected.trim(), output.trim());
    }

    private static class A<T> {

        public @Nullable String nullableString;
        public String nonNullableString;
        public @Nullable List<@Nullable String> nullableList;
        public @Nullable String @Nullable [] nullableArray;
        public @Nullable T @Nullable [] nullableGenericArray;
        public @Nullable T nullableGenericType;
        public @Nullable Boolean nullableBoolean;
        public Boolean nonNullableBoolean;
        public @Nullable Boolean isNullableFlag;
        public Boolean isNonNullableFlag;

        public <D extends T> @Nullable D getData() {
            return null;
        }

        public String getTest() {
            return "";
        }

        public @Nullable String getTestNullable() {
            return null;
        }

    }

    @Test
    public void testVariants() {
        testVariant(NullabilityDefinition.nullAndUndefinedUnion,       "list?: Nullable<Nullable<string>[]>", "type Nullable<T> = T | null | undefined");
        testVariant(NullabilityDefinition.undefinedUnion,              "list?: Nullable<Nullable<string>[]>", "type Nullable<T> = T | undefined");
        testVariant(NullabilityDefinition.nullUnion,                   "list: Nullable<Nullable<string>[]>", "type Nullable<T> = T | null");
        testVariant(NullabilityDefinition.nullAndUndefinedInlineUnion, "list?: (string | null | undefined)[] | null");
        testVariant(NullabilityDefinition.undefinedInlineUnion,        "list?: (string | undefined)[]");
        testVariant(NullabilityDefinition.nullInlineUnion,             "list: (string | null)[] | null");
    }

    private static void testVariant(NullabilityDefinition nullabilityDefinition, String... expected) {
        final Settings settings = TestUtils.settings();
        settings.nullableAnnotations.add(Nullable.class);
        settings.nullabilityDefinition = nullabilityDefinition;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(B.class));
        for (String s : expected) {
            Assertions.assertTrue(output.contains(s));
        }
    }

    private static class B {
        public @Nullable List<@Nullable String> list;
    }

    @Test
    public void testCombinationsOfOptionalAndNullable() {
        testCombinationOfOptionalAndNullable(
                OptionalPropertiesDeclaration.questionMark,
                NullabilityDefinition.nullInlineUnion,
                "list?: (string | null)[] | null;");
        testCombinationOfOptionalAndNullable(
                OptionalPropertiesDeclaration.nullableType,
                NullabilityDefinition.nullInlineUnion,
                "list: (string | null)[] | null;");
        testCombinationOfOptionalAndNullable(
                OptionalPropertiesDeclaration.nullableAndUndefinableType,
                NullabilityDefinition.nullAndUndefinedInlineUnion,
                "list: (string | null | undefined)[] | null | undefined;");
        testCombinationOfOptionalAndNullable(
                OptionalPropertiesDeclaration.nullableType,
                NullabilityDefinition.nullAndUndefinedUnion,
                "list: Nullable<Nullable<string>[]> | null;");
    }

    private static void testCombinationOfOptionalAndNullable(
            OptionalPropertiesDeclaration optionalPropertiesDeclaration,
            NullabilityDefinition nullabilityDefinition,
            String expected
    ) {
        final Settings settings = TestUtils.settings();
        settings.optionalAnnotations.add(Nullable.class);
        settings.nullableAnnotations.add(Nullable.class);
        settings.optionalPropertiesDeclaration = optionalPropertiesDeclaration;
        settings.nullabilityDefinition = nullabilityDefinition;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(B.class));
        Assertions.assertTrue(output.contains(expected), "Unexpected actual output: " + output);
    }

    @Test
    public void testNullableAnnotationTarget() {
        final Settings settings = TestUtils.settings();
        settings.nullableAnnotations.add(javax.annotation.Nullable.class);
        Assertions.assertThrows(RuntimeException.class, () -> new TypeScriptGenerator(settings).generateTypeScript(Input.from(A.class)));
    }

    @Test
    public void testDate() {
        final Settings settings = TestUtils.settings();
        settings.nullableAnnotations.add(Nullable.class);
        settings.nullabilityDefinition = NullabilityDefinition.nullAndUndefinedUnion;
        settings.mapDate = DateMapping.asString;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithDate.class));
        Assertions.assertTrue(output.contains("date?: Nullable<DateAsString>"));
    }

    private static class ClassWithDate {
        public @Nullable Date date;
    }

}
