
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.*;
import cz.habarta.typescript.generator.util.Predicate;
import cz.habarta.typescript.generator.util.Utils;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

import java.lang.reflect.*;
import java.net.URLClassLoader;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class Input {

    private final List<SourceType<Type>> sourceTypes;

    private Input(List<SourceType<Type>> sourceTypes) {
        this.sourceTypes = sourceTypes;
    }

    public List<SourceType<Type>> getSourceTypes() {
        return sourceTypes;
    }

    public static Input from(Type... types) {
        Objects.requireNonNull(types, "types");
        final List<SourceType<Type>> sourceTypes = new ArrayList<>();
        for (Type type : types) {
            sourceTypes.add(new SourceType<>(type));
        }
        return new Input(sourceTypes);
    }

    public static Input fromClassNamesAndJaxrsApplication(List<String> classNames, List<String> classNamePatterns, String jaxrsApplicationClassName,
            boolean automaticJaxrsApplication, Predicate<String> isClassNameExcluded, URLClassLoader classLoader, boolean debug) {
        return fromClassNamesAndJaxrsApplication(classNames, classNamePatterns, null, jaxrsApplicationClassName, automaticJaxrsApplication, isClassNameExcluded, classLoader, debug);
    }

    public static Input fromClassNamesAndJaxrsApplication(List<String> classNames, List<String> classNamePatterns, List<String> classesWithAnnotations, String jaxrsApplicationClassName,
            boolean automaticJaxrsApplication, Predicate<String> isClassNameExcluded, URLClassLoader classLoader, boolean debug) {
        Objects.requireNonNull(classLoader, "classLoader");
        final ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            try (ScanResult scanResult =
                         new ClassGraph()
                                 .enableAllInfo()       // Scan classes, methods, fields, annotations
                                 .scan()) {
                final List<SourceType<Type>> types = new ArrayList<>();
                ClassInfoList allClasses = scanResult.getAllClasses();
                if (classNames != null) {
                    types.addAll(fromClassNames(allClasses, classNames));
                }
                if (classNamePatterns != null) {
                    types.addAll(fromClassNamePatterns(allClasses, classNamePatterns));
                }
                if (classesWithAnnotations != null) {
                    classesWithAnnotations.stream()
                            .forEach(anno -> types.addAll(fromClassNames(scanResult.getClassesWithAnnotation(anno))));
                }
                if (jaxrsApplicationClassName != null) {
                    types.addAll(fromClassNamePatterns(allClasses, Arrays.asList(jaxrsApplicationClassName)));
                }
                if (automaticJaxrsApplication) {
                    types.addAll(JaxrsApplicationScanner.scanAutomaticJaxrsApplication(scanResult, isClassNameExcluded));
                }
                if (types.isEmpty()) {
                    final String errorMessage = "No input classes found.";
                    TypeScriptGenerator.getLogger().error(errorMessage);
                    throw new RuntimeException(errorMessage);
                }
                return new Input(types);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    private static List<SourceType<Type>> fromClassNamePatterns(ClassInfoList classNames, List<String> classNamePatterns) {
        return fromClassNames(filterClassNames(classNames, classNamePatterns));
    }

    private static List<SourceType<Type>> fromClassNames(ClassInfoList classNames, List<String> names) {
        return fromClassNames(classNames.filter(ci -> names.contains(ci.getName())));
    }

    private static List<SourceType<Type>> fromClassNames(ClassInfoList classNames) {
        return classNames
                .filter(ci -> !ci.isSynthetic() && !ci.isAnonymousInnerClass())
                .loadClasses().stream()
                .map(cl -> new SourceType<Type>(cl, null, null))
                .collect(Collectors.toList());
    }

    static ClassInfoList filterClassNames(ClassInfoList classNames, List<String> globs) {
        final List<Pattern> regexps = Utils.globsToRegexps(globs);
        return classNames.filter(ci -> Utils.classNameMatches(ci.getName(), regexps));
    }
    static List<String> filterClassNames(List<String> names, List<String> globs) {
        final List<Pattern> regexps = Utils.globsToRegexps(globs);
        return names.stream()
                .filter(s -> Utils.classNameMatches(s, regexps))
                .collect(Collectors.toList());
    }

}
