
package cz.habarta.typescript.generator;

import java.lang.reflect.*;
import java.util.*;


public class GenericsTypeProcessor implements TypeProcessor {

    @Override
    public TypeProcessor.Result processType(Type javaType, TypeProcessor.Context context) {
        if (javaType instanceof TypeVariable) {
            final TypeVariable<?> typeVariable = (TypeVariable) javaType;
            return new Result(new GenericVariableType(typeVariable.getName()));
        }
        if (javaType instanceof Class) {
            final Class<?> javaClass = (Class<?>) javaType;
            if (javaClass.getTypeParameters().length > 0) {
                return processGenericClass(javaClass, javaClass.getTypeParameters(), context);
            }
        }
        if (javaType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) javaType;
            if (parameterizedType.getRawType() instanceof Class) {
                final Class<?> javaClass = (Class<?>) parameterizedType.getRawType();
                return processGenericClass(javaClass, parameterizedType.getActualTypeArguments(), context);
            }
        }
        return null;
    }

    private Result processGenericClass(Class<?> rawType, Type[] typeArguments, TypeProcessor.Context context) {
        if (!Collection.class.isAssignableFrom(rawType) && !Map.class.isAssignableFrom(rawType)) {
            final List<Class<?>> discoveredClasses = new ArrayList<>();
            // raw type
            final String rawTsTypeName = context.getMappedName(rawType);
            discoveredClasses.add(rawType);
            // type arguments
            final List<TsType> tsTypeArguments = new ArrayList<>();
            for (Type typeArgument : typeArguments) {
                final TypeProcessor.Result typeArgumentResult = context.processType(typeArgument);
                tsTypeArguments.add(typeArgumentResult.getTsType());
                discoveredClasses.addAll(typeArgumentResult.getDiscoveredClasses());
            }
            // result
            final GenericStructuralType type = new GenericStructuralType(rawTsTypeName, tsTypeArguments);
            return new Result(type, discoveredClasses);
        }
        return null;
    }

    private static class GenericStructuralType extends TsType.StructuralType {

        public final List<TsType> typeArguments;

        public GenericStructuralType(String name, List<TsType> typeArguments) {
            super(name);
            this.typeArguments = typeArguments;
        }

        @Override
        public String toString() {
            return name + "<" + ModelCompiler.join(typeArguments, ", ") + ">";
        }
    }
    
    private static class GenericVariableType extends TsType.BasicType {

        public GenericVariableType(String name) {
            super(name);
        }

    }

}
