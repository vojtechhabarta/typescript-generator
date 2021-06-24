
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.SourceType;
import cz.habarta.typescript.generator.util.Utils;
import io.github.classgraph.ScanResult;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;


public class JaxrsApplicationScanner {

    public static List<SourceType<Type>> scanJaxrsApplication(Class<?> jaxrsApplicationClass, Predicate<String> isClassNameExcluded) {
        final ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(jaxrsApplicationClass.getClassLoader());
            TypeScriptGenerator.getLogger().info("Scanning JAX-RS application: " + jaxrsApplicationClass.getName());
            final Constructor<?> constructor = jaxrsApplicationClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            final Object instance = constructor.newInstance();
            final Set<Class<?>> applicationClasses;
            if (instance instanceof jakarta.ws.rs.core.Application) {
                applicationClasses = ((jakarta.ws.rs.core.Application) instance).getClasses();
            } else if (instance instanceof javax.ws.rs.core.Application) {
                applicationClasses = ((javax.ws.rs.core.Application) instance).getClasses();
            } else {
                applicationClasses = Collections.emptySet();
            }
            final List<Class<?>> resourceClasses = new ArrayList<>();
            for (Class<?> cls : applicationClasses) {
                if (cls.isAnnotationPresent(jakarta.ws.rs.Path.class) || cls.isAnnotationPresent(javax.ws.rs.Path.class)) {
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
        final List<String> namesOfResourceClasses = Utils.concat(
                scanResult.getClassesWithAnnotation(jakarta.ws.rs.Path.class.getName()).getNames(),
                scanResult.getClassesWithAnnotation(javax.ws.rs.Path.class.getName()).getNames()
        );
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
        Collections.sort(resourceClasses, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        final List<SourceType<Type>> sourceTypes = new ArrayList<>();
        if (applicationClass != null) {
            sourceTypes.add(new SourceType<>(applicationClass));
        }
        for (Class<?> resourceClass : resourceClasses) {
            if (isClassNameExcluded == null || !isClassNameExcluded.test(resourceClass.getName())) {
                sourceTypes.add(new SourceType<>(resourceClass));
            }
        }
        return sourceTypes;
    }

}
