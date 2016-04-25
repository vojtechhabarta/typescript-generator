package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.SymbolTable;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class DefaultTypeProcessorTest {

    @Test
    public void testTypeConversion() {
        TypeProcessor converter = new DefaultTypeProcessor();
        final TypeProcessor.Context context = getTestContext(converter);
        assertEquals(context.getSymbol(A.class).toString(), converter.processType(A.class, context).getTsType().toString());
        assertEquals(context.getSymbol(B.class).toString(), converter.processType(B.class, context).getTsType().toString());
        assertEquals(TsType.Void, converter.processType(void.class, context).getTsType());
        assertEquals(TsType.Number, converter.processType(BigDecimal.class, context).getTsType());
        assertEquals(TsType.String, converter.processType(UUID.class, context).getTsType());
        assertEquals(TsType.Date, converter.processType(ZonedDateTime.class, context).getTsType());
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
        return new TypeProcessor.Context(new SymbolTable(TestUtils.settings()), typeProcessor);
    }

}
