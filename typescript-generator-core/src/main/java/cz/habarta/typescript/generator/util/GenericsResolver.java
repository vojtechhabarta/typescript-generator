
package cz.habarta.typescript.generator.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class GenericsResolver {

    public static Type resolveField(Class<?> cls, Field field) {
        final Type fieldType = field.getGenericType();
        return resolveType(cls, fieldType, field.getDeclaringClass());
    }

    public static Type resolveType(Class<?> contextClass, Type type, Class<?> declaringClass) {
        final ResolvedClass resolvedClass = traverseSomeInheritancePath(new ResolvedClass(contextClass, Collections.emptyMap()), declaringClass);
        return resolvedClass != null ? resolvedClass.resolveType(type) : type;
    }

    private static ResolvedClass traverseSomeInheritancePath(ResolvedClass descendant, Class<?> ancestor) {
        if (descendant.rawClass == ancestor) {
            return descendant;
        }
        for (ResolvedClass directAncestor : descendant.getDirectAncestors()) {
            final ResolvedClass resolvedClass = traverseSomeInheritancePath(directAncestor, ancestor);
            if (resolvedClass != null) {
                return resolvedClass;
            }
        }
        return null;
    }

    private static class ResolvedClass {
        public final Class<?> rawClass;
        public final Map<String, Type> resolvedTypeParameters;

        public ResolvedClass(Class<?> rawClass, Map<String, Type> resolvedTypeParameters) {
            this.rawClass = rawClass;
            this.resolvedTypeParameters = resolvedTypeParameters != null ? resolvedTypeParameters : Collections.emptyMap();
        }

        private List<ResolvedClass> getDirectAncestors() {
            final List<Type> ancestors = new ArrayList<>();
            final Class<?> cls = rawClass;
            if (cls.getSuperclass() != null) {
                ancestors.add(cls.getGenericSuperclass());
            }
            ancestors.addAll(Arrays.asList(cls.getGenericInterfaces()));
            return ancestors.stream()
                    .map(this::resolveAncestor)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        public ResolvedClass resolveAncestor(Type ancestor) {
            final Pair<Class<?>, List<Type>> rawClassAndTypeArguments = Utils.getRawClassAndTypeArguments(ancestor);
            final Class<?> cls = rawClassAndTypeArguments.getValue1();
            final List<TypeVariable<?>> typeVariables = Arrays.asList(cls.getTypeParameters());
            final List<Type> typeArguments = rawClassAndTypeArguments.getValue2();
            final Map<String, Type> typeParameters = new LinkedHashMap<>();
            final int count = Math.min(typeVariables.size(), typeArguments.size());
            for (int i = 0; i < count; i++) {
                typeParameters.put(typeVariables.get(i).getName(), resolveType(typeArguments.get(i)));
            }
            return new ResolvedClass(cls, typeParameters);
        }

        private Type resolveType(Type type) {
            if (type instanceof TypeVariable) {
                final TypeVariable<?> typeVariable = (TypeVariable<?>) type;
                return resolvedTypeParameters.getOrDefault(typeVariable.getName(), typeVariable);
            }
            if (type instanceof ParameterizedType) {
                final ParameterizedType parameterizedType = (ParameterizedType) type;
                return Utils.createParameterizedType(
                        parameterizedType.getRawType(),
                        Stream.of(parameterizedType.getActualTypeArguments())
                                .map(typeArgument -> resolveType(typeArgument))
                                .collect(Collectors.toList())
                );
            }
            return type;
        }

        @Override
        public String toString() {
            return rawClass.getName()
                    + "<"
                    + resolvedTypeParameters.entrySet().stream()
                            .map(entry -> entry.getKey() + "=" + entry.getValue().getTypeName())
                            .collect(Collectors.joining(", "))
                    + ">";
        }
    }

}
