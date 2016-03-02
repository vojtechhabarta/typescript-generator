
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.emitter.*;
import cz.habarta.typescript.generator.parser.*;
import java.lang.reflect.*;
import java.util.*;


public class ModelCompiler {

    private final Settings settings;
    private final TypeProcessor typeProcessor;

    public ModelCompiler(Settings settings, TypeProcessor typeProcessor) {
        this.settings = settings;
        this.typeProcessor = typeProcessor;
    }

    public TsModel javaToTypeScript(Model model) {
        final CompilationContext context = new CompilationContext(model, new TsModel());
        for (BeanModel bean : model.getBeans()) {
            processBean(context, bean);
        }
        for (EnumModel enumModel : model.getEnums()) {
            processEnum(context, enumModel);
        }
        return context.tsModel;
    }

    private void processBean(CompilationContext context, BeanModel bean) {
        final TsBeanModel tsBean = new TsBeanModel(bean, typeFromJava(bean.getBeanClass()), typeFromJava(bean.getParent()), bean.getComments());
        context.tsModel.getBeans().add(tsBean);
        context = context.bean(bean, tsBean);
        for (PropertyModel jBean : bean.getProperties()) {
            processProperty(context, jBean);
        }
    }

    private void processProperty(CompilationContext context, PropertyModel property) {
        final TsType originalType = typeFromJava(property.getType(), property.getName(), context.bean.getBeanClass());
        final LinkedHashSet<TsType.AliasType> typeAliases = new LinkedHashSet<>();
        final TsType replacedType = replaceTypes(originalType, typeAliases);
        final TsType tsType = property.isOptional() ? replacedType.optional() : replacedType;
        final TsPropertyModel tsPropertyModel = new TsPropertyModel(property.getName(), tsType, property.getComments());
        context.tsBean.getProperties().add(tsPropertyModel);
        context.tsModel.getTypeAliases().addAll(typeAliases);
    }

    private void processEnum(CompilationContext context, EnumModel enumModel) {
        final TsEnumModel tsEnum = new TsEnumModel(enumModel, typeFromJava(enumModel.getEnumClass()), enumModel.getComments(), new ArrayList<>(enumModel.getValues()));
        context.tsModel.getEnums().add(tsEnum);
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

    public TsType typeFromJavaWithReplacement(Type javaType) {
        final TsType type = typeFromJava(javaType);
        return replaceTypes(type, new LinkedHashSet<TsType.AliasType>());
    }

    public TsType typeFromJava(Type javaType) {
        return typeFromJava(javaType, null, null);
    }

    public TsType typeFromJava(Type javaType, final String usedInProperty, final Class<?> usedInClass) {
        if (javaType == null) {
            return null;
        }
        final TypeProcessor.Result result = typeProcessor.processType(javaType, new TypeProcessor.Context() {
            @Override
            public String getMappedName(Class<?> cls) {
                return ModelCompiler.this.getMappedName(cls);
            }
            @Override
            public TypeProcessor.Result processType(Type javaType) {
                return typeProcessor.processType(javaType, this);
            }
        });
        if (result != null) {
            return result.getTsType();
        } else {
            if (usedInClass != null && usedInProperty != null) {
                System.out.println(String.format("Warning: Unsupported type '%s' used in '%s.%s'", javaType, usedInClass.getSimpleName(), usedInProperty));
            } else {
                System.out.println(String.format("Warning: Unsupported type '%s'", javaType));
            }
            return TsType.Any;
        }
    }

    private String getMappedName(Class<?> cls) {
        if (cls == null) {
            return null;
        }
        String name = cls.getSimpleName();
        if (settings.removeTypeNamePrefix != null && name.startsWith(settings.removeTypeNamePrefix)) {
            name = name.substring(settings.removeTypeNamePrefix.length(), name.length());
        }
        if (settings.removeTypeNameSuffix != null && name.endsWith(settings.removeTypeNameSuffix)) {
            name = name.substring(0, name.length() - settings.removeTypeNameSuffix.length());
        }
        if (settings.addTypeNamePrefix != null) {
            name = settings.addTypeNamePrefix + name;
        }
        if (settings.addTypeNameSuffix != null) {
            name = name + settings.addTypeNameSuffix;
        }
        return name;
    }

    private TsType replaceTypes(TsType type, LinkedHashSet<TsType.AliasType> typeAliases) {
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
        if (type instanceof TsType.OptionalType) {
            final TsType.OptionalType optionalType = (TsType.OptionalType) type;
            return new TsType.OptionalType(replaceTypes(optionalType.type, typeAliases));
        }
        if (type instanceof TsType.BasicArrayType) {
            final TsType.BasicArrayType basicArrayType = (TsType.BasicArrayType) type;
            return new TsType.BasicArrayType(replaceTypes(basicArrayType.elementType, typeAliases));
        }
        if (type instanceof TsType.IndexedArrayType) {
            final TsType.IndexedArrayType indexedArrayType = (TsType.IndexedArrayType) type;
            return new TsType.IndexedArrayType(
                    replaceTypes(indexedArrayType.indexType, typeAliases),
                    replaceTypes(indexedArrayType.elementType, typeAliases));
        }
        return type;
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

}
