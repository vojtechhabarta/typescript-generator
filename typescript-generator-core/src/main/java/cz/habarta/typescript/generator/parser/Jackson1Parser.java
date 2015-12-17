
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.ser.*;
import org.codehaus.jackson.type.JavaType;


public class Jackson1Parser extends ModelParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Jackson1Parser(Logger logger, Settings settings, TypeProcessor typeProcessor) {
        super(logger, settings, typeProcessor);
    }

    @Override
    protected BeanModel parseBean(SourceType<Class<?>> sourceClass) {
        final List<PropertyModel> properties = new ArrayList<>();
        final BeanHelper beanHelper = getBeanHelper(sourceClass.type);
        if (beanHelper != null) {
            for (BeanPropertyWriter beanPropertyWriter : beanHelper.getProperties()) {
                if (!isParentProperty(beanPropertyWriter.getName(), sourceClass.type)) {
                    Type propertyType = beanPropertyWriter.getGenericPropertyType();
                    if (propertyType == JsonNode.class) {
                        propertyType = Object.class;
                    }
                    properties.add(processTypeAndCreateProperty(beanPropertyWriter.getName(), propertyType, sourceClass.type));
                }
            }
        }

        final JsonSubTypes jsonSubTypes = sourceClass.type.getAnnotation(JsonSubTypes.class);
        if (jsonSubTypes != null) {
            for (JsonSubTypes.Type type : jsonSubTypes.value()) {
                addBeanToQueue(new SourceType<>(type.value(), sourceClass.type, "<subClass>"));
            }
        }
        final Class<?> superclass = sourceClass.type.getSuperclass() == Object.class ? null : sourceClass.type.getSuperclass();
        if (superclass != null) {
            addBeanToQueue(new SourceType<>(superclass, sourceClass.type, "<superClass>"));
        }
        return new BeanModel(sourceClass.type, superclass, properties);
    }

    private boolean isParentProperty(String property, Class<?> cls) {
        if (cls.getSuperclass() == Object.class) {
            return false;
        } else {
            final BeanHelper beanHelper = getBeanHelper(cls.getSuperclass());
            if (beanHelper != null) {
                for (BeanPropertyWriter beanPropertyWriter : beanHelper.getProperties()) {
                    if (beanPropertyWriter.getName().equals(property)) {
                        return true;
                    }
                }
            }
            return false;
        }
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
