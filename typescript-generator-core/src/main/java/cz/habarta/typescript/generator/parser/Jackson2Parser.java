
package cz.habarta.typescript.generator.parser;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import cz.habarta.typescript.generator.*;
import cz.habarta.typescript.generator.compiler.EnumKind;
import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import cz.habarta.typescript.generator.util.Predicate;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;


public class Jackson2Parser extends ModelParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Jackson2Parser(Settings settings, TypeProcessor typeProcessor) {
        this(settings, typeProcessor, false);
    }

    public Jackson2Parser(Settings settings, TypeProcessor typeProcessor, boolean useJaxbAnnotations) {
        super(settings, typeProcessor);
        if (!settings.disableJackson2ModuleDiscovery) {
            objectMapper.registerModules(ObjectMapper.findModules(settings.classLoader));
        }
        if (useJaxbAnnotations) {
            AnnotationIntrospector introspector = new JaxbAnnotationIntrospector(objectMapper.getTypeFactory());
            objectMapper.setAnnotationIntrospector(introspector);
        }
    }

    @Override
    protected DeclarationModel parseClass(SourceType<Class<?>> sourceClass) {
        if (sourceClass.type.isEnum()) {
            return parseEnumOrObjectEnum(sourceClass);
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
                Type propertyType = getGenericType(propertyMember);
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

                if (settings.useJackson2RequiredForOptional) {
                    optional = ! beanPropertyWriter.isRequired();
                }

                for (Class<? extends Annotation> optionalAnnotation : settings.optionalAnnotations) {
                    if (beanPropertyWriter.getAnnotation(optionalAnnotation) != null) {
                        optional = true;
                        break;
                    }
                }
                // @JsonUnwrapped
                PropertyModel.PullProperties pullProperties = null;
                final Member originalMember = beanPropertyWriter.getMember().getMember();
                if (originalMember instanceof AccessibleObject) {
                    final AccessibleObject accessibleObject = (AccessibleObject) originalMember;
                    final JsonUnwrapped annotation = accessibleObject.getAnnotation(JsonUnwrapped.class);
                    if (annotation != null && annotation.enabled()) {
                        pullProperties = new PropertyModel.PullProperties(annotation.prefix(), annotation.suffix());
                    }
                }
                properties.add(processTypeAndCreateProperty(beanPropertyWriter.getName(), propertyType, optional, sourceClass.type, originalMember, pullProperties));
            }
        }
        if (sourceClass.type.isEnum()) {
            return new BeanModel(sourceClass.type, null, null, null, null, null, properties, null);
        }

        final String discriminantProperty;
        final String discriminantLiteral;

        final JsonTypeInfo jsonTypeInfo = sourceClass.type.getAnnotation(JsonTypeInfo.class);
        final JsonTypeInfo parentJsonTypeInfo;
        if (isSupported(jsonTypeInfo)) {
            // this is parent
            discriminantProperty = getDiscriminantPropertyName(jsonTypeInfo);
            discriminantLiteral = null;
        } else if (isSupported(parentJsonTypeInfo = getAnnotationRecursive(sourceClass.type, JsonTypeInfo.class))) {
            // this is child class
            discriminantProperty = getDiscriminantPropertyName(parentJsonTypeInfo);
            discriminantLiteral = getTypeName(sourceClass.type);
        } else {
            // not part of explicit hierarchy
            discriminantProperty = null;
            discriminantLiteral = null;
        }
        
        final List<Class<?>> taggedUnionClasses;
        final JsonSubTypes jsonSubTypes = sourceClass.type.getAnnotation(JsonSubTypes.class);
        if (jsonSubTypes != null) {
            taggedUnionClasses = new ArrayList<>();
            for (JsonSubTypes.Type type : jsonSubTypes.value()) {
                addBeanToQueue(new SourceType<>(type.value(), sourceClass.type, "<subClass>"));
                taggedUnionClasses.add(type.value());
            }
        } else {
            taggedUnionClasses = null;
        }
        final Type superclass = sourceClass.type.getGenericSuperclass() == Object.class ? null : sourceClass.type.getGenericSuperclass();
        if (superclass != null) {
            addBeanToQueue(new SourceType<>(superclass, sourceClass.type, "<superClass>"));
        }
        final List<Type> interfaces = Arrays.asList(sourceClass.type.getGenericInterfaces());
        for (Type aInterface : interfaces) {
            addBeanToQueue(new SourceType<>(aInterface, sourceClass.type, "<interface>"));
        }
        return new BeanModel(sourceClass.type, superclass, taggedUnionClasses, discriminantProperty, discriminantLiteral, interfaces, properties, null);
    }

    private static Type getGenericType(Member member) {
        if (member instanceof Method) {
            return ((Method) member).getGenericReturnType();
        }
        if (member instanceof Field) {
            return ((Field) member).getGenericType();
        }
        return null;
    }

    private static boolean isSupported(JsonTypeInfo jsonTypeInfo) {
        return jsonTypeInfo != null &&
                jsonTypeInfo.include() == JsonTypeInfo.As.PROPERTY &&
                (jsonTypeInfo.use() == JsonTypeInfo.Id.NAME || jsonTypeInfo.use() == JsonTypeInfo.Id.CLASS);
    }

    private String getDiscriminantPropertyName(JsonTypeInfo jsonTypeInfo) {
        return jsonTypeInfo.property().isEmpty()
                ? jsonTypeInfo.use().getDefaultPropertyName()
                : jsonTypeInfo.property();
    }

    private String getTypeName(final Class<?> cls) {
        // find @JsonTypeName recursively
        final JsonTypeName jsonTypeName = getAnnotationRecursive(cls, JsonTypeName.class);
        if (jsonTypeName != null && ! jsonTypeName.value().isEmpty()) {
            return jsonTypeName.value();
        }
        // find @JsonSubTypes.Type recursively
        final JsonSubTypes jsonSubTypes = getAnnotationRecursive(cls, JsonSubTypes.class, new Predicate<JsonSubTypes>() {
            @Override
            public boolean test(JsonSubTypes types) {
                return getJsonSubTypeForClass(types, cls) != null;
            }
        });
        if (jsonSubTypes != null) {
            final JsonSubTypes.Type jsonSubType = getJsonSubTypeForClass(jsonSubTypes, cls);
            if (!jsonSubType.name().isEmpty()) {
                return jsonSubType.name();
            }
        }
        // use simplified class name if it's not an interface or abstract
        if(!cls.isInterface() && !Modifier.isAbstract(cls.getModifiers())) {
            return cls.getName().substring(cls.getName().lastIndexOf(".") + 1);
        }
        return null;
    }

    private static JsonSubTypes.Type getJsonSubTypeForClass(JsonSubTypes types, Class<?> cls) {
        for (JsonSubTypes.Type type : types.value()) {
            if (type.value().equals(cls)) {
                return type;
            }
        }
        return null;
    }

    private static <T extends Annotation> T getAnnotationRecursive(Class<?> cls, Class<T> annotationClass) {
        return getAnnotationRecursive(cls, annotationClass, null);
    }

    private static <T extends Annotation> T getAnnotationRecursive(Class<?> cls, Class<T> annotationClass, Predicate<T> annotationFilter) {
        if (cls == null) {
            return null;
        }
        final T annotation = cls.getAnnotation(annotationClass);
        if (annotation != null && (annotationFilter == null || annotationFilter.test(annotation))) {
            return annotation;
        }
        for (Class<?> aInterface : cls.getInterfaces()) {
            final T interfaceAnnotation = getAnnotationRecursive(aInterface, annotationClass, annotationFilter);
            if (interfaceAnnotation != null) {
                return interfaceAnnotation;
            }
        }
        final T superclassAnnotation = getAnnotationRecursive(cls.getSuperclass(), annotationClass, annotationFilter);
        if (superclassAnnotation != null) {
            return superclassAnnotation;
        }
        return null;
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

    private DeclarationModel parseEnumOrObjectEnum(SourceType<Class<?>> sourceClass) {
        final JsonFormat jsonFormat = sourceClass.type.getAnnotation(JsonFormat.class);
        if (jsonFormat != null && jsonFormat.shape() == JsonFormat.Shape.OBJECT) {
            return parseBean(sourceClass);
        }
        final boolean isNumberBased = jsonFormat != null && (
                jsonFormat.shape() == JsonFormat.Shape.NUMBER ||
                jsonFormat.shape() == JsonFormat.Shape.NUMBER_FLOAT ||
                jsonFormat.shape() == JsonFormat.Shape.NUMBER_INT);

        final List<EnumMemberModel> enumMembers = new ArrayList<>();
        if (sourceClass.type.isEnum()) {
            final Class<?> enumClass = (Class<?>) sourceClass.type;

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
                        if (isNumberBased) {
                            final Number value = getNumberEnumValue(field, valueMethod, index++);
                            enumMembers.add(new EnumMemberModel(field.getName(), value, null));
                        } else {
                            final String value = getStringEnumValue(field, valueMethod);
                            enumMembers.add(new EnumMemberModel(field.getName(), value, null));
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(String.format("Cannot get enum values for '%s' enum", enumClass.getName()));
                e.printStackTrace(System.out);
            }
        }

        return new EnumModel(sourceClass.type, isNumberBased ? EnumKind.NumberBased : EnumKind.StringBased, enumMembers, null);
    }

    private Number getNumberEnumValue(Field field, Method valueMethod, int index) throws Exception {
        if (valueMethod != null) {
            final Object valueObject = invokeJsonValueMethod(field, valueMethod);
            if (valueObject instanceof Number) {
                return (Number) valueObject;
            }
        }
        return index;
    }

    private String getStringEnumValue(Field field, Method valueMethod) throws Exception {
        if (valueMethod != null) {
            final Object valueObject = invokeJsonValueMethod(field, valueMethod);
            if (valueObject instanceof String) {
                return (String) valueObject;
            }
        }
        if (field.isAnnotationPresent(JsonProperty.class)) {
            final JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
            if (!jsonProperty.value().equals(JsonProperty.USE_DEFAULT_NAME)) {
                return jsonProperty.value();
            }
        }
        return field.getName();
    }

    private Object invokeJsonValueMethod(Field field, Method valueMethod) throws ReflectiveOperationException {
        field.setAccessible(true);
        final Object constant = field.get(null);
        valueMethod.setAccessible(true);
        final Object valueObject = valueMethod.invoke(constant);
        return valueObject;
    }

}
