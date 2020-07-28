package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.ExcludingTypeProcessor;
import cz.habarta.typescript.generator.OptionalProperties;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TypeProcessor;
import cz.habarta.typescript.generator.util.Pair;
import cz.habarta.typescript.generator.util.PropertyMember;
import cz.habarta.typescript.generator.util.Utils;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbVisibility;
import javax.json.bind.config.PropertyNamingStrategy;
import javax.json.bind.config.PropertyVisibilityStrategy;

// simplified+dependency free version of apache johnzon JsonbAccessMode
public class JsonbParser extends ModelParser {

    private final Class<? extends Annotation> johnzonAny;

    public static class Factory extends ModelParser.Factory {

        @Override
        public TypeProcessor getSpecificTypeProcessor() {
            return new ExcludingTypeProcessor(Collections.emptyList());
        }

        @Override
        public JsonbParser create(Settings settings, TypeProcessor commonTypeProcessor,
                                  List<RestApplicationParser> restApplicationParsers) {
            return new JsonbParser(settings, commonTypeProcessor, restApplicationParsers);
        }

    }

    public JsonbParser(Settings settings, TypeProcessor commonTypeProcessor) {
        this(settings, commonTypeProcessor, Collections.emptyList());
    }

    public JsonbParser(Settings settings, TypeProcessor commonTypeProcessor,
                       List<RestApplicationParser> restApplicationParsers) {
        super(settings, commonTypeProcessor, restApplicationParsers);
        johnzonAny = loadJohnzonAnyClass();
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Annotation> loadJohnzonAnyClass() {
        try {
            return (Class<? extends Annotation>) settings.classLoader
                    .loadClass("org.apache.johnzon.mapper.JohnzonAny");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    protected DeclarationModel parseClass(final SourceType<Class<?>> sourceClass) {
        if (sourceClass.type.isEnum()) {
            return ModelParser.parseEnum(sourceClass);
        } else {
            return parseBean(sourceClass);
        }
    }

    // simplistic impl handling @JsonbProperty and @JsonbTransient on fields
    private BeanModel parseBean(final SourceType<Class<?>> sourceClass) {
        final JsonbPropertyExtractor extractor = createExtractor();
        final  List<PropertyModel> properties = extractor.visit(sourceClass.type);

        final Type superclass = sourceClass.type.getGenericSuperclass() == Object.class ? null
                : sourceClass.type.getGenericSuperclass();
        if (superclass != null) {
            addBeanToQueue(new SourceType<>(superclass, sourceClass.type, "<superClass>"));
        }
        final List<Type> interfaces = Arrays.asList(sourceClass.type.getGenericInterfaces());
        for (Type aInterface : interfaces) {
            addBeanToQueue(new SourceType<>(aInterface, sourceClass.type, "<interface>"));
        }
        return new BeanModel(
                sourceClass.type, superclass, null, null, null,
                interfaces, properties, null);
    }

    private JsonbPropertyExtractor createExtractor() {
        return new JsonbPropertyExtractor(
                johnzonAny,
                new PropertyNamingStrategyFactory(Optional.ofNullable(settings.jsonbConfiguration).map(c -> c.namingStrategy).orElse("IDENTITY")).create(),
                new DefaultPropertyVisibilityStrategy(settings.classLoader),
                new FieldAndMethodAccessMode(johnzonAny));
    }

    private class JsonbPropertyExtractor {
        private final Class<? extends Annotation> johnzonAny;
        private final PropertyNamingStrategy naming;
        private final PropertyVisibilityStrategy visibility;
        private final BaseAccessMode delegate;

        private JsonbPropertyExtractor(
                final Class<? extends Annotation> johnzonAny,
                final PropertyNamingStrategy propertyNamingStrategy,
                final PropertyVisibilityStrategy visibilityStrategy,
                final BaseAccessMode delegate) {
            this.johnzonAny = johnzonAny;
            this.naming = propertyNamingStrategy;
            this.visibility = visibilityStrategy;
            this.delegate = delegate;
        }

        private List<PropertyModel> visit(final Class<?> clazz) {
            return Stream.of(clazz.getConstructors())
                    .filter(it -> it.isAnnotationPresent(JsonbCreator.class))
                    .findFirst()
                    .map(it -> new ArrayList<>(Stream.concat(visitConstructor(it), visitClass(clazz).stream())
                            .collect(Collectors.toMap(PropertyModel::getName, Function.identity(), (a, b) -> a)) // merge models
                            .values()))
                    .orElseGet(() -> new ArrayList<>(visitClass(clazz)));
        }

        private Stream<PropertyModel> visitConstructor(final Constructor<?> constructor) {
            // JSON-B 1.0 assumes all constructor params are required even if impls can diverge on that due
            // to user feedbacks so for our libraryDefinition let's assume it is true.
            // only exception is about optional wrappers which can be optional indeed
            final List<Type> parameterTypes = settings.getTypeParser().getConstructorParameterTypes(constructor);
            final List<Pair<Parameter, Type>> parameters = Utils.zip(Arrays.asList(constructor.getParameters()), parameterTypes);
            return parameters.stream()
                    .map(it -> {
                        final Type type = it.getValue2();
                        final Parameter parameter = it.getValue1();
                        final Optional<JsonbProperty> property = Optional.ofNullable(
                                parameter.getAnnotation(JsonbProperty.class));
                        final PropertyMember propertyMember = new PropertyMember(
                                parameter, it.getValue2(), parameter.getAnnotatedType(), parameter::getAnnotation);
                        return JsonbParser.this.processTypeAndCreateProperty(
                                property
                                    .map(JsonbProperty::value)
                                    .filter(p -> !p.isEmpty())
                                    .orElseGet(parameter::getName),
                                type, null,
                                settings.optionalProperties != OptionalProperties.useLibraryDefinition ?
                                        isPropertyOptional(propertyMember) :
                                        (isOptional(type) || OptionalInt.class == type ||
                                        OptionalLong.class == type || OptionalDouble.class == type ||
                                        property.map(JsonbProperty::nillable).orElse(false)),
                                constructor.getDeclaringClass(), new ParameterMember(parameter),
                                null, null);
                    });
        }

        private List<PropertyModel> visitClass(final Class<?> clazz) {
            return delegate.find(clazz).entrySet().stream()
                    .filter(e -> !isTransient(e.getValue(), visibility))
                    .filter(e -> johnzonAny == null || e.getValue().getAnnotation(johnzonAny) == null)
                    .map(e -> {
                        final DecoratedType decoratedType = e.getValue();
                        final Member member = findMember(decoratedType);
                        final PropertyMember propertyMember = wrapMember(
                                settings.getTypeParser(), member, decoratedType::getAnnotation, member.getName(), member.getDeclaringClass());

                        final JsonbProperty property = decoratedType.getAnnotation(JsonbProperty.class);
                        final String key = property == null || property.value().isEmpty() ? naming.translateName(e.getKey()) : property.value();
                        return JsonbParser.this.processTypeAndCreateProperty(
                                key, Field.class.isInstance(member) ?
                                        settings.getTypeParser().getFieldType(Field.class.cast(member)) :
                                        settings.getTypeParser().getMethodReturnType(Method.class.cast(member)),
                                null, settings.optionalProperties == OptionalProperties.useLibraryDefinition ||
                                        JsonbParser.this.isPropertyOptional(propertyMember),
                                clazz, member, null, null);
                    })
                    .sorted(Comparator.comparing(PropertyModel::getName))
                    .collect(Collectors.toList());
        }

        private Member findMember(final DecoratedType value) {
            if (FieldAndMethodAccessMode.CompositeDecoratedType.class.isInstance(value)) { // unwrap to use the right reader
                final FieldAndMethodAccessMode.CompositeDecoratedType<?> decoratedType = FieldAndMethodAccessMode.CompositeDecoratedType.class.cast(value);
                final DecoratedType type1 = decoratedType.getType1();
                final DecoratedType type2 = decoratedType.getType2();
                if (FieldAccessMode.FieldDecoratedType.class.isInstance(type1)) {
                    return findMember(type1);
                }
                return findMember(type2);
            } else if (JsonbParser.FieldAccessMode.FieldDecoratedType.class.isInstance(value)){
                return JsonbParser.FieldAccessMode.FieldDecoratedType.class.cast(value).getField();
            } else if (MethodAccessMode.MethodDecoratedType.class.isInstance(value)){
                return MethodAccessMode.MethodDecoratedType.class.cast(value).getMethod();
            }
            throw new IllegalArgumentException("Unsupported reader: " + value);
        }

        private boolean isOptional(final Type type) {
            return ParameterizedType.class.isInstance(type) && Optional.class == ParameterizedType.class.cast(type).getRawType();
        }

        private boolean isTransient(final JsonbParser.DecoratedType dt, final PropertyVisibilityStrategy visibility) {
            if (!FieldAndMethodAccessMode.CompositeDecoratedType.class.isInstance(dt)) {
                return isTransient(dt) || shouldSkip(visibility, dt);
            }
            final FieldAndMethodAccessMode.CompositeDecoratedType<?> cdt = FieldAndMethodAccessMode.CompositeDecoratedType.class.cast(dt);
            return isTransient(cdt.getType1()) || isTransient(cdt.getType2()) ||
                    (shouldSkip(visibility, cdt.getType1()) && shouldSkip(visibility, cdt.getType2()));
        }

        private boolean shouldSkip(final PropertyVisibilityStrategy visibility, final JsonbParser.DecoratedType t) {
            return isNotVisible(visibility, t);
        }

        private boolean isTransient(final JsonbParser.DecoratedType t) {
            if (t.getAnnotation(JsonbTransient.class) != null) {
                return true;
            }
            if (JsonbParser.FieldAccessMode.FieldDecoratedType.class.isInstance(t)) {
                final Field field = JsonbParser.FieldAccessMode.FieldDecoratedType.class.cast(t).getField();
                return Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers());
            }
            return false;
        }

        private boolean isNotVisible(final PropertyVisibilityStrategy visibility, final JsonbParser.DecoratedType t) {
            return !(JsonbParser.FieldAccessMode.FieldDecoratedType.class.isInstance(t) ?
                    visibility.isVisible(JsonbParser.FieldAccessMode.FieldDecoratedType.class.cast(t).getField())
                    : (MethodAccessMode.MethodDecoratedType.class.isInstance(t) &&
                    visibility.isVisible(MethodAccessMode.MethodDecoratedType.class.cast(t).getMethod())));
        }
    }

    private interface DecoratedType {
        Type getType();
        <T extends Annotation> T getAnnotation(Class<T> clazz);
        <T extends Annotation> T getClassOrPackageAnnotation(Class<T> clazz);
    }

    private interface BaseAccessMode  {
        Map<String, JsonbParser.DecoratedType> find(Class<?> clazz);
    }

    private static class FieldAccessMode implements BaseAccessMode {
        private final Class<? extends Annotation> johnzonAny;

        public FieldAccessMode(final Class<? extends Annotation> johnzonAny) {
            this.johnzonAny = johnzonAny;
        }

        @Override
        public Map<String, JsonbParser.DecoratedType> find(final Class<?> clazz) {
            final Map<String, JsonbParser.DecoratedType> readers = new HashMap<>();
            for (final Map.Entry<String, Field> f : fields(clazz, true).entrySet()) {
                final String key = f.getKey();
                if (isIgnored(key) || (johnzonAny != null && Meta.getAnnotation(f.getValue(), johnzonAny) != null)) {
                    continue;
                }

                final Field field = f.getValue();
                readers.put(key, new FieldDecoratedType(field, field.getGenericType()));
            }
            return readers;
        }

        protected boolean isIgnored(final String key) {
            return key.contains("$");
        }

        protected Map<String, Field> fields(final Class<?> clazz, final boolean includeFinalFields) {
            final Map<String, Field> fields = new HashMap<>();
            Class<?> current = clazz;
            while (current != null && current != Object.class) {
                for (final Field f : current.getDeclaredFields()) {
                    final String name = f.getName();
                    final int modifiers = f.getModifiers();
                    if (fields.containsKey(name)
                            || Modifier.isStatic(modifiers)
                            || Modifier.isTransient(modifiers)
                            || (!includeFinalFields && Modifier.isFinal(modifiers))) {
                        continue;
                    }
                    fields.put(name, f);
                }
                current = current.getSuperclass();
            }
            return fields;
        }

        private static class FieldDecoratedType implements JsonbParser.DecoratedType {
            protected final Field field;
            protected final Type type;

            public FieldDecoratedType(final Field field, final Type type) {
                this.field = field;
                this.field.setAccessible(true);
                this.type = type;
            }

            @Override
            public <T extends Annotation> T getClassOrPackageAnnotation(final Class<T> clazz) {
                return Meta.getClassOrPackageAnnotation(field, clazz);
            }

            public Field getField() {
                return field;
            }

            @Override
            public Type getType() {
                return type;
            }

            @Override
            public <T extends Annotation> T getAnnotation(final Class<T> clazz) {
                return Meta.getAnnotation(field, clazz);
            }

            @Override
            public String toString() {
                return "FieldDecoratedType{" +
                        "field=" + field +
                        '}';
            }
        }
    }

    private static class MethodAccessMode implements BaseAccessMode {
        private final Class<? extends Annotation> johnzonAny;

        public MethodAccessMode(final Class<? extends Annotation> johnzonAny) {
            this.johnzonAny = johnzonAny;
        }

        @Override
        public Map<String, DecoratedType> find(final Class<?> clazz) {
            final Map<String, DecoratedType> readers = new HashMap<>();
            if (Records.isRecord(clazz)) {
                readers.putAll(Stream.of(clazz.getMethods())
                        .filter(it -> it.getDeclaringClass() != Object.class && it.getParameterCount() == 0)
                        .filter(it -> !"toString".equals(it.getName()) && !"hashCode".equals(it.getName()))
                        .filter(it -> !isIgnored(it.getName()) && johnzonAny != null && Meta.getAnnotation(it, johnzonAny) == null)
                        .collect(Collectors.toMap(Method::getName, it -> new MethodDecoratedType(it, it.getGenericReturnType()) {
                        })));
            } else {
                final PropertyDescriptor[] propertyDescriptors = getPropertyDescriptors(clazz);
                for (final PropertyDescriptor descriptor : propertyDescriptors) {
                    final Method readMethod = descriptor.getReadMethod();
                    final String name = descriptor.getName();
                    if (readMethod != null && readMethod.getDeclaringClass() != Object.class) {
                        if (isIgnored(name) || johnzonAny != null && Meta.getAnnotation(readMethod, johnzonAny) != null) {
                            continue;
                        }
                        readers.put(name, new MethodDecoratedType(readMethod, readMethod.getGenericReturnType()));
                    } else if (readMethod == null && descriptor.getWriteMethod() != null && // isXXX, not supported by javabeans
                            (descriptor.getPropertyType() == Boolean.class || descriptor.getPropertyType() == boolean.class)) {
                        try {
                            final Method method = clazz.getMethod(
                                    "is" + Character.toUpperCase(name.charAt(0)) + (name.length() > 1 ? name.substring(1) : ""));
                            readers.put(name, new MethodDecoratedType(method, method.getGenericReturnType()));
                        } catch (final NoSuchMethodException e) {
                            // no-op
                        }
                    }
                }
            }
            return readers;
        }

        protected boolean isIgnored(final String name) {
            return name.equals("metaClass") || name.contains("$");
        }

        private PropertyDescriptor[] getPropertyDescriptors(final Class<?> clazz) {
            final PropertyDescriptor[] propertyDescriptors;
            try {
                propertyDescriptors = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
            } catch (final IntrospectionException e) {
                throw new IllegalStateException(e);
            }
            return propertyDescriptors;
        }

        public static class MethodDecoratedType implements DecoratedType {
            protected final Method method;
            protected final Type type;

            public MethodDecoratedType(final Method method, final Type type) {
                this.method = method;
                method.setAccessible(true);
                this.type = type;
            }

            @Override
            public <T extends Annotation> T getClassOrPackageAnnotation(final Class<T> clazz) {
                return Meta.getClassOrPackageAnnotation(method, clazz);
            }

            public Method getMethod() {
                return method;
            }

            @Override
            public Type getType() {
                return type;
            }

            @Override
            public <T extends Annotation> T getAnnotation(final Class<T> clazz) {
                return Meta.getAnnotation(method, clazz);
            }

            @Override
            public String toString() {
                return "MethodDecoratedType{" +
                        "method=" + method +
                        '}';
            }
        }
    }

    private static class FieldAndMethodAccessMode implements BaseAccessMode {
        private final FieldAccessMode fields;
        private final MethodAccessMode methods;

        private FieldAndMethodAccessMode(final Class<? extends Annotation> johnzonAny) {
            this.fields = new FieldAccessMode(johnzonAny);
            this.methods = new MethodAccessMode(johnzonAny);
        }

        @Override
        public Map<String, JsonbParser.DecoratedType> find(final Class<?> clazz) {
            final Map<String, JsonbParser.DecoratedType> methodReaders = this.methods.find(clazz);
            final boolean record = Records.isRecord(clazz);
            if (record) {
                return methodReaders;
            }

            final Map<String, JsonbParser.DecoratedType> fieldsReaders = this.fields.find(clazz);
            final Map<String, JsonbParser.DecoratedType> readers = new HashMap<>(fieldsReaders);

            for (final Map.Entry<String, JsonbParser.DecoratedType> entry : methodReaders.entrySet()) {
                final Method mr = MethodAccessMode.MethodDecoratedType.class.cast(entry.getValue()).getMethod();
                final String fieldName = record ?
                        mr.getName() :
                        Introspector.decapitalize(mr.getName().startsWith("is") ?
                                mr.getName().substring(2) : mr.getName().substring(3));
                final Field f = getField(fieldName, clazz);

                final JsonbParser.DecoratedType existing = readers.get(entry.getKey());
                if (existing == null) {
                    if (f != null) { // useful to hold the Field and transient state for example, just as fallback
                        readers.put(entry.getKey(), new CompositeDecoratedType<>(
                                entry.getValue(), new FieldAccessMode.FieldDecoratedType(f, f.getType())));
                    } else {
                        readers.put(entry.getKey(), entry.getValue());
                    }
                } else {
                    readers.put(entry.getKey(), new CompositeDecoratedType<>(entry.getValue(), existing));
                }
            }

            return readers;
        }

        private Field getField(final String fieldName, final Class<?> type) {
            Class<?> t = type;
            while (t != Object.class && t != null) {
                try {
                    return t.getDeclaredField(fieldName);
                } catch (final NoSuchFieldException e) {
                    // no-op
                }
                t = t.getSuperclass();
            }
            return null;
        }

        public static class CompositeDecoratedType<T extends DecoratedType> implements DecoratedType {
            protected final T type1;
            protected final T type2;

            private CompositeDecoratedType(final T type1, final T type2) {
                this.type1 = type1;
                this.type2 = type2;
            }

            @Override
            public <A extends Annotation> A getClassOrPackageAnnotation(final Class<A> clazz) {
                final A found = type1.getClassOrPackageAnnotation(clazz);
                return found == null ? type2.getClassOrPackageAnnotation(clazz) : found;
            }

            @Override
            public <A extends Annotation> A getAnnotation(final Class<A> clazz) {
                final A found = type1.getAnnotation(clazz);
                return found == null ? type2.getAnnotation(clazz) : found;
            }

            @Override
            public Type getType() {
                return type1.getType();
            }

            public DecoratedType getType1() {
                return type1;
            }

            public DecoratedType getType2() {
                return type2;
            }

            @Override
            public String toString() {
                return "CompositeDecoratedType{" +
                        "type1=" + type1 +
                        ", type2=" + type2 +
                        '}';
            }
        }
    }


    private static class DefaultPropertyVisibilityStrategy implements PropertyVisibilityStrategy {
        private final ClassLoader classLoader;
        private final ConcurrentMap<Class<?>, PropertyVisibilityStrategy> strategies = new ConcurrentHashMap<>();

        public DefaultPropertyVisibilityStrategy(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        public boolean isVisible(final Field field) {
            if (field.getAnnotation(JsonbProperty.class) != null) {
                return true;
            }
            final PropertyVisibilityStrategy strategy = strategies.computeIfAbsent(
                    field.getDeclaringClass(), this::visibilityStrategy);
            return strategy == this ? Modifier.isPublic(field.getModifiers()) : strategy.isVisible(field);
        }

        @Override
        public boolean isVisible(final Method method) {
            final PropertyVisibilityStrategy strategy = strategies.computeIfAbsent(
                    method.getDeclaringClass(), this::visibilityStrategy);
            return strategy == this ? Modifier.isPublic(method.getModifiers()) : strategy.isVisible(method);
        }

        private PropertyVisibilityStrategy visibilityStrategy(final Class<?> type) {
            JsonbVisibility visibility = type.getAnnotation(JsonbVisibility.class);
            if (visibility != null) {
                try {
                    return visibility.value().getConstructor().newInstance();
                } catch (final ReflectiveOperationException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            Package p = type.getPackage();
            while (p != null) {
                visibility = p.getAnnotation(JsonbVisibility.class);
                if (visibility != null) {
                    try {
                        return visibility.value().getConstructor().newInstance();
                    } catch (final ReflectiveOperationException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
                final String name = p.getName();
                final int end = name.lastIndexOf('.');
                if (end < 0) {
                    break;
                }
                final String parentPack = name.substring(0, end);
                p = Package.getPackage(parentPack);
                if (p == null) {
                    try {
                        p = classLoader.loadClass(parentPack + ".package-info").getPackage();
                    } catch (final ClassNotFoundException e) {
                        // no-op
                    }
                }
            }
            return this;
        }
    }

    private static class PropertyNamingStrategyFactory {
        private final Object value;

        public PropertyNamingStrategyFactory(final Object value) {
            this.value = value;
        }

        public PropertyNamingStrategy create() {
            if (String.class.isInstance(value)) {
                final String val = value.toString();
                switch (val) {
                    case PropertyNamingStrategy.IDENTITY:
                        return propertyName -> propertyName;
                    case PropertyNamingStrategy.LOWER_CASE_WITH_DASHES:
                        return new ConfigurableNamingStrategy(Character::toLowerCase, '-');
                    case PropertyNamingStrategy.LOWER_CASE_WITH_UNDERSCORES:
                        return new ConfigurableNamingStrategy(Character::toLowerCase, '_');
                    case PropertyNamingStrategy.UPPER_CAMEL_CASE:
                        return camelCaseStrategy();
                    case PropertyNamingStrategy.UPPER_CAMEL_CASE_WITH_SPACES:
                        final PropertyNamingStrategy camelCase = camelCaseStrategy();
                        final PropertyNamingStrategy space = new ConfigurableNamingStrategy(Function.identity(), ' ');
                        return propertyName -> camelCase.translateName(space.translateName(propertyName));
                    case PropertyNamingStrategy.CASE_INSENSITIVE:
                        return propertyName -> propertyName;
                    default:
                        throw new IllegalArgumentException(val + " unknown as PropertyNamingStrategy");
                }
            }
            if (PropertyNamingStrategy.class.isInstance(value)) {
                return PropertyNamingStrategy.class.cast(value);
            }
            throw new IllegalArgumentException(value + " not supported as PropertyNamingStrategy");
        }

        private PropertyNamingStrategy camelCaseStrategy() {
            return propertyName -> Character.toUpperCase(propertyName.charAt(0)) + (propertyName.length() > 1 ? propertyName.substring(1) : "");
        }

        private static class ConfigurableNamingStrategy implements PropertyNamingStrategy {
            private final Function<Character, Character> converter;
            private final char separator;

            public ConfigurableNamingStrategy(final Function<Character, Character> wordConverter, final char sep) {
                this.converter = wordConverter;
                this.separator = sep;
            }

            @Override
            public String translateName(final String propertyName) {
                final StringBuilder global = new StringBuilder();

                final StringBuilder current = new StringBuilder();
                for (int i = 0; i < propertyName.length(); i++) {
                    final char c = propertyName.charAt(i);
                    if (Character.isUpperCase(c)) {
                        final char transformed = converter.apply(c);
                        if (current.length() > 0) {
                            global.append(current).append(separator);
                            current.setLength(0);
                        }
                        current.append(transformed);
                    } else {
                        current.append(c);
                    }
                }
                if (current.length() > 0) {
                    global.append(current);
                } else {
                    global.setLength(global.length() - 1); // remove last sep
                }
                return global.toString();
            }
        }
    }

    private static class Records {
        private static final Method IS_RECORD;

        static {
            Method isRecord = null;
            try {
                isRecord = Class.class.getMethod("isRecord");
            } catch (final NoSuchMethodException e) {
                // no-op
            }
            IS_RECORD = isRecord;
        }

        private Records() {
            // no-op
        }

        public static boolean isRecord(final Class<?> clazz) {
            try {
                return IS_RECORD != null && Boolean.class.cast(IS_RECORD.invoke(clazz));
            } catch (final InvocationTargetException | IllegalAccessException e) {
                return false;
            }
        }
    }

    private static final class Meta {
        private Meta() {
            // no-op
        }

        private static <T extends Annotation> T getAnnotation(final AnnotatedElement holder, final Class<T> api) {
            return getDirectAnnotation(holder, api);
        }

        private static <T extends Annotation> T getClassOrPackageAnnotation(final Method holder, final Class<T> api) {
            return getIndirectAnnotation(api, holder::getDeclaringClass, () -> holder.getDeclaringClass().getPackage());
        }

        private static <T extends Annotation> T getClassOrPackageAnnotation(final Field holder, final Class<T> api) {
            return getIndirectAnnotation(api, holder::getDeclaringClass, () -> holder.getDeclaringClass().getPackage());
        }

        private static <T extends Annotation> T getDirectAnnotation(final AnnotatedElement holder, final Class<T> api) {
            final T annotation = holder.getAnnotation(api);
            if (annotation != null) {
                return annotation;
            }
            return findMeta(holder.getAnnotations(), api);
        }

        private static <T extends Annotation> T getIndirectAnnotation(final Class<T> api,
                                                                      final Supplier<Class<?>> ownerSupplier,
                                                                      final Supplier<Package> packageSupplier) {
            final T ownerAnnotation = ownerSupplier.get().getAnnotation(api);
            if (ownerAnnotation != null) {
                return ownerAnnotation;
            }
            final Package pck = packageSupplier.get();
            if (pck != null) {
                return pck.getAnnotation(api);
            }
            return null;
        }

        public static <T extends Annotation> T findMeta(final Annotation[] annotations, final Class<T> api) {
            for (final Annotation a : annotations) {
                final Class<? extends Annotation> userType = a.annotationType();
                final T aa = userType.getAnnotation(api);
                if (aa != null) {
                    boolean overriden = false;
                    final Map<String, Method> mapping = new HashMap<String, Method>();
                    for (final Class<?> cm : Arrays.asList(api, userType)) {
                        for (final Method m : cm.getMethods()) {
                            overriden = mapping.put(m.getName(), m) != null || overriden;
                        }
                    }
                    if (!overriden) {
                        return aa;
                    }
                    return api.cast(newAnnotation(mapping, a, aa));
                }
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        private static <T extends Annotation> T newAnnotation(final Map<String, Method> methodMapping, final Annotation user, final T johnzon) {
            return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[]{johnzon.annotationType()},
                    (proxy, method, args) -> {
                        final Method m = methodMapping.get(method.getName());
                        try {
                            if (m.getDeclaringClass() == user.annotationType()) {
                                return m.invoke(user, args);
                            }
                            return m.invoke(johnzon, args);
                        } catch (final InvocationTargetException ite) {
                            throw ite.getTargetException();
                        }
                    });
        }
    }

    private static class ParameterMember implements Member, AnnotatedElement {
        private final Parameter parameter;

        public ParameterMember(final Parameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public Class<?> getDeclaringClass() {
            return parameter.getDeclaringExecutable().getDeclaringClass();
        }

        @Override
        public String getName() {
            return parameter.getName();
        }

        @Override
        public int getModifiers() {
            return parameter.getModifiers();
        }

        @Override
        public boolean isSynthetic() {
            return parameter.isSynthetic();
        }


        @Override
        public <T extends Annotation> T getAnnotation(final Class<T> type) {
            return parameter.getAnnotation(type);
        }

        @Override
        public Annotation[] getAnnotations() {
            return parameter.getAnnotations();
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return parameter.getDeclaredAnnotations();
        }
    }
}
