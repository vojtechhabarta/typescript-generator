package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.SymbolTable;
import io.vavr.control.Option;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

@SuppressWarnings("unused")
public class DefaultTypeProcessorTest {

    private final TypeProcessor converter = new DefaultTypeProcessor();
    final TypeProcessor.Context context = getTestContext(converter);

    @Test
    public void testTypeConversion() {
        assertEquals(context.getSymbol(A.class).getFullName(), converter.processType(A.class, context).getTsType().toString());
        assertEquals(context.getSymbol(B.class).getFullName(), converter.processType(B.class, context).getTsType().toString());
        assertEquals(TsType.Void, converter.processType(void.class, context).getTsType());
        assertEquals(TsType.Number, converter.processType(BigDecimal.class, context).getTsType());
        assertEquals(TsType.String, converter.processType(UUID.class, context).getTsType());
        assertEquals(TsType.Number.optional(), converter.processType(OptionalInt.class, context).getTsType());
        assertEquals(TsType.Number.optional(), converter.processType(OptionalLong.class, context).getTsType());
        assertEquals(TsType.Number.optional(), converter.processType(OptionalDouble.class, context).getTsType());
    }

    @Test
    public void testWildcards() throws NoSuchFieldException {
        assertEquals("string[]", typeOf("x", C.class, converter, context).toString());
        assertEquals("any[]", typeOf("y", C.class, converter, context).toString());
        assertEquals("any[]", typeOf("z", C.class, converter, context).toString());
    }

    @Test
    public void testVavrTypes() throws NoSuchFieldException {
        assertEquals("string[]", typeOf("x", D.class, converter, context).toString());
        assertEquals("{ [index: string]: number }", typeOf("y", D.class, converter, context).toString());
        assertEquals(TsType.Number.optional(), typeOf("z", D.class, converter, context));
    }

    private TsType typeOf(String z, Class<?> clazz, TypeProcessor converter, TypeProcessor.Context context) throws NoSuchFieldException {
        return converter.processType(clazz.getDeclaredField(z).getGenericType(), context).getTsType();
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

    private static class D {
        io.vavr.collection.List<String> x;
        io.vavr.collection.Map<String, BigInteger> y;
        Option<BigDecimal> z;
    }

    public static TypeProcessor.Context getTestContext(final TypeProcessor typeProcessor) {
        return new TypeProcessor.Context(new SymbolTable(TestUtils.settings()), typeProcessor, null);
    }

}
