
package cz.habarta.typescript.generator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class StyleConfigurationTest {

    public static class A {
        public int getX() {
            return -1;
        }

        public @org.jspecify.annotations.Nullable B getB() {
            return null;
        }
    }

    public static class B {
        public @org.jspecify.annotations.Nullable String getS() {
            return null;
        }
    }

    @Test
    public void testTypeNameCustomizations() {
        final Settings settings = TestUtils.settings();
        settings.removeTypeNamePrefix = "Json";
        settings.removeTypeNameSuffix = "Class";
        settings.addTypeNamePrefix = "I";
        settings.addTypeNameSuffix = "JSON";

        final TsType tsType = TestUtils.compileType(settings, JsonTestClass.class);
        assertEquals("ITestJSON", tsType.toString());
    }

    private static class JsonTestClass {
    }

}
