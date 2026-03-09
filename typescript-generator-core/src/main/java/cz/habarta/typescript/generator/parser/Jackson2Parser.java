
package cz.habarta.typescript.generator.parser;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.cfg.MutableConfigOverride;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.databind.deser.CreatorProperty;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.deser.impl.BeanPropertyMap;
import com.fasterxml.jackson.databind.deser.std.StdDelegatingDeserializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import cz.habarta.typescript.generator.ExcludingTypeProcessor;
import cz.habarta.typescript.generator.Jackson2ConfigurationResolved;
import cz.habarta.typescript.generator.OptionalProperties;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.TypeProcessor;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.compiler.EnumKind;
import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import cz.habarta.typescript.generator.type.JUnionType;
import cz.habarta.typescript.generator.util.Pair;
import cz.habarta.typescript.generator.util.PropertyMember;
import cz.habarta.typescript.generator.util.Utils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
            final AnnotationIntrospector original = objectMapper.getSerializationConfig().getAnnotationIntrospector();
            final AnnotationIntrospector jakartaIntrospector = new JakartaXmlBindAnnotationIntrospector(objectMapper.getTypeFactory());
            final AnnotationIntrospector javaxIntrospector = new JaxbAnnotationIntrospector(objectMapper.getTypeFactory());
            final AnnotationIntrospectorPair jaxbPair = new AnnotationIntrospectorPair(jakartaIntrospector, javaxIntrospector);
            final AnnotationIntrospectorPair pair = new AnnotationIntrospectorPair(jaxbPair, original);
            objectMapper.setAnnotationIntrospector(pair);
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
                            final Jackson2ConfigurationResolved config = jackson2TypeContext.parser.settings.jackson2Configuration;
                            // JsonSerialize
                            final JsonSerialize jsonSerialize = jackson2TypeContext.beanProperty.getAnnotation(JsonSerialize.class);
                            if (jsonSerialize != null && config != null && config.serializerTypeMappings != null) {
                                @SuppressWarnings("unchecked")
                                final Class<? extends JsonSerializer<?>> using = (Class<? extends JsonSerializer<?>>)
                                    (context.isInsideCollection() ? jsonSerialize.contentUsing() : jsonSerialize.using());
                                final String mappedType = config.serializerTypeMappings.get(using);
                                if (mappedType != null) {
                                    return new TypeProcessor.Result(new TsType.VerbatimType(mappedType));
                                }
                            }
                            // JsonDeserialize
                            final JsonDeserialize jsonDeserialize = jackson2TypeContext.beanProperty.getAnnotation(JsonDeserialize.class);
                            if (jsonDeserialize != null && config != null && config.deserializerTypeMappings != null) {
                                @SuppressWarnings("unchecked")
                                final Class<? extends JsonDeserializer<?>> using = (Class<? extends JsonDeserializer<?>>)
                                    (context.isInsideCollection() ? jsonDeserialize.contentUsing() : jsonDeserialize.using());
                                final String mappedType = config.deserializerTypeMappings.get(using);
                                if (mappedType != null) {
                                    return new TypeProcessor.Result(new TsType.VerbatimType(mappedType));
                                }
                            }
                            // disableObjectIdentityFeature
                            if (!jackson2TypeContext.disableObjectIdentityFeature) {
                                final Type resultType = jackson2TypeContext.parser.processIdentity(javaType, jackson2TypeContext.beanProperty);
                                if (resultType != null) {
                                    return context.withTypeContext(null).processType(resultType);
                                }
                            }
                            // Map.Entry
                            final Class<?> rawClass = Utils.getRawClassOrNull(javaType);
                            if (rawClass != null && Map.Entry.class.isAssignableFrom(rawClass)) {
                                final ObjectMapper objectMapper = jackson2TypeContext.parser.objectMapper;
                                final SerializationConfig serializationConfig = objectMapper.getSerializationConfig();
                                final BeanDescription beanDescription = serializationConfig
                                        .introspect(TypeFactory.defaultInstance().constructType(rawClass));
                                final JsonFormat.Value formatOverride = serializationConfig.getDefaultPropertyFormat(Map.Entry.class);
                                final JsonFormat.Value formatFromAnnotation = beanDescription.findExpectedFormat(null);
                                final JsonFormat.Value format = JsonFormat.Value.merge(formatFromAnnotation, formatOverride);
                                if (format.getShape() != JsonFormat.Shape.OBJECT) {
                                    final Type mapType = Utils.replaceRawClassInType(javaType, Map.class);
                                    return context.processType(mapType);
                                }
                            }
                        }
                        return null;
                    }
                }
        );
    }

    private static class Jackson2TypeContext {
        public final Jackson2Parser parser;
        public final BeanProperty beanProperty;
        public final boolean disableObjectIdentityFeature;

        public Jackson2TypeContext(Jackson2Parser parser, BeanProperty beanProperty, boolean disableObjectIdentityFeature) {
            this.parser = parser;
            this.beanProperty = beanProperty;
            this.disableObjectIdentityFeature = disableObjectIdentityFeature;
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

        final Class<?> view = settings.jackson2Configuration != null ? settings.jackson2Configuration.view : null;
        final BeanHelpers beanHelpers = getBeanHelpers(sourceClass.type, view);
        if (beanHelpers != null) {
            for (final Pair<BeanProperty, PropertyAccess> pair : beanHelpers.getPropertiesAndAccess()) {
                final BeanProperty beanProperty = pair.getValue1();
                final PropertyAccess access = pair.getValue2();
                final Member member = beanProperty.getMember().getMember();
                final PropertyMember propertyMember = wrapMember(settings.getTypeParser(), member, getCreatorIndex(beanProperty), beanProperty::getAnnotation, beanProperty.getName(), sourceClass.type);
                if (propertyMember == null) {
                    continue;
                }
                Type propertyType = propertyMember.getType();
                final List<String> propertyComments = getComments(beanProperty.getAnnotation(JsonPropertyDescription.class));

                final Jackson2TypeContext jackson2TypeContext = new Jackson2TypeContext(
                        this,
                        beanProperty,
                        settings.jackson2Configuration != null && settings.jackson2Configuration.disableObjectIdentityFeature);

                if( settings.includePropertyFunctionImpl != null && !settings.includePropertyFunctionImpl.apply( member ) ) {
                    continue;
                }
                
                if (!isAnnotatedPropertyIncluded(beanProperty::getAnnotation, sourceClass.type.getName() + "." + beanProperty.getName())) {
                    continue;
                }
                final boolean optional = settings.optionalProperties == OptionalProperties.useLibraryDefinition
                        ? !beanProperty.isRequired()
                        : isPropertyOptional(propertyMember);
                // @JsonUnwrapped
                PropertyModel.PullProperties pullProperties = null;
                final JsonUnwrapped annotation = beanProperty.getAnnotation(JsonUnwrapped.class);
                if (annotation != null && annotation.enabled()) {
                    pullProperties = new PropertyModel.PullProperties(annotation.prefix(), annotation.suffix());
                }
                properties.add(processTypeAndCreateProperty(beanProperty.getName(), propertyType, jackson2TypeContext, optional, access, sourceClass.type, member, pullProperties, propertyComments));
            }
        }
        if (sourceClass.type.isEnum()) {
            return new BeanModel(sourceClass.type, null, null, null, null, null, properties, classComments);
        }

        final String discriminantProperty;
        final boolean syntheticDiscriminantProperty;
        final String discriminantLiteral;

        final Pair<Class<?>, JsonTypeInfo> classWithJsonTypeInfo = Pair.of(sourceClass.type, sourceClass.type.getAnnotation(JsonTypeInfo.class));
        final Pair<Class<?>, JsonTypeInfo> parentClassWithJsonTypeInfo;
        final boolean isTaggedUnionParent = isTaggedUnion(classWithJsonTypeInfo);
        if (isTaggedUnionParent) {
            // this is parent
            final JsonTypeInfo jsonTypeInfo = classWithJsonTypeInfo.getValue2();
            discriminantProperty = getDiscriminantPropertyName(jsonTypeInfo);
            syntheticDiscriminantProperty = isDiscriminantPropertySynthetic(jsonTypeInfo);
            discriminantLiteral = isInterfaceOrAbstract(sourceClass.type) ? null : getTypeName(sourceClass.type);
        } else if (isTaggedUnion(parentClassWithJsonTypeInfo = getAnnotationRecursive(sourceClass.type, JsonTypeInfo.class))) {
            // this is child class
            final JsonTypeInfo parentJsonTypeInfo = parentClassWithJsonTypeInfo.getValue2();
            discriminantProperty = getDiscriminantPropertyName(parentJsonTypeInfo);
            syntheticDiscriminantProperty = isDiscriminantPropertySynthetic(parentJsonTypeInfo);
            discriminantLiteral = getTypeName(sourceClass.type);
        } else {
            // not part of explicit hierarchy
            discriminantProperty = null;
            syntheticDiscriminantProperty = false;
            discriminantLiteral = null;
        }

        if (discriminantProperty != null) {
            final PropertyModel foundDiscriminantProperty = properties.stream()
                    .filter(property -> Objects.equals(property.getName(), discriminantProperty))
                    .findFirst()
                    .orElse(null);
            if (foundDiscriminantProperty != null) {
                if (syntheticDiscriminantProperty) {
                    TypeScriptGenerator.getLogger().warning(String.format(
                            "Class '%s' has duplicate property '%s'. "
                                    + "For more information see 'https://github.com/vojtechhabarta/typescript-generator/issues/392'.",
                            sourceClass.type.getName(), discriminantProperty));
                } else {
                    properties.remove(foundDiscriminantProperty);
                }
            }
        }

        final List<Class<?>> taggedUnionClasses = getSubClassesFromAnnotation(sourceClass.type)
                .or(() -> isTaggedUnionParent ? getSubClassesFromResolver(sourceClass.type) : Optional.empty())
                .orElse(null);
        if (taggedUnionClasses != null) {
            taggedUnionClasses.forEach(subClass -> addBeanToQueue(new SourceType<>(subClass, sourceClass.type, "<subClass>")));
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

    private static Integer getCreatorIndex(BeanProperty beanProperty) {
        if (beanProperty instanceof CreatorProperty) {
            final CreatorProperty creatorProperty = (CreatorProperty) beanProperty;
            return creatorProperty.getCreatorIndex();
        } else {
            return null;
        }
    }

    // @JsonIdentityInfo and @JsonIdentityReference
    private Type processIdentity(Type propertyType, BeanProperty beanProperty) {

        final Class<?> clsT = Utils.getRawClassOrNull(propertyType);
        final Class<?> clsW = beanProperty.getType().getRawClass();
        final Class<?> cls = clsT != null ? clsT : clsW;

        if (cls != null) {
            final JsonIdentityInfo identityInfoC = getAnnotationRecursive(cls, JsonIdentityInfo.class).getValue2();
            final JsonIdentityInfo identityInfoP = beanProperty.getAnnotation(JsonIdentityInfo.class);
            final JsonIdentityInfo identityInfo = identityInfoP != null ? identityInfoP : identityInfoC;
            if (identityInfo == null) {
                return null;
            }
            final JsonIdentityReference identityReferenceC = getAnnotationRecursive(cls, JsonIdentityReference.class).getValue2();
            final JsonIdentityReference identityReferenceP = beanProperty.getAnnotation(JsonIdentityReference.class);
            final JsonIdentityReference identityReference = identityReferenceP != null ? identityReferenceP : identityReferenceC;
            final boolean alwaysAsId = identityReference != null && identityReference.alwaysAsId();

            final Type idType;
            if (identityInfo.generator() == ObjectIdGenerators.None.class) {
                return null;
            } else if (identityInfo.generator() == ObjectIdGenerators.PropertyGenerator.class) {
                final BeanHelpers beanHelpers = getBeanHelpers(cls, null);
                if (beanHelpers == null) {
                    return null;
                }
                final List<BeanProperty> properties = beanHelpers.getProperties();
                final Optional<BeanProperty> idPropertyOptional = properties.stream()
                        .filter(p -> p.getName().equals(identityInfo.property()))
                        .findFirst();
                if (idPropertyOptional.isPresent()) {
                    final BeanProperty idProperty = idPropertyOptional.get();
                    final Member idMember = idProperty.getMember().getMember();
                    final PropertyMember idPropertyMember = wrapMember(settings.getTypeParser(), idMember, getCreatorIndex(idProperty), idProperty::getAnnotation, idProperty.getName(), cls);
                    idType = idPropertyMember != null ? idPropertyMember.getType() : Object.class;
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
                    : new JUnionType(propertyType, idType);
        }
        return null;
    }

    private boolean isTaggedUnion(Pair<Class<?>, JsonTypeInfo> classWithJsonTypeInfo) {
        final Class<?> cls = classWithJsonTypeInfo.getValue1();
        final JsonTypeInfo jsonTypeInfo = classWithJsonTypeInfo.getValue2();
        if (cls == null || Utils.hasAnyAnnotation(cls::getAnnotation, settings.disableTaggedUnionAnnotations)) {
            return false;
        }
        return jsonTypeInfo != null &&
                (jsonTypeInfo.include() == JsonTypeInfo.As.PROPERTY || jsonTypeInfo.include() == JsonTypeInfo.As.EXISTING_PROPERTY) &&
                (jsonTypeInfo.use() == JsonTypeInfo.Id.NAME || jsonTypeInfo.use() == JsonTypeInfo.Id.CLASS);
    }

    private boolean isDiscriminantPropertySynthetic(JsonTypeInfo jsonTypeInfo) {
        return jsonTypeInfo.include() == JsonTypeInfo.As.PROPERTY;
    }

    private String getDiscriminantPropertyName(JsonTypeInfo jsonTypeInfo) {
        return jsonTypeInfo.property().isEmpty()
                ? jsonTypeInfo.use().getDefaultPropertyName()
                : jsonTypeInfo.property();
    }

    private String getTypeName(Class<?> cls) {
        try {
            final SerializationConfig config = objectMapper.getSerializationConfig();
            final JavaType javaType = config.constructType(cls);
            final TypeSerializer typeSerializer = objectMapper.getSerializerProviderInstance().findTypeSerializer(javaType);
            final TypeIdResolver typeIdResolver = typeSerializer.getTypeIdResolver();
            if (typeIdResolver.getMechanism() == JsonTypeInfo.Id.NAME) {
                final List<NamedType> subtypes = getSubtypesFromResolver(cls);
                final String typeName = subtypes.stream()
                        .filter(subtype -> Objects.equals(subtype.getType(), cls))
                        .filter(NamedType::hasName)
                        .map(NamedType::getName)
                        .findFirst()
                        .orElse(null);
                if (typeName == null) {
                    return isInterfaceOrAbstract(cls) ? null : typeIdResolver.idFromBaseType();
                } else {
                    return typeName;
                }
            } else {
                return typeIdResolver.idFromBaseType();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private Optional<List<Class<?>>> getSubClassesFromAnnotation(Class<?> cls) {
        return Optional.ofNullable(cls.getAnnotation(JsonSubTypes.class))
                .map(jsonSubTypes -> Arrays.stream(jsonSubTypes.value())
                        .map(jsonSubType -> jsonSubType.value())
                        .collect(Collectors.toList()));
    }

    private Optional<List<Class<?>>> getSubClassesFromResolver(Class<?> cls) {
        final List<NamedType> subtypes = getSubtypesFromResolver(cls);
        final List<Class<?>> subClasses = subtypes.stream()
            .map(subtype -> subtype.getType())
            .filter(subClass -> !Objects.equals(subClass, cls))
            .collect(Collectors.toList());
        return subClasses.isEmpty() ? Optional.empty() : Optional.of(subClasses);
    }

    /**
     * @return subtypes of specified class including the class itself
     */
    private List<NamedType> getSubtypesFromResolver(Class<?> cls) {
        final SerializationConfig config = objectMapper.getSerializationConfig();
        final SubtypeResolver subtypeResolver = config.getSubtypeResolver();
        final BeanDescription beanDescription = config.introspectClassAnnotations(cls);
        final AnnotatedClass annotatedClass = beanDescription.getClassInfo();
        final Collection<NamedType> deserializationSubtypes = subtypeResolver.collectAndResolveSubtypesByTypeId(config, annotatedClass);
        final Collection<NamedType> serializationSubtypes = subtypeResolver.collectAndResolveSubtypesByClass(config, annotatedClass);
        final LinkedHashSet<NamedType> subtypes = Stream
                .concat(deserializationSubtypes.stream(), serializationSubtypes.stream())
                .filter(namedType -> cls.isAssignableFrom(namedType.getType()))  // `SubtypeResolver` returns all types from `JsonSubTypes` annotations, not only subtypes
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new ArrayList<>(subtypes);
    }

    private boolean isInterfaceOrAbstract(Class<?> cls) {
        return cls.isInterface() || Modifier.isAbstract(cls.getModifiers());
    }

    private static <T extends Annotation> Pair<Class<?>, T> getAnnotationRecursive(Class<?> cls, Class<T> annotationClass) {
        return getAnnotationRecursive(cls, annotationClass, null);
    }

    private static <T extends Annotation> Pair<Class<?>, T> getAnnotationRecursive(Class<?> cls, Class<T> annotationClass, Predicate<T> annotationFilter) {
        if (cls == null) {
            return Pair.of(null, null);
        }
        final T annotation = cls.getAnnotation(annotationClass);
        if (annotation != null && (annotationFilter == null || annotationFilter.test(annotation))) {
            return Pair.of(cls, annotation);
        }
        for (Class<?> aInterface : cls.getInterfaces()) {
            final Pair<Class<?>, T> classWithAnnotation = getAnnotationRecursive(aInterface, annotationClass, annotationFilter);
            if (classWithAnnotation.getValue2() != null) {
                return classWithAnnotation;
            }
        }
        final Pair<Class<?>, T> classWithAnnotation = getAnnotationRecursive(cls.getSuperclass(), annotationClass, annotationFilter);
        if (classWithAnnotation.getValue2() != null) {
            return classWithAnnotation;
        }
        return Pair.of(null, null);
    }

    private BeanHelpers getBeanHelpers(Class<?> beanClass, Class<?> view) {
        if (beanClass == null) {
            return null;
        }
        if (beanClass == Enum.class) {
            return null;
        }
        final JavaType javaType = objectMapper.constructType(beanClass);
        final BeanSerializerHelper beanSerializerHelper = createBeanSerializerHelper(javaType);
        final BeanDeserializerHelper beanDeserializerHelper = createBeanDeserializerHelper(javaType);
        if (beanSerializerHelper != null || beanDeserializerHelper != null) {
            return new BeanHelpers(beanClass, view, beanSerializerHelper, beanDeserializerHelper);
        }
        return null;
    }

    private BeanSerializerHelper createBeanSerializerHelper(JavaType javaType) {
        try {
            final DefaultSerializerProvider.Impl serializerProvider = new DefaultSerializerProvider.Impl()
                    .createInstance(objectMapper.getSerializationConfig(), objectMapper.getSerializerFactory());
            JsonSerializer<?> jsonSerializer = BeanSerializerFactory.instance.createSerializer(serializerProvider, javaType);
            if (jsonSerializer != null && jsonSerializer instanceof StdDelegatingSerializer) {
                jsonSerializer = ((StdDelegatingSerializer ) jsonSerializer).getDelegatee();
            }
            if (jsonSerializer != null && jsonSerializer instanceof BeanSerializer) {
                return new BeanSerializerHelper((BeanSerializer) jsonSerializer);
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private BeanDeserializerHelper createBeanDeserializerHelper(JavaType javaType) {
        try {
            final DeserializationContext deserializationContext = new DefaultDeserializationContext.Impl(objectMapper.getDeserializationContext().getFactory())
                    .createInstance(objectMapper.getDeserializationConfig(), null, null);
            final BeanDescription beanDescription = deserializationContext.getConfig().introspect(javaType);
            JsonDeserializer<?> jsonDeserializer = BeanDeserializerFactory.instance.createBeanDeserializer(deserializationContext, javaType, beanDescription);
            if (jsonDeserializer != null && jsonDeserializer instanceof StdDelegatingDeserializer) {
                jsonDeserializer = ((StdDelegatingDeserializer<?> ) jsonDeserializer).getDelegatee();
            }
            if (jsonDeserializer != null && jsonDeserializer instanceof BeanDeserializer) {
                return new BeanDeserializerHelper((BeanDeserializer) jsonDeserializer);
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    // for tests
    protected List<BeanProperty> getBeanProperties(Class<?> beanClass) {
        return getBeanHelpers(beanClass, null).getProperties();
    }

    private static class BeanHelpers {
        public final Class<?> beanClass;
        public final Class<?> view;
        public final BeanSerializerHelper serializer;
        public final BeanDeserializerHelper deserializer;

        public BeanHelpers(Class<?> beanClass, Class<?> view, BeanSerializerHelper serializer, BeanDeserializerHelper deserializer) {
            this.beanClass = beanClass;
            this.view = view;
            this.serializer = serializer;
            this.deserializer = deserializer;
        }

        public List<BeanProperty> getProperties() {
            return getPropertiesAndAccess().stream()
                    .map(Pair::getValue1)
                    .collect(Collectors.toList());
        }

        public List<Pair<BeanProperty, PropertyAccess>> getPropertiesAndAccess() {
            return getPropertiesPairs().stream()
                    .map(pair -> pair.getValue1() != null
                            ? Pair.of(pair.getValue1(), pair.getValue2() != null ? PropertyAccess.ReadWrite : PropertyAccess.ReadOnly)
                            : Pair.of(pair.getValue2(), PropertyAccess.WriteOnly)
                    )
                    .collect(Collectors.toList());
        }

        private List<Pair<BeanProperty, BeanProperty>> getPropertiesPairs() {
            final List<BeanProperty> serializableProperties = getSerializableProperties();
            final List<BeanProperty> deserializableProperties = getDeserializableProperties();
            final List<Pair<BeanProperty, BeanProperty>> properties = Stream
                    .concat(
                            serializableProperties.stream()
                                    .filter(this::inView)
                                    .map(property -> Pair.of(property, getBeanProperty(deserializableProperties, property.getName()))),
                            deserializableProperties.stream()
                                    .filter(this::inView)
                                    .filter(property -> getBeanProperty(serializableProperties, property.getName()) == null)
                                    .map(property -> Pair.of((BeanProperty) null, property))
                    )
                    .collect(Collectors.toCollection(ArrayList::new));

            // sort
            final Comparator<Pair<BeanProperty, BeanProperty>> bySerializationOrder = (pair1, pair2) ->
                    pair1.getValue1() != null && pair2.getValue1() != null
                            ? Integer.compare(
                                    serializableProperties.indexOf(pair1.getValue1()),
                                    serializableProperties.indexOf(pair2.getValue1()))
                            : 0;
            final Comparator<Pair<BeanProperty, BeanProperty>> byIndex = Comparator.comparing(
                    pair -> getIndex(pair),
                    Comparator.nullsLast(Comparator.naturalOrder()));
            final List<Field> fields = Utils.getAllFields(beanClass);
            final Comparator<Pair<BeanProperty, BeanProperty>> byFieldIndex = Comparator.comparing(
                    pair -> getFieldIndex(fields, pair),
                    Comparator.nullsLast(Comparator.naturalOrder()));
            properties.sort(bySerializationOrder
                    .thenComparing(byIndex)
                    .thenComparing(byFieldIndex));
            return properties;
        }

        private boolean inView(BeanProperty beanProperty) {
            if (view == null) {
                return true;
            }
            final JsonView annotation = beanProperty.getAnnotation(JsonView.class);
            if (annotation == null || annotation.value().length == 0) {
                return true;
            }
            return Stream.of(annotation.value())
                    .anyMatch(v -> v.isAssignableFrom(view));
        }

        private static BeanProperty getBeanProperty(List<BeanProperty> properties, String name) {
            return properties.stream()
                    .filter(dp -> Objects.equals(dp.getName(), name))
                    .findFirst()
                    .orElse(null);
        }

        private static Integer getIndex(Pair<BeanProperty, BeanProperty> pair) {
            final Integer index1 = getIndex(pair.getValue1());
            return index1 != null ? index1 : getIndex(pair.getValue2());
        }

        private static Integer getIndex(BeanProperty property) {
            if (property == null) {
                return null;
            }
            return property.getMetadata().getIndex();
        }

        private static Integer getFieldIndex(List<Field> fields, Pair<BeanProperty, BeanProperty> pair) {
            final Integer fieldIndex1 = getFieldIndex(fields, pair.getValue1());
            return fieldIndex1 != null ? fieldIndex1 : getFieldIndex(fields, pair.getValue2());
        }

        private static Integer getFieldIndex(List<Field> fields, BeanProperty property) {
            if (property == null) {
                return null;
            }
            final int index = fields.indexOf(property.getMember().getMember());
            return index != -1 ? index : null;
        }

        private List<BeanProperty> getSerializableProperties() {
            return serializer != null
                    ? Arrays.asList(serializer.getProps())
                    : Collections.emptyList();
        }

        private List<BeanProperty> getDeserializableProperties() {
            return deserializer != null
                    ? Arrays.asList(deserializer.getBeanProperties().getPropertiesInInsertionOrder())
                    : Collections.emptyList();
        }
    }

    private static class BeanSerializerHelper extends BeanSerializer {
        private static final long serialVersionUID = 1;

        public BeanSerializerHelper(BeanSerializer src) {
            super(src);
        }

        public BeanPropertyWriter[] getProps() {
            return _props;
        }
    }

    private static class BeanDeserializerHelper extends BeanDeserializer {
        private static final long serialVersionUID = 1;

        public BeanDeserializerHelper(BeanDeserializer src) {
            super(src);
        }

        public BeanPropertyMap getBeanProperties() {
            return _beanProperties;
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
            @SuppressWarnings("unchecked")
            final Class<Enum<?>> enumClass = (Class<Enum<?>>) sourceClass.type;
            final List<Field> constants = Arrays.stream(enumClass.getEnumConstants()).map(e -> {
                try {
                    return enumClass.getDeclaredField(e.name());
                } catch (NoSuchFieldException noSuchFieldException) {
                    throw new RuntimeException(noSuchFieldException);
                }
            }).collect(Collectors.toList());
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
                    enumMembers.add(new EnumMemberModel(constant.getName(), (String) value, constant, constantComments));
                } else if (value instanceof Number) {
                    enumMembers.add(new EnumMemberModel(constant.getName(), (Number) value, constant, constantComments));
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
