
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.JaxrsApplicationScanner;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.util.Parameter;
import cz.habarta.typescript.generator.util.Predicate;
import cz.habarta.typescript.generator.util.Utils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;


public class JaxrsApplicationParser {

    private final Settings settings;
    private final Predicate<String> isClassNameExcluded;
    private final Set<String> defaultExcludes;
    private final JaxrsApplicationModel model;

    public JaxrsApplicationParser(Settings settings) {
        this.settings = settings;
        this.isClassNameExcluded = settings.getExcludeFilter();
        this.defaultExcludes = new LinkedHashSet<>(getDefaultExcludedClassNames());
        this.model = new JaxrsApplicationModel();
    }

    public JaxrsApplicationModel getModel() {
        return model;
    }

    public static class Result {
        public List<SourceType<Type>> discoveredTypes;
        public Result() {
            discoveredTypes = new ArrayList<>();
        }
        public Result(List<SourceType<Type>> discoveredTypes) {
            this.discoveredTypes = discoveredTypes;
        }
    }

    public Result tryParse(SourceType<?> sourceType) {
        if (!(sourceType.type instanceof Class<?>)) {
            return null;
        }
        final Class<?> cls = (Class<?>) sourceType.type;

        // application
        if (Application.class.isAssignableFrom(cls)) {
            final ApplicationPath applicationPathAnnotation = cls.getAnnotation(ApplicationPath.class);
            if (applicationPathAnnotation != null) {
                model.setApplicationPath(applicationPathAnnotation.value());
            }
            model.setApplicationName(cls.getSimpleName());
            final List<SourceType<Type>> discoveredTypes = JaxrsApplicationScanner.scanJaxrsApplication(cls, isClassNameExcluded);
            return new Result(discoveredTypes);
        }

        // resource
        final Path path = cls.getAnnotation(Path.class);
        if (path != null) {
            System.out.println("Parsing JAX-RS resource: " + cls.getName());
            final Result result = new Result();
            parseResource(result, new ResourceContext(cls, path.value()), cls);
            return result;
        }

        return null;
    }

