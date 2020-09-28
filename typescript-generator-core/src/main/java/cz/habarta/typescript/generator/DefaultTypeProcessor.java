
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.Symbol;
import cz.habarta.typescript.generator.type.JTypeWithNullability;
import cz.habarta.typescript.generator.type.JUnionType;
import cz.habarta.typescript.generator.util.Utils;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;


public class DefaultTypeProcessor implements TypeProcessor {

    private final LoadedDataLibraries known;

    public DefaultTypeProcessor() {
        this(null);
    }

    public DefaultTypeProcessor(LoadedDataLibraries dataLibraries) {
        this.known = LoadedDataLibraries.join(getKnownClasses(), dataLibraries);
    }

    private static boolean isAssignableFrom(List<Class<?>> classes, Class<?> cls) {
        return classes.stream().anyMatch(c -> c.isAssignableFrom(cls));
    }

    @Override
    public Result processType(Type javaType, Context context) {
        if (Objects.equals(javaType, Object.class)) {
            return new Result(TsType.Any);
        }
        if (javaType instanceof Class) {
            final Class<?> javaClass = (Class<?>) javaType;
            if (isAssignableFrom(known.stringClasses, javaClass)) {
                return new Result(TsType.String);
            }
            if (isAssignableFrom(known.numberClasses, javaClass)) {
                return new Result(TsType.Number);
            }
            if (isAssignableFrom(known.booleanClasses, javaClass)) {
                return new Result(TsType.Boolean);
            }
            if (isAssignableFrom(known.dateClasses, javaClass)) {
                return new Result(TsType.Date);
            }
            if (isAssignableFrom(known.voidClasses, javaClass)) {
                return new Result(TsType.Void);
            }
        }
        if (javaType instanceof Class) {
            final Class<?> javaClass = (Class<?>) javaType;
            final Symbol importedSymbol = context.getSymbolIfImported(javaClass);
            if (importedSymbol != null) {
                return new Result(new TsType.ReferenceType(importedSymbol));
            }
        }
        if (javaType instanceof Class) {
            final Class<?> javaClass = (Class<?>) javaType;
            if (isAssignableFrom(known.anyClasses, javaClass)) {
                return new Result(TsType.Any);
            }
            if (javaClass.isArray()) {
                final Result result = context.processTypeInsideCollection(javaClass.getComponentType());
                return new Result(new TsType.BasicArrayType(result.getTsType()), result.getDiscoveredClasses());
            }
            if (javaClass.isEnum()) {
                return new Result(new TsType.EnumReferenceType(context.getSymbol(javaClass)), javaClass);
            }
            if (isAssignableFrom(known.listClasses, javaClass)) {
                final Result result = context.processTypeInsideCollection(Object.class);
                return new Result(new TsType.BasicArrayType(result.getTsType()), result.getDiscoveredClasses());
            }
            if (isAssignableFrom(known.mapClasses, javaClass)) {
                return processMapType(String.class, Object.class, context);
            }
            if (OptionalInt.class.isAssignableFrom(javaClass) ||
                    OptionalLong.class.isAssignableFrom(javaClass) ||
                    OptionalDouble.class.isAssignableFrom(javaClass)) {
                return new Result(TsType.Number.optional());
            }
            if (isAssignableFrom(known.wrapperClasses, javaClass)) {
                return new Result(TsType.Any);
            }
            // generic structural type used without type arguments
            if (javaClass.getTypeParameters().length > 0) {
                final List<TsType> tsTypeArguments = new ArrayList<>();
                for (int i = 0; i < javaClass.getTypeParameters().length; i++) {
                    tsTypeArguments.add(TsType.Any);
                }
                return new Result(new TsType.GenericReferenceType(context.getSymbol(javaClass), tsTypeArguments));
            }
            // structural type
            return new Result(new TsType.ReferenceType(context.getSymbol(javaClass)), javaClass);
        }
        if (javaType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) javaType;
            if (parameterizedType.getRawType() instanceof Class) {
                final Class<?> javaClass = (Class<?>) parameterizedType.getRawType();
                if (isAssignableFrom(known.listClasses, javaClass)) {
                    final Result result = context.processTypeInsideCollection(parameterizedType.getActualTypeArguments()[0]);
                    return new Result(new TsType.BasicArrayType(result.getTsType()), result.getDiscoveredClasses());
                }
                if (isAssignableFrom(known.mapClasses, javaClass)) {
                    return processMapType(parameterizedType.getActualTypeArguments()[0], parameterizedType.getActualTypeArguments()[1], context);
                }
                if (isAssignableFrom(known.optionalClasses, javaClass)) {
                    final Result result = context.processType(parameterizedType.getActualTypeArguments()[0]);
                    return new Result(result.getTsType().optional(), result.getDiscoveredClasses());
                }
                if (isAssignableFrom(known.wrapperClasses, javaClass)) {
                    final Result result = context.processType(parameterizedType.getActualTypeArguments()[0]);
                    return new Result(result.getTsType(), result.getDiscoveredClasses());
                }
                // generic structural type
                final List<Class<?>> discoveredClasses = new ArrayList<>();
                discoveredClasses.add(javaClass);
                final List<TsType> tsTypeArguments = new ArrayList<>();
                for (Type typeArgument : parameterizedType.getActualTypeArguments()) {
                    final TypeProcessor.Result typeArgumentResult = context.processType(typeArgument);
                    tsTypeArguments.add(typeArgumentResult.getTsType());
                    discoveredClasses.addAll(typeArgumentResult.getDiscoveredClasses());
                }
                return new Result(new TsType.GenericReferenceType(context.getSymbol(javaClass), tsTypeArguments), discoveredClasses);
            }
        }
        if (javaType instanceof GenericArrayType) {
            final GenericArrayType genericArrayType = (GenericArrayType) javaType;
            final Result result = context.processTypeInsideCollection(genericArrayType.getGenericComponentType());
            return new Result(new TsType.BasicArrayType(result.getTsType()), result.getDiscoveredClasses());
        }
        if (javaType instanceof TypeVariable) {
            final TypeVariable<?> typeVariable = (TypeVariable<?>) javaType;
            if (typeVariable.getGenericDeclaration() instanceof Method) {
                // example method: public <T extends Number> T getData();
                return context.processType(typeVariable.getBounds()[0]);
            }
            return new Result(new TsType.GenericVariableType(typeVariable.getName()));
        }
        if (javaType instanceof WildcardType) {
            final WildcardType wildcardType = (WildcardType) javaType;
            final Type[] upperBounds = wildcardType.getUpperBounds();
            return upperBounds.length > 0
                    ? context.processType(upperBounds[0])
                    : new Result(TsType.Any);
        }
        if (javaType instanceof JUnionType) {
            final JUnionType unionType = (JUnionType) javaType;
            final List<Result> results = unionType.getTypes().stream()
                    .map(type -> context.processType(type))
                    .collect(Collectors.toList());
            return new Result(
                    new TsType.UnionType(results.stream()
                            .map(result -> result.getTsType())
                            .collect(Collectors.toList())),
                    results.stream()
                            .flatMap(result -> result.getDiscoveredClasses().stream())
                            .collect(Collectors.toList())
            );
        }
        if (javaType instanceof JTypeWithNullability) {
            final JTypeWithNullability typeWithNullability = (JTypeWithNullability) javaType;
            final Result result = context.processType(typeWithNullability.getType());
            return new Result(
                    typeWithNullability.isNullable() ? new TsType.NullableType(result.getTsType()) : result.getTsType(),
                    result.getDiscoveredClasses()
            );
        }
        return null;
    }

    private Result processMapType(Type keyType, Type valueType, Context context) {
        final Result keyResult = context.processType(keyType);
        final Result valueResult = context.processTypeInsideCollection(valueType);
        final TsType valueTsType = valueResult.getTsType();
        if (keyResult.getTsType() instanceof TsType.EnumReferenceType) {
            return new Result(
                    new TsType.MappedType(keyResult.getTsType(), TsType.MappedType.QuestionToken.Question, valueTsType),
                    Utils.concat(keyResult.getDiscoveredClasses(), valueResult.getDiscoveredClasses())
            );
        } else {
            return new Result(
                    new TsType.IndexedArrayType(TsType.String, valueTsType),
                    valueResult.getDiscoveredClasses()
            );
        }
    }

    private static LoadedDataLibraries getKnownClasses() {
        return new LoadedDataLibraries(
            Arrays.asList(char.class, Character.class, String.class, UUID.class),
            Arrays.asList(byte.class, short.class, int.class, long.class, float.class, double.class, Number.class),
            Arrays.asList(boolean.class, Boolean.class),
            Arrays.asList(Date.class, Calendar.class, Temporal.class),
            Arrays.asList(),
            Arrays.asList(void.class, Void.class),
            Arrays.asList(Collection.class),
            Arrays.asList(Map.class),
            Arrays.asList(Optional.class),
            Arrays.asList(JAXBElement.class),
            Arrays.asList(),
            Arrays.asList()
        );
    }

}
