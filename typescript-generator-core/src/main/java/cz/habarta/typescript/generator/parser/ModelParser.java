
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Logger;


public abstract class ModelParser {

    protected final Logger logger;
    protected final Settings settings;
    protected final TypeProcessor typeProcessor;
    private final Queue<ClassWithUsage> classQueue = new LinkedList<>();

    public ModelParser(Logger logger, Settings settings, TypeProcessor typeProcessor) {
        this.logger = logger;
        this.settings = settings;
        this.typeProcessor = typeProcessor;
    }

    public Model parseModel(Class<?> cls) {
        return parseModel(Arrays.asList(cls));
    }

    public Model parseModel(List<? extends Class<?>> classes) {
        for (Class<?> cls : classes) {
            classQueue.add(new ClassWithUsage(cls, null, null));
        }
        return parseQueue();
    }

    private Model parseQueue() {
        final LinkedHashMap<Class<?>, BeanModel> parsedClasses = new LinkedHashMap<>();
        ClassWithUsage classWithUsage;
        while ((classWithUsage = classQueue.poll()) != null) {
            final Class<?> cls = classWithUsage.beanClass;
            if (!parsedClasses.containsKey(cls)) {
                logger.info("Parsing '" + cls.getName() + "'" +
                        (classWithUsage.usedInClass != null ? " used in '" + classWithUsage.usedInClass.getSimpleName() + "." + classWithUsage.usedInProperty + "'" : ""));
                final BeanModel bean = parseBean(classWithUsage);
                parsedClasses.put(cls, bean);
            }
        }
        return new Model(new ArrayList<>(parsedClasses.values()));
    }

    protected abstract BeanModel parseBean(ClassWithUsage classWithUsage);

    protected void addBeanToQueue(ClassWithUsage classWithUsage) {
        classQueue.add(classWithUsage);
    }

    protected PropertyModel processTypeAndCreateProperty(String name, Type type, Class<?> usedInClass) {
        List<Class<?>> classes = discoverClassesUsedInType(type);
        for (Class<?> cls : classes) {
            classQueue.add(new ClassWithUsage(cls, name, usedInClass));
        }
        return new PropertyModel(name, type, null);
    }

    private List<Class<?>> discoverClassesUsedInType(Type type) {
        final TypeProcessor.Result result = typeProcessor.processType(type, new TypeProcessor.Context() {
            @Override
            public String getMappedName(Class<?> cls) {
                return "NA";
            }
            @Override
            public TypeProcessor.Result processType(Type javaType) {
                return typeProcessor.processType(javaType, this);
            }
        });
        return result != null ? result.getDiscoveredClasses() : Collections.<Class<?>>emptyList();
    }

}
