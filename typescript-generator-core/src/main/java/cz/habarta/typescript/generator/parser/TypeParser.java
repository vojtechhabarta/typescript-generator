
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.type.JGenericArrayType;
import cz.habarta.typescript.generator.type.JParameterizedType;
import cz.habarta.typescript.generator.type.JTypeVariable;
import cz.habarta.typescript.generator.type.JTypeWithNullability;
import cz.habarta.typescript.generator.type.JWildcardType;
import cz.habarta.typescript.generator.util.Utils;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import kotlin.Metadata;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.reflect.KClassifier;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.KProperty;
import kotlin.reflect.KType;
import kotlin.reflect.KTypeParameter;
import kotlin.reflect.KTypeProjection;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.ReflectJvmMapping;


public class TypeParser {

    private final JavaTypeParser javaTypeParser;
    private final KotlinTypeParser kotlinTypeParser;

    public TypeParser(List<Class<? extends Annotation>> optionalAnnotations) {
        this.javaTypeParser = new JavaTypeParser(optionalAnnotations);
        this.kotlinTypeParser = new KotlinTypeParser(javaTypeParser);
    }

    private interface LanguageTypeParser {
        public Type getFieldType(Field field);
        public Type getMethodReturnType(Method method);
        public List<Type> getMethodParameterTypes(Method method);
    }

    private LanguageTypeParser getTypeParser(Class<?> declaringClass) {
        final boolean isKotlinClass = KotlinTypeParser.isKotlinClass(declaringClass);
        return isKotlinClass ? kotlinTypeParser : javaTypeParser;
    }

    public Type getFieldType(Field field) {
        return getTypeParser(field.getDeclaringClass()).getFieldType(field);
    }

    public Type getMethodReturnType(Method method) {
        return getTypeParser(method.getDeclaringClass()).getMethodReturnType(method);
    }

    public List<Type> getMethodParameterTypes(Method method) {
        return getTypeParser(method.getDeclaringClass()).getMethodParameterTypes(method);
    }

    //
    // Java
    //

    private static class JavaTypeParser implements LanguageTypeParser {

        private final List<Class<? extends Annotation>> optionalAnnotations;

        public JavaTypeParser(List<Class<? extends Annotation>> optionalAnnotations) {
            this.optionalAnnotations = optionalAnnotations;
        }

        @Override
        public Type getFieldType(Field field) {
            return getType(field.getAnnotatedType());
        }

        @Override
        public Type getMethodReturnType(Method method) {
            return getType(method.getAnnotatedReturnType());
        }

        @Override
        public List<Type> getMethodParameterTypes(Method method) {
            return Arrays.stream(method.getAnnotatedParameterTypes())
                    .map(annotatedType -> getType(annotatedType))
                    .collect(Collectors.toList());
        }

        private Type getType(AnnotatedType annotatedType) {
            final Type type = getBareType(annotatedType);
            if (Utils.hasAnyAnnotation(annotatedType::getAnnotation, optionalAnnotations)) {
                return new JTypeWithNullability(type, true);
            } else {
                return type;
            }
        }

        private Type getBareType(AnnotatedType annotatedType) {
            final Type type = annotatedType.getType();
            if (isArrayOfPrimitiveType(type)) {
                return type;
            }
            if (annotatedType instanceof AnnotatedParameterizedType) {
                final AnnotatedParameterizedType annotatedParameterizedType = (AnnotatedParameterizedType) annotatedType;
                final ParameterizedType parameterizedType = (ParameterizedType) type;
                return new JParameterizedType(
                        parameterizedType.getRawType(),
                        getTypes(annotatedParameterizedType.getAnnotatedActualTypeArguments()),
                        parameterizedType.getOwnerType());
            }
            if (annotatedType instanceof AnnotatedArrayType) {
                final AnnotatedArrayType annotatedArrayType = (AnnotatedArrayType) annotatedType;
                return new JGenericArrayType(getType(annotatedArrayType.getAnnotatedGenericComponentType()));
            }
            return type;
        }

        private Type[] getTypes(AnnotatedType[] annotatedTypes) {
            return Stream.of(annotatedTypes)
                    .map(annotatedType -> getType(annotatedType))
                    .toArray(Type[]::new);
        }

    }

    //
    // Kotlin
    //

    private static class KotlinTypeParser implements LanguageTypeParser {

        private final JavaTypeParser javaTypeParser;

        public KotlinTypeParser(JavaTypeParser javaTypeParser) {
            this.javaTypeParser = javaTypeParser;
        }

        public static boolean isKotlinClass(Class<?> cls) {
            return cls.isAnnotationPresent(Metadata.class);
        }

