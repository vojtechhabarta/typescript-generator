package cz.habarta.typescript.generator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.util.Arrays;

import org.junit.Test;

public class CustomTypeConversionTest {

    @Test
    public void testCustomTypeConversion() {
        Settings settings = new Settings();
        // suppose we want to override how A is parsed
        settings.customTypeParser = new JavaToTypescriptTypeConverter() {

            @Override
            public TsType typeFromJava(Type javaType, JavaToTypescriptTypeConverter fallback) {
                if (javaType.equals(B.class)) {
                    return TsType.Number.getOptionalReference();
                }
                return null;
            }
        };

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JavaToTypescriptTypeConverter converter = TypeScriptGenerator.generateTypeScript(Arrays.asList(A.class), settings, out);
        assertEquals("A", converter.typeFromJava(A.class).toString());
        assertTrue(new String(out.toByteArray()).trim().contains("x?: number;"));
    }

    public static class A {
        public B getX() {
            return null;
        }
    }

    public static class B {
        public B getX() {
            return null;
        }
    }
}
