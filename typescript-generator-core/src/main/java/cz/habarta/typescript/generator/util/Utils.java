
package cz.habarta.typescript.generator.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;


public class Utils {

    private Utils() {
    }
    
    public static String join(Iterable<? extends Object> values, String delimiter) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object value : values) {
            if (first) {
                first = false;
            } else {
                sb.append(delimiter);
            }
            sb.append(value);
        }
        return sb.toString();
    }

    public static Class<?> getRawClassOrNull(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            final Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class<?>) {
                return (Class<?>) rawType;
            }
        }
        return null;
    }

}
