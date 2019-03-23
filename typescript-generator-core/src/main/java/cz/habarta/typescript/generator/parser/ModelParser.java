
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.*;
import cz.habarta.typescript.generator.compiler.EnumKind;
import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import cz.habarta.typescript.generator.util.Utils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public abstract class ModelParser {

    protected final Settings settings;
    private final Javadoc javadoc;
    private final Queue<SourceType<? extends Type>> typeQueue;
    private final TypeProcessor commonTypeProcessor;
    private final List<RestApplicationParser> restApplicationParsers;
        
    public static abstract class Factory {

        public TypeProcessor getSpecificTypeProcessor() {
            return null;
        }

        public abstract ModelParser create(Settings settings, TypeProcessor commonTypeProcessor, List<RestApplicationParser> restApplicationParsers);

    }

    public ModelParser(Settings settings, TypeProcessor commonTypeProcessor, List<RestApplicationParser> restApplicationParsers) {
        this.settings = settings;
        this.javadoc = new Javadoc(settings);
        this.typeQueue = new LinkedList<>();
        this.restApplicationParsers = restApplicationParsers;
        this.commonTypeProcessor = commonTypeProcessor;
    }

    public Model parseModel(Type type) {
        return parseModel(Arrays.asList(new SourceType<>(type)));
    }

    public Model parseModel(List<SourceType<Type>> types) {
        typeQueue.addAll(types);
        Model model = parseQueue();
        if (!settings.ignoreSwaggerAnnotations) {
            model = Swagger.enrichModel(model);
        }
        model = javadoc.enrichModel(model);
        return model;
    }

    private Model parseQueue() {
        final Collection<Type> parsedTypes = new ArrayList<>();  // do not use hashcodes, we can only count on `equals` since we use custom `ParameterizedType`s
        final List<BeanModel> beans = new ArrayList<>();
        final List<EnumModel> enums = new ArrayList<>();
        SourceType<? extends Type> sourceType;
        while ((sourceType = typeQueue.poll()) != null) {
            if (parsedTypes.contains(sourceType.type)) {
                continue;
            }
            parsedTypes.add(sourceType.type);

            // REST resource
            boolean parsedByRestApplicationParser = false;
            for (RestApplicationParser restApplicationParser : restApplicationParsers) {
                final JaxrsApplicationParser.Result jaxrsResult = restApplicationParser.tryParse(sourceType);
                if (jaxrsResult != null) {
                    typeQueue.addAll(jaxrsResult.discoveredTypes);
                    parsedByRestApplicationParser = true;
                }
            }
            if (parsedByRestApplicationParser) {
                continue;
            }

            final TypeProcessor.Result result = commonTypeProcessor.processTypeInTemporaryContext(sourceType.type, null, settings);
            if (result != null) {
                if (sourceType.type instanceof Class<?> && result.getTsType() instanceof TsType.ReferenceType) {
                    final Class<?> cls = (Class<?>) sourceType.type;
                    TypeScriptGenerator.getLogger().verbose("Parsing '" + cls.getName() + "'" +
                            (sourceType.usedInClass != null ? " used in '" + sourceType.usedInClass.getSimpleName() + "." + sourceType.usedInMember + "'" : ""));
                    final DeclarationModel model = parseClass(sourceType.asSourceClass());
                    if (model instanceof EnumModel) {
                        enums.add((EnumModel) model);
                    } else if (model instanceof BeanModel) {
                        beans.add((BeanModel) model);
                    } else {
                        throw new RuntimeException();
                    }
                }
                for (Class<?> cls : result.getDiscoveredClasses()) {
                    typeQueue.add(new SourceType<>(cls, sourceType.usedInClass, sourceType.usedInMember));
                }
            }
        }
        final List<RestApplicationModel> restModels = restApplicationParsers.stream()
                .map(RestApplicationParser::getModel)
                .collect(Collectors.toList());
        return new Model(beans, enums, restModels);
    }

    protected abstract DeclarationModel parseClass(SourceType<Class<?>> sourceClass);

    protected static void checkMember(Member propertyMember, String propertyName, Class<?> sourceClass) {
        if (!(propertyMember instanceof Field) && !(propertyMember instanceof Method)) {
            throw new RuntimeException(String.format(
                    "Unexpected member type '%s' in property '%s' in class '%s'",
                    propertyMember != null ? propertyMember.getClass().getName() : null,
                    propertyName,
                    sourceClass.getName()));
        }
    }

    protected boolean isAnnotatedPropertyIncluded(Function<Class<? extends Annotation>, Annotation> getAnnotationFunction, String propertyDescription) {
        boolean isIncluded = settings.includePropertyAnnotations.isEmpty()
                || Utils.hasAnyAnnotation(getAnnotationFunction, settings.includePropertyAnnotations);
        if (!isIncluded) {
            TypeScriptGenerator.getLogger().verbose("Skipping '" + propertyDescription + "' because it doesn't have any annotation from 'includePropertyAnnotations'");
            return false;
        }
        boolean isExcluded = Utils.hasAnyAnnotation(getAnnotationFunction, settings.excludePropertyAnnotations);
        if (isExcluded) {
            TypeScriptGenerator.getLogger().verbose("Skipping '" + propertyDescription + "' because it has some annotation from 'excludePropertyAnnotations'");
            return false;
        }
        return true;
    }

    protected boolean isAnnotatedPropertyOptional(Function<Class<? extends Annotation>, Annotation> getAnnotationFunction) {
        if (settings.optionalProperties == OptionalProperties.all) {
            return true;
        }
        if (settings.optionalProperties == null || settings.optionalProperties == OptionalProperties.useSpecifiedAnnotations) {
            return Utils.hasAnyAnnotation(getAnnotationFunction, settings.optionalAnnotations);
        }
        return false;
    }

    protected static DeclarationModel parseEnum(SourceType<Class<?>> sourceClass) {
        final List<EnumMemberModel> values = new ArrayList<>();
        if (sourceClass.type.isEnum()) {
            @SuppressWarnings("unchecked")
            final Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) sourceClass.type;
            for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
                values.add(new EnumMemberModel(enumConstant.name(), enumConstant.name(), null));
            }
        }
        return new EnumModel(sourceClass.type, EnumKind.StringBased, values, null);
    }

    protected void addBeanToQueue(SourceType<? extends Type> sourceType) {
        typeQueue.add(sourceType);
    }

    protected PropertyModel processTypeAndCreateProperty(String name, Type type, Object typeContext, boolean optional, Class<?> usedInClass, Member originalMember, PropertyModel.PullProperties pullProperties, List<String> comments) {
        final List<Class<?>> classes = commonTypeProcessor.discoverClassesUsedInType(type, typeContext, settings);
        for (Class<?> cls : classes) {
            typeQueue.add(new SourceType<>(cls, usedInClass, name));
        }
        return new PropertyModel(name, type, optional, originalMember, pullProperties, typeContext, comments);
    }

    public static boolean containsProperty(List<PropertyModel> properties, String propertyName) {
        for (PropertyModel property : properties) {
            if (property.getName().equals(propertyName)) {
                return true;
            }
        }
        return false;
    }

}
