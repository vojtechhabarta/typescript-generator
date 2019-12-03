
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.util.UnionType;
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
import kotlin.reflect.KType;
import kotlin.reflect.KTypeParameter;
import kotlin.reflect.jvm.ReflectJvmMapping;


public class DefaultTypeProcessor implements TypeProcessor {

    @Override
    public Result processType(Type javaType, Context context) {
        return processType(javaType, null, context);
    }

    @Override
    public Result processType(Type javaType, KType kType,  Context context) {
        if (KnownTypes.containsKey(javaType)) {
            return new Result(KnownTypes.get(javaType), kType);
        }
        if (javaType instanceof Class) {
            final Class<?> javaClass = (Class<?>) javaType;
            if (Temporal.class.isAssignableFrom(javaClass)) {
                return new Result(TsType.Date, kType);
            }
        }
        if (javaType instanceof Class) {
            final Class<?> javaClass = (Class<?>) javaType;
            if (javaClass.isArray()) {
                final Result result = context.processType(javaClass.getComponentType(), getArgument(kType, 0));
                return new Result(new TsType.BasicArrayType(result.getTsType()), kType, result.getDiscoveredClasses());
            }
            if (javaClass.isEnum()) {
                return new Result(new TsType.EnumReferenceType(context.getSymbol(javaClass)), kType, javaClass);
            }
            if (Collection.class.isAssignableFrom(javaClass)) {
                return new Result(new TsType.BasicArrayType(TsType.Any), kType);
            }
            if (Map.class.isAssignableFrom(javaClass)) {
                return new Result(new TsType.IndexedArrayType(TsType.String, TsType.Any), kType);
            }
            if (OptionalInt.class.isAssignableFrom(javaClass) ||
                    OptionalLong.class.isAssignableFrom(javaClass) ||
                    OptionalDouble.class.isAssignableFrom(javaClass)) {
                return new Result(TsType.Number.optional(), kType);
            }
            if (JAXBElement.class.isAssignableFrom(javaClass)) {
                return new Result(TsType.Any, kType);
            }
            // generic structural type used without type arguments
            if (javaClass.getTypeParameters().length > 0) {
                final List<TsType> tsTypeArguments = new ArrayList<>();
                for (int i = 0; i < javaClass.getTypeParameters().length; i++) {
                    tsTypeArguments.add(TsType.Any);
                }
                return new Result(new TsType.GenericReferenceType(context.getSymbol(javaClass), tsTypeArguments), kType);
            }
            // structural type
            return new Result(new TsType.ReferenceType(context.getSymbol(javaClass)), kType, javaClass);
        }
        if (javaType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) javaType;
            if (parameterizedType.getRawType() instanceof Class) {
                final Class<?> javaClass = (Class<?>) parameterizedType.getRawType();
                final Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (Collection.class.isAssignableFrom(javaClass)) {
                    final Result result = context.processType(actualTypeArguments[0], getArgument(kType, 0));
                    return new Result(new TsType.BasicArrayType(result.getTsType()), kType, result.getDiscoveredClasses());
                }
                if (Map.class.isAssignableFrom(javaClass)) {
                    final Result keyResult = context.processType(actualTypeArguments[0], getArgument(kType, 0));
                    final Result valueResult = context.processType(actualTypeArguments[1],getArgument(kType, 1));
                    if (keyResult.getTsType() instanceof TsType.EnumReferenceType) {
                        return new Result(
                                new TsType.MappedType(keyResult.getTsType(), TsType.MappedType.QuestionToken.Question, valueResult.getTsType()), kType,
                                Utils.concat(keyResult.getDiscoveredClasses(), valueResult.getDiscoveredClasses())
                        );
                    } else {
                        return new Result(
                                new TsType.IndexedArrayType(TsType.String, valueResult.getTsType()), kType,
                                valueResult.getDiscoveredClasses()
                        );
                    }
                }
                if (Optional.class.isAssignableFrom(javaClass)) {
                    final Result result = context.processType(actualTypeArguments[0], getArgument(kType, 0));
                    return new Result(result.getTsType().optional(), kType, result.getDiscoveredClasses());
                }
                if (JAXBElement.class.isAssignableFrom(javaClass)) {
                    final Result result = context.processType(actualTypeArguments[0], getArgument(kType, 0));
                    return new Result(result.getTsType(), kType, result.getDiscoveredClasses());
                }
                // generic structural type
                final List<Class<?>> discoveredClasses = new ArrayList<>();
                discoveredClasses.add(javaClass);
                final List<TsType> tsTypeArguments = new ArrayList<>();
                for (int i = 0; i < actualTypeArguments.length; i++) {
                    Type typeArgument = actualTypeArguments[i];
                    final TypeProcessor.Result typeArgumentResult = context.processType(typeArgument, getArgument(kType, i));
                    tsTypeArguments.add(typeArgumentResult.getTsType());
                    discoveredClasses.addAll(typeArgumentResult.getDiscoveredClasses());
                }
                return new Result(new TsType.GenericReferenceType(context.getSymbol(javaClass), tsTypeArguments), kType, discoveredClasses);
            }
        }
        if (javaType instanceof GenericArrayType) {
            final GenericArrayType genericArrayType = (GenericArrayType) javaType;
            final Result result = context.processType(genericArrayType.getGenericComponentType(), getArgument(kType, 0));
            return new Result(new TsType.BasicArrayType(result.getTsType()), kType, result.getDiscoveredClasses());
        }
        if (javaType instanceof TypeVariable) {
            final TypeVariable<?> typeVariable = (TypeVariable<?>) javaType;
            if (typeVariable.getGenericDeclaration() instanceof Method) {
                // example method: public <T extends Number> T getData();
                if (kType != null) {
                    KTypeParameter typeParameter = (KTypeParameter) kType.getClassifier();
                    if (typeParameter != null) {
                        final Result result = context.processType(typeVariable.getBounds()[0], typeParameter.getUpperBounds().get(0));
                        if (kType.isMarkedNullable()) {
                            return new Result(new TsType.OptionalType(result.getTsType()), null, result.getDiscoveredClasses());
                        }
                        return result;
                    }
                }
                return context.processType(typeVariable.getBounds()[0]);
            }
            return new Result(new TsType.GenericVariableType(typeVariable.getName()), kType);
        }
        if (javaType instanceof WildcardType) {
            final WildcardType wildcardType = (WildcardType) javaType;
            final Type[] upperBounds = wildcardType.getUpperBounds();

            KType upperBound = null;
            if (kType != null) {
                KTypeParameter typeParameter = (KTypeParameter) kType.getClassifier();
                if (typeParameter != null) {
                    upperBound = typeParameter.getUpperBounds().get(0);
                }
            }

            return upperBounds.length > 0
                    ? context.processType(upperBounds[0], upperBound)
                    : new Result(TsType.Any, kType);
        }
        if (javaType instanceof UnionType) {
            final UnionType unionType = (UnionType) javaType;
            final List<Result> results = unionType.types.stream()
                    .map(type -> context.processType(type))
                    .collect(Collectors.toList());
            return new Result(
                    new TsType.UnionType(results.stream()
                            .map(result -> result.getTsType())
                            .collect(Collectors.toList())), kType,
                    results.stream()
                            .flatMap(result -> result.getDiscoveredClasses().stream())
                            .collect(Collectors.toList())
            );
        }
        return null;
    }

    private KType getArgument(KType kType, int index) {
        if (kType == null) {
            return null;
        }

        return kType.getArguments().get(index).getType();
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
