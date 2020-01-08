
package cz.habarta.typescript.generator.util;

import java.lang.reflect.Field;
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


public class GenericsResolver {

    public static Type resolveField(Class<?> cls, Field field) {
        final Type fieldType = field.getGenericType();
        return resolveType(cls, fieldType, field.getDeclaringClass());
    }

    public static Type resolveType(Class<?> contextClass, Type type, Class<?> declaringClass) {
        final List<ResolvedClass> path = traverseSomeInheritancePath(contextClass, declaringClass);
        final ResolvedClass resolvedClass = path != null && !path.isEmpty() ? path.get(0) : null;
        return resolvedClass != null ? resolvedClass.resolveType(type) : type;
    }

    public static List<String> mapGenericVariablesToBase(Class<?> derivedClass, Class<?> baseClass) {
        final List<ResolvedClass> path = traverseSomeInheritancePath(derivedClass, baseClass);
        if (path == null) {
            return null;
        }
        Collections.reverse(path);
        List<String> result = Arrays.stream(derivedClass.getTypeParameters())
                .map(TypeVariable::getName)
                .collect(Collectors.toList());
        for (ResolvedClass resolvedClass : path.subList(0, path.size())) {
            result = result.stream()
                    .map(typeVariableName -> mapGenericVariableToParent(typeVariableName, resolvedClass))
                    .collect(Collectors.toList());
        }
        return result;
    }

    private static String mapGenericVariableToParent(String typeVariableName, ResolvedClass resolvedParent) {
        if (typeVariableName == null) {
            return null;
        }
        for (int i = 0; i < resolvedParent.typeArguments.size(); i++) {
            final Type argument = resolvedParent.typeArguments.get(i);
            if (argument instanceof TypeVariable) {
                final TypeVariable<?> variable = (TypeVariable<?>) argument;
                if (Objects.equals(variable.getName(), typeVariableName)) {
                    return resolvedParent.rawClass.getTypeParameters()[i].getName();
                }
            }
        }
        return null;
    }

    private static List<ResolvedClass> traverseSomeInheritancePath(Class<?> descendant, Class<?> ancestor) {
        return traverseSomeInheritancePath(new ResolvedClass(descendant, null, null), ancestor);
    }

    private static List<ResolvedClass> traverseSomeInheritancePath(ResolvedClass descendant, Class<?> ancestor) {
        if (descendant.rawClass == ancestor) {
            return new ArrayList<>();
        }
        for (ResolvedClass directAncestor : descendant.getDirectAncestors()) {
            final List<ResolvedClass> path = traverseSomeInheritancePath(directAncestor, ancestor);
            if (path != null) {
                path.add(directAncestor);
                return path;
            }
        }
        return null;
    }

    private static class ResolvedClass {
        public final Class<?> rawClass;
        public final List<Type> typeArguments;
        public final Map<String, Type> resolvedTypeParameters;

        public ResolvedClass(Class<?> rawClass, List<Type> typeArguments, Map<String, Type> resolvedTypeParameters) {
            this.rawClass = rawClass;
            this.typeArguments = Utils.listFromNullable(typeArguments);
            this.resolvedTypeParameters = Utils.mapFromNullable(resolvedTypeParameters);
        }

        public List<ResolvedClass> getDirectAncestors() {
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
            final List<Type> arguments = rawClassAndTypeArguments.getValue2();
            final Map<String, Type> typeParameters = new LinkedHashMap<>();
            final int count = Math.min(typeVariables.size(), arguments.size());
            for (int i = 0; i < count; i++) {
                typeParameters.put(typeVariables.get(i).getName(), resolveType(arguments.get(i)));
            }
            return new ResolvedClass(cls, arguments, typeParameters);
        }

        private Type resolveType(Type type) {
            if (type instanceof TypeVariable) {
                final TypeVariable<?> typeVariable = (TypeVariable<?>) type;
                return resolvedTypeParameters.getOrDefault(typeVariable.getName(), typeVariable);
            }
            return Utils.transformContainedTypes(type, this::resolveType);
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
