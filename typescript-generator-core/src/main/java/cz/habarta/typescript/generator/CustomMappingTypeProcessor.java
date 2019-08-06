
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.util.Pair;
import cz.habarta.typescript.generator.util.Utils;
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
        final Pair<Class<?>, List<Type>> rawClassAndTypeArguments = Utils.getRawClassAndTypeArguments(javaType);
        if (rawClassAndTypeArguments == null) {
            return null;
        }
        final Class<?> rawClass = rawClassAndTypeArguments.getValue1();
        final List<Type> typeArguments = rawClassAndTypeArguments.getValue2();
        final Settings.CustomTypeMapping mapping = customMappings.get(rawClass.getName());
        if (mapping == null) {
            return null;
        }

        final List<Class<?>> discoveredClasses = new ArrayList<>();
        final Function<Integer, TsType> processGenericParameter = index -> {
            final Type typeArgument = typeArguments.get(index);
            final TypeProcessor.Result typeArgumentResult = context.processType(typeArgument);
            discoveredClasses.addAll(typeArgumentResult.getDiscoveredClasses());
            return typeArgumentResult.getTsType();
        };
        if (mapping.tsType.typeParameters != null) {
            final List<TsType> tsTypeArguments = new ArrayList<>();
            for (String typeParameter : mapping.tsType.typeParameters) {
                final TsType tsType;
                final int index = mapping.javaType.indexOfTypeParameter(typeParameter);
                if (index != -1) {
                    tsType = processGenericParameter.apply(index);
                } else {
                    tsType = new TsType.VerbatimType(typeParameter);
                }
                tsTypeArguments.add(tsType);
            }
            return new Result(new TsType.GenericBasicType(mapping.tsType.rawName, tsTypeArguments), discoveredClasses);
        } else {
            final int index = mapping.javaType.indexOfTypeParameter(mapping.tsType.rawName);
            if (index != -1) {
                final TsType tsType = processGenericParameter.apply(index);
                return new Result(tsType, discoveredClasses);
            } else {
                return new Result(new TsType.BasicType(mapping.tsType.rawName), discoveredClasses);
            }
        }
    }

}
