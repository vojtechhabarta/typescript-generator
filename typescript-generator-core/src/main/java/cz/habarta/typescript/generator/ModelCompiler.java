
package cz.habarta.typescript.generator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import cz.habarta.typescript.generator.emitter.TsBeanModel;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.emitter.TsPropertyModel;
import cz.habarta.typescript.generator.parser.BeanModel;
import cz.habarta.typescript.generator.parser.Model;
import cz.habarta.typescript.generator.parser.PropertyModel;


public class ModelCompiler {

    private final Logger logger;
    private final Settings settings;

    public ModelCompiler(Logger logger, Settings settings) {
        this.logger = logger;
        this.settings = settings;
    }

    public List<Class<?>> discoverClasses(Type type) {
        final List<Class<?>> discoveredClasses = new ArrayList<>();
        typeFromJava(type, null, null, false, discoveredClasses);
        return discoveredClasses;
    }

    public TsModel javaToTypescript(Model model) {
        final CompilationContext context = new CompilationContext(model, new TsModel());
        for (BeanModel bean : model.getBeans()) {
            processBean(context, bean);
        }
        return context.tsModel;
    }

    private void processBean(CompilationContext context, BeanModel bean) {
        final TsBeanModel tsBean = new TsBeanModel(getMappedName(bean.getBeanClass()), getMappedName(bean.getParent()));
        context.tsModel.getBeans().add(tsBean);
        context = context.bean(bean, tsBean);
        for (PropertyModel jBean : bean.getProperties()) {
            processProperty(context, jBean);
        }
    }

    private void processProperty(CompilationContext context, PropertyModel property) {
        final TsType originalType = typeFromJava(property.getType(), property.getName(), context.bean.getBeanClass(), true, null);
        final LinkedHashSet<TsType.EnumType> replacedEnums = new LinkedHashSet<>();
        final LinkedHashSet<TsType.AliasType> typeAliases = new LinkedHashSet<>();
        final TsType tsType = replaceTypes(originalType, replacedEnums, typeAliases);
        List<String> comments = null;
        if (!replacedEnums.isEmpty()) {
            comments = new ArrayList<>();
            comments.add("Original type: " + originalType);
            for (TsType.EnumType replacedEnum : replacedEnums) {
                comments.add(replacedEnum.toString() + ": " + join(replacedEnum.values, ", "));
            }
        }
        final TsPropertyModel tsPropertyModel = new TsPropertyModel(property.getName(), tsType, concat(property.getComments(), comments));
        context.tsBean.getProperties().add(tsPropertyModel);
        context.tsModel.getTypeAliases().addAll(typeAliases);
    }

    private static class CompilationContext {

        public final Model model;
        public final TsModel tsModel;
        public final BeanModel bean;
        public final TsBeanModel tsBean;

        public CompilationContext(Model model, TsModel tsModel) {
            this(model, tsModel, null, null);
        }

        private CompilationContext(Model model, TsModel tsModel, BeanModel bean, TsBeanModel tsBean) {
            this.model = model;
            this.tsModel = tsModel;
            this.bean = bean;
            this.tsBean = tsBean;
        }

        public CompilationContext bean(BeanModel bean, TsBeanModel tsBean) {
            return new CompilationContext(model, tsModel, bean, tsBean);
        }

    }

    private TsType typeFromJava(Type javaType, final String usedInProperty, final Class<?> usedInClass, final boolean logWarnings, final List<Class<?>> discoveredClasses) {
        if (settings.customTypeParser != null) {
            TsType customType = settings.customTypeParser.typeFromJava(javaType, new JavaToTypescriptTypeParser() {
                @Override
                public TsType typeFromJava(Type javaType, JavaToTypescriptTypeParser fallback) {
                    return ModelCompiler.this.typeFromJava(javaType, usedInProperty, usedInClass, logWarnings, discoveredClasses);
                };
            });
            if (customType != null) {
                return customType;
            }
        }
        if (KnownTypes.containsKey(javaType)) return KnownTypes.get(javaType);
        if (javaType instanceof Class) {
            final Class<?> javaClass = (Class<?>) javaType;
            if (javaClass.isArray()) {
                return new TsType.BasicArrayType(typeFromJava(javaClass.getComponentType(), usedInProperty, usedInClass, logWarnings, discoveredClasses));
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
            if (discoveredClasses != null) {
                discoveredClasses.add(javaClass);
            }
            return new TsType.StructuralType(getMappedName(javaClass));
        }
        if (javaType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) javaType;
            if (parameterizedType.getRawType() instanceof Class) {
                final Class<?> javaClass = (Class<?>) parameterizedType.getRawType();
                if (List.class.isAssignableFrom(javaClass)) {
                    return new TsType.BasicArrayType(typeFromJava(parameterizedType.getActualTypeArguments()[0], usedInProperty, usedInClass, logWarnings, discoveredClasses));
                }
                if (Map.class.isAssignableFrom(javaClass)) {
                    return new TsType.IndexedArrayType(TsType.String, typeFromJava(parameterizedType.getActualTypeArguments()[1], usedInProperty, usedInClass, logWarnings, discoveredClasses));
                }
            }
        }
        if (logWarnings) {
            logger.warning(String.format("Unsupported type '%s' used in '%s.%s'", javaType, usedInClass.getSimpleName(), usedInProperty));
        }
        return TsType.Any;
    }

    private String getMappedName(Class<?> cls) {
        if (cls == null) {
            return null;
        }
        String name = cls.getSimpleName();
        if (settings.removeTypeNameSuffix != null && name.endsWith(settings.removeTypeNameSuffix)) {
            name = name.substring(0, name.length() - settings.removeTypeNameSuffix.length());
        }
        name = settings.defaultCustomTypePrefix + name;
        return name;
    }

    private TsType replaceTypes(TsType type, LinkedHashSet<TsType.EnumType> replacedEnums, LinkedHashSet<TsType.AliasType> typeAliases) {
        if (type == TsType.Date) {
            if (settings.mapDate == DateMapping.asNumber) {
                typeAliases.add(TsType.DateAsNumber);
                return TsType.DateAsNumber;
            }
            if (settings.mapDate == DateMapping.asString) {
                typeAliases.add(TsType.DateAsString);
                return TsType.DateAsString;
            }
        }
        if (type instanceof TsType.EnumType) {
            final TsType.EnumType enumType = (TsType.EnumType) type;
            replacedEnums.add(enumType);
            return TsType.String;
        }
        if (type instanceof TsType.BasicArrayType) {
            final TsType.BasicArrayType basicArrayType = (TsType.BasicArrayType) type;
            return new TsType.BasicArrayType(replaceTypes(basicArrayType.elementType, replacedEnums, typeAliases));
        }
        if (type instanceof TsType.IndexedArrayType) {
            final TsType.IndexedArrayType indexedArrayType = (TsType.IndexedArrayType) type;
            return new TsType.IndexedArrayType(
                    replaceTypes(indexedArrayType.indexType, replacedEnums, typeAliases),
                    replaceTypes(indexedArrayType.elementType, replacedEnums, typeAliases));
        }
        return type;
    }

    private static Map<Type, TsType> getKnownTypes() {
        final Map<Type, TsType> knownTypes = new LinkedHashMap<>();
        knownTypes.put(Object.class, TsType.Any);
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

    private static <T> List<T> concat(List<? extends T> first, List<? extends T> second) {
        if (first == null && second == null) {
            return null;
        }
        final List<T> result = new ArrayList<>();
        if (first != null) {
            result.addAll(first);
        }
        if (second != null) {
            result.addAll(second);
        }
        return result;
    }
}
