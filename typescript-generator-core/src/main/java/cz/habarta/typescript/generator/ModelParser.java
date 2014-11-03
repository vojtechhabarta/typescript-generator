
package cz.habarta.typescript.generator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Logger;


public abstract class ModelParser {

    protected final Logger logger;
    protected final Settings settings;
    private Queue<Class<?>> classQueue;

    public ModelParser(Logger logger, Settings settings) {
        this.logger = logger;
        this.settings = settings;
    }

    public Model parseModel(Class<?> cls) {
        return parseModel(Arrays.asList(cls));
    }

    public Model parseModel(List<? extends Class<?>> classes) {
        classQueue = new LinkedList<>(classes);
        return parseQueue();
    }

    private Model parseQueue() {
        final LinkedHashMap<Class<?>, BeanModel> parsedClasses = new LinkedHashMap<>();
        Class<?> cls;
        while ((cls = classQueue.poll()) != null) {
            if (!parsedClasses.containsKey(cls)) {
                logger.info(String.format("Parsing '%s'", cls.getName()));
                final BeanModel bean = parseBean(cls);
                parsedClasses.put(cls, bean);
            }
        }
        return new Model(new ArrayList<>(parsedClasses.values()));
    }

    protected abstract BeanModel parseBean(Class<?> beanClass);

    protected PropertyModel processTypeAndCreateProperty(String name, Type type) {
        final TsType originalType = typeFromJava(type);
        final LinkedHashSet<TsType.EnumType> replacedEnums = new LinkedHashSet<>();
        final TsType tsType = TsType.replaceEnumsWithStrings(originalType, replacedEnums);
        List<String> comments = null;
        if (!replacedEnums.isEmpty()) {
            comments = new ArrayList<>();
            comments.add("Original type: " + originalType);
            for (TsType.EnumType replacedEnum : replacedEnums) {
                comments.add(replacedEnum.toString() + ": " + join(replacedEnum.getValues(), ", "));
            }
        }
        return new PropertyModel(name, type, tsType, comments);
    }

    protected String getMappedName(Class<?> cls) {
        final String name = cls.getSimpleName();
        if (settings.removeTypeNameSuffix != null && name.endsWith(settings.removeTypeNameSuffix)) {
            return name.substring(0, name.length() - settings.removeTypeNameSuffix.length());
        } else {
            return name;
        }
    }

    private TsType typeFromJava(Type javaType) {
        if (KnownTypes.containsKey(javaType)) return KnownTypes.get(javaType);
        if (javaType instanceof Class) {
            final Class<?> javaClass = (Class<?>) javaType;
            if (javaClass.isArray()) {
                return new TsType.BasicArrayType(typeFromJava(javaClass.getComponentType()));
            }
            if (javaClass.isEnum()) {
                @SuppressWarnings("unchecked")
                final Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) javaClass;
                final List<java.lang.String> values = new ArrayList<>();
                for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
                    values.add(enumConstant.name());
                }
                return new TsType.EnumType(getMappedName(javaClass), values);
            }
            if (List.class.isAssignableFrom(javaClass)) {
                return new TsType.BasicArrayType(TsType.Any);
            }
            if (Map.class.isAssignableFrom(javaClass)) {
                return new TsType.IndexedArrayType(TsType.String, TsType.Any);
            }
            // consider it structural
            classQueue.add(javaClass);
            return new TsType.StructuralType(getMappedName(javaClass));
        }
        if (javaType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) javaType;
            if (parameterizedType.getRawType() instanceof Class) {
                final Class<?> javaClass = (Class<?>) parameterizedType.getRawType();
                if (List.class.isAssignableFrom(javaClass)) {
                    return new TsType.BasicArrayType(typeFromJava(parameterizedType.getActualTypeArguments()[0]));
                }
                if (Map.class.isAssignableFrom(javaClass)) {
                    return new TsType.IndexedArrayType(TsType.String, typeFromJava(parameterizedType.getActualTypeArguments()[1]));
                }
            }
        }
        logger.warning(String.format("Unsupported type '%s'", javaType));
        return TsType.Any;
    }

    private static Map<Type, TsType> getKnownTypes() {
        final Map<Type, TsType> knownTypes = new LinkedHashMap<>();
        knownTypes.put(Byte.class, TsType.Number);
        knownTypes.put(Byte.TYPE, TsType.Number);
        knownTypes.put(Short.class, TsType.Number);
        knownTypes.put(Short.TYPE, TsType.Number);
        knownTypes.put(Integer.class, TsType.Number);
        knownTypes.put(Integer.TYPE, TsType.Number);
        knownTypes.put(Long.class, TsType.Number);
        knownTypes.put(Long.TYPE, TsType.Number);
        knownTypes.put(Float.class, TsType.Number);
        knownTypes.put(Float.TYPE, TsType.Number);
        knownTypes.put(Double.class, TsType.Number);
        knownTypes.put(Double.TYPE, TsType.Number);
        knownTypes.put(Boolean.class, TsType.Boolean);
        knownTypes.put(Boolean.TYPE, TsType.Boolean);
        knownTypes.put(Character.class, TsType.String);
        knownTypes.put(Character.TYPE, TsType.String);
        knownTypes.put(String.class, TsType.String);
        knownTypes.put(Date.class, TsType.Date);
        return knownTypes;
    }

    private static final Map<Type, TsType> KnownTypes = getKnownTypes();

    private static String join(Iterable<? extends Object> values, String delimiter) {
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

}
