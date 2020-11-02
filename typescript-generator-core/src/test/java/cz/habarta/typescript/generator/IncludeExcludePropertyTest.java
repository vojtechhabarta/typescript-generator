
package cz.habarta.typescript.generator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;


@SuppressWarnings("unused")
@RunWith(Parameterized.class)
public class IncludeExcludePropertyTest {

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<Object[]> data() {
        return Arrays.stream(JsonLibrary.values())
                .filter(library -> library != JsonLibrary.jackson1 && library != JsonLibrary.jsonb)
                .map(library -> new Object[]{library})
                .collect(Collectors.toList());
    }

    private final JsonLibrary library;

    public IncludeExcludePropertyTest(JsonLibrary library) {
        this.library = library;
    }


    @Test
    public void testInclude() {
        final Settings settings = TestUtils.settings();
        settings.jsonLibrary = library;
        settings.includePropertyAnnotations = Arrays.asList(MyInclude.class);
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithAnnotatedProperties.class));
        Assert.assertTrue(!output.contains("property1"));
        Assert.assertTrue(output.contains("property2"));
        Assert.assertTrue(!output.contains("property3"));
        Assert.assertTrue(output.contains("property4"));
    }

    @Test
    public void testExclude() {
        final Settings settings = TestUtils.settings();
        settings.jsonLibrary = library;
        settings.excludePropertyAnnotations = Arrays.asList(MyExclude.class);
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithAnnotatedProperties.class));
        Assert.assertTrue(output.contains("property1"));
        Assert.assertTrue(output.contains("property2"));
        Assert.assertTrue(!output.contains("property3"));
        Assert.assertTrue(!output.contains("property4"));
    }

    @Test
    public void testBoth() {
        final Settings settings = TestUtils.settings();
        settings.jsonLibrary = library;

        settings.includePropertyAnnotations = Arrays.asList(MyInclude.class);
        settings.excludePropertyAnnotations = Arrays.asList(MyExclude.class);
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithAnnotatedProperties.class));
        Assert.assertTrue(!output.contains("property1"));
        Assert.assertTrue(output.contains("property2"));
        Assert.assertTrue(!output.contains("property3"));
        Assert.assertTrue(!output.contains("property4"));
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
