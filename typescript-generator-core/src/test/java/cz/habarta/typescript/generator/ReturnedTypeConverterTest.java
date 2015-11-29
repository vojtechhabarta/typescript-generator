package cz.habarta.typescript.generator;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import org.junit.Test;

public class ReturnedTypeConverterTest {

    @Test
    public void testTypeConversion() {
        JavaToTypescriptTypeConverter converter = TypeScriptGenerator.generateTypeScript(Arrays.asList(A.class), new Settings(),
                new ByteArrayOutputStream());
        assertEquals("A", converter.typeFromJava(A.class).toString());
        assertEquals("B", converter.typeFromJava(B.class).toString());
    }

    private static class A {
        B x;
    }

    private static class B {
        B x;
    }
}
