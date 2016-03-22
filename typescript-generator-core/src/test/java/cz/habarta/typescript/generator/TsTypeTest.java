package cz.habarta.typescript.generator;

import static org.junit.Assert.*;

import org.junit.*;

public class TsTypeTest {

    @Test
    public void testEquals() {
        assertEquals(new TsType.ReferenceType(new String("Foo")), new TsType.ReferenceType("Foo"));
    }

    @Test
    public void testNotEquals() {
        assertNotEquals(new TsType.ReferenceType("Foo"), new TsType.ReferenceType("Bar"));
    }

    @Test
    public void testNotEqualsNull() {
        assertNotEquals(new TsType.ReferenceType("Foo"), null);
    }
}
