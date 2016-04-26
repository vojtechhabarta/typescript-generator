
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.compiler.SymbolTable;
import cz.habarta.typescript.generator.*;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.*;


public abstract class ModelParser {

    protected final Settings settings;
    protected final TypeProcessor typeProcessor;
    private final Javadoc javadoc;
    private final Queue<SourceType<? extends Type>> typeQueue = new LinkedList<>();

    public ModelParser(Settings settings, TypeProcessor typeProcessor) {
        this.settings = settings;
        this.typeProcessor = typeProcessor;
        this.javadoc = new Javadoc(settings.javadocXmlFiles);
    }

    public Model parseModel(Type type) {
        return parseModel(Arrays.asList(new SourceType<>(type)));
    }

    public Model parseModel(List<SourceType<Type>> types) {
        typeQueue.addAll(types);
        final Model model = parseQueue();
        final Model modelWithJavadoc = javadoc.enrichModel(model);
        return modelWithJavadoc;
    }

    private Model parseQueue() {
        final Set<Type> parsedTypes = new LinkedHashSet<>();
        final List<BeanModel> beans = new ArrayList<>();
        final List<EnumModel> enums = new ArrayList<>();
        SourceType<?> sourceType;
        while ((sourceType = typeQueue.poll()) != null) {
            if (parsedTypes.contains(sourceType.type)) {
                continue;
            }
            parsedTypes.add(sourceType.type);
            final TypeProcessor.Result result = processType(sourceType.type);
            if (result != null) {
                if (sourceType.type instanceof Class<?> && result.getTsType() instanceof TsType.ReferenceType) {
                    final Class<?> cls = (Class<?>) sourceType.type;
                    System.out.println("Parsing '" + cls.getName() + "'" +
                            (sourceType.usedInClass != null ? " used in '" + sourceType.usedInClass.getSimpleName() + "." + sourceType.usedInMember + "'" : ""));
                    if (cls.isEnum()) {
                        final EnumModel enumModel = parseEnum(sourceType.asSourceClass());
                        enums.add(enumModel);
                    } else {
                        final BeanModel bean = parseBean(sourceType.asSourceClass());
                        beans.add(bean);
                    }
                }
                for (Class<?> cls : result.getDiscoveredClasses()) {
                    typeQueue.add(new SourceType<>(cls, sourceType.usedInClass, sourceType.usedInMember));
                }
            }
        }
        return new Model(beans, enums);
    }

    protected abstract BeanModel parseBean(SourceType<Class<?>> sourceClass);

    protected EnumModel parseEnum(SourceType<Class<?>> sourceClass) {
        final List<String> values = new ArrayList<>();
        if (sourceClass.type.isEnum()) {
            @SuppressWarnings("unchecked")
            final Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) sourceClass.type;
            for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
                values.add(enumConstant.name());
            }
        }
        return new EnumModel(sourceClass.type, values, null);
    }

    protected void addBeanToQueue(SourceType<? extends Type> sourceType) {
        typeQueue.add(sourceType);
    }

    protected PropertyModel processTypeAndCreateProperty(String name, Type type, boolean optional, Class<?> usedInClass, Member originalMember) {
        List<Class<?>> classes = discoverClassesUsedInType(type);
        for (Class<?> cls : classes) {
            typeQueue.add(new SourceType<>(cls, usedInClass, name));
        }
        return new PropertyModel(name, type, optional, originalMember, null);
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
