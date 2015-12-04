package cz.habarta.typescript.generator;

import java.io.*;
import java.lang.reflect.*;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class CustomTypeConversionTest {

    @Test
    public void testCustomTypeConversion() {
        Settings settings = new Settings();
        // suppose we want to override how A is parsed
        settings.customTypeProcessor = new TypeProcessor() {
            @Override
            public TypeProcessor.Result processType(Type javaType, TypeProcessor.Context context) {
                if (javaType.equals(B.class)) {
                    return new Result(TsType.Number.getOptionalReference());
                }
                return null;
            }
        };

        TypeProcessor typeProcessor = TypeScriptGenerator.createTypeProcessor(settings);
        final TypeProcessor.Context context = DefaultTypeProcessorTest.getTestContext(typeProcessor);
        assertEquals("A", typeProcessor.processType(A.class, context).getTsType().toString());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TypeScriptGenerator.generateTypeScript(Arrays.asList(A.class), settings, out);
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

    @Test
    public void testCustomOptional() throws Exception {
        final Settings settings = new Settings();
        settings.customTypeProcessor = new TypeProcessor() {
            @Override
            public TypeProcessor.Result processType(Type javaType, TypeProcessor.Context context) {
                final Type[] typeArguments = tryGetParameterizedTypeArguments(javaType, CustomOptional.class);
                if (typeArguments != null) {
                    final TypeProcessor.Result result = context.processType(typeArguments[0]);
                    return new Result(result.getTsType().getOptionalReference(), result.getDiscoveredClasses());
                }
                return null;
            }
        };
        final TypeProcessor typeProcessor = TypeScriptGenerator.createTypeProcessor(settings);
        final TypeProcessor.Context context = DefaultTypeProcessorTest.getTestContext(typeProcessor);
        {
            final Type maybeObjectFieldType = CustomOptionalUsage.class.getField("maybeObject").getGenericType();
            final TypeProcessor.Result result = typeProcessor.processType(maybeObjectFieldType, context);
            assertEquals(Arrays.asList(SomeObject.class), result.getDiscoveredClasses());
        }
        {
            final StringWriter out = new StringWriter();
            TypeScriptGenerator.generateTypeScript(Arrays.asList(CustomOptionalUsage.class), settings, out);
            final String dts = out.toString();
            assertTrue(dts.contains("maybeObject?: SomeObject"));
        }
    }

    private static Type[] tryGetParameterizedTypeArguments(Type javaType, Class<?> requiredRawType) {
        if (javaType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) javaType;
            if (parameterizedType.getRawType() instanceof Class) {
                final Class<?> javaClass = (Class<?>) parameterizedType.getRawType();
                if (requiredRawType.isAssignableFrom(javaClass)) {
                    return parameterizedType.getActualTypeArguments();
                }
            }
        }
        return null;
    }

    public static class CustomOptionalUsage {
        public CustomOptional<SomeObject> maybeObject;
    }

    public static class SomeObject {
    }

    public static class CustomOptional<T> {
    }

}
