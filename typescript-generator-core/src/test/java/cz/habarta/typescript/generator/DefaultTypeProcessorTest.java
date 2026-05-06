
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.SymbolTable;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;


@SuppressWarnings("unused")
public class DefaultTypeProcessorTest {

    @Test
    public void testTypeConversion() {
        final TypeProcessor converter = new DefaultTypeProcessor();
        final TypeProcessor.Context context = getTestContext(converter);
        assertThat(processType(converter, context, A.class).toString()).isEqualTo(context.getSymbol(A.class).getFullName());
        assertThat(processType(converter, context, B.class).toString()).isEqualTo(context.getSymbol(B.class).getFullName());
        assertThat(processType(converter, context, void.class)).isEqualTo(TsType.Void);
        assertThat(processType(converter, context, BigDecimal.class)).isEqualTo(TsType.Number);
        assertThat(processType(converter, context, UUID.class)).isEqualTo(TsType.String);
        assertThat(processType(converter, context, OptionalInt.class)).isEqualTo(TsType.Number.optional());
        assertThat(processType(converter, context, OptionalLong.class)).isEqualTo(TsType.Number.optional());
        assertThat(processType(converter, context, OptionalDouble.class)).isEqualTo(TsType.Number.optional());
    }

    @Test
    public void testWildcards() throws NoSuchFieldException {
        final TypeProcessor converter = new DefaultTypeProcessor();
        final TypeProcessor.Context context = getTestContext(converter);
        assertThat(processType(converter, context, C.class.getDeclaredField("x").getGenericType()).toString()).isEqualTo("string[]");
        assertThat(processType(converter, context, C.class.getDeclaredField("y").getGenericType()).toString()).isEqualTo("any[]");
        assertThat(processType(converter, context, C.class.getDeclaredField("z").getGenericType()).toString()).isEqualTo("any[]");
    }

    @SuppressWarnings("NullAway.Init")
    private static class A {
        B x;
    }

    @SuppressWarnings("NullAway.Init")
    private static class B {
        B x;
    }

    @SuppressWarnings("NullAway.Init")
    private static class C {
        List<? extends String> x;
        List<? super String> y;
        List<?> z;
    }

    public static TypeProcessor.Context getTestContext(final TypeProcessor typeProcessor) {
        return new TypeProcessor.Context(new SymbolTable(TestUtils.settings()), typeProcessor, null);
    }

    @Test
    public void testRawTypes() {
        final String output = new TypeScriptGenerator(TestUtils.settings()).generateTypeScript(Input.from(DummyBean.class));
        Assertions.assertTrue(output.contains("rawListProperty: any[]"));
        Assertions.assertTrue(output.contains("rawMapProperty: { [index: string]: any }"));
    }

    private static TsType processType(TypeProcessor converter, TypeProcessor.Context context, Type javaType) {
        return requireNonNull(converter.processType(javaType, context)).getTsType();
    }
}
