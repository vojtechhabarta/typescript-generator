
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.ExcludingTypeProcessor;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TypeProcessor;
import cz.habarta.typescript.generator.util.PropertyMember;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.introspect.NopAnnotationIntrospector;
import org.codehaus.jackson.map.ser.BeanPropertyWriter;
import org.codehaus.jackson.map.ser.BeanSerializer;
import org.codehaus.jackson.map.ser.BeanSerializerFactory;
import org.codehaus.jackson.type.JavaType;


public class Jackson1Parser extends ModelParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class Factory extends ModelParser.Factory {

        @Override
        public TypeProcessor getSpecificTypeProcessor() {
            return createSpecificTypeProcessor();
        }

        @Override
        public Jackson1Parser create(Settings settings, TypeProcessor commonTypeProcessor, List<RestApplicationParser> restApplicationParsers) {
            return new Jackson1Parser(settings, commonTypeProcessor, restApplicationParsers);
        }

    }

    public Jackson1Parser(Settings settings, TypeProcessor commonTypeProcessor) {
        this(settings, commonTypeProcessor, Collections.emptyList());
    }

    public Jackson1Parser(Settings settings, TypeProcessor commonTypeProcessor, List<RestApplicationParser> restApplicationParsers) {
        super(settings, commonTypeProcessor, restApplicationParsers);
        if (!settings.optionalAnnotations.isEmpty()) {
            final AnnotationIntrospector defaultAnnotationIntrospector = objectMapper.getSerializationConfig().getAnnotationIntrospector();
            final AnnotationIntrospector allAnnotationIntrospector = new NopAnnotationIntrospector() {
                @Override
                public boolean isHandled(Annotation ann) {
                    return true;
                }
            };
            this.objectMapper.setAnnotationIntrospector(new AnnotationIntrospector.Pair(defaultAnnotationIntrospector, allAnnotationIntrospector));
        }
    }

    private static TypeProcessor createSpecificTypeProcessor() {
        return new ExcludingTypeProcessor(Arrays.asList(JsonNode.class.getName()));
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

        final BeanHelper beanHelper = getBeanHelper(sourceClass.type);
        if (beanHelper != null) {
            for (final BeanPropertyWriter beanPropertyWriter : beanHelper.getProperties()) {
                final Member member = beanPropertyWriter.getMember().getMember();
                final PropertyMember propertyMember = wrapMember(settings.getTypeParser(), member, /*creatorIndex*/ null, beanPropertyWriter::getAnnotation, beanPropertyWriter.getName(), sourceClass.type);
                if (propertyMember == null) {
                    continue;
                }
                Type propertyType = beanPropertyWriter.getGenericPropertyType();
                if (!isAnnotatedPropertyIncluded(beanPropertyWriter::getAnnotation, sourceClass.type.getName() + "." + beanPropertyWriter.getName())) {
                    continue;
                }
                final boolean optional = isPropertyOptional(propertyMember);
                properties.add(processTypeAndCreateProperty(beanPropertyWriter.getName(), propertyType, null, optional, null, sourceClass.type, member, null, null));
            }
        }

        final JsonTypeInfo jsonTypeInfo = sourceClass.type.getAnnotation(JsonTypeInfo.class);
        if (jsonTypeInfo != null && jsonTypeInfo.include() == JsonTypeInfo.As.PROPERTY) {
            if (!containsProperty(properties, jsonTypeInfo.property())) {
                properties.add(new PropertyModel(jsonTypeInfo.property(), String.class, false, null, null, null, null, null));
            }
        }

        final JsonSubTypes jsonSubTypes = sourceClass.type.getAnnotation(JsonSubTypes.class);
        if (jsonSubTypes != null) {
            for (JsonSubTypes.Type type : jsonSubTypes.value()) {
                addBeanToQueue(new SourceType<>(type.value(), sourceClass.type, "<subClass>"));
            }
        }
        final Type superclass = sourceClass.type.getGenericSuperclass() == Object.class ? null : sourceClass.type.getGenericSuperclass();
        if (superclass != null) {
            addBeanToQueue(new SourceType<>(superclass, sourceClass.type, "<superClass>"));
        }
        final List<Type> interfaces = Arrays.asList(sourceClass.type.getGenericInterfaces());
        for (Type aInterface : interfaces) {
            addBeanToQueue(new SourceType<>(aInterface, sourceClass.type, "<interface>"));
        }
        return new BeanModel(sourceClass.type, superclass, null, null, null, interfaces, properties, null);
    }

    private BeanHelper getBeanHelper(Class<?> beanClass) {
        if (beanClass == null) {
            return null;
        }
        try {
            final SerializationConfig serializationConfig = objectMapper.getSerializationConfig();
            final JavaType simpleType = objectMapper.constructType(beanClass);
            final JsonSerializer<?> jsonSerializer = BeanSerializerFactory.instance.createSerializer(serializationConfig, simpleType, null);
            if (jsonSerializer == null) {
                return null;
            }
            if (jsonSerializer instanceof BeanSerializer) {
                return new BeanHelper((BeanSerializer) jsonSerializer);
            } else {
                final String jsonSerializerName = jsonSerializer.getClass().getName();
                throw new RuntimeException(String.format("Unknown serializer '%s' for class '%s'", jsonSerializerName, beanClass));
            }
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        }
    }

    private static class BeanHelper extends BeanSerializer {

        public BeanHelper(BeanSerializer src) {
            super(src);
        }

        public BeanPropertyWriter[] getProperties() {
            return _props;
        }

    }

}
