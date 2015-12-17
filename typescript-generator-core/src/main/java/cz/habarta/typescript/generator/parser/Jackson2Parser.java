
package cz.habarta.typescript.generator.parser;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.*;
import cz.habarta.typescript.generator.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Logger;


public class Jackson2Parser extends ModelParser {

    private final ObjectMapper objectMapper = new ObjectMapper();
    

    public Jackson2Parser(Logger logger, Settings settings, TypeProcessor typeProcessor) {
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
            final DefaultSerializerProvider.Impl serializerProvider1 = (DefaultSerializerProvider.Impl) objectMapper.getSerializerProvider();
            final DefaultSerializerProvider.Impl serializerProvider2 = serializerProvider1.createInstance(objectMapper.getSerializationConfig(), objectMapper.getSerializerFactory());
            final JavaType simpleType = objectMapper.constructType(beanClass);
            final JsonSerializer<?> jsonSerializer = BeanSerializerFactory.instance.createSerializer(serializerProvider2, simpleType);
            if (jsonSerializer == null) {
                return null;
            }
            if (jsonSerializer instanceof BeanSerializer) {
                return new BeanHelper((BeanSerializer) jsonSerializer);
            } else {
                final String jsonSerializerName = jsonSerializer.getClass().getName();
                final String message = String.format("Unknown serializer '%s' for class '%s'", jsonSerializerName, beanClass);
//                throw new RuntimeException(message);
                logger.warning(message);
                return null;
            }
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        }
    }

    private static class BeanHelper extends BeanSerializer {
        private static final long serialVersionUID = 1;

        public BeanHelper(BeanSerializer src) {
            super(src);
        }

        public BeanPropertyWriter[] getProperties() {
            return _props;
        }

    }

}
