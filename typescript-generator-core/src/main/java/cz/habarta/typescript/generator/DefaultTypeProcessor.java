
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.type.JTypeWithNullability;
import cz.habarta.typescript.generator.type.JUnionType;
import cz.habarta.typescript.generator.util.Utils;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;


public class DefaultTypeProcessor implements TypeProcessor {

    @Override
    public Result processType(Type javaType, Context context) {
        if (KnownTypes.containsKey(javaType)) {
            return new Result(KnownTypes.get(javaType));
        }
        if (javaType instanceof Class) {
            final Class<?> javaClass = (Class<?>) javaType;
            if (Temporal.class.isAssignableFrom(javaClass)) {
                return new Result(TsType.Date);
            }
        }
        if (javaType instanceof Class) {
            final Class<?> javaClass = (Class<?>) javaType;
            if (javaClass.isArray()) {
                final Result result = context.processType(javaClass.getComponentType());
                return new Result(new TsType.BasicArrayType(result.getTsType()), result.getDiscoveredClasses());
            }
            if (javaClass.isEnum()) {
                return new Result(new TsType.EnumReferenceType(context.getSymbol(javaClass)), javaClass);
            }
            if (Collection.class.isAssignableFrom(javaClass)) {
                return new Result(new TsType.BasicArrayType(TsType.Any));
            }
            if (Map.class.isAssignableFrom(javaClass)) {
                return new Result(new TsType.IndexedArrayType(TsType.String, TsType.Any));
            }
            if (OptionalInt.class.isAssignableFrom(javaClass) ||
                    OptionalLong.class.isAssignableFrom(javaClass) ||
                    OptionalDouble.class.isAssignableFrom(javaClass)) {
                return new Result(TsType.Number.optional());
            }
            if (JAXBElement.class.isAssignableFrom(javaClass)) {
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
                if (Collection.class.isAssignableFrom(javaClass)) {
                    final Result result = context.processType(parameterizedType.getActualTypeArguments()[0]);
                    return new Result(new TsType.BasicArrayType(result.getTsType()), result.getDiscoveredClasses());
                }
                if (Map.class.isAssignableFrom(javaClass)) {
                    final Result keyResult = context.processType(parameterizedType.getActualTypeArguments()[0]);
                    final Result valueResult = context.processType(parameterizedType.getActualTypeArguments()[1]);
                    if (keyResult.getTsType() instanceof TsType.EnumReferenceType) {
                        return new Result(
                                new TsType.MappedType(keyResult.getTsType(), TsType.MappedType.QuestionToken.Question, valueResult.getTsType()),
                                Utils.concat(keyResult.getDiscoveredClasses(), valueResult.getDiscoveredClasses())
                        );
                    } else {
                        return new Result(
                                new TsType.IndexedArrayType(TsType.String, valueResult.getTsType()),
                                valueResult.getDiscoveredClasses()
                        );
                    }
                }
                if (Optional.class.isAssignableFrom(javaClass)) {
                    final Result result = context.processType(parameterizedType.getActualTypeArguments()[0]);
                    return new Result(result.getTsType().optional(), result.getDiscoveredClasses());
                }
                if (JAXBElement.class.isAssignableFrom(javaClass)) {
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
            final Result result = context.processType(genericArrayType.getGenericComponentType());
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

    private static Map<Type, TsType> getKnownTypes() {
        final Map<Type, TsType> knownTypes = new LinkedHashMap<>();
        // java.lang
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
        knownTypes.put(void.class, TsType.Void);
        knownTypes.put(Void.class, TsType.Void);
        knownTypes.put(Number.class, TsType.Number);
        // other java packages
        knownTypes.put(BigDecimal.class, TsType.Number);
        knownTypes.put(BigInteger.class, TsType.Number);
        knownTypes.put(Date.class, TsType.Date);
        knownTypes.put(UUID.class, TsType.String);
        return knownTypes;
    }

    private static final Map<Type, TsType> KnownTypes = getKnownTypes();

}
