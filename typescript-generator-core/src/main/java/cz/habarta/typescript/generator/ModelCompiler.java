
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.emitter.*;
import cz.habarta.typescript.generator.parser.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;


public class ModelCompiler {

    private final Logger logger;
    private final Settings settings;
    private final TypeProcessor typeProcessor;

    public ModelCompiler(Logger logger, Settings settings, TypeProcessor typeProcessor) {
        this.logger = logger;
        this.settings = settings;
        this.typeProcessor = typeProcessor;
    }

    public TsModel javaToTypeScript(Model model) {
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
        final TsType originalType = typeFromJava(property.getType(), property.getName(), context.bean.getBeanClass());
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

    private TsType typeFromJava(Type javaType, final String usedInProperty, final Class<?> usedInClass) {
        final TypeProcessor.Result result = typeProcessor.processType(javaType, new TypeProcessor.Context() {
            @Override
            public String getMappedName(Class<?> cls) {
                return ModelCompiler.this.getMappedName(cls);
            }
            @Override
            public TypeProcessor.Result processType(Type javaType) {
                final TypeProcessor.Result nestedResult = typeProcessor.processType(javaType, this);
                if (nestedResult != null) {
                    return nestedResult;
                } else {
                    logger.warning(String.format("Unsupported type '%s' used in '%s.%s'", javaType, usedInClass.getSimpleName(), usedInProperty));
                    return new TypeProcessor.Result(TsType.Any);
                }
            }
        });
        return result.getTsType();
    }

    public String getMappedName(Class<?> cls) {
        if (cls == null) {
            return null;
        }
        String name = cls.getSimpleName();
        if (settings.removeTypeNameSuffix != null && name.endsWith(settings.removeTypeNameSuffix)) {
            name = name.substring(0, name.length() - settings.removeTypeNameSuffix.length());
        }
        if (settings.addTypeNamePrefix != null) {
            name = settings.addTypeNamePrefix + name;
        }
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
