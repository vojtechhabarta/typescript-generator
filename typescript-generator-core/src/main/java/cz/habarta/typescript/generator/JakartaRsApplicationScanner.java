
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.SourceType;
import io.github.classgraph.ScanResult;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;


public class JakartaRsApplicationScanner {

    public static List<SourceType<Type>> scanJakartaRsApplication(Class<?> jaxrsApplicationClass, Predicate<String> isClassNameExcluded) {
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
            return new JakartaRsApplicationScanner().scanJakartaRsApplication(jaxrsApplicationClass, resourceClasses, isClassNameExcluded);
        } catch (ReflectiveOperationException e) {
            throw reportError(e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    public static List<SourceType<Type>> scanAutomaticJakartaRsApplication(ScanResult scanResult, Predicate<String> isClassNameExcluded) {
        final List<String> namesOfResourceClasses = scanResult.getClassesWithAnnotation(Path.class.getName()).getNames();
        final List<Class<?>> resourceClasses = Input.loadClasses(namesOfResourceClasses);
        TypeScriptGenerator.getLogger().info(String.format("Found %d root resources.", resourceClasses.size()));
        return new JakartaRsApplicationScanner().scanJakartaRsApplication(null, resourceClasses, isClassNameExcluded);
    }

    private static RuntimeException reportError(ReflectiveOperationException e) {
        final String url = "https://github.com/vojtechhabarta/typescript-generator/wiki/JAX-RS-Application";
        final String message = "Cannot load JAX-RS application. For more information see " + url + ".";
        TypeScriptGenerator.getLogger().error(message);
        return new RuntimeException(message, e);
    }

    List<SourceType<Type>> scanJakartaRsApplication(Class<?> applicationClass, List<Class<?>> resourceClasses, Predicate<String> isClassNameExcluded) {
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
