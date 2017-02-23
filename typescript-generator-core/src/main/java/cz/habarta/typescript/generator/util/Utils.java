
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

    public static String joinPath(String part1, String part2) {
        final List<String> parts = new ArrayList<>();
        addPathPart(parts, part1);
        addPathPart(parts, part2);
        return join(parts, "/");
    }

    private static void addPathPart(List<String> parts, String part) {
        if (part != null) {
            final String trimmed = trimSlash(part);
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }
    }

    private static String trimSlash(String path) {
        path = path.startsWith("/") ? path.substring(1) : path;
        path = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        return path;
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

    public static <T> List<T> listFromNullable(List<T> list) {
        return list != null ? list : Collections.<T>emptyList();
    }

    public static <T> List<T> removeNulls(List<T> list) {
        final ArrayList<T> result = new ArrayList<>(list);
        result.removeAll(Collections.singleton(null));
        return result;
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
