
package cz.habarta.typescript.generator.util;

import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;


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

    public static <T> List<T> listFromNullable(T item) {
        return item != null ? Arrays.asList(item) : Collections.<T>emptyList();
    }

    public static String readString(InputStream stream) {
        final Scanner s = new Scanner(stream, "UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static String readString(InputStream stream, String lineEndings) {
        return normalizeLineEndings(readString(stream), lineEndings);
    }

    private static String normalizeLineEndings(String text, String lineEndings) {
        return text.replaceAll("\\r\\n|\\n|\\r", lineEndings);
    }

}
