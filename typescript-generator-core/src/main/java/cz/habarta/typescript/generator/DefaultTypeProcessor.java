
package cz.habarta.typescript.generator;

import java.lang.reflect.*;
import java.util.*;

import cz.habarta.typescript.generator.TsType.*;


public class DefaultTypeProcessor implements TypeProcessor {

    @Override
    public Result processType(Type javaType, Context context) {
        if (KnownTypes.containsKey(javaType)) return new Result(KnownTypes.get(javaType));
        if (javaType instanceof Class) {
            final Class<?> javaClass = (Class<?>) javaType;
            if (javaClass.isArray()) {
                final Result result = context.processType(javaClass.getComponentType());
                return new Result(new TsType.BasicArrayType(result.getTsType()), result.getDiscoveredClasses());
            }
            if (javaClass.isEnum()) {
                @SuppressWarnings("unchecked")
                final Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) javaClass;
                final List<java.lang.String> values = new ArrayList<>();
                for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
                    values.add(enumConstant.name());
                }
                return new Result(new TsType.EnumType(context.getMappedName(javaClass), values));
            }
            if (List.class.isAssignableFrom(javaClass)) {
                return new Result(new TsType.BasicArrayType(TsType.Any));
            }
            if (Map.class.isAssignableFrom(javaClass)) {
                return new Result(new TsType.IndexedArrayType(TsType.String, TsType.Any));
            }
            // consider it structural
            return new Result(new TsType.StructuralType(context.getMappedName(javaClass)), javaClass);
        }
        if (javaType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) javaType;
            if (parameterizedType.getRawType() instanceof Class) {
                final Class<?> javaClass = (Class<?>) parameterizedType.getRawType();
                if (List.class.isAssignableFrom(javaClass) || Set.class.isAssignableFrom(javaClass)) {
                    final Result result = context.processType(parameterizedType.getActualTypeArguments()[0]);
                    return new Result(new TsType.BasicArrayType(result.getTsType()), result.getDiscoveredClasses());
                } else if (Map.class.isAssignableFrom(javaClass)) {
                    final Result result = context.processType(parameterizedType.getActualTypeArguments()[1]);
                    return new Result(new TsType.IndexedArrayType(TsType.String, result.getTsType()), result.getDiscoveredClasses());
                } else {
                    // for example A<String, Integer>
                    List<TsType> genericInstances = new ArrayList<>();
                    List<Class<?>> discovered = new ArrayList<>();
                    for (Type type: parameterizedType.getActualTypeArguments()) {
                        Result result = context.processType(type);
                        discovered.addAll(result.getDiscoveredClasses());
                        genericInstances.add(result.getTsType());
                    }
                    Result baseResult = context.processType(parameterizedType.getRawType());
                    discovered.addAll(baseResult.getDiscoveredClasses());
                    return new Result(new GenericInstanceType(baseResult.getTsType(), genericInstances), discovered);
                }
            }
        }
        if (javaType instanceof TypeVariable) {
            // for example the generic "T" in List<T>
            return new Result(new GenericParamType(((TypeVariable<?>) javaType).getName()), new ArrayList<Class<?>>());
        }
        return null;
    }

    private static Map<Type, TsType> getKnownTypes() {
        final Map<Type, TsType> knownTypes = new LinkedHashMap<>();
        knownTypes.put(Object.class, TsType.Any);
        knownTypes.put(Byte.class, TsType.Number);
        knownTypes.put(Byte.TYPE, TsType.Number);
        knownTypes.put(Short.class, TsType.Number);
        knownTypes.put(Short.TYPE, TsType.Number);
        knownTypes.put(Integer.class, TsType.Number);
        knownTypes.put(Integer.TYPE, TsType.Number);
        knownTypes.put(Long.class, TsType.Number);
        knownTypes.put(Long.TYPE, TsType.Number);
        knownTypes.put(Float.class, TsType.Number);
        knownTypes.put(Float.TYPE, TsType.Number);
        knownTypes.put(Double.class, TsType.Number);
        knownTypes.put(Double.TYPE, TsType.Number);
        knownTypes.put(Boolean.class, TsType.Boolean);
        knownTypes.put(Boolean.TYPE, TsType.Boolean);
        knownTypes.put(Character.class, TsType.String);
        knownTypes.put(Character.TYPE, TsType.String);
        knownTypes.put(String.class, TsType.String);
        knownTypes.put(Date.class, TsType.Date);
        knownTypes.put(void.class, TsType.Void);
        return knownTypes;
    }

    private static final Map<Type, TsType> KnownTypes = getKnownTypes();

}
