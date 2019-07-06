
package cz.habarta.typescript.generator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


public class CustomMappingTypeProcessor implements TypeProcessor {

    private final Map<String, Settings.CustomTypeMapping> customMappings;

    public CustomMappingTypeProcessor(List<Settings.CustomTypeMapping> customMappings) {
        this.customMappings = customMappings.stream().collect(Collectors.toMap(
                mapping -> mapping.javaType.rawName,
                mapping -> mapping));
    }

    @Override
    public Result processType(Type javaType, Context context) {
        if (javaType instanceof Class) {
            final Class<?> javaClass = (Class<?>) javaType;
            final Settings.CustomTypeMapping mapping = customMappings.get(javaClass.getName());
            if (mapping != null) {
                return new Result(new TsType.BasicType(mapping.tsType.rawName));
            }
        }
        if (javaType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) javaType;
            if (parameterizedType.getRawType() instanceof Class) {
                final Class<?> javaClass = (Class<?>) parameterizedType.getRawType();
                final Settings.CustomTypeMapping mapping = customMappings.get(javaClass.getName());
                if (mapping != null) {
                    final List<Class<?>> discoveredClasses = new ArrayList<>();
                    final Function<Integer, TsType> processGenericParameter = index -> {
                        final Type typeArgument = parameterizedType.getActualTypeArguments()[index];
                        final TypeProcessor.Result typeArgumentResult = context.processType(typeArgument);
                        discoveredClasses.addAll(typeArgumentResult.getDiscoveredClasses());
                        return typeArgumentResult.getTsType();
                    };
                    if (mapping.tsType.typeParameters != null) {
                        final List<TsType> tsTypeArguments = new ArrayList<>();
                        for (String typeParameter : mapping.tsType.typeParameters) {
                            final int index = mapping.javaType.typeParameters.indexOf(typeParameter);
                            final TsType tsType = processGenericParameter.apply(index);
                            tsTypeArguments.add(tsType);
                        }
                        return new Result(new TsType.GenericBasicType(mapping.tsType.rawName, tsTypeArguments), discoveredClasses);
                    } else {
                        final int index = mapping.javaType.typeParameters.indexOf(mapping.tsType.rawName);
                        if (index != -1) {
                            final TsType tsType = processGenericParameter.apply(index);
                            return new Result(tsType, discoveredClasses);
                        } else {
                            return new Result(new TsType.BasicType(mapping.tsType.rawName), discoveredClasses);
                        }
                    }
                }
            }
        }
        return null;
    }

}
