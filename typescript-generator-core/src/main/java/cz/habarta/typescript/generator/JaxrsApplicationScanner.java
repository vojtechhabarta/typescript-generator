
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.*;
import cz.habarta.typescript.generator.util.Predicate;
import cz.habarta.typescript.generator.util.Utils;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;


public class JaxrsApplicationScanner {

    private Predicate<String> isClassNameExcluded;
    private Set<String> defaultExcludes;
    private Queue<Class<?>> resourceQueue;
    private List<SourceType<Type>> discoveredTypes;

    public List<SourceType<Type>> scanJaxrsApplication(String jaxrsApplicationClassName, Predicate<String> isClassNameExcluded) {
        try {
            final List<Class<?>> resourceClasses = jaxrsApplicationClassName != null
                    ? scanJaxrsApplicationForJaxrsResources(jaxrsApplicationClassName)
                    : scanClasspathForJaxrsResources();
            return scanJaxrsApplication(resourceClasses, isClassNameExcluded);
        } catch (ReflectiveOperationException e) {
            final String url = "https://github.com/vojtechhabarta/typescript-generator/wiki/JAX-RS-Application";
            final String message = "Cannot load JAX-RS application. For more information see " + url + ".";
            System.out.println(message);
            throw new RuntimeException(message, e);
        }
    }

    private static List<Class<?>> scanJaxrsApplicationForJaxrsResources(String jaxrsApplicationClassName) throws ReflectiveOperationException {
        System.out.println("Scanning JAX-RS application: " + jaxrsApplicationClassName);
        final Class<?> jaxrsApplicationClass = Thread.currentThread().getContextClassLoader().loadClass(jaxrsApplicationClassName);
        final Constructor<?> constructor = jaxrsApplicationClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        final Application application = (Application) constructor.newInstance();
        return new ArrayList<>(application.getClasses());
    }

    private static List<Class<?>> scanClasspathForJaxrsResources() throws ReflectiveOperationException {
        final FastClasspathScanner scanner = new FastClasspathScanner().scan();
        final List<String> namesOfResourceClasses = scanner.getNamesOfClassesWithAnnotation(Path.class);
        final List<Class<?>> classes = new ArrayList<>();
        for (String className : namesOfResourceClasses) {
            classes.add(Thread.currentThread().getContextClassLoader().loadClass(className));
        }
        System.out.println(String.format("Found: %d root resources.", classes.size()));
        return classes;
    }

    List<SourceType<Type>> scanJaxrsApplication(List<Class<?>> resourceClasses, Predicate<String> isClassNameExcluded) {
        resourceQueue = new LinkedList<>();
        discoveredTypes = new ArrayList<>();
        this.isClassNameExcluded = isClassNameExcluded;
        this.defaultExcludes = new LinkedHashSet<>(getDefaultExcludedClassNames());
        final LinkedHashSet<Class<?>> scannedResources = new LinkedHashSet<>();
        Collections.sort(resourceClasses, new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> o1, Class<?> o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        for (Class<?> resourceClass : resourceClasses) {
            if (resourceClass.isAnnotationPresent(Path.class)) {
                resourceQueue.add(resourceClass);
            }
        }
        Class<?> resourceClass;
        while ((resourceClass = resourceQueue.poll()) != null) {
            if (!scannedResources.contains(resourceClass) && !isExcluded(resourceClass)) {
                System.out.println("Scanning JAX-RS resource: " + resourceClass.getName());
                scanResource(resourceClass);
                scannedResources.add(resourceClass);
            }
        }
        return discoveredTypes;
    }

    private void scanResource(Class<?> resourceClass) {
        final List<Method> methods = Arrays.asList(resourceClass.getMethods());
        Collections.sort(methods, new Comparator<Method>() {
            @Override
            public int compare(Method o1, Method o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        for (Method method : methods) {
            scanResourceMethod(resourceClass, method);
        }
    }

    private void scanResourceMethod(Class<?> resourceClass, Method method) {
        if (isHttpMethod(method)) {
            // JAX-RS specification - 3.3.2.1 Entity Parameters
            final Type entityParameterType = getEntityParameterType(method);
            if (entityParameterType != null) {
                foundType(entityParameterType, resourceClass, method.getName());
            }
            // JAX-RS specification - 3.3.3 Return Type
            final Class<?> returnType = method.getReturnType();
            final Type genericReturnType = method.getGenericReturnType();
            if (returnType == void.class || returnType == Response.class) {
                // no discovered Type
            } else if (genericReturnType instanceof ParameterizedType && returnType == GenericEntity.class) {
                final ParameterizedType parameterizedReturnType = (ParameterizedType) genericReturnType;
                foundType(parameterizedReturnType.getActualTypeArguments()[0], resourceClass, method.getName());
            } else {
                foundType(genericReturnType, resourceClass, method.getName());
            }
        }
        // JAX-RS specification - 3.4.1 Sub Resources
        if (method.isAnnotationPresent(Path.class) && !isHttpMethod(method)) {
            resourceQueue.add(method.getReturnType());
        }
    }

    private void foundType(Type type, Class<?> usedInClass, String usedInMember) {
        if (!isExcluded(type)) {
            discoveredTypes.add(new SourceType<>(type, usedInClass, usedInMember));
        }
    }

    private boolean isExcluded(Type type) {
        final Class<?> cls = Utils.getRawClassOrNull(type);
        if (cls != null && isClassNameExcluded != null && isClassNameExcluded.test(cls.getName())) {
            return true;
        }
        if (cls != null && defaultExcludes.contains(cls.getName())) {
            return true;
        }
        for (Class<?> standardEntityClass : getStandardEntityClasses()) {
            if (standardEntityClass.isAssignableFrom(cls)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHttpMethod(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(HttpMethod.class)) {
                return true;
            }
        }
        return false;
    }

    private static Type getEntityParameterType(Method method) {
        final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            if (!hasAnyAnnotation(parameterAnnotations[i], Arrays.asList(
                    MatrixParam.class,
                    QueryParam.class,
                    PathParam.class,
                    CookieParam.class,
                    HeaderParam.class,
                    Context.class,
                    FormParam.class
                    ))) {
                return method.getGenericParameterTypes()[i];
            }
        }
        return null;
    }

    private static boolean hasAnyAnnotation(Annotation[] parameterAnnotations, List<Class<? extends Annotation>> annotationClasses) {
        for (Class<? extends Annotation> annotationClass : annotationClasses) {
            for (Annotation parameterAnnotation : parameterAnnotations) {
                if (annotationClass.isInstance(parameterAnnotation)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<Class<?>> getStandardEntityClasses() {
        // JAX-RS specification - 4.2.4 Standard Entity Providers
        return Arrays.asList(
                byte[].class,
                java.lang.String.class,
                java.io.InputStream.class,
                java.io.Reader.class,
                java.io.File.class,
                javax.activation.DataSource.class,
                javax.xml.transform.Source.class,
                javax.xml.bind.JAXBElement.class,
                MultivaluedMap.class,
                StreamingOutput.class,
                java.lang.Boolean.class, java.lang.Character.class, java.lang.Number.class,
                long.class, int.class, short.class, byte.class, double.class, float.class, boolean.class, char.class);
    }

    private static List<String> getDefaultExcludedClassNames() {
        return Arrays.asList(
                "org.glassfish.jersey.media.multipart.FormDataBodyPart"
        );
    }

}
