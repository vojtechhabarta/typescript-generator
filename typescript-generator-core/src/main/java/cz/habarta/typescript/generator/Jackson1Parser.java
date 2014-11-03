
package cz.habarta.typescript.generator;

import java.util.*;
import java.util.logging.Logger;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.ser.*;
import org.codehaus.jackson.type.JavaType;


public class Jackson1Parser extends ModelParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Jackson1Parser(Logger logger, Settings settings) {
        super(logger, settings);
    }

    @Override
    protected BeanModel parseBean(Class<?> beanClass) {
        try {
            final SerializationConfig serializationConfig = objectMapper.getSerializationConfig();
            final JavaType simpleType = objectMapper.constructType(beanClass);
            
            final JsonSerializer<?> jsonSerializer = BeanSerializerFactory.instance.createSerializer(serializationConfig, simpleType, null);
            final BeanHelper beanHelper = new BeanHelper((BeanSerializer) jsonSerializer);
            
            final List<PropertyModel> properties = new ArrayList<>();
            for (BeanPropertyWriter beanPropertyWriter : beanHelper.getProperties()) {
                properties.add(processTypeAndCreateProperty(beanPropertyWriter.getName(), beanPropertyWriter.getGenericPropertyType()));
            }
            return new BeanModel(getMappedName(beanClass), properties);
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
