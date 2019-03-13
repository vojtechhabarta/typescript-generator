
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.*;
import io.github.classgraph.ScanResult;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;
import javax.ws.rs.*;
import javax.ws.rs.core.*;


public class JaxrsApplicationScanner {

    public static List<SourceType<Type>> scanJaxrsApplication(Class<?> jaxrsApplicationClass, Predicate<String> isClassNameExcluded) {
        final ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(jaxrsApplicationClass.getClassLoader());
            TypeScriptGenerator.getLogger().info("Scanning JAX-RS application: " + jaxrsApplicationClass.getName());
            final Constructor<?> constructor = jaxrsApplicationClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            final Application application = (Application) constructor.newInstance();
            final List<Class<?>> resourceClasses = new ArrayList<>();
            for (Class<?> cls : application.getClasses()) {
                if (cls.isAnnotationPresent(Path.class)) {
                    resourceClasses.add(cls);
                }
            }
            return new JaxrsApplicationScanner().scanJaxrsApplication(jaxrsApplicationClass, resourceClasses, isClassNameExcluded);
        } catch (ReflectiveOperationException e) {
            throw reportError(e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    public static List<SourceType<Type>> scanAutomaticJaxrsApplication(ScanResult scanResult, Predicate<String> isClassNameExcluded) {
        final List<String> namesOfResourceClasses = scanResult.getClassesWithAnnotation(Path.class.getName()).getNames();
        final List<Class<?>> resourceClasses = Input.loadClasses(namesOfResourceClasses);
        TypeScriptGenerator.getLogger().info(String.format("Found %d root resources.", resourceClasses.size()));
        return new JaxrsApplicationScanner().scanJaxrsApplication(null, resourceClasses, isClassNameExcluded);
    }

    private static RuntimeException reportError(ReflectiveOperationException e) {
        final String url = "https://github.com/vojtechhabarta/typescript-generator/wiki/JAX-RS-Application";
        final String message = "Cannot load JAX-RS application. For more information see " + url + ".";
        TypeScriptGenerator.getLogger().error(message);
        return new RuntimeException(message, e);
    }

    List<SourceType<Type>> scanJaxrsApplication(Class<?> applicationClass, List<Class<?>> resourceClasses, Predicate<String> isClassNameExcluded) {
        Collections.sort(resourceClasses, new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> o1, Class<?> o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        final List<SourceType<Type>> sourceTypes = new ArrayList<>();
        if (applicationClass != null) {
            sourceTypes.add(new SourceType<Type>(applicationClass));
        }
        for (Class<?> resourceClass : resourceClasses) {
            if (isClassNameExcluded == null || !isClassNameExcluded.test(resourceClass.getName())) {
                sourceTypes.add(new SourceType<Type>(resourceClass));
            }
        }
        return sourceTypes;
    }

}
