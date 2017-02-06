
package cz.habarta.typescript.generator.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

/**
 * java.lang.reflect.Parameter replacement for Java 7.
 * Remove on Java 8.
 */
public class Parameter {

    private final Method method;
    private final int index;

    private Parameter(Method method, int index) {
        this.method = method;
        this.index = index;
    }

    public static List<Parameter> ofMethod(Method method) {
        final List<Parameter> parameters = new ArrayList<>();
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            parameters.add(new Parameter(method, i));
        }
        return parameters;
    }

    public String getName() {
//        return method.getParameters()[index].getName();
        try {
            final Method getParametersMethod = method.getClass().getMethod("getParameters");
            if (getParametersMethod != null) {
                final Object[] parameters = (Object[]) getParametersMethod.invoke(method);
                final Object parameter = parameters[index];
                final Method getNameMethod = parameter.getClass().getMethod("getName");
                final String name = (String) getNameMethod.invoke(parameter);
                return name;
            }
        } catch (Exception e) {
            // ignore and return default value
        }
        return "arg" + index;
    }

    public Type getParameterizedType() {
        return method.getGenericParameterTypes()[index];
    }

    public Annotation[] getAnnotations() {
        return method.getParameterAnnotations()[index];
    }

    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        for (Annotation annotation : getAnnotations()) {
            if (annotationClass.isInstance(annotation)) {
                return (T) annotation;
            }
        }
        return null;
    }

}
