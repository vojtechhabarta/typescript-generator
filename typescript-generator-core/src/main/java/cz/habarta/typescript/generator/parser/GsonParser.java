package cz.habarta.typescript.generator.parser;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import cz.habarta.typescript.generator.ExcludingTypeProcessor;
import cz.habarta.typescript.generator.GsonConfiguration;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TypeProcessor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GsonParser extends ModelParser {

    public static class Factory extends ModelParser.Factory {

        @Override
        public TypeProcessor getSpecificTypeProcessor() {
            return createSpecificTypeProcessor();
        }

        @Override
        public GsonParser create(Settings settings, TypeProcessor commonTypeProcessor,
                List<RestApplicationParser> restApplicationParsers) {
            return new GsonParser(settings, commonTypeProcessor, restApplicationParsers);
        }

    }

    private final Gson gson;

    public GsonParser(Settings settings, TypeProcessor commonTypeProcessor) {
        this(settings, commonTypeProcessor, Collections.emptyList());
    }

    public GsonParser(Settings settings, TypeProcessor commonTypeProcessor,
            List<RestApplicationParser> restApplicationParsers) {
        super(settings, commonTypeProcessor, restApplicationParsers);
        if (!settings.optionalAnnotations.isEmpty()) {

        }
        final GsonConfiguration config = settings.gsonConfiguration;
        int modifiers = config != null && config.excludeFieldsWithModifiers != null
            ? Settings.parseModifiers(config.excludeFieldsWithModifiers, Modifier.fieldModifiers())
            : Modifier.STATIC | Modifier.TRANSIENT;
        this.gson = new GsonBuilder()
            .excludeFieldsWithModifiers(modifiers)
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                    return !isAnnotatedPropertyIncluded(fieldAttributes::getAnnotation, fieldAttributes.getDeclaringClass().getName() + "." + fieldAttributes.getName());
                }

                @Override
                public boolean shouldSkipClass(Class<?> aClass) {
                    return false;
                }
            })
            .create();
    }

    private static TypeProcessor createSpecificTypeProcessor() {
        return new ExcludingTypeProcessor(Arrays.asList());
    }

    @Override
    protected DeclarationModel parseClass(SourceType<Class<?>> sourceClass) {
        if (sourceClass.type.isEnum()) {
            return ModelParser.parseEnum(sourceClass);
        } else {
            return parseBean(sourceClass);
        }
    }

    private BeanModel parseBean(SourceType<Class<?>> sourceClass) {
        final List<PropertyModel> properties = new ArrayList<>();
        Class<?> cls = sourceClass.type;
        while (cls != null) {
            for (Field field : cls.getDeclaredFields()) {
                if (gson.excluder().excludeField(field, /*serialize*/true)) {
                    continue;
                }
                String name = field.getName();
                SerializedName serializedName = field.getAnnotation(SerializedName.class);
                if (serializedName != null) {
                    name = serializedName.value();
                }
                properties.add(new PropertyModel(name, field.getGenericType(), false, field, null, null, null));
                addBeanToQueue(new SourceType<>(field.getGenericType(), sourceClass.type, name));
            }
            cls = cls.getSuperclass();
        }

        final Type superclass = sourceClass.type.getGenericSuperclass() == Object.class ? null
                : sourceClass.type.getGenericSuperclass();
        if (superclass != null) {
            addBeanToQueue(new SourceType<>(superclass, sourceClass.type, "<superClass>"));
        }
        final List<Type> interfaces = Arrays.asList(sourceClass.type.getGenericInterfaces());
        for (Type aInterface : interfaces) {
            addBeanToQueue(new SourceType<>(aInterface, sourceClass.type, "<interface>"));
        }
        return new BeanModel(sourceClass.type, superclass, null, null, null, interfaces, properties, null);
    }

}
