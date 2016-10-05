package cz.habarta.typescript.generator;

import static cz.habarta.typescript.generator.TsType.*;
import cz.habarta.typescript.generator.compiler.Symbol;
import java.util.Arrays;
import static org.junit.Assert.*;

import org.junit.*;

public class TsTypeTest {

    @Test
    public void testEquals() {
        assertEquals(new TsType.ReferenceType(new Symbol(new String("Foo"))), new TsType.ReferenceType(new Symbol("Foo")));
    }

    @Test
    public void testNotEquals() {
        assertNotEquals(new TsType.ReferenceType(new Symbol("Foo")), new TsType.ReferenceType(new Symbol("Bar")));
    }

    @Test
    public void testNotEqualsNull() {
        assertNotEquals(new TsType.ReferenceType(new Symbol("Foo")), null);
    }

    @Test
    public void testTypeParentheses() {
        final Settings settings = TestUtils.settings();
        assertEquals("string | number", new UnionType(Arrays.asList(String, Number)).format(settings));
        assertEquals("string | number[]", new UnionType(Arrays.asList(String, new BasicArrayType(Number))).format(settings));
        assertEquals("(string | number)[]", new BasicArrayType(new UnionType(Arrays.asList(String, Number))).format(settings));
        assertEquals("(string | number)[][]", new BasicArrayType(new BasicArrayType(new UnionType(Arrays.asList(String, Number)))).format(settings));
        assertEquals("{ [index: string]: string | number }", new IndexedArrayType(String, new UnionType(Arrays.asList(String, Number))).format(settings));
    }

}
