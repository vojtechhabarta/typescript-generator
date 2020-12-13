
package cz.habarta.typescript.generator.ext;

import cz.habarta.typescript.generator.Extension;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.compiler.TsModelTransformer;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsBeanModel;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.emitter.TsNumberLiteral;
import cz.habarta.typescript.generator.emitter.TsPropertyModel;
import cz.habarta.typescript.generator.emitter.TsStringLiteral;
import cz.habarta.typescript.generator.parser.BeanModel;
import cz.habarta.typescript.generator.parser.PropertyModel;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * This is an example extension that generates default values of properties in a class,
 * it gets those values from class instance (object) created using default (parameter-less) constructor.
 */
public class DefaultsFromInstanceExtension extends Extension {

    @Override
    public EmitterExtensionFeatures getFeatures() {
        final EmitterExtensionFeatures features = new EmitterExtensionFeatures();
        features.generatesRuntimeCode = true;
        features.worksWithPackagesMappedToNamespaces = true;
        return features;
    }

    @Override
    public List<TransformerDefinition> getTransformers() {
        return Arrays.asList(new TransformerDefinition(ModelCompiler.TransformationPhase.BeforeEnums, this::transformModel));
    }

    protected TsModel transformModel(TsModelTransformer.Context context, TsModel model) {
        final List<TsBeanModel> beans = model.getBeans().stream()
                .map(bean -> transformBean(context, bean))
                .collect(Collectors.toList());
        return model.withBeans(beans);
    }

    protected TsBeanModel transformBean(TsModelTransformer.Context context, TsBeanModel tsBean) {
        if (!tsBean.isClass()) {
            return tsBean;
        }
        final BeanModel bean = context.getBeanModelOrigin(tsBean);
        if (bean == null) {
            return tsBean;
        }
        final Class<?> originClass = bean.getOrigin();
        if (originClass == null) {
            return tsBean;
        }
        try {
            final Constructor<?> constructor = originClass.getConstructor();
            final Object instance = constructor.newInstance();
            final List<TsPropertyModel> properties = tsBean.getProperties().stream()
                    .map(tsProperty -> withDefaultValue(bean, instance, tsProperty))
                    .collect(Collectors.toList());
            return tsBean.withProperties(properties);
        } catch (Exception e) {
            TypeScriptGenerator.getLogger().verbose(String.format(
                    "Cannot create instance of class '%s' to get default values: %s",
                    originClass.getName(), e.getMessage()));
            return tsBean;
        }
    }

    protected TsPropertyModel withDefaultValue(BeanModel bean, Object instance, TsPropertyModel tsProperty) {
        final Object defaultValue = getDefaultValue(bean, instance, tsProperty.getName());
        if (defaultValue instanceof String) {
            return tsProperty.withDefaultValue(new TsStringLiteral((String) defaultValue));
        } else if (defaultValue instanceof Number) {
            return tsProperty.withDefaultValue(new TsNumberLiteral((Number) defaultValue));
        } else {
            return tsProperty;
        }
    }

    protected Object getDefaultValue(BeanModel bean, Object instance, String propertyName) {
        final PropertyModel property = bean.getProperty(propertyName);
        if (property == null) {
            return null;
        }
        final Member member = property.getOriginalMember();
        if (member == null) {
            return null;
        }
        try {
            if (member instanceof Field) {
                final Field field = (Field) member;
                field.setAccessible(true);
                final Object value = field.get(instance);
                return value;
            }
            if (member instanceof Method) {
                final Method method = (Method) member;
                method.setAccessible(true);
                final Object value = method.invoke(instance);
                return value;
            }
            return null;
        } catch (ReflectiveOperationException e) {
            TypeScriptGenerator.getLogger().verbose(String.format(
                    "Cannot get default value of property '%s' of class '%s': %s",
                    propertyName, bean.getOrigin().getName(), e.getMessage()));
            return null;
        }
    }

}
