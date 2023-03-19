
package cz.habarta.typescript.generator.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import cz.habarta.typescript.generator.type.JGenericArrayType;
import cz.habarta.typescript.generator.type.JParameterizedType;
import cz.habarta.typescript.generator.type.JTypeWithNullability;
import cz.habarta.typescript.generator.type.JUnionType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public final class Utils {

    private Utils() {
    }

    public static String joinPath(String part1, String part2) {
        final String path = Stream.of(part1, part2)
            .filter(part -> part != null && !part.isEmpty())  // remove empty parts
            .reduce((a, b) -> trimRightSlash(a) + "/" + trimLeftSlash(b))  // join
            .orElse("");  // if all parts are empty
        return trimLeftSlash(path);  // trim leading slash
    }

    private static String trimLeftSlash(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private static String trimRightSlash(String path) {
        return  path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    public static Class<?> getRawClassOrNull(Type type) {
        final Pair<Class<?>, Optional<List<Type>>> rawClassAndTypeArguments = getRawClassAndTypeArguments(type);
        return rawClassAndTypeArguments != null ? rawClassAndTypeArguments.getValue1() : null;
    }
    
    public static Pair<Class<?>, Optional<List<Type>>> getRawClassAndTypeArguments(Type type) {
        if (type instanceof Class) {
            final Class<?> javaClass = (Class<?>) type;
            return javaClass.getTypeParameters().length != 0
                    ? Pair.of(javaClass, Optional.empty())  // raw usage of generic class
                    : Pair.of(javaClass, Optional.of(Collections.emptyList()));  // non-generic class
        }
        if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            if (parameterizedType.getRawType() instanceof Class) {
                final Class<?> javaClass = (Class<?>) parameterizedType.getRawType();
                return Pair.of(javaClass, Optional.of(Arrays.asList(parameterizedType.getActualTypeArguments())));
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

    public static Stream<Class<?>> getInheritanceChain(Class<?> cls) {
        return Stream.iterate(cls, c -> c != null, (Class<?> c) -> c.getSuperclass())
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

    public static <T, K, U> Collector<T, ?, Map<K,U>> toMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper
    ) {
        return Collectors.toMap(
                keyMapper,
                valueMapper,
                (a, b) -> {
                    throw new IllegalStateException("Duplicate key " + a);
                },
                LinkedHashMap::new
        );
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

    public static List<Annotation> getRepeatableAnnotation(Annotation directAnnotation, Annotation containerAnnotation) {
        final List<Annotation> repeatableAnnotations = new ArrayList<>();
        if (directAnnotation != null) {
            repeatableAnnotations.add(directAnnotation);
        }
        if (containerAnnotation != null) {
            final Annotation[] annotations = Utils.getAnnotationElementValue(containerAnnotation, "value", Annotation[].class);
            Stream.of(annotations).forEach(repeatableAnnotations::add);
        }
        return repeatableAnnotations;
    }

    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A getMigratedAnnotation(AnnotatedElement annotatedElement, Class<A> annotationClass, Class<?> fallbackAnnotationClass) {
        final A annotation = annotatedElement.getAnnotation(annotationClass);
        if (annotation != null) {
            return annotation;
        }
        if (fallbackAnnotationClass != null) {
            final Object fallbackAnnotation = annotatedElement.getAnnotation((Class<Annotation>)fallbackAnnotationClass);
            if (fallbackAnnotation != null) {
                return asMigrationProxy(fallbackAnnotation, annotationClass);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T asMigrationProxy(Object object, Class<T> clazz) {
        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class<?>[]{clazz},
                (proxy, method, args) -> {
                    try {
                        final Method fallbackMethod = object.getClass().getMethod(method.getName(), method.getParameterTypes());
                        return fallbackMethod.invoke(object, args);
                    } catch (ReflectiveOperationException e) {
                        return null;
                    }
                }
        );
    }


    public static Type replaceRawClassInType(Type type, Class<?> newClass) {
        if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            return createParameterizedType(newClass, parameterizedType.getActualTypeArguments());
        }
        return newClass;
    }

    public static ParameterizedType createParameterizedType(final Type rawType, final List<Type> actualTypeArguments) {
        return createParameterizedType(rawType, actualTypeArguments.toArray(new Type[0]));
    }

    public static ParameterizedType createParameterizedType(final Type rawType, final Type... actualTypeArguments) {
        return new JParameterizedType(rawType, actualTypeArguments, null);
    }

    public static Type transformContainedTypes(Type type, Function<Type, Type> transformer) {
        if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            return new JParameterizedType(
                    parameterizedType.getRawType(),
                    transformTypes(parameterizedType.getActualTypeArguments(), transformer),
                    parameterizedType.getOwnerType()
            );
        }
        if (type instanceof GenericArrayType) {
            final GenericArrayType genericArrayType = (GenericArrayType) type;
            return new JGenericArrayType(
                    transformer.apply(genericArrayType.getGenericComponentType())
            );
        }
        if (type instanceof JUnionType) {
            final JUnionType unionType = (JUnionType) type;
            return new JUnionType(
                    transformTypes(unionType.getTypes(), transformer)
            );
        }
        if (type instanceof JTypeWithNullability) {
            final JTypeWithNullability typeWithNullability = (JTypeWithNullability) type;
            return new JTypeWithNullability(
                    transformer.apply(typeWithNullability.getType()),
                    typeWithNullability.isNullable()
            );
        }
        return type;
    }

    private static List<Type> transformTypes(List<Type> types, Function<Type, Type> transformer) {
        return types.stream()
                .map(transformer)
                .collect(Collectors.toList());
    }

    private static Type[] transformTypes(Type[] types, Function<Type, Type> transformer) {
        return Stream.of(types)
                .map(transformer)
                .toArray(Type[]::new);
    }

    public static boolean isPrimitiveType(Type type) {
        if (type instanceof Class<?>) {
            final Class<?> cls = (Class<?>) type;
            return cls.isPrimitive();
        }
        return false;
    }

    private static final Map<String, Class<?>> primitiveTypes = Stream
            .of(byte.class, short.class, int.class, long.class, float.class, double.class, boolean.class, char.class, void.class)
            .collect(Utils.toMap(cls -> cls.getName(), cls -> cls));

    public static Class<?> getPrimitiveType(String typeName) {
        return primitiveTypes.get(typeName);
    }

    public static Class<?> getArrayClass(Class<?> componentType, int dimensions) {
        return Array.newInstance(componentType, new int[dimensions]).getClass();
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

    public static <T1, T2> List<Pair<T1, T2>> zip(List<T1> list1, List<T2> list2) {
        final List<Pair<T1, T2>> result = new ArrayList<>();
        final int size = Math.min(list1.size(), list2.size());
        for (int i = 0; i < size; i++) {
            result.add(Pair.of(list1.get(i), list2.get(i)));
        }
        return result;
    }

    public static List<String> readLines(InputStream stream) {
        return splitMultiline(readString(stream), false);
    }

    public static String readString(InputStream stream) {
        try (Scanner scanner = new Scanner(stream, "UTF-8")) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
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
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    public static <T> T loadJson(ObjectMapper objectMapper, InputStream inputStream, Class<T> type) {
        try {
            return objectMapper.readValue(inputStream, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public static <T> Supplier<T> memoize(Supplier<T> supplier) {
        final AtomicReference<T> value = new AtomicReference<>();
        return () -> value.updateAndGet(current -> current != null ? current : Objects.requireNonNull(supplier.get()));
    }

}
