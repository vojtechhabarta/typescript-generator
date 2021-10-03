
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.Symbol;
import cz.habarta.typescript.generator.type.JTypeWithNullability;
import cz.habarta.typescript.generator.type.JUnionType;
import cz.habarta.typescript.generator.util.GenericsResolver;
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
        return assignableFrom(classes, cls).isPresent();
    }

    private static Optional<Class<?>> assignableFrom(List<Class<?>> classes, Class<?> cls) {
        return classes.stream().filter(c -> c.isAssignableFrom(cls)).findFirst();
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
            // list, map, optional, wrapper
            final Result knownGenericTypeResult = processKnownGenericType(javaClass, javaClass, context);
            if (knownGenericTypeResult != null) {
                return knownGenericTypeResult;
            }
            if (OptionalInt.class.isAssignableFrom(javaClass) ||
                    OptionalLong.class.isAssignableFrom(javaClass) ||
                    OptionalDouble.class.isAssignableFrom(javaClass)) {
                return new Result(TsType.Number.optional());
            }
            // generic structural type used without type arguments
            if (javaClass.getTypeParameters().length > 0) {
                final List<TsType> tsTypeArguments = new ArrayList<>();
                final List<Class<?>> discoveredClasses = new ArrayList<>();
                for (int i = 0; i < javaClass.getTypeParameters().length; i++) {
                    TypeVariable<?> typeVariable = javaClass.getTypeParameters()[i];
                    final List<TsType> bounds = new ArrayList<>();
                    for (int j = 0; j < typeVariable.getBounds().length; j++) {
                        Type boundType = typeVariable.getBounds()[j];
                        if (!Object.class.equals(boundType)) {
                            Result res = context.processType(boundType);
                            bounds.add(res.getTsType());
                            discoveredClasses.addAll(res.getDiscoveredClasses());
                        }
                    }
                    switch (bounds.size()) {
                        case 0:
                            tsTypeArguments.add(TsType.Any);
                            break;
                        case 1:
                            tsTypeArguments.add(bounds.get(0));
                            break;
                        default:
                            tsTypeArguments.add(new TsType.IntersectionType(bounds));
                            break;
                    }
                }
                return new Result(new TsType.GenericReferenceType(context.getSymbol(javaClass), tsTypeArguments), discoveredClasses);
            }
            // structural type
            return new Result(new TsType.ReferenceType(context.getSymbol(javaClass)), javaClass);
        }
        if (javaType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) javaType;
            if (parameterizedType.getRawType() instanceof Class) {
                final Class<?> javaClass = (Class<?>) parameterizedType.getRawType();
                // list, map, optional, wrapper
                final Result knownGenericTypeResult = processKnownGenericType(javaType, javaClass, context);
                if (knownGenericTypeResult != null) {
                    return knownGenericTypeResult;
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

    private Result processKnownGenericType(Type javaType, Class<?> rawClass, Context context) {

        final Optional<Class<?>> listBaseClass = assignableFrom(known.listClasses, rawClass);
        if (listBaseClass.isPresent()) {
            final List<Type> resolvedGenericVariables = GenericsResolver.resolveBaseGenericVariables(listBaseClass.get(), javaType);
            final Result result = context.processTypeInsideCollection(resolvedGenericVariables.get(0));
            return new Result(new TsType.BasicArrayType(result.getTsType()), result.getDiscoveredClasses());
        }

        final Optional<Class<?>> mapBaseClass = assignableFrom(known.mapClasses, rawClass);
        if (mapBaseClass.isPresent()) {
            final List<Type> resolvedGenericVariables = GenericsResolver.resolveBaseGenericVariables(mapBaseClass.get(), javaType);
            final Result keyResult = context.processType(resolvedGenericVariables.get(0));
            final Result valueResult = context.processTypeInsideCollection(resolvedGenericVariables.get(1));
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

        final Optional<Class<?>> optionalBaseClass = assignableFrom(known.optionalClasses, rawClass);
        if (optionalBaseClass.isPresent()) {
            final List<Type> resolvedGenericVariables = GenericsResolver.resolveBaseGenericVariables(optionalBaseClass.get(), javaType);
            final Result result = context.processType(resolvedGenericVariables.get(0));
            return new Result(result.getTsType().optional(), result.getDiscoveredClasses());
        }

        final Optional<Class<?>> wrapperBaseClass = assignableFrom(known.wrapperClasses, rawClass);
        if (wrapperBaseClass.isPresent()) {
            final List<Type> resolvedGenericVariables = GenericsResolver.resolveBaseGenericVariables(wrapperBaseClass.get(), javaType);
            final Result result = context.processType(resolvedGenericVariables.get(0));
            return new Result(result.getTsType(), result.getDiscoveredClasses());
        }

        return null;
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
