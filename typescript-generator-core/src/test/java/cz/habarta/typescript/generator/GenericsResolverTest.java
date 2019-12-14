
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.util.GenericsResolver;
import cz.habarta.typescript.generator.util.Utils;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.Assert;
import org.junit.Test;


public class GenericsResolverTest {

    @Test
    public void testStringField() throws Exception {
        final Class<?> cls = F1String.class;
        final Type type = GenericsResolver.resolveField(cls, cls.getField("field"));
        Assert.assertEquals(String.class, type);
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
        Assert.assertEquals(Utils.createParameterizedType(List.class, String.class), type);
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
        Assert.assertEquals(Utils.createParameterizedType(Map.class, String.class, Utils.createParameterizedType(List.class, Long.class)), type);
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
        Assert.assertEquals(Utils.createParameterizedType(List.class, Number.class), type);
    }

    @Test
    public void testInheritancePathWithUnresolvedVariable1() throws Exception {
        final Class<?> cls = P123.class;
        final Type type = GenericsResolver.resolveField(cls, cls.getField("field"));
        Assert.assertEquals(Utils.createParameterizedType(List.class, createTypeVariable(P123.class, "B")), type);
    }

    @Test
    public void testInheritancePathWithUnresolvedVariable2() throws Exception {
        final Class<?> cls = P12.class;
        final Type type = GenericsResolver.resolveField(cls, cls.getField("field"));
        Assert.assertEquals(createTypeVariable(P12.class, "V"), type);
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
        Assert.assertEquals(Arrays.asList(null, null, "T"), mappedTypeParameters);
    }

    @Test
    public void testGenericVariableMappingToBase2() {
        final List<String> mappedTypeParameters = GenericsResolver.mapGenericVariablesToBase(R12.class, R1.class);
        Assert.assertEquals(Arrays.asList("T", "S"), mappedTypeParameters);
    }

    static class R1<S, T> {
    }
    static class R12<U, V> extends R1<V, U> {
    }
    static class R123<A, B, C> extends R12<C, List<B>> {
    }

    // this returns incomplete implementation just for tests
    private static <D extends GenericDeclaration> TypeVariable<D> createTypeVariable(D genericDeclaration, String name) {
        final Type[] bounds = new Type[0];
        final AnnotatedType[] annotatedBounds = new AnnotatedType[0];
        final Annotation[] annotations = new Annotation[0];
        return new TypeVariable<D>() {
            @Override
            public Type[] getBounds() {
                return bounds;
            }

            @Override
            public D getGenericDeclaration() {
                return genericDeclaration;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public AnnotatedType[] getAnnotatedBounds() {
                return annotatedBounds;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                for (Annotation annotation : getAnnotations()) {
                    if (annotationClass.isInstance(annotation)) {
                        return (T) annotation;
                    }
                }
                return null;
            }

            @Override
            public Annotation[] getAnnotations() {
                return annotations;
            }

            @Override
            public Annotation[] getDeclaredAnnotations() {
                return getAnnotations();
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj instanceof TypeVariable) {
                    final TypeVariable<?> that = (TypeVariable<?>) obj;
                    return
                        Objects.equals(genericDeclaration, that.getGenericDeclaration()) &&
                        Objects.equals(name, that.getName());
                } else {
                    return false;
                }
            }

            @Override
            public int hashCode() {
                return genericDeclaration.hashCode() ^ name.hashCode();
            }

            @Override
            public String toString() {
                return getName();
            }

        };
    }

}
