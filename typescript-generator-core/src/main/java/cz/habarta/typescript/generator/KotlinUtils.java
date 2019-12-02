package cz.habarta.typescript.generator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import kotlin.Metadata;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.KProperty;
import kotlin.reflect.jvm.ReflectJvmMapping;

public abstract class KotlinUtils {

    /**
     * States whether the return type of the given method is nullable.
     *
     * The fallbackName gives the name of the underlying field if this function is a getter function:
     * Kotlin generates getters, which can be picked up by the serializer, but then it's not a Kotlin function.
     * As we are in a Kotlin class, this _must_ be a getter for the underlying field.
     *
     * @param method the Java method
     * @param fallbackName the name of the field which _could_ be underlying
     * @return whether the return type of the function is nullable
     */
    public static boolean isReturnTypeNullable(Method method, String fallbackName) {
        if (method != null && KotlinUtils.isKotlinClass(method.getDeclaringClass())) {
            KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);

            // As this is a kotlin class, a valid java method MUST with a null kotlin
            // function MUST be a kotlin property getter
            if (function == null && fallbackName != null) {
                try {
                    final Field field = method.getDeclaringClass().getDeclaredField(fallbackName);
                    return isFieldNullable(field);
                } catch (NoSuchFieldException e) {
                    return false;
                }

            } else if (function != null) {
                return function.getReturnType().isMarkedNullable();
            }
        }
        return false;
    }

    public static boolean isParameterNullable(int parameterIndex, Method method) {
        if (method != null && KotlinUtils.isKotlinClass(method.getDeclaringClass())) {
            KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
            return isParameterNullable(parameterIndex, function);
        }
        return false;
    }

    public static boolean isParameterNullable(int parameterIndex, Constructor<?> constructor) {
        if (constructor != null && KotlinUtils.isKotlinClass(constructor.getDeclaringClass())) {
            KFunction<?> function = ReflectJvmMapping.getKotlinFunction(constructor);
            return isParameterNullable(parameterIndex, function);
        }
        return false;
    }

    public static boolean isFieldNullable(Field field) {
        if (field != null && KotlinUtils.isKotlinClass(field.getDeclaringClass())) {
            final KProperty<?> kotlinProperty = ReflectJvmMapping.getKotlinProperty(field);
            if (kotlinProperty == null) return false;
            return kotlinProperty.getReturnType().isMarkedNullable();
        }
        return false;
    }

    private static boolean isKotlinClass(Class<?> type) {
        return type != null && type.getDeclaredAnnotation(Metadata.class) != null;
    }

    private static boolean isParameterNullable(int parameterIndex, KFunction<?> function) {
        if (function != null) {
            List<KParameter> parameters = function.getParameters();
            return parameters
                    .stream()
                    .filter(p -> KParameter.Kind.VALUE.equals(p.getKind()))
                    .collect(Collectors.toList())
                    .get(parameterIndex)
                    .getType()
                    .isMarkedNullable();
        }

        return false;
    }


}
