
package cz.habarta.typescript.generator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


@SuppressWarnings("unused")
public class IncludeExcludePropertyTest {

    public static Stream<JsonLibrary> data() {
        return Arrays.stream(JsonLibrary.values())
                .filter(library -> library != JsonLibrary.jsonb);
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("data")
    public void testInclude(JsonLibrary library) {
        final Settings settings = TestUtils.settings();
        settings.jsonLibrary = library;
        settings.includePropertyAnnotations = Arrays.asList(MyInclude.class);
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithAnnotatedProperties.class));
        Assertions.assertTrue(!output.contains("property1"));
        Assertions.assertTrue(output.contains("property2"));
        Assertions.assertTrue(!output.contains("property3"));
        Assertions.assertTrue(output.contains("property4"));
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("data")
    public void testExclude(JsonLibrary library) {
        final Settings settings = TestUtils.settings();
        settings.jsonLibrary = library;
        settings.excludePropertyAnnotations = Arrays.asList(MyExclude.class);
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithAnnotatedProperties.class));
        Assertions.assertTrue(output.contains("property1"));
        Assertions.assertTrue(output.contains("property2"));
        Assertions.assertTrue(!output.contains("property3"));
        Assertions.assertTrue(!output.contains("property4"));
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("data")
    public void testBoth(JsonLibrary library) {
        final Settings settings = TestUtils.settings();
        settings.jsonLibrary = library;

        settings.includePropertyAnnotations = Arrays.asList(MyInclude.class);
        settings.excludePropertyAnnotations = Arrays.asList(MyExclude.class);
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithAnnotatedProperties.class));
        Assertions.assertTrue(!output.contains("property1"));
        Assertions.assertTrue(output.contains("property2"));
        Assertions.assertTrue(!output.contains("property3"));
        Assertions.assertTrue(!output.contains("property4"));
    }

    @Retention(RetentionPolicy.RUNTIME)
    private static @interface MyInclude {
    }

    @Retention(RetentionPolicy.RUNTIME)
    private static @interface MyExclude {
    }

    private static class ClassWithAnnotatedProperties {

        public String property1;

        @MyInclude
        public String property2;

        @MyExclude
        public String property3;

        @MyInclude
        @MyExclude
        public String property4;

    }

}
