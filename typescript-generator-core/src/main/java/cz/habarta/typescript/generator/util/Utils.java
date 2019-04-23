
package cz.habarta.typescript.generator.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


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

    public static Comparator<Method> methodComparator() {
        return (Method m1, Method m2) -> {
            final int nameDiff = m1.getName().compareToIgnoreCase(m2.getName());
            if (nameDiff != 0) {
                return nameDiff;
            }
            final int parameterTypesDiff = Arrays.asList(m1.getParameterTypes()).toString().compareTo(Arrays.asList(m2.getParameterTypes()).toString());
            if (parameterTypesDiff != 0) {
                return parameterTypesDiff;
            }
            return 0;
        };
    }

    public static List<Method> getAllMethods(Class<?> cls) {
        return getInheritanceChain(cls)
                .flatMap(c -> Stream.of(c.getDeclaredMethods()))
                .collect(Collectors.toList());
    }

    public static List<Field> getAllFields(Class<?> cls) {
        return getInheritanceChain(cls)
                .flatMap(c -> Stream.of(c.getDeclaredFields()))
                .collect(Collectors.toList());
    }

    private static Stream<Class<?>> getInheritanceChain(Class<?> cls) {
        return generateStream(cls, c -> c != null, (Class<?> c) -> c.getSuperclass())
                .collect(toReversedCollection())
                .stream();
    }

    public static <T> Collector<T, ?, Collection<T>> toReversedCollection() {
        return Collector.<T, ArrayDeque<T>, Collection<T>>of(
                ArrayDeque::new,
                (deque, item) -> deque.addFirst(item),
                (deque1, deque2) -> { deque2.addAll(deque1); return deque2; },
                deque -> deque);
    }

    // remove on Java 9 and replace with Stream.iterate
    private static <T> Stream<T> generateStream(T seed, Predicate<? super T> hasNext, UnaryOperator<T> next) {
        final Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(new Iterator<T>() {
            private T last = seed;

            @Override
            public boolean hasNext() {
                return hasNext.test(last);
            }

            @Override
            public T next() {
                final T current = last;
                last = next.apply(last);
                return current;
            }
        }, Spliterator.ORDERED);

        return StreamSupport.stream(spliterator, false);
    }

    public static boolean hasAnyAnnotation(
            Function<Class<? extends Annotation>, Annotation> getAnnotationFunction,
            List<Class<? extends Annotation>> annotations) {
        return annotations.stream()
                .map(getAnnotationFunction)
                .anyMatch(Objects::nonNull);
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

    public static Type replaceRawClassInType(Type type, Class<?> newClass) {
        if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            return createParameterizedType(newClass, parameterizedType.getActualTypeArguments());
        }
        return newClass;
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

    public static <K, V> Map<K, V> mapFromNullable(Map<K, V> map) {
        return map != null ? map : Collections.<K, V>emptyMap();
    }

    public static <T> List<T> removeNulls(List<T> list) {
        final ArrayList<T> result = new ArrayList<>(list);
        result.removeAll(Collections.singleton(null));
        return result;
    }

    public static <T> List<T> removeAll(List<T> list, List<T> toBeRemoved) {
        final ArrayList<T> result = new ArrayList<>(list);
        result.removeAll(toBeRemoved);
        return result;
    }

    public static <T> Collector<T, ?, List<T>> toSortedList(Comparator<? super T> comparator) {
        return Collectors.collectingAndThen(
                Collectors.toCollection(ArrayList::new),
                list -> {
                    list.sort(comparator);
                    return list;
                }
        );
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

    public static String normalizeLineEndings(String text, String lineEndings) {
        return text.replaceAll("\\r\\n|\\n|\\r", lineEndings);
    }

    public static List<String> splitMultiline(String text, boolean trimOneLeadingSpaceOnLines) {
        if (text == null) {
            return null;
        }
        final List<String> result = new ArrayList<>();
        final String[] lines = text.split("\\r\\n|\\n|\\r");
        for (String line : lines) {
            result.add(trimOneLeadingSpaceOnLines ? trimOneLeadingSpaceOnly(line) : line);
        }
        return result;
    }

    private static String trimOneLeadingSpaceOnly(String line) {
        if (line.startsWith(" ")) {
            return line.substring(1);
        }
        return line;
    }

    public static File replaceExtension(File file, String newExtension) {
        final String name = file.getName();
        final int dotIndex = name.lastIndexOf(".");
        final int index = dotIndex != -1 ? dotIndex : name.length();
        return new File(file.getParent(), name.substring(0, index) + newExtension);
    }

    public static boolean classNameMatches(String className, List<Pattern> regexps) {
        for (Pattern regexp : regexps) {
            if (regexp.matcher(className).matches()) {
                return true;
            }
        }
        return false;
    }

    public static List<Pattern> globsToRegexps(List<String> globs) {
        if (globs == null) {
            return null;
        }
        final List<Pattern> regexps = new ArrayList<>();
        for (String glob : globs) {
            regexps.add(globToRegexp(glob));
        }
        return regexps;
    }

    /**
     * Creates regexp for glob pattern.
     * Replaces "*" with "[^.\$]*" and "**" with ".*".
     */
    private static Pattern globToRegexp(String glob) {
        final Pattern globToRegexpPattern = Pattern.compile("(\\*\\*)|(\\*)");
        final Matcher matcher = globToRegexpPattern.matcher(glob);
        final StringBuffer sb = new StringBuffer();
        int lastEnd = 0;
        while (matcher.find()) {
            sb.append(Pattern.quote(glob.substring(lastEnd, matcher.start())));
            if (matcher.group(1) != null) {
                sb.append(Matcher.quoteReplacement(".*"));
            }
            if (matcher.group(2) != null) {
                sb.append(Matcher.quoteReplacement("[^.$]*"));
            }
            lastEnd = matcher.end();
        }
        sb.append(Pattern.quote(glob.substring(lastEnd, glob.length())));
        return Pattern.compile(sb.toString());
    }

    public static ObjectMapper getObjectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.setDefaultPrettyPrinter(new StandardJsonPrettyPrinter("  ", "\n"));
        return objectMapper;
    }

    public static String objectToString(Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String exceptionToString(Throwable e) {
        final StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

}
