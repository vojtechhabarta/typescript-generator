package cz.habarta.typescript.generator;

import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.util.*;

import org.junit.*;

import cz.habarta.typescript.generator.util.*;

public class ClassUtilsTest {

    @Test
    public void testGetGenericTypes() throws NoSuchMethodException, SecurityException {
        Type returnType = A.class.getMethod("x").getGenericReturnType();
        ParameterizedType parameterizedType = (ParameterizedType) returnType;
        List<String> actual = ClassUtils.getGenericDeclarationNames((Class<?>) parameterizedType.getRawType());
        assertEquals(1, actual.size());
        assertEquals("T", actual.get(0));
    }

    static class A<T> {
        public A<T> x() {
            return null;
        }
    }
}
