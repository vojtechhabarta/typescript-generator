package cz.habarta.typescript.generator;

import static org.junit.Assert.*;

import org.junit.*;

public class TsTypeTest {

    @Test
    public void testEquals() {
        assertEquals(new TsType.StructuralType(new String("Foo")), new TsType.StructuralType("Foo"));
    }

    @Test
    public void testNotEquals() {
        assertNotEquals(new TsType.StructuralType("Foo"), new TsType.StructuralType("Bar"));
    }

    @Test
    public void testNotEqualsNull() {
        assertNotEquals(new TsType.StructuralType("Foo"), null);
    }
}
