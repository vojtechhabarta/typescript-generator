package cz.habarta.typescript.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class StyleConfigurationTest {

    public static class A {
        public int getX() {
            return -1;
        }
        public B getB() {
            return null;
        }
    }

    public static class B {
        public String getS() {
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
