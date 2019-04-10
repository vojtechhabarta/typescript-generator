
package cz.habarta.typescript.generator.parser;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.MutableConfigOverride;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import cz.habarta.typescript.generator.ExcludingTypeProcessor;
import cz.habarta.typescript.generator.Jackson2ConfigurationResolved;
import cz.habarta.typescript.generator.OptionalProperties;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TypeProcessor;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.compiler.EnumKind;
import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import cz.habarta.typescript.generator.util.UnionType;
import cz.habarta.typescript.generator.util.Utils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Jackson2Parser extends ModelParser {

    public static class Jackson2ParserFactory extends ModelParser.Factory {

        private final boolean useJaxbAnnotations;

        public Jackson2ParserFactory() {
            this(false);
        }

        private Jackson2ParserFactory(boolean useJaxbAnnotations) {
            this.useJaxbAnnotations = useJaxbAnnotations;
        }

        @Override
        public TypeProcessor getSpecificTypeProcessor() {
            return createSpecificTypeProcessor();
        }

        @Override
        public Jackson2Parser create(Settings settings, TypeProcessor commonTypeProcessor, List<RestApplicationParser> restApplicationParsers) {
            return new Jackson2Parser(settings, commonTypeProcessor, restApplicationParsers, useJaxbAnnotations);
        }

    }

    public static class JaxbParserFactory extends Jackson2ParserFactory {
        
        public JaxbParserFactory() {
            super(true);
        }
        
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Jackson2Parser(Settings settings, TypeProcessor typeProcessor) {
        this(settings, typeProcessor, Collections.emptyList(), false);
    }

    public Jackson2Parser(Settings settings, TypeProcessor commonTypeProcessor, List<RestApplicationParser> restApplicationParsers, boolean useJaxbAnnotations) {
        super(settings, commonTypeProcessor, restApplicationParsers);
        if (settings.jackson2ModuleDiscovery) {
            objectMapper.registerModules(ObjectMapper.findModules(settings.classLoader));
        }
        for (Class<? extends Module> moduleClass : settings.jackson2Modules) {
            try {
                objectMapper.registerModule(moduleClass.getConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(String.format("Cannot instantiate Jackson2 module '%s'", moduleClass.getName()), e);
            }
        }
        if (useJaxbAnnotations) {
            AnnotationIntrospector introspector = new JaxbAnnotationIntrospector(objectMapper.getTypeFactory());
            objectMapper.setAnnotationIntrospector(introspector);
        }
        final Jackson2ConfigurationResolved config = settings.jackson2Configuration;
        if (config != null) {
            setVisibility(PropertyAccessor.FIELD, config.fieldVisibility);
            setVisibility(PropertyAccessor.GETTER, config.getterVisibility);
            setVisibility(PropertyAccessor.IS_GETTER, config.isGetterVisibility);
            setVisibility(PropertyAccessor.SETTER, config.setterVisibility);
            setVisibility(PropertyAccessor.CREATOR, config.creatorVisibility);
            if (config.shapeConfigOverrides != null) {
                config.shapeConfigOverrides.entrySet()
                        .forEach(entry -> setShapeOverride(entry.getKey(), entry.getValue()));
            }
            if (config.enumsUsingToString) {
                objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
                objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
            }
        }
    }

    private void setVisibility(PropertyAccessor accessor, JsonAutoDetect.Visibility visibility) {
        if (visibility != null) {
            objectMapper.setVisibility(accessor, visibility);
        }
    }

    private void setShapeOverride(Class<?> cls, JsonFormat.Shape shape) {
        final MutableConfigOverride configOverride = objectMapper.configOverride(cls);
        configOverride.setFormat(
                JsonFormat.Value.merge(
                        configOverride.getFormat(),
                        JsonFormat.Value.forShape(shape)));
    }

    private static TypeProcessor createSpecificTypeProcessor() {
        return new TypeProcessor.Chain(
                new ExcludingTypeProcessor(Arrays.asList(JsonNode.class.getName())),
                new TypeProcessor() {
                    @Override
                    public TypeProcessor.Result processType(Type javaType, TypeProcessor.Context context) {
                        if (context.getTypeContext() instanceof Jackson2TypeContext) {
                            final Jackson2TypeContext jackson2TypeContext = (Jackson2TypeContext) context.getTypeContext();
                            final Type resultType = jackson2TypeContext.parser.processIdentity(javaType, jackson2TypeContext.beanPropertyWriter);
                            if (resultType != null) {
                                return context.withTypeContext(null).processType(resultType);
                            }
                        }
                        return null;
                    }
                }
        );
    }

    private static class Jackson2TypeContext {
        public final Jackson2Parser parser;
        public final BeanPropertyWriter beanPropertyWriter;

        public Jackson2TypeContext(Jackson2Parser parser, BeanPropertyWriter beanPropertyWriter) {
            this.parser = parser;
            this.beanPropertyWriter = beanPropertyWriter;
        }
    }

    @Override
    protected DeclarationModel parseClass(SourceType<Class<?>> sourceClass) {
        final List<String> classComments = getComments(sourceClass.type.getAnnotation(JsonClassDescription.class));
        if (sourceClass.type.isEnum()) {
            return parseEnumOrObjectEnum(sourceClass, classComments);
        } else {
            return parseBean(sourceClass, classComments);
        }
    }

    private BeanModel parseBean(SourceType<Class<?>> sourceClass, List<String> classComments) {
        final List<PropertyModel> properties = new ArrayList<>();

        final BeanHelper beanHelper = getBeanHelper(sourceClass.type);
        if (beanHelper != null) {
            for (final BeanPropertyWriter beanPropertyWriter : beanHelper.getProperties()) {
                final Member propertyMember = beanPropertyWriter.getMember().getMember();
                checkMember(propertyMember, beanPropertyWriter.getName(), sourceClass.type);
                Type propertyType = getGenericType(propertyMember);
                final List<String> propertyComments = getComments(beanPropertyWriter.getAnnotation(JsonPropertyDescription.class));

                // Map.Entry
                final Class<?> propertyRawClass = Utils.getRawClassOrNull(propertyType);
                if (propertyRawClass != null && Map.Entry.class.isAssignableFrom(propertyRawClass)) {
                    final BeanDescription propertyDescription = objectMapper.getSerializationConfig().introspect(beanPropertyWriter.getType());
                    final JsonFormat.Value formatOverride = objectMapper.getSerializationConfig().getDefaultPropertyFormat(Map.Entry.class);
                    final JsonFormat.Value formatFromAnnotation = propertyDescription.findExpectedFormat(null);
                    final JsonFormat.Value format = JsonFormat.Value.merge(formatFromAnnotation, formatOverride);
                    if (format.getShape() != JsonFormat.Shape.OBJECT) {
                        propertyType = Utils.replaceRawClassInType(propertyType, Map.class);
                    }
                }

                final Jackson2TypeContext jackson2TypeContext = new Jackson2TypeContext(this, beanPropertyWriter);

                if (!isAnnotatedPropertyIncluded(beanPropertyWriter::getAnnotation, sourceClass.type.getName() + "." + beanPropertyWriter.getName())) {
                    continue;
                }
                final boolean optional = settings.optionalProperties == OptionalProperties.useLibraryDefinition
                        ? !beanPropertyWriter.isRequired()
                        : isAnnotatedPropertyOptional(beanPropertyWriter::getAnnotation);
                // @JsonUnwrapped
                PropertyModel.PullProperties pullProperties = null;
                final JsonUnwrapped annotation = beanPropertyWriter.getAnnotation(JsonUnwrapped.class);
                if (annotation != null && annotation.enabled()) {
                    pullProperties = new PropertyModel.PullProperties(annotation.prefix(), annotation.suffix());
                }
                properties.add(processTypeAndCreateProperty(beanPropertyWriter.getName(), propertyType, jackson2TypeContext, optional, sourceClass.type, propertyMember, pullProperties, propertyComments));
            }
        }
        if (sourceClass.type.isEnum()) {
            return new BeanModel(sourceClass.type, null, null, null, null, null, properties, classComments);
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
        return new BeanModel(sourceClass.type, superclass, taggedUnionClasses, discriminantProperty, discriminantLiteral, interfaces, properties, classComments);
    }

    // @JsonIdentityInfo and @JsonIdentityReference
    private Type processIdentity(Type propertyType, BeanPropertyWriter propertyWriter) {

        final Class<?> clsT = Utils.getRawClassOrNull(propertyType);
        final Class<?> clsW = propertyWriter.getType().getRawClass();
        final Class<?> cls = clsT != null ? clsT : clsW;

        if (cls != null) {
            final JsonIdentityInfo identityInfoC = cls.getAnnotation(JsonIdentityInfo.class);
            final JsonIdentityInfo identityInfoP = propertyWriter.getAnnotation(JsonIdentityInfo.class);
            final JsonIdentityInfo identityInfo = identityInfoP != null ? identityInfoP : identityInfoC;
            if (identityInfo == null) {
                return null;
            }
            final JsonIdentityReference identityReferenceC = cls.getAnnotation(JsonIdentityReference.class);
            final JsonIdentityReference identityReferenceP = propertyWriter.getAnnotation(JsonIdentityReference.class);
            final JsonIdentityReference identityReference = identityReferenceP != null ? identityReferenceP : identityReferenceC;
            final boolean alwaysAsId = identityReference != null && identityReference.alwaysAsId();

            final Type idType;
            if (identityInfo.generator() == ObjectIdGenerators.None.class) {
                return null;
            } else if (identityInfo.generator() == ObjectIdGenerators.PropertyGenerator.class) {
                final BeanHelper beanHelper = getBeanHelper(cls);
                if (beanHelper == null) {
                    return null;
                }
                final BeanPropertyWriter[] properties = beanHelper.getProperties();
                final Optional<BeanPropertyWriter> idProperty = Stream.of(properties)
                        .filter(p -> p.getName().equals(identityInfo.property()))
                        .findFirst();
                if (idProperty.isPresent()) {
                    final BeanPropertyWriter idPropertyWriter = idProperty.get();
                    final Member idPropertyMember = idPropertyWriter.getMember().getMember();
                    checkMember(idPropertyMember, idPropertyWriter.getName(), cls);
                    idType = getGenericType(idPropertyMember);
                } else {
                    return null;
                }
            } else if (identityInfo.generator() == ObjectIdGenerators.IntSequenceGenerator.class) {
                idType = Integer.class;
            } else if (identityInfo.generator() == ObjectIdGenerators.UUIDGenerator.class) {
                idType = String.class;
            } else if (identityInfo.generator() == ObjectIdGenerators.StringIdGenerator.class) {
                idType = String.class;
            } else {
                idType = Object.class;
            }
            return alwaysAsId
                    ? idType
                    : new UnionType(propertyType, idType);
        }
        return null;
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

    private DeclarationModel parseEnumOrObjectEnum(SourceType<Class<?>> sourceClass, List<String> classComments) {
        final JsonFormat jsonFormat = sourceClass.type.getAnnotation(JsonFormat.class);
        if (jsonFormat != null && jsonFormat.shape() == JsonFormat.Shape.OBJECT) {
            return parseBean(sourceClass, classComments);
        }
        final boolean isNumberBased = jsonFormat != null && (
                jsonFormat.shape() == JsonFormat.Shape.NUMBER ||
                jsonFormat.shape() == JsonFormat.Shape.NUMBER_FLOAT ||
                jsonFormat.shape() == JsonFormat.Shape.NUMBER_INT);

        final List<EnumMemberModel> enumMembers = new ArrayList<>();
        if (sourceClass.type.isEnum()) {
            final Class<?> enumClass = (Class<?>) sourceClass.type;
            final Field[] allEnumFields = enumClass.getDeclaredFields();
            final List<Field> constants = Arrays.stream(allEnumFields).filter(Field::isEnumConstant).collect(Collectors.toList());
            for (Field constant : constants) {
                Object value;
                try {
                    constant.setAccessible(true);
                    final String enumJson = objectMapper.writeValueAsString(constant.get(null));
                    value = objectMapper.readValue(enumJson, new TypeReference<Object>(){});
                } catch (Throwable e) {
                    TypeScriptGenerator.getLogger().error(String.format("Cannot get enum value for constant '%s.%s'", enumClass.getName(), constant.getName()));
                    TypeScriptGenerator.getLogger().verbose(Utils.exceptionToString(e));
                    value = constant.getName();
                }

                final List<String> constantComments = getComments(constant.getAnnotation(JsonPropertyDescription.class));
                if (value instanceof String) {
                    enumMembers.add(new EnumMemberModel(constant.getName(), (String) value, constantComments));
                } else if (value instanceof Number) {
                    enumMembers.add(new EnumMemberModel(constant.getName(), (Number) value, constantComments));
                } else {
                    TypeScriptGenerator.getLogger().warning(String.format("'%s' enum as a @JsonValue that isn't a String or Number, ignoring", enumClass.getName()));
                }
            }
        }

        return new EnumModel(sourceClass.type, isNumberBased ? EnumKind.NumberBased : EnumKind.StringBased, enumMembers, classComments);
    }

    private static List<String> getComments(JsonClassDescription classDescriptionAnnotation) {
        final String propertyDescriptionValue = classDescriptionAnnotation != null ? classDescriptionAnnotation.value() : null;
        final List<String> classComments = Utils.splitMultiline(propertyDescriptionValue, false);
        return classComments;
    }

    private static List<String> getComments(JsonPropertyDescription propertyDescriptionAnnotation) {
        final String propertyDescriptionValue = propertyDescriptionAnnotation != null ? propertyDescriptionAnnotation.value() : null;
        final List<String> propertyComments = Utils.splitMultiline(propertyDescriptionValue, false);
        return propertyComments;
    }

}
