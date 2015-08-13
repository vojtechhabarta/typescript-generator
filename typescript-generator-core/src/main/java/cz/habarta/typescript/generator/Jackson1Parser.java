
package cz.habarta.typescript.generator;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.io.SerializedString;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.ser.BeanPropertyWriter;
import org.codehaus.jackson.map.ser.BeanSerializer;
import org.codehaus.jackson.map.ser.BeanSerializerFactory;
import org.codehaus.jackson.map.ser.std.EnumSerializer;
import org.codehaus.jackson.map.util.EnumValues;
import org.codehaus.jackson.type.JavaType;

public class Jackson1Parser extends ModelParser {

	private final ObjectMapper objectMapper = new ObjectMapper();

	public Jackson1Parser(Logger logger, Settings settings) {
		super(logger, settings);
	}

	@Override
	protected BeanModel parseBean(ClassWithUsage classWithUsage) {
		final List<PropertyModel> properties = new ArrayList<>();
		final BeanHelper beanHelper = getBeanHelper(classWithUsage.beanClass);
		if (beanHelper != null) {
			for (BeanPropertyWriter beanPropertyWriter : beanHelper.getProperties()) {
				if (!isParentProperty(beanPropertyWriter.getName(), classWithUsage.beanClass)) {
					Type propertyType = beanPropertyWriter.getGenericPropertyType();
					if (propertyType.equals(JsonNode.class)) {
						propertyType = Object.class;
					}
					properties.add(
							processTypeAndCreateProperty(beanPropertyWriter.getName(), propertyType, classWithUsage.beanClass));
				}
			}
		}

		final JsonSubTypes jsonSubTypes = classWithUsage.beanClass.getAnnotation(JsonSubTypes.class);
		if (jsonSubTypes != null) {
			for (JsonSubTypes.Type type : jsonSubTypes.value()) {
				addBeanToQueue(new ClassWithUsage(type.value(), "<subClass>", classWithUsage.beanClass));
			}
		}
		final Class<?> superclass = classWithUsage.beanClass.getSuperclass().equals(Object.class) ? null
				: classWithUsage.beanClass.getSuperclass();
		if (superclass != null) {
			addBeanToQueue(new ClassWithUsage(superclass, "<superClass>", classWithUsage.beanClass));
		}
		return new BeanModel(getMappedName(classWithUsage.beanClass), getMappedName(superclass), properties);
	}

	private boolean isParentProperty(String property, Class<?> cls) {
		if (cls.getSuperclass().equals(Object.class)) {
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
		try {
			final SerializationConfig serializationConfig = objectMapper.getSerializationConfig();
			final JavaType simpleType = objectMapper.constructType(beanClass);
			final JsonSerializer<?> jsonSerializer = BeanSerializerFactory.instance.createSerializer(serializationConfig,
					simpleType, null);
			if (jsonSerializer == null) {
				return null;
			}
			if (jsonSerializer instanceof BeanSerializer) {
				return new BeanHelper((BeanSerializer) jsonSerializer);
			} else {
				final String jsonSerializerName = jsonSerializer != null ? jsonSerializer.getClass().getName() : null;
				throw new RuntimeException(
						String.format("Unknown serializer '%s' for class '%s'", jsonSerializerName, beanClass));
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

	@Override
	protected EnumModel parseEnum(ClassWithUsage classWithUsage) {
		final List<String> values = new ArrayList<>();
		final EnumHelper enumHelper = getEnumHelper(classWithUsage.beanClass);
		if (enumHelper != null) {
			for (SerializedString value : enumHelper.getValues().values()) {
				values.add(value.getValue());
			}
		}
		return new EnumModel(getMappedName(classWithUsage.beanClass), values);
	}

	private EnumHelper getEnumHelper(Class<?> beanClass) {
		try {
			final SerializationConfig serializationConfig = objectMapper.getSerializationConfig();
			final JavaType simpleType = objectMapper.constructType(beanClass);
			final JsonSerializer<?> jsonSerializer = BeanSerializerFactory.instance.createSerializer(serializationConfig,
					simpleType, null);
			if (jsonSerializer == null) {
				return null;
			}

			if (jsonSerializer instanceof EnumSerializer) {
				return new EnumHelper((EnumSerializer) jsonSerializer);
			} else {
				final String jsonSerializerName = jsonSerializer != null ? jsonSerializer.getClass().getName() : null;
				throw new RuntimeException(
						String.format("Unknown serializer '%s' for class '%s'", jsonSerializerName, beanClass));
			}
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		}
	}

	private static class EnumHelper {

		final EnumSerializer enumSerializer;

		public EnumHelper(EnumSerializer enumSerializer) {
			this.enumSerializer = enumSerializer;
		}

		public EnumValues getValues() {
			return this.enumSerializer.getEnumValues();
		}
	}

}
