package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.TsType.BasicArrayType;
import cz.habarta.typescript.generator.TsType.IndexedArrayType;
import static cz.habarta.typescript.generator.TsType.Number;
import cz.habarta.typescript.generator.TsType.ObjectType;
import static cz.habarta.typescript.generator.TsType.String;
import cz.habarta.typescript.generator.TsType.UnionType;
import cz.habarta.typescript.generator.compiler.Symbol;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.junit.Test;

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

    @Test
    public void testRecordType() {
        final Settings settings = TestUtils.settings();
        settings.useRecordType = true;
        assertEquals("Record<string, number>", new IndexedArrayType(String, Number).format(settings));

    }

    @Test
    public void testObjectType() {
        final Settings settings = TestUtils.settings();
        assertEquals("{ a: string; b: string | number; c: {}; d: { x: string; }; }", new TsType.ObjectType(Arrays.asList(
                new TsProperty("a", String),
                new TsProperty("b", new UnionType(Arrays.asList(String, Number))),
                new TsProperty("c", new ObjectType(Arrays.<TsProperty>asList())),
                new TsProperty("d", new ObjectType(Arrays.asList(
                        new TsProperty("x", String)
                )))
        )).format(settings));
    }

}
