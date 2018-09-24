
package cz.habarta.typescript.generator.parser;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import cz.habarta.typescript.generator.Jackson2Configuration;
import cz.habarta.typescript.generator.OptionalProperties;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TypeProcessor;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.compiler.EnumKind;
import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import cz.habarta.typescript.generator.util.Predicate;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class Jackson2Parser extends ModelParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Jackson2Parser(Settings settings, TypeProcessor typeProcessor) {
        this(settings, typeProcessor, false);
        final Jackson2Configuration config = settings.jackson2Configuration;
        if (config != null) {
            setVisibility(PropertyAccessor.FIELD, config.fieldVisibility);
            setVisibility(PropertyAccessor.GETTER, config.getterVisibility);
            setVisibility(PropertyAccessor.IS_GETTER, config.isGetterVisibility);
            setVisibility(PropertyAccessor.SETTER, config.setterVisibility);
            setVisibility(PropertyAccessor.CREATOR, config.creatorVisibility);
        }
    }

    private void setVisibility(PropertyAccessor accessor, JsonAutoDetect.Visibility visibility) {
        if (visibility != null) {
            objectMapper.setVisibility(accessor, visibility);
        }
    }

    public Jackson2Parser(Settings settings, TypeProcessor typeProcessor, boolean useJaxbAnnotations) {
        super(settings, typeProcessor);
        if (settings.jackson2ModuleDiscovery) {
            objectMapper.registerModules(ObjectMapper.findModules(settings.classLoader));
        }
        for (Class<? extends Module> moduleClass : settings.jackson2Modules) {
            try {
                objectMapper.registerModule(moduleClass.newInstance());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(String.format("Cannot instantiate Jackson2 module '%s'", moduleClass.getName()), e);
            }
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
            for (final BeanPropertyWriter beanPropertyWriter : beanHelper.getProperties()) {
                final Member propertyMember = beanPropertyWriter.getMember().getMember();
                checkMember(propertyMember, beanPropertyWriter.getName(), sourceClass.type);
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
                        TypeScriptGenerator.getLogger().info("Skipping " + sourceClass.type + "." + beanPropertyWriter.getName() + " because it is missing an annotation from includePropertyAnnotations!");
                        continue;
                    }
                }
                final boolean optional = settings.optionalProperties == OptionalProperties.useLibraryDefinition
                        ? !beanPropertyWriter.isRequired()
                        : isAnnotatedPropertyOptional(new AnnotatedProperty() {
                            @Override
                            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                                return beanPropertyWriter.getAnnotation(annotationClass);
                            }
                        });
                // @JsonUnwrapped
                PropertyModel.PullProperties pullProperties = null;
                final JsonUnwrapped annotation = beanPropertyWriter.getAnnotation(JsonUnwrapped.class);
                if (annotation != null && annotation.enabled()) {
                    pullProperties = new PropertyModel.PullProperties(annotation.prefix(), annotation.suffix());
                }
                properties.add(processTypeAndCreateProperty(beanPropertyWriter.getName(), propertyType, optional, sourceClass.type, propertyMember, pullProperties));
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
            discriminantLiteral = getTypeName(parentJsonTypeInfo, sourceClass.type);
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

    private String getTypeName(JsonTypeInfo parentJsonTypeInfo, final Class<?> cls) {
        // Id.CLASS
        if (parentJsonTypeInfo.use() == JsonTypeInfo.Id.CLASS) {
            return cls.getName();
        }
        // find @JsonTypeName recursively
        final JsonTypeName jsonTypeName = getAnnotationRecursive(cls, JsonTypeName.class);
        if (jsonTypeName != null && !jsonTypeName.value().isEmpty()) {
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
                    TypeScriptGenerator.getLogger().verbose(String.format("Unknown serializer '%s' for class '%s'", jsonSerializerName, beanClass));
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
                Field valueField = null;

                Field[] allEnumFields = enumClass.getDeclaredFields();
                List<Field> constants = Arrays.stream(allEnumFields).filter(Field::isEnumConstant).collect(Collectors.toList());

                final BeanInfo beanInfo = Introspector.getBeanInfo(enumClass);
                valueMethod = Arrays.stream(beanInfo.getMethodDescriptors())
                        .map(MethodDescriptor::getMethod)
                        .filter(method -> method.isAnnotationPresent(JsonValue.class))
                        .findAny().orElse(null);

                if (valueMethod == null) {
                    List<Field> instanceFields = Arrays.stream(allEnumFields).filter(field -> !field.isEnumConstant()).collect(Collectors.toList());
                    valueField = instanceFields.stream()
                            .filter(field -> field.isAnnotationPresent(JsonValue.class))
                            .findAny().orElse(null);
                }

                int index = 0;
                for (Field constant : constants) {
                    Object value;
                    if (valueField != null) {
                        value = getFieldJsonValue(constant, valueField);
                    } else if (isNumberBased) {
                        value = getNumberEnumValue(constant, valueMethod, index++);
                    } else {
                        value = getMethodEnumValue(constant, valueMethod);
                    }

                    if (value instanceof String) {
                        enumMembers.add(new EnumMemberModel(constant.getName(), (String) value, null));
                    } else if (value instanceof Number) {
                        enumMembers.add(new EnumMemberModel(constant.getName(), (Number) value, null));
                    } else {
                        TypeScriptGenerator.getLogger().warning(String.format("'%s' enum as a @JsonValue that isn't a String or Number, ignoring", enumClass.getName()));
                    }
                }
            } catch (Exception e) {
                TypeScriptGenerator.getLogger().error(String.format("Cannot get enum values for '%s' enum", enumClass.getName()));
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

    private Object getMethodEnumValue(Field field, Method valueMethod) throws Exception {
        if (valueMethod != null) {
            final Object valueObject = invokeJsonValueMethod(field, valueMethod);
            if (valueObject instanceof String || valueObject instanceof Number) {
                return valueObject;
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

    private Object getFieldJsonValue(Field field, Field jsonValueField) throws ReflectiveOperationException {
        field.setAccessible(true);
        final Object constant = field.get(null);
        jsonValueField.setAccessible(true);
        return jsonValueField.get(constant);
    }
}