    private void parseResource(Result result, ResourceContext context, Class<?> resourceClass) {
        // subContext
        final Map<String, Type> pathParamTypes = new LinkedHashMap<>();
        for (Field field : resourceClass.getDeclaredFields()) {
            final PathParam pathParamAnnotation = field.getAnnotation(PathParam.class);
            if (pathParamAnnotation != null) {
                pathParamTypes.put(pathParamAnnotation.value(), field.getType());
            }
        }
        final ResourceContext subContext = context.subPathParamTypes(pathParamTypes);
        // parse resource methods
        final List<Method> methods = Arrays.asList(resourceClass.getMethods());
        Collections.sort(methods, new Comparator<Method>() {
            @Override
            public int compare(Method o1, Method o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        for (Method method : methods) {
            parseResourceMethod(result, subContext, resourceClass, method);
        }
    }

    private void parseResourceMethod(Result result, ResourceContext context, Class<?> resourceClass, Method method) {
        final Path pathAnnotation = method.getAnnotation(Path.class);
        // subContext
        context = context.subPath(pathAnnotation);
        final Map<String, Type> pathParamTypes = new LinkedHashMap<>();
        final List<Parameter> methodParameters = Parameter.ofMethod(method);
        for (Parameter parameter : methodParameters) {
            final PathParam pathParamAnnotation = parameter.getAnnotation(PathParam.class);
            if (pathParamAnnotation != null) {
                pathParamTypes.put(pathParamAnnotation.value(), parameter.getParameterizedType());
            }
        }
        context = context.subPathParamTypes(pathParamTypes);
        // JAX-RS specification - 3.3 Resource Methods
        final HttpMethod httpMethod = getHttpMethod(method);
        if (httpMethod != null) {
            // swagger
            final SwaggerOperation swaggerOperation = settings.ignoreSwaggerAnnotations
                    ? new SwaggerOperation()
                    : Swagger.parseSwaggerAnnotations(method);
            if (swaggerOperation.possibleResponses != null) {
                for (SwaggerResponse response : swaggerOperation.possibleResponses) {
                    if (response.responseType != null) {
                        foundType(result, response.responseType, resourceClass, method.getName());
                    }
                }
            }
            if (swaggerOperation.hidden) {
                return;
            }
            // path parameters
            final List<MethodParameterModel> pathParams = new ArrayList<>();
            final PathTemplate pathTemplate = PathTemplate.parse(context.path);
            for (PathTemplate.Part part : pathTemplate.getParts()) {
                if (part instanceof PathTemplate.Parameter) {
                    final PathTemplate.Parameter parameter = (PathTemplate.Parameter) part;
                    final Type type = context.pathParamTypes.get(parameter.getName());
                    pathParams.add(new MethodParameterModel(parameter.getName(), type != null ? type : String.class));
                }
            }
            // query parameters
            final List<MethodParameterModel> queryParams = new ArrayList<>();
            final List<Parameter> params = Parameter.ofMethod(method);
            for (Parameter param : params) {
                final QueryParam queryParamAnnotation = param.getAnnotation(QueryParam.class);
                if (queryParamAnnotation != null) {
                    queryParams.add(new MethodParameterModel(queryParamAnnotation.value(), param.getParameterizedType()));
                }
            }
            // JAX-RS specification - 3.3.2.1 Entity Parameters
            final MethodParameterModel entityParameter = getEntityParameter(method);
            if (entityParameter != null) {
                foundType(result, entityParameter.getType(), resourceClass, method.getName());
            }
            // JAX-RS specification - 3.3.3 Return Type
            final Class<?> returnType = method.getReturnType();
            final Type genericReturnType = method.getGenericReturnType();
            final Type modelReturnType;
            if (returnType == void.class) {
                modelReturnType = returnType;
            } else if (returnType == Response.class) {
                if (swaggerOperation.responseType != null) {
                    modelReturnType = swaggerOperation.responseType;
                    foundType(result, modelReturnType, resourceClass, method.getName());
                } else {
                    modelReturnType = Object.class;
                }
            } else if (genericReturnType instanceof ParameterizedType && returnType == GenericEntity.class) {
                final ParameterizedType parameterizedReturnType = (ParameterizedType) genericReturnType;
                modelReturnType = parameterizedReturnType.getActualTypeArguments()[0];
                foundType(result, modelReturnType, resourceClass, method.getName());
            } else {
                modelReturnType = genericReturnType;
                foundType(result, modelReturnType, resourceClass, method.getName());
            }
            // comments
            final List<String> comments = Swagger.getOperationComments(swaggerOperation);
            // create method
            model.getMethods().add(new JaxrsMethodModel(resourceClass, method.getName(), modelReturnType,
                    context.rootResource, httpMethod.value(), context.path, pathParams, queryParams, entityParameter, comments));
        }
        // JAX-RS specification - 3.4.1 Sub Resources
        if (pathAnnotation != null && httpMethod == null) {
            parseResource(result, context, method.getReturnType());
        }
    }

    private void foundType(Result result, Type type, Class<?> usedInClass, String usedInMember) {
        if (!isExcluded(type)) {
            result.discoveredTypes.add(new SourceType<>(type, usedInClass, usedInMember));
        }
    }

    private boolean isExcluded(Type type) {
        final Class<?> cls = Utils.getRawClassOrNull(type);
        if (cls == null) {
            return false;
        }
        if (isClassNameExcluded != null && isClassNameExcluded.test(cls.getName())) {
            return true;
        }
        if (defaultExcludes.contains(cls.getName())) {
            return true;
        }

        for (Class<?> standardEntityClass : getStandardEntityClasses()) {
            if (standardEntityClass.isAssignableFrom(cls)) {
                return true;
            }
        }
        return false;
    }

    private static HttpMethod getHttpMethod(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            final HttpMethod httpMethodAnnotation = annotation.annotationType().getAnnotation(HttpMethod.class);
            if (httpMethodAnnotation != null) {
                return httpMethodAnnotation;
            }
        }
        return null;
    }

    private static MethodParameterModel getEntityParameter(Method method) {
        final List<Parameter> parameters = Parameter.ofMethod(method);
        for (Parameter parameter : parameters) {
            if (!hasAnyAnnotation(parameter, Arrays.asList(
                    MatrixParam.class,
                    QueryParam.class,
                    PathParam.class,
                    CookieParam.class,
                    HeaderParam.class,
                    Context.class,
                    FormParam.class
                    ))) {
                return new MethodParameterModel(parameter.getName(), parameter.getParameterizedType());
            }
        }
        return null;
    }

    private static boolean hasAnyAnnotation(Parameter parameter, List<Class<? extends Annotation>> annotationClasses) {
        for (Class<? extends Annotation> annotationClass : annotationClasses) {
            for (Annotation parameterAnnotation : parameter.getAnnotations()) {
                if (annotationClass.isInstance(parameterAnnotation)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<Class<?>> getStandardEntityClasses() {
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

    private static class ResourceContext {
        public final Class<?> rootResource;
        public final String path;
        public final Map<String, Type> pathParamTypes;

        public ResourceContext(Class<?> rootResource, String path) {
            this(rootResource, path, new LinkedHashMap<String, Type>());
        }

        private ResourceContext(Class<?> rootResource, String path, Map<String, Type> pathParamTypes) {
            this.rootResource = rootResource;
            this.path = path;
            this.pathParamTypes = pathParamTypes;
        }

        ResourceContext subPath(Path pathAnnotation) {
            final String subPath = pathAnnotation != null ? pathAnnotation.value() : null;
            return new ResourceContext(rootResource, Utils.joinPath(path, subPath), pathParamTypes);
        }

        ResourceContext subPathParamTypes(Map<String, Type> subPathParamTypes) {
            final Map<String, Type> newPathParamTypes = new LinkedHashMap<>();
            newPathParamTypes.putAll(pathParamTypes);
            if (subPathParamTypes != null) {
                newPathParamTypes.putAll(subPathParamTypes);
            }
            return new ResourceContext(rootResource, path, newPathParamTypes);
        }
    }

}
