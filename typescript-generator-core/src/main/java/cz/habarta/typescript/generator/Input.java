
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.*;
import cz.habarta.typescript.generator.util.Predicate;
import cz.habarta.typescript.generator.util.Utils;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult;
import java.lang.reflect.*;
import java.net.URLClassLoader;
import java.util.*;
import java.util.regex.Pattern;


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
            final ClasspathScanner classpathScanner = new ClasspathScanner(classLoader, debug);
            final List<SourceType<Type>> types = new ArrayList<>();
            if (classNames != null) {
                types.addAll(fromClassNames(classNames));
            }
            if (classNamePatterns != null) {
                types.addAll(fromClassNamePatterns(classpathScanner.scanClasspath(), classNamePatterns));
            }
            if(classesWithAnnotations != null) {
                types.addAll(fromClassNames(classpathScanner.scanClasspath().getNamesOfClassesWithAnnotationsAnyOf(classesWithAnnotations.stream().toArray(String[]::new))));
            }
            if (jaxrsApplicationClassName != null) {
                types.addAll(fromClassNames(Arrays.asList(jaxrsApplicationClassName)));
            }
            if (automaticJaxrsApplication) {
                types.addAll(JaxrsApplicationScanner.scanAutomaticJaxrsApplication(classpathScanner.scanClasspath(), isClassNameExcluded));
            }
            if (types.isEmpty()) {
                final String errorMessage = "No input classes found.";
                TypeScriptGenerator.getLogger().error(errorMessage);
                throw new RuntimeException(errorMessage);
            }
            return new Input(types);
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    private static class ClasspathScanner {

        private final URLClassLoader classLoader;
        private final boolean verbose;
        private ScanResult scanResult = null;

        public ClasspathScanner(URLClassLoader classLoader, boolean verbose) {
            this.classLoader = classLoader;
            this.verbose = verbose;
        }

        public ScanResult scanClasspath() {
            if (scanResult == null) {
                TypeScriptGenerator.getLogger().info("Scanning classpath");
                final Date scanStart = new Date();
                final ScanResult result = new FastClasspathScanner()
                        .overrideClasspath((Object[])classLoader.getURLs())
                        .verbose(verbose)
                        .scan();
                final int count = result.getNamesOfAllClasses().size();
                final Date scanEnd = new Date();
                final double timeInSeconds = (scanEnd.getTime() - scanStart.getTime()) / 1000.0;
                TypeScriptGenerator.getLogger().info(String.format("Scanning finished in %.2f seconds. Total number of classes: %d.", timeInSeconds, count));
                scanResult = result;
            }
            return scanResult;
        }

    }

    private static List<SourceType<Type>> fromClassNamePatterns(ScanResult scanResult, List<String> classNamePatterns) {
        final List<String> allClassNames = new ArrayList<>();
        allClassNames.addAll(scanResult.getNamesOfAllStandardClasses());
        allClassNames.addAll(scanResult.getNamesOfAllInterfaceClasses());
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
