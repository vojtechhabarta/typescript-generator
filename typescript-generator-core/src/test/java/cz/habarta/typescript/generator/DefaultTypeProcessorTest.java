package cz.habarta.typescript.generator;

import java.lang.reflect.*;
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

    private static class A {
        B x;
    }

    private static class B {
        B x;
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
