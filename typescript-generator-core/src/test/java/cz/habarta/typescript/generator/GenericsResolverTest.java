
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.type.JTypeVariable;
import cz.habarta.typescript.generator.util.GenericsResolver;
import cz.habarta.typescript.generator.util.Utils;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class GenericsResolverTest {

    @Test
    public void testStringField() throws Exception {
        final Class<?> cls = F1String.class;
        final Type type = GenericsResolver.resolveField(cls, cls.getField("field"));
        Assertions.assertEquals(String.class, type);
    }

    static class F1<T> {
        public T field;
    }
    static class F1String extends F1<String> {
    }

    @Test
    public void testListOfStringField() throws Exception {
        final Class<?> cls = F2String.class;
        final Type type = GenericsResolver.resolveField(cls, cls.getField("list"));
        Assertions.assertEquals(Utils.createParameterizedType(List.class, String.class), type);
    }

    static class F2<T> {
        public List<T> list;
    }
    static class F2String extends F2<String> {
    }

    @Test
    public void testMapOfStringAndListOfLongField() throws Exception {
        final Class<?> cls = F3StringLong.class;
        final Type type = GenericsResolver.resolveField(cls, cls.getField("map"));
        Assertions.assertEquals(Utils.createParameterizedType(Map.class, String.class, Utils.createParameterizedType(List.class, Long.class)), type);
    }

    static class F3<K, V> {
        public Map<K, List<V>> map;
    }
    static class F3StringLong extends F3<String, Long> {
    }

    @Test
    public void testInheritancePath() throws Exception {
        final Class<?> cls = P123Number.class;
        final Type type = GenericsResolver.resolveField(cls, cls.getField("field"));
        Assertions.assertEquals(Utils.createParameterizedType(List.class, Number.class), type);
    }

    @Test
    public void testInheritancePathWithUnresolvedVariable1() throws Exception {
        final Class<?> cls = P123.class;
        final Type type = GenericsResolver.resolveField(cls, cls.getField("field"));
        Assertions.assertEquals(Utils.createParameterizedType(List.class, new JTypeVariable<>(P123.class, "B")), type);
    }

    @Test
    public void testInheritancePathWithUnresolvedVariable2() throws Exception {
        final Class<?> cls = P12.class;
        final Type type = GenericsResolver.resolveField(cls, cls.getField("field"));
        Assertions.assertEquals(new JTypeVariable<>(P12.class, "V"), type);
    }

    static class P1<T> {
        public T field;
    }
    static class P12<U, V> extends P1<V> {
    }
    static class P123<A, B, C> extends P12<C, List<B>> {
    }
    static class P123Number extends P123<String, Number, Boolean> {
    }

    @Test
    public void testGenericVariableMappingToBase1() {
        final List<String> mappedTypeParameters = GenericsResolver.mapGenericVariablesToBase(R123.class, R1.class);
        Assertions.assertEquals(Arrays.asList(null, null, "T"), mappedTypeParameters);
    }

    @Test
    public void testGenericVariableMappingToBase2() {
        final List<String> mappedTypeParameters = GenericsResolver.mapGenericVariablesToBase(R12.class, R1.class);
        Assertions.assertEquals(Arrays.asList("T", "S"), mappedTypeParameters);
    }

    static class R1<S, T> {
    }
    static class R12<U, V> extends R1<V, U> {
    }
    static class R123<A, B, C> extends R12<C, List<B>> {
    }

    @Test
    public void testResolvingGenericVariablesInContextType1() throws NoSuchFieldException {
        final Type contextType = MyClass.class.getField("property1").getGenericType();
        final List<Type> resolvedTypeParameters = GenericsResolver.resolveBaseGenericVariables(BaseClass.class, contextType);
        Assertions.assertEquals(Arrays.asList("java.lang.String", "java.lang.Integer"), getTypeNames(resolvedTypeParameters));
    }

    @Test
    public void testResolvingGenericVariablesInContextType3() throws NoSuchFieldException {
        final Type contextType = MyClass.class.getField("property3").getGenericType();
        final List<Type> resolvedTypeParameters = GenericsResolver.resolveBaseGenericVariables(BaseClass.class, contextType);
        Assertions.assertEquals(Arrays.asList("java.lang.Integer", "java.lang.Boolean"), getTypeNames(resolvedTypeParameters));
    }

    @Test
    public void testResolvingGenericVariablesInContextTypeBase() throws NoSuchFieldException {
        final Type contextType = MyClass.class.getField("propertyBase").getGenericType();
        final List<Type> resolvedTypeParameters = GenericsResolver.resolveBaseGenericVariables(BaseClass.class, contextType);
        Assertions.assertEquals(Arrays.asList("java.lang.Integer", "java.lang.String"), getTypeNames(resolvedTypeParameters));
    }

    static class BaseClass<A, B> {}

    static class SubClass1<B> extends BaseClass<String, B> {}

    static class SubClass3<X, Y, Z> extends BaseClass<Z, Y> {}

    static class MyClass {
        public SubClass1<Integer> property1;
        public SubClass3<String, Boolean, Integer> property3;
        public BaseClass<Integer, String> propertyBase;
    }

    @Test
    public void testResolvingRawUsage1() throws NoSuchFieldException {
        final Type contextType = RawUsage.class.getField("rawMap").getGenericType();
        final List<Type> resolvedTypeParameters = GenericsResolver.resolveBaseGenericVariables(Map.class, contextType);
        Assertions.assertEquals(Arrays.asList("java.lang.Object", "java.lang.Object"), getTypeNames(resolvedTypeParameters));
    }

    @Test
    public void testResolvingRawUsage2() throws NoSuchFieldException {
        final Type contextType = RawUsage.class.getField("rawStringKeyMap").getGenericType();
        final List<Type> resolvedTypeParameters = GenericsResolver.resolveBaseGenericVariables(Map.class, contextType);
        Assertions.assertEquals(Arrays.asList("java.lang.Object", "java.lang.Object"), getTypeNames(resolvedTypeParameters));
    }

    static class RawUsage {
        public Map rawMap;
        public StringKeyMap rawStringKeyMap;
    }

    static interface StringKeyMap<T> extends Map<String, T> {}


    @Test
    public void testResolvingFixedDescendant() throws NoSuchFieldException {
        final Type contextType = StringMapDescendantUsage.class.getField("stringMapDescendant").getGenericType();
        final List<Type> resolvedTypeParameters = GenericsResolver.resolveBaseGenericVariables(Map.class, contextType);
        Assertions.assertEquals(Arrays.asList("java.lang.String", "java.lang.String"), getTypeNames(resolvedTypeParameters));
    }

    static class StringMapDescendantUsage {
        public StringMapDescendant stringMapDescendant;
    }

    static interface StringMapDescendant extends StringMap {}

    static interface StringMap extends Map<String, String> {}

    private static List<String> getTypeNames(List<Type> types) {
        return types.stream().map(Type::getTypeName).collect(Collectors.toList());
    }

}
