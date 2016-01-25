package cz.habarta.typescript.generator;

import java.lang.reflect.*;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DefaultTypeProcessorTest {

    @Test
    public void testTypeConversion() {
        TypeProcessor converter = new DefaultTypeProcessor();
        final TypeProcessor.Context context = getTestContext(converter);
        assertEquals("A", converter.processType(A.class, context).getTsType().toString());
        assertEquals("B", converter.processType(B.class, context).getTsType().toString());
        assertEquals(TsType.Void, converter.processType(void.class, context).getTsType());
    }

    @Test
    public void testWildcards() throws NoSuchFieldException {
        TypeProcessor converter = new DefaultTypeProcessor();
        final TypeProcessor.Context context = getTestContext(converter);
        assertEquals("string[]", converter.processType(C.class.getDeclaredField("x").getGenericType(), context).getTsType().toString());
        assertEquals("any[]", converter.processType(C.class.getDeclaredField("y").getGenericType(), context).getTsType().toString());
        assertEquals("any[]", converter.processType(C.class.getDeclaredField("z").getGenericType(), context).getTsType().toString());
    }

    private static class A {
        B x;
    }

    private static class B {
        B x;
    }

    private static class C {
        List<? extends String> x;
        List<? super String> y;
        List<?> z;
    }

    public static TypeProcessor.Context getTestContext(final TypeProcessor typeProcessor) {
        return new TypeProcessor.Context() {
            @Override
            public String getMappedName(Class<?> cls) {
                return cls.getSimpleName();
            }
            @Override
            public TypeProcessor.Result processType(Type javaType) {
                return typeProcessor.processType(javaType, this);
            }
        };
    }

}
