
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.*;
import java.lang.reflect.*;
import java.util.*;


public class Input {

    private final List<SourceType<Type>> sourceTypes;

    private Input(List<SourceType<Type>> sourceTypes) {
        this.sourceTypes = sourceTypes;
    }

    public List<SourceType<Type>> getSourceTypes() {
        return sourceTypes;
    }

    public static Input from(Type... types) {
        final List<SourceType<Type>> sourceTypes = new ArrayList<>();
        for (Type type : types) {
            sourceTypes.add(new SourceType<>(type));
        }
        return new Input(sourceTypes);
    }

    private static Input fromClassNames(List<String> classNames, ClassLoader classLoader) {
        try {
            final List<SourceType<Type>> types = new ArrayList<>();
            for (String className : classNames) {
                types.add(new SourceType<Type>(classLoader.loadClass(className), null, null));
            }
            return new Input(types);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Input fromJaxrsApplication(String jaxrsApplicationClassName, List<String> excludedClassNames, ClassLoader classLoader) {
        final List<SourceType<Type>> sourceTypes = new JaxrsApplicationScanner(classLoader).scanJaxrsApplication(jaxrsApplicationClassName, excludedClassNames);
        return new Input(sourceTypes);
    }

    public static Input fromClassNamesAndJaxrsApplication(List<String> classNames, String jaxrsApplicationClassName, List<String> excludedClassNames, ClassLoader classLoader) {
        final List<SourceType<Type>> types = new ArrayList<>();
        if (classNames != null) {
            types.addAll(fromClassNames(classNames, classLoader).getSourceTypes());
        }
        if (jaxrsApplicationClassName != null) {
            types.addAll(fromJaxrsApplication(jaxrsApplicationClassName, excludedClassNames, classLoader).getSourceTypes());
        }
        if (types.isEmpty()) {
            final String errorMessage = "No input classes found.";
            System.out.println(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        return new Input(types);
    }

}
