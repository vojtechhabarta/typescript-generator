
package cz.habarta.typescript.generator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;


public class CustomMappingTypeProcessor implements TypeProcessor {

    private final Map<String, String> customMappings;

    public CustomMappingTypeProcessor(Map<String, String> customMappings) {
        this.customMappings = customMappings;
    }

    @Override
    public Result processType(Type javaType, Context context) {
        if (javaType instanceof Class) {
            final Class<?> javaClass = (Class<?>) javaType;
            final String tsTypeName = customMappings.get(javaClass.getName());
            if (tsTypeName != null) {
                return new Result(new TsType.BasicType(tsTypeName));
            }
        }
        if (javaType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) javaType;
            if (parameterizedType.getRawType() instanceof Class) {
                final Class<?> javaClass = (Class<?>) parameterizedType.getRawType();
                final String tsTypeName = customMappings.get(javaClass.getName());
                if (tsTypeName != null) {
                    final List<Class<?>> discoveredClasses = new ArrayList<>();
                    final List<TsType> tsTypeArguments = new ArrayList<>();
                    for (Type typeArgument : parameterizedType.getActualTypeArguments()) {
                        final TypeProcessor.Result typeArgumentResult = context.processType(typeArgument);
                        tsTypeArguments.add(typeArgumentResult.getTsType());
                        discoveredClasses.addAll(typeArgumentResult.getDiscoveredClasses());
                    }
                    return new Result(new TsType.GenericBasicType(tsTypeName, tsTypeArguments), discoveredClasses);
                }
            }
        }
        return null;
    }

}
