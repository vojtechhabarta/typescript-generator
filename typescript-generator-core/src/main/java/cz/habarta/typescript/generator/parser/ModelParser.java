
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.*;
import cz.habarta.typescript.generator.compiler.EnumKind;
import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import cz.habarta.typescript.generator.compiler.SymbolTable;
import cz.habarta.typescript.generator.util.Utils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;


public abstract class ModelParser {

    protected final Settings settings;
    protected final TypeProcessor typeProcessor;
    private final Javadoc javadoc;
    private final Queue<SourceType<? extends Type>> typeQueue = new LinkedList<>();

    public ModelParser(Settings settings, TypeProcessor typeProcessor) {
        this(settings, typeProcessor, null);
    }

    public ModelParser(Settings settings, TypeProcessor typeProcessor, List<String> parserSpecificExcludes) {
        this.settings = settings;
        this.typeProcessor = new TypeProcessor.Chain(
                new ExcludingTypeProcessor(parserSpecificExcludes),
                typeProcessor
        );
        this.javadoc = new Javadoc(settings.javadocXmlFiles);
    }

    public TypeProcessor getSpecificTypeProcessor() {
        return typeProcessor;
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
        final JaxrsApplicationParser jaxrsApplicationParser = new JaxrsApplicationParser(settings);
        final Collection<Type> parsedTypes = new ArrayList<>();  // do not use hashcodes, we can only count on `equals` since we use custom `ParameterizedType`s
        final List<BeanModel> beans = new ArrayList<>();
        final List<EnumModel> enums = new ArrayList<>();
        SourceType<? extends Type> sourceType;
        while ((sourceType = typeQueue.poll()) != null) {
            if (parsedTypes.contains(sourceType.type)) {
                continue;
            }
            parsedTypes.add(sourceType.type);

            // JAX-RS resource
            final JaxrsApplicationParser.Result jaxrsResult = jaxrsApplicationParser.tryParse(sourceType);
            if (jaxrsResult != null) {
                typeQueue.addAll(jaxrsResult.discoveredTypes);
                continue;
            }

            final TypeProcessor.Result result = processType(sourceType.type);
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
        return new Model(beans, enums, jaxrsApplicationParser.getModel());
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

    protected PropertyModel processTypeAndCreateProperty(String name, Type type, boolean optional, Class<?> usedInClass, Member originalMember, PropertyModel.PullProperties pullProperties) {
        List<Class<?>> classes = discoverClassesUsedInType(type);
        for (Class<?> cls : classes) {
            typeQueue.add(new SourceType<>(cls, usedInClass, name));
        }
        return new PropertyModel(name, type, optional, originalMember, pullProperties, null);
    }

    private List<Class<?>> discoverClassesUsedInType(Type type) {
        final TypeProcessor.Result result = processType(type);
        return result != null ? result.getDiscoveredClasses() : Collections.<Class<?>>emptyList();
    }

    private TypeProcessor.Result processType(Type type) {
        return typeProcessor.processType(type, new TypeProcessor.Context(new SymbolTable(settings), typeProcessor));
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
