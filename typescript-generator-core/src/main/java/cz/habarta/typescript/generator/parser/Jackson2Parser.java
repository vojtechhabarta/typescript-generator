
package cz.habarta.typescript.generator.parser;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import cz.habarta.typescript.generator.*;
import cz.habarta.typescript.generator.compiler.EnumKind;
import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;


public class Jackson2Parser extends ModelParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Jackson2Parser(Settings settings, TypeProcessor typeProcessor) {
        this(settings, typeProcessor, false);
    }

    public Jackson2Parser(Settings settings, TypeProcessor typeProcessor, boolean useJaxbAnnotations) {
        super(settings, typeProcessor);
        if (useJaxbAnnotations) {
            AnnotationIntrospector introspector = new JaxbAnnotationIntrospector(objectMapper.getTypeFactory());
            objectMapper.setAnnotationIntrospector(introspector);
        }
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
                    boolean optional = false;
                    for (Class<? extends Annotation> optionalAnnotation : settings.optionalAnnotations) {
                        if (beanPropertyWriter.getAnnotation(optionalAnnotation) != null) {
                            optional = true;
                            break;
                        }
                    }
                    final Member originalMember = beanPropertyWriter.getMember().getMember();
                    properties.add(processTypeAndCreateProperty(beanPropertyWriter.getName(), propertyType, optional, sourceClass.type, originalMember));
                }
            }
        }

        final JsonTypeInfo jsonTypeInfo = sourceClass.type.getAnnotation(JsonTypeInfo.class);
        if (jsonTypeInfo != null && jsonTypeInfo.include() == JsonTypeInfo.As.PROPERTY) {
            if (!containsProperty(properties, jsonTypeInfo.property())) {
                properties.add(new PropertyModel(jsonTypeInfo.property(), String.class, false, null, null));
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
        return new BeanModel(sourceClass.type, superclass, interfaces, properties);
    }

    private boolean isParentProperty(String property, Class<?> cls) {
        final List<Class<?>> parents = new ArrayList<>();
        if (cls.getSuperclass() != Object.class) {
            parents.add(cls.getSuperclass());
        }
        parents.addAll(Arrays.asList(cls.getInterfaces()));
        for (Class<?> parent : parents) {
            final BeanHelper beanHelper = getBeanHelper(parent);
            if (beanHelper != null) {
                for (BeanPropertyWriter beanPropertyWriter : beanHelper.getProperties()) {
                    if (beanPropertyWriter.getName().equals(property)) {
                        return true;
                    }
                }
            }
        }
        return false;
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
                if (settings.displaySerializerWarning) {
                    System.out.println(String.format("Warning: Unknown serializer '%s' for class '%s'", jsonSerializerName, beanClass));
                }
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

    @Override
    protected EnumModel<?> parseEnum(SourceType<Class<?>> sourceClass) {
        final JsonFormat jsonFormat = sourceClass.type.getAnnotation(JsonFormat.class);
        final boolean isNumberBased = jsonFormat != null && (
                jsonFormat.shape() == JsonFormat.Shape.NUMBER ||
                jsonFormat.shape() == JsonFormat.Shape.NUMBER_FLOAT ||
                jsonFormat.shape() == JsonFormat.Shape.NUMBER_INT);
        if (isNumberBased) {
            return parseNumberEnum(sourceClass);
        } else {
            return super.parseEnum(sourceClass);
        }
    }

    private EnumModel<Number> parseNumberEnum(SourceType<Class<?>> sourceClass) {
        final Class<?> enumClass = sourceClass.type;
        final List<EnumMemberModel<Number>> members = new ArrayList<>();

        try {
            Method valueMethod = null;
            final BeanInfo beanInfo = Introspector.getBeanInfo(enumClass);
            for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
                final Method readMethod = propertyDescriptor.getReadMethod();
                if (readMethod.isAnnotationPresent(JsonValue.class)) {
                    valueMethod = readMethod;
                }
            }

            int index = 0;
            for (Field field : enumClass.getFields()) {
                if (field.isEnumConstant()) {
                    final Number value;
                    if (valueMethod != null) {
                        final Object constant = field.get(null);
                        value = (Number) valueMethod.invoke(constant);
                    } else {
                        value = index++;
                    }
                    members.add(new EnumMemberModel<>(field.getName(), value, null));
                }
            }
        } catch (Exception e) {
            System.out.println(String.format("Cannot get enum values for '%s' enum", enumClass.getName()));
            e.printStackTrace(System.out);
        }

        return new EnumModel<>(enumClass, EnumKind.NumberBased, members, null);
    }

}
