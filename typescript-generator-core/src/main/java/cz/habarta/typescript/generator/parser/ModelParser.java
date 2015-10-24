
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Logger;


public abstract class ModelParser {

    protected final Logger logger;
    protected final Settings settings;
    protected final ModelCompiler compiler;
    private final Queue<ClassWithUsage> classQueue = new LinkedList<>();

    public ModelParser(Logger logger, Settings settings, ModelCompiler compiler) {
        this.logger = logger;
        this.settings = settings;
        this.compiler = compiler;
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
        final List<Class<?>> classes = compiler.discoverClasses(type);
        for (Class<?> cls : classes) {
            classQueue.add(new ClassWithUsage(cls, name, usedInClass));
        }
        return new PropertyModel(name, type, null);
    }

}