        @Override
        public Type getFieldType(Field field) {
            final KProperty<?> kProperty = ReflectJvmMapping.getKotlinProperty(field);
            if (kProperty != null) {
                return getType(kProperty.getReturnType());
            }
            return javaTypeParser.getFieldType(field);
        }

        @Override
        public Type getMethodReturnType(Method method) {
            final KFunction<?> kFunction = ReflectJvmMapping.getKotlinFunction(method);
            if (kFunction != null) {
                return getType(kFunction.getReturnType());
            } else {
                // `method` might be a getter so try to find a corresponding field and pass it to Kotlin reflection
                final KClass<?> kClass = JvmClassMappingKt.getKotlinClass(method.getDeclaringClass());
                final Optional<Field> field = KClasses.getMemberProperties(kClass).stream()
                        .filter(kProperty -> Objects.equals(ReflectJvmMapping.getJavaGetter(kProperty), method))
                        .map(kProperty -> ReflectJvmMapping.getJavaField(kProperty))
                        .filter(Objects::nonNull)
                        .findFirst();
                if (field.isPresent()) {
                    return getFieldType(field.get());
                }
            }
            return javaTypeParser.getMethodReturnType(method);
        }

        @Override
        public List<Type> getMethodParameterTypes(Method method) {
            final KFunction<?> kFunction = ReflectJvmMapping.getKotlinFunction(method);
            if (kFunction != null) {
                final List<KParameter> kParameters = kFunction.getParameters().stream()
                        .filter(kParameter -> kParameter.getKind() == KParameter.Kind.VALUE)
                        .collect(Collectors.toList());
                return getTypes(kParameters.stream()
                        .map(parameter -> parameter.getType())
                        .collect(Collectors.toList())
                );
            }
            return javaTypeParser.getMethodParameterTypes(method);
        }

        private Type getType(KType kType) {
            if (kType == null) {
                return new JWildcardType();
            }
            final Type type = getBareType(kType);
            return new JTypeWithNullability(type, kType.isMarkedNullable());
        }

        private Type getBareType(KType kType) {
            final KClassifier kClassifier = kType.getClassifier();
            if (kClassifier instanceof KClass) {
                final KClass<?> kClass = (KClass<?>) kClassifier;
                final Class<?> javaClass = JvmClassMappingKt.getJavaClass(kClass);
                if (isArrayOfPrimitiveType(javaClass)) {
                    return javaClass;
                }
                final List<KTypeProjection> arguments = kType.getArguments();
                if (arguments.isEmpty()) {
                    return javaClass;
                } else if (javaClass.isArray()) {
                    return new JGenericArrayType(getType(arguments.get(0).getType()));
                } else {
                    final List<Type> javaArguments = arguments.stream()
                            .map(argument -> getType(argument.getType()))
                            .collect(Collectors.toList());
                    return Utils.createParameterizedType(javaClass, javaArguments);
                }
            }
            if (kClassifier instanceof KTypeParameter) {
                final KTypeParameter kTypeParameter = (KTypeParameter) kClassifier;
                final TypeVariable<?> typeVariable = getJavaTypeVariable(kType);
                final Type[] bounds = getTypes(kTypeParameter.getUpperBounds()).toArray(new Type[0]);
                return new JTypeVariable<>(
                        typeVariable != null ? typeVariable.getGenericDeclaration() : null,
                        kTypeParameter.getName(),
                        bounds,
                        typeVariable != null ? typeVariable.getAnnotatedBounds() : null,
                        typeVariable != null ? typeVariable.getAnnotations() : null,
                        typeVariable != null ? typeVariable.getDeclaredAnnotations() : null
                );
            }
            throw new RuntimeException("Unexpected type: " + kType.toString());
        }

        private List<Type> getTypes(List<KType> kTypes) {
            return kTypes.stream()
                    .map(kType -> getType(kType))
                    .collect(Collectors.toList());
        }

        private TypeVariable<?> getJavaTypeVariable(KType kType) {
            try {
                final Type javaType = ReflectJvmMapping.getJavaType(kType);
                if (javaType instanceof TypeVariable) {
                    return (TypeVariable<?>) javaType;
                } else {
                    return null;
                }
            } catch (Throwable e) {
                return null;
            }
        }

    }

    //
    // common
    //

    private static boolean isArrayOfPrimitiveType(Type type) {
        if (type instanceof Class<?>) {
            final Class<?> cls = (Class<?>) type;
            if (cls.isArray() && cls.getComponentType().isPrimitive()) {
                return true;
            }
        }
        return false;
    }

}
