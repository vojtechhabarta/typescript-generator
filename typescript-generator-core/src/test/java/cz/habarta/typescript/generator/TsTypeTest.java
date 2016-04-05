package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.Symbol;
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
}
