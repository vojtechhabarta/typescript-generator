package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.*;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;

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

    public GsonParser(Settings settings, TypeProcessor commonTypeProcessor) {
        this(settings, commonTypeProcessor, Collections.emptyList());
    }

    public GsonParser(Settings settings, TypeProcessor commonTypeProcessor,
            List<RestApplicationParser> restApplicationParsers) {
        super(settings, commonTypeProcessor, restApplicationParsers);
        if (!settings.optionalAnnotations.isEmpty()) {

        }
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

                properties.add(
                        new PropertyModel(field.getName(), field.getGenericType(), false, field, null, null, null));
                addBeanToQueue(new SourceType<>(field.getGenericType(), sourceClass.type, field.getName()));
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
