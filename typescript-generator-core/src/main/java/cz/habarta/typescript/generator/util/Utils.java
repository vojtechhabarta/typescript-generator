
package cz.habarta.typescript.generator.util;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
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

    public static <T> T getAnnotationElementValue(AnnotatedElement annotatedElement, String annotationClassName, String annotationElementName, Class<T> annotationElementType) {
        final Annotation annotation = getAnnotation(annotatedElement, annotationClassName);
        return getAnnotationElementValue(annotation, annotationElementName, annotationElementType);
    }

    public static Annotation getAnnotation(AnnotatedElement annotatedElement, String annotationClassName) {
        if (annotatedElement != null) {
            for (Annotation annotation : annotatedElement.getAnnotations()) {
                if (annotation.annotationType().getName().equals(annotationClassName)) {
                    return annotation;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAnnotationElementValue(Annotation annotation, String annotationElementName, Class<T> annotationElementType) {
        try {
            if (annotation != null) {
                for (Method method : annotation.getClass().getMethods()) {
                    if (method.getName().equals(annotationElementName)) {
                        final Object value = method.invoke(annotation);
                        if (annotationElementType.isInstance(value)) {
                            return (T) value;
                        }
                    }
                }
            }
            return null;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static ParameterizedType createParameterizedType(final Type rawType, final Type... actualTypeArguments) {
        final Type ownerType = null;
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return actualTypeArguments;
            }

            @Override
            public Type getRawType() {
                return rawType;
            }

            @Override
            public Type getOwnerType() {
                return ownerType;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj instanceof ParameterizedType) {
                    final ParameterizedType that = (ParameterizedType) obj;
                    return
                        Objects.equals(ownerType, that.getOwnerType()) &&
                        Objects.equals(rawType, that.getRawType()) &&
                        Arrays.equals(actualTypeArguments, that.getActualTypeArguments());
                } else {
                    return false;
                }
            }

            @Override
            public int hashCode() {
                return Objects.hash(ownerType, rawType, actualTypeArguments);
            }
        };
    }

    public static <T> List<T> concat(List<? extends T> list1, List<? extends T> list2) {
        if (list1 == null && list2 == null) {
            return null;
        }
        final List<T> result = new ArrayList<>();
        if (list1 != null) result.addAll(list1);
        if (list2 != null) result.addAll(list2);
        return result;
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

    public static List<String> readLines(InputStream stream) {
        return splitMultiline(readString(stream), false);        
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

    public static List<String> splitMultiline(String text, boolean trimLines) {
        final List<String> result = new ArrayList<>();
        final String[] lines = text.split("\\r\\n|\\n|\\r");
        for (String line : lines) {
            result.add(trimLines ? line.trim() : line);
        }
        return result;
    }

    public static File replaceExtension(File file, String newExtension) {
        final String name = file.getName();
        final int dotIndex = name.lastIndexOf(".");
        final int index = dotIndex != -1 ? dotIndex : name.length();
        return new File(file.getParent(), name.substring(0, index) + newExtension);
    }

}
