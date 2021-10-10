package cz.habarta.typescript.generator.ext;

import cz.habarta.typescript.generator.Input;
import cz.habarta.typescript.generator.JsonLibrary;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TestUtils;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class PropertyPolymorphismExtensionTest {

    @Retention(RetentionPolicy.RUNTIME)
    private @interface Marker {
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface PropertyName {
        String name();
    }

    private static class TestA {
        @SuppressWarnings("unused")
        TestB b;
    }

    @Marker
    private static class TestB {
    }

    private static class TestBSub1 extends TestB {
    }

    @PropertyName(name="foo")
    private static class TestBSub2 extends TestB {
    }

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        settings.jsonLibrary = JsonLibrary.gson;
        settings.extensions.add(new PropertyPolymorphismExtension(cls -> cls==TestB.class, subType -> {
            String name = subType.getSimpleName();
            return name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);

        }));

        final String output = new TypeScriptGenerator(settings)
                .generateTypeScript(Input.from(TestA.class, TestBSub1.class, TestBSub2.class));
        assertTrue(output.contains("interface TestA {\n" + "    b: TestBRef;\n" + "}"), output);
        assertTrue(output.contains("interface TestBRef {"), output);
        assertTrue(output.contains("testBSub2: TestBSub2;"), output);
    }

    @Test
    public void testWithConfiguration() {
        final Settings settings = TestUtils.settings();
        settings.jsonLibrary = JsonLibrary.gson;
        PropertyPolymorphismExtension extension = new PropertyPolymorphismExtension();
        Map<String, String> config = new HashMap<>();
        config.put(PropertyPolymorphismExtension.MARKER_ANNOTATION, Marker.class.getName());
        config.put(PropertyPolymorphismExtension.NAME_ANNOTATION, PropertyName.class.getName());
        config.put(PropertyPolymorphismExtension.NAME_ELEMENT, "name");

        extension.setConfiguration(config);
        settings.extensions.add(extension);

        final String output = new TypeScriptGenerator(settings)
                .generateTypeScript(Input.from(TestA.class, TestBSub1.class, TestBSub2.class));
        assertTrue(output.contains("b: TestBRef;"), output);
        assertTrue(output.contains("interface TestBRef {"), output);
        assertTrue(output.contains("foo: TestBSub2;"), output);
        assertTrue(output.contains("testBSub1: TestBSub1;"), output);
    }
}
