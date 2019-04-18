
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.SourceType;
import cz.habarta.typescript.generator.util.Utils;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import java.lang.reflect.Type;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
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
        return fromClassNamesAndJaxrsApplication(classNames, classNamePatterns, null, null, null,
            jaxrsApplicationClassName, automaticJaxrsApplication, isClassNameExcluded, classLoader,
            debug);
    }

    public static Input fromClassNamesAndJaxrsApplication(List<String> classNames,
        List<String> classNamePatterns, List<String> classesWithAnnotations,
        List<String> classesImplementingInterfaces, List<String> classesExtendingClasses,
        String jaxrsApplicationClassName,
        boolean automaticJaxrsApplication, Predicate<String> isClassNameExcluded,
        URLClassLoader classLoader, boolean debug) {
        final ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            if (classLoader != null) {
                Thread.currentThread().setContextClassLoader(classLoader);
            }
            try (final ClasspathScanner classpathScanner = new ClasspathScanner(classLoader, debug)) {
                final List<SourceType<Type>> types = new ArrayList<>();
                if (classNames != null) {
                    types.addAll(fromClassNames(classNames));
                }
                if (classNamePatterns != null) {
                    types.addAll(fromClassNamePatterns(classpathScanner.getScanResult(), classNamePatterns));
                }
                if (classesImplementingInterfaces != null) {
                    final ScanResult scanResult = classpathScanner.getScanResult();
                    final List<SourceType<Type>> c = fromClassNames(
                        classesImplementingInterfaces.stream()
                            .flatMap(interf -> scanResult.getClassesImplementing(interf).getNames()
                                .stream())
                            .distinct()
                            .collect(Collectors.toList())
                    );
                    types.addAll(c);
                }
                if (classesExtendingClasses != null) {
                    final ScanResult scanResult = classpathScanner.getScanResult();
                    final List<SourceType<Type>> c = fromClassNames(
                        classesExtendingClasses.stream()
                            .flatMap(superclass -> scanResult.getSubclasses(superclass).getNames()
                                .stream())
                            .distinct()
                            .collect(Collectors.toList())
                    );
                    types.addAll(c);
                }
                if (classesWithAnnotations != null) {
                    final ScanResult scanResult = classpathScanner.getScanResult();
                    types.addAll(fromClassNames(classesWithAnnotations.stream()
                            .flatMap(annotation -> scanResult.getClassesWithAnnotation(annotation).getNames().stream())
                            .distinct()
                            .collect(Collectors.toList())
                    ));
                }
                if (jaxrsApplicationClassName != null) {
                    types.addAll(fromClassNames(Arrays.asList(jaxrsApplicationClassName)));
                }
                if (automaticJaxrsApplication) {
                    types.addAll(JaxrsApplicationScanner.scanAutomaticJaxrsApplication(classpathScanner.getScanResult(), isClassNameExcluded));
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

    private static class ClasspathScanner implements AutoCloseable {

        private final URLClassLoader classLoader;
        private final boolean verbose;
        private ScanResult scanResult = null;

        public ClasspathScanner(URLClassLoader classLoader, boolean verbose) {
            this.classLoader = classLoader;
            this.verbose = verbose;
        }

        public ScanResult getScanResult() {
            if (scanResult == null) {
                TypeScriptGenerator.getLogger().info("Scanning classpath");
                final Date scanStart = new Date();
                ClassGraph classGraph = new ClassGraph()
                        .enableClassInfo()
                        .enableAnnotationInfo()
                        .ignoreClassVisibility();
                if (classLoader != null) {
                    classGraph = classGraph.overrideClasspath((Object[])classLoader.getURLs());
                }
                if (verbose) {
                    classGraph = classGraph.verbose();
                }
                final ScanResult result = classGraph.scan();
                final int count = result.getAllClasses().size();
                final Date scanEnd = new Date();
                final double timeInSeconds = (scanEnd.getTime() - scanStart.getTime()) / 1000.0;
                TypeScriptGenerator.getLogger().info(String.format("Scanning finished in %.2f seconds. Total number of classes: %d.", timeInSeconds, count));
                scanResult = result;
            }
            return scanResult;
        }

        @Override
        public void close() {
            if (scanResult != null) {
                scanResult.close();
            }
        }

    }

    private static List<SourceType<Type>> fromClassNamePatterns(ScanResult scanResult, List<String> classNamePatterns) {
        final List<String> allClassNames = new ArrayList<>();
        allClassNames.addAll(scanResult.getAllStandardClasses().getNames());
        allClassNames.addAll(scanResult.getAllInterfaces().getNames());
        Collections.sort(allClassNames);
        final List<String> classNames = filterClassNames(allClassNames, classNamePatterns);
        TypeScriptGenerator.getLogger().info(String.format("Found %d classes matching pattern.", classNames.size()));
        return fromClassNames(classNames);
    }

    private static List<SourceType<Type>> fromClassNames(List<String> classNames) {
        final List<SourceType<Type>> types = new ArrayList<>();
        for (Class<?> cls : loadClasses(classNames)) {
            // skip synthetic classes (as those generated by java compiler for switch with enum)
            // and anonymous classes (should not be processed and they do not have SimpleName)
            if (!cls.isSynthetic() && !cls.isAnonymousClass()) {
                types.add(new SourceType<Type>(cls, null, null));
            }
        }
        return types;
    }

    static List<Class<?>> loadClasses(List<String> classNames) {
        final List<Class<?>> classes = new ArrayList<>();
        for (String className : classNames) {
            try {
                final Class<?> cls = Thread.currentThread().getContextClassLoader().loadClass(className);
                classes.add(cls);
            } catch (ReflectiveOperationException e) {
                TypeScriptGenerator.getLogger().error(String.format("Cannot load class '%s'", className));
                e.printStackTrace(System.out);
            }
        }
        return classes;
    }

    static List<String> filterClassNames(List<String> classNames, List<String> globs) {
        final List<Pattern> regexps = Utils.globsToRegexps(globs);
        final List<String> result = new ArrayList<>();
        for (String className : classNames) {
            if (Utils.classNameMatches(className, regexps)) {
                result.add(className);
            }
        }
        return result;
    }

}
