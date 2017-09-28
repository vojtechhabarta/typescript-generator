
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.*;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.introspect.NopAnnotationIntrospector;
import org.codehaus.jackson.map.ser.*;
import org.codehaus.jackson.type.JavaType;


public class Jackson1Parser extends ModelParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Jackson1Parser(Settings settings, TypeProcessor typeProcessor) {
        super(settings, typeProcessor);
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
            for (BeanPropertyWriter beanPropertyWriter : beanHelper.getProperties()) {
                final Member propertyMember = beanPropertyWriter.getMember().getMember();
                checkMember(propertyMember, beanPropertyWriter.getName(), sourceClass.type);
                Type propertyType = beanPropertyWriter.getGenericPropertyType();
                if (propertyType == JsonNode.class) {
                    propertyType = Object.class;
                }
                boolean isInAnnotationFilter = settings.includePropertyAnnotations.isEmpty();
                if (!isInAnnotationFilter) {
                    for (Class<? extends Annotation> optionalAnnotation : settings.includePropertyAnnotations) {
                        if (beanPropertyWriter.getAnnotation(optionalAnnotation) != null) {
                            isInAnnotationFilter = true;
                            break;
                        }
                    }
                    if (!isInAnnotationFilter) {
                        System.out.println("Skipping " + sourceClass.type + "." + beanPropertyWriter.getName() + " because it is missing an annotation from includePropertyAnnotations!");
                        continue;
                    }
                }
                final boolean optional = isAnnotatedPropertyOptional((AnnotatedElement) propertyMember);
                final Member originalMember = beanPropertyWriter.getMember().getMember();
                properties.add(processTypeAndCreateProperty(beanPropertyWriter.getName(), propertyType, optional, sourceClass.type, originalMember, null));
            }
        }

        final JsonTypeInfo jsonTypeInfo = sourceClass.type.getAnnotation(JsonTypeInfo.class);
        if (jsonTypeInfo != null && jsonTypeInfo.include() == JsonTypeInfo.As.PROPERTY) {
            if (!containsProperty(properties, jsonTypeInfo.property())) {
                properties.add(new PropertyModel(jsonTypeInfo.property(), String.class, false, null, null, null));
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
