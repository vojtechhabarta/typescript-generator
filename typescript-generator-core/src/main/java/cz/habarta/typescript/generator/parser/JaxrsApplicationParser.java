
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.JaxrsApplicationScanner;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.TypeProcessor;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.type.JTypeWithNullability;
import cz.habarta.typescript.generator.util.GenericsResolver;
import cz.habarta.typescript.generator.util.Pair;
import cz.habarta.typescript.generator.util.Utils;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;


public class JaxrsApplicationParser extends RestApplicationParser {

    public static class Factory extends RestApplicationParser.Factory {

        @Override
        public TypeProcessor getSpecificTypeProcessor() {
            return (javaType, context) -> {
                final Class<?> rawClass = Utils.getRawClassOrNull(javaType);
                if (rawClass != null) {
                    for (Map.Entry<Class<?>, TsType> entry : getStandardEntityClassesMapping().entrySet()) {
                        final Class<?> cls = entry.getKey();
                        final TsType type = entry.getValue();
                        if (cls.isAssignableFrom(rawClass)) {
                            return type != null ? new TypeProcessor.Result(type) : null;
                        }
                    }
                    if (getDefaultExcludedClassNames().contains(rawClass.getName())) {
                        return new TypeProcessor.Result(TsType.Any);
                    }
                }
                return null;
            };
        }

        @Override
        public JaxrsApplicationParser create(Settings settings, TypeProcessor commonTypeProcessor) {
            return new JaxrsApplicationParser(settings, commonTypeProcessor);
        }

    };

    public JaxrsApplicationParser(Settings settings, TypeProcessor commonTypeProcessor) {
        super(settings, commonTypeProcessor, new RestApplicationModel(RestApplicationType.Jaxrs));
    }

    @Override
    public Result tryParse(SourceType<?> sourceType) {
        if (!(sourceType.type instanceof Class<?>)) {
            return null;
        }
        final Class<?> cls = (Class<?>) sourceType.type;

        // application
        if (Application.class.isAssignableFrom(cls) || javax(Application.class).isAssignableFrom(cls)) {
            final ApplicationPath applicationPathAnnotation = getRsAnnotation(cls, ApplicationPath.class);
            if (applicationPathAnnotation != null) {
                model.setApplicationPath(applicationPathAnnotation.value());
            }
            model.setApplicationName(cls.getSimpleName());
            final List<SourceType<Type>> discoveredTypes = JaxrsApplicationScanner.scanJaxrsApplication(cls, isClassNameExcluded);
            return new Result(discoveredTypes);
        }

        // resource
        final Path path = getRsAnnotation(cls, Path.class);
        if (path != null) {
            TypeScriptGenerator.getLogger().verbose("Parsing JAX-RS resource: " + cls.getName());
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
            final PathParam pathParamAnnotation = getRsAnnotation(field, PathParam.class);
            if (pathParamAnnotation != null) {
                pathParamTypes.put(pathParamAnnotation.value(), field.getType());
            }
        }
        final ResourceContext subContext = context.subPathParamTypes(pathParamTypes);
        // parse resource methods
        final List<Method> methods = Arrays.asList(resourceClass.getMethods());
        Collections.sort(methods, Utils.methodComparator());
        for (Method method : methods) {
            parseResourceMethod(result, subContext, resourceClass, method);
        }
    }

    private void parseResourceMethod(Result result, ResourceContext context, Class<?> resourceClass, Method method) {
        final Path pathAnnotation = getRsAnnotation(method, Path.class);
        // subContext
        context = context.subPath(pathAnnotation != null ? pathAnnotation.value() : null);
        final Map<String, Type> pathParamTypes = new LinkedHashMap<>();
        for (Parameter parameter : method.getParameters()) {
            final PathParam pathParamAnnotation = getRsAnnotation(parameter, PathParam.class);
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
                    final Type type = context.pathParamTypes.get(parameter.getOriginalName());
                    final Type paramType = type != null ? type : String.class;
                    final Type resolvedParamType = GenericsResolver.resolveType(resourceClass, paramType, method.getDeclaringClass());
                    pathParams.add(new MethodParameterModel(parameter.getValidName(), resolvedParamType));
                    foundType(result, resolvedParamType, resourceClass, method.getName());
                }
            }
            // query parameters
            final List<RestQueryParam> queryParams = new ArrayList<>();
            for (Parameter param : method.getParameters()) {
                final QueryParam queryParamAnnotation = getRsAnnotation(param, QueryParam.class);
                if (queryParamAnnotation != null) {
                    queryParams.add(new RestQueryParam.Single(new MethodParameterModel(queryParamAnnotation.value(), param.getParameterizedType()), false));
                    foundType(result, param.getParameterizedType(), resourceClass, method.getName());
                }
                final BeanParam beanParamAnnotation = getRsAnnotation(param, BeanParam.class);
                if (beanParamAnnotation != null) {
                    final Class<?> beanParamClass = param.getType();
                    final BeanModel paramBean = getQueryParameters(beanParamClass);
                    if (paramBean != null) {
                        queryParams.add(new RestQueryParam.Bean(paramBean));
                        for (PropertyModel property : paramBean.getProperties()) {
                            foundType(result, property.getType(), beanParamClass, property.getName());
                        }
                    }
                }
            }
            // JAX-RS specification - 3.3.2.1 Entity Parameters
            final List<Type> parameterTypes = settings.getTypeParser().getMethodParameterTypes(method);
            final List<Pair<Parameter, Type>> parameters = Utils.zip(Arrays.asList(method.getParameters()), parameterTypes);
            final MethodParameterModel entityParameter = getEntityParameter(resourceClass, method, parameters);
            if (entityParameter != null) {
                foundType(result, entityParameter.getType(), resourceClass, method.getName());
            }
            // JAX-RS specification - 3.3.3 Return Type
            final Class<?> returnType = method.getReturnType();
            final Type parsedReturnType = settings.getTypeParser().getMethodReturnType(method);
            final Type plainReturnType = JTypeWithNullability.getPlainType(parsedReturnType);
            final Type modelReturnType;
            if (returnType == void.class) {
                //for async response also use swagger
                if (hasAnyAnnotation(method.getParameters(), Arrays.asList(Suspended.class, javax(Suspended.class)))) {
                    if (swaggerOperation.responseType != null) {
                        modelReturnType = swaggerOperation.responseType;
                    } else {
                        modelReturnType = Object.class;
                    }
                } else {
                    modelReturnType = returnType;
                }
            } else if (returnType == Response.class || returnType == javax(Response.class)) {
                if (swaggerOperation.responseType != null) {
                    modelReturnType = swaggerOperation.responseType;
                } else {
                    modelReturnType = Object.class;
                }
            } else if (plainReturnType instanceof ParameterizedType && (returnType == GenericEntity.class || returnType == javax(GenericEntity.class))) {
                final ParameterizedType parameterizedReturnType = (ParameterizedType) plainReturnType;
                modelReturnType = parameterizedReturnType.getActualTypeArguments()[0];
            } else {
                modelReturnType = parsedReturnType;
            }
            final Type resolvedModelReturnType = GenericsResolver.resolveType(resourceClass, modelReturnType, method.getDeclaringClass());
            foundType(result, resolvedModelReturnType, resourceClass, method.getName());
            // comments
            final List<String> comments = Swagger.getOperationComments(swaggerOperation);
            // create method
            model.getMethods().add(restMethodBuilder.build(resourceClass, method.getName(), resolvedModelReturnType, method,
                    context.rootResource, httpMethod.value(), context.path, pathParams, queryParams, entityParameter, comments));
        }
        // JAX-RS specification - 3.4.1 Sub Resources
        if (pathAnnotation != null && httpMethod == null) {
            parseResource(result, context, method.getReturnType());
        }
    }

    private static HttpMethod getHttpMethod(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            final HttpMethod httpMethodAnnotation = getRsAnnotation(annotation.annotationType(), HttpMethod.class);
            if (httpMethodAnnotation != null) {
                return httpMethodAnnotation;
            }
        }
        return null;
    }

    private static BeanModel getQueryParameters(Class<?> paramBean) {
        final List<PropertyModel> properties = new ArrayList<>();
        final List<Field> fields = Utils.getAllFields(paramBean);
        for (Field field : fields) {
            final QueryParam annotation = getRsAnnotation(field, QueryParam.class);
            if (annotation != null) {
                properties.add(new PropertyModel(annotation.value(), field.getGenericType(), /*optional*/true, null, field, null, null, null));
            }
        }
        try {
            final BeanInfo beanInfo = Introspector.getBeanInfo(paramBean);
            for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
                final Method writeMethod = propertyDescriptor.getWriteMethod();
                if (writeMethod != null) {
                    final QueryParam annotation = getRsAnnotation(writeMethod, QueryParam.class);
                    if (annotation != null) {
                        properties.add(new PropertyModel(annotation.value(), propertyDescriptor.getPropertyType(), /*optional*/true, null, writeMethod, null, null, null));
                    }
                }
            }
        } catch (IntrospectionException e) {
            TypeScriptGenerator.getLogger().warning(String.format("Cannot introspect '%s' class: " + e.getMessage(), paramBean));
        }
        if (properties.isEmpty()) {
            return null;
        } else {
            return new BeanModel(paramBean, null, null, null, null, null, properties, null);
        }
    }

    private MethodParameterModel getEntityParameter(Class<?> resourceClass, Method method, List<Pair<Parameter, Type>> parameters) {
        for (Pair<Parameter, Type> pair : parameters) {
            if (!Utils.hasAnyAnnotation(annotationClass -> pair.getValue1().getAnnotation(annotationClass), Arrays.asList(
                    MatrixParam.class, javax(MatrixParam.class),
                    QueryParam.class, javax(QueryParam.class),
                    PathParam.class, javax(PathParam.class),
                    CookieParam.class, javax(CookieParam.class),
                    HeaderParam.class, javax(HeaderParam.class),
                    Suspended.class, javax(Suspended.class),
                    Context.class, javax(Context.class),
                    FormParam.class, javax(FormParam.class),
                    BeanParam.class, javax(BeanParam.class)
            ))) {
                final Type resolvedType = GenericsResolver.resolveType(resourceClass, pair.getValue2(), method.getDeclaringClass());
                return new MethodParameterModel(pair.getValue1().getName(), resolvedType);
            }
        }
        return null;
    }
    
    private static boolean hasAnyAnnotation(Parameter[] parameters, List<Class<? extends Annotation>> annotationClasses) {
        return Stream.of(parameters)
                .anyMatch(parameter -> Utils.hasAnyAnnotation(parameter::getAnnotation, annotationClasses));
    }
    
    private static Map<Class<?>, TsType> getStandardEntityClassesMapping() {
        // JAX-RS specification - 4.2.4 Standard Entity Providers
        if (standardEntityClassesMapping == null) {
            final Map<Class<?>, TsType> map = new LinkedHashMap<>();
            // null value means that class is handled by DefaultTypeProcessor
            map.put(byte[].class, TsType.Any);
            map.put(java.lang.String.class, null);
            map.put(java.io.InputStream.class, TsType.Any);
            map.put(java.io.Reader.class, TsType.Any);
            map.put(java.io.File.class, TsType.Any);
            map.put(javax.activation.DataSource.class, TsType.Any);
            map.put(javax.xml.transform.Source.class, TsType.Any);
            map.put(jakarta.xml.bind.JAXBElement.class, null);
            map.put(javax.xml.bind.JAXBElement.class, null);
            map.put(MultivaluedMap.class, TsType.Any);
            map.put(javax(MultivaluedMap.class), TsType.Any);
            map.put(StreamingOutput.class, TsType.Any);
            map.put(javax(StreamingOutput.class), TsType.Any);
            map.put(java.lang.Boolean.class, null);
            map.put(java.lang.Character.class, null);
            map.put(java.lang.Number.class, null);
            map.put(long.class, null);
            map.put(int.class, null);
            map.put(short.class, null);
            map.put(byte.class, null);
            map.put(double.class, null);
            map.put(float.class, null);
            map.put(boolean.class, null);
            map.put(char.class, null);
            standardEntityClassesMapping = map;
        }
        return standardEntityClassesMapping;
    }

    private static Map<Class<?>, TsType> standardEntityClassesMapping;

    private static List<String> getDefaultExcludedClassNames() {
        return Arrays.asList(
                "org.glassfish.jersey.media.multipart.FormDataBodyPart"
        );
    }

    static <A extends Annotation> A getRsAnnotation(AnnotatedElement annotatedElement, Class<A> jakartaAnnotationClass) {
        final Class<?> javaxAnnotationClass = javax(jakartaAnnotationClass);
        return Utils.getMigratedAnnotation(annotatedElement, jakartaAnnotationClass, javaxAnnotationClass);
    }

    private static <T> Class<T> javax(Class<T> jakartaClass) {
        @SuppressWarnings("unchecked")
        final Class<T> cls = (Class<T>) javaxClasses.get().get(jakartaClass);
        if (cls == null) {
            throw new IllegalArgumentException(jakartaClass.getName());
        }
        return cls;
    }

    private static final Supplier<Map<Class<?>, Class<?>>> javaxClasses = Utils.memoize(() -> {
        final Map<Class<?>, Class<?>> map = new LinkedHashMap<>();
        map.put(jakarta.ws.rs.ApplicationPath.class, javax.ws.rs.ApplicationPath.class);
        map.put(jakarta.ws.rs.BeanParam.class, javax.ws.rs.BeanParam.class);
        map.put(jakarta.ws.rs.CookieParam.class, javax.ws.rs.CookieParam.class);
        map.put(jakarta.ws.rs.FormParam.class, javax.ws.rs.FormParam.class);
        map.put(jakarta.ws.rs.HeaderParam.class, javax.ws.rs.HeaderParam.class);
        map.put(jakarta.ws.rs.HttpMethod.class, javax.ws.rs.HttpMethod.class);
        map.put(jakarta.ws.rs.MatrixParam.class, javax.ws.rs.MatrixParam.class);
        map.put(jakarta.ws.rs.Path.class, javax.ws.rs.Path.class);
        map.put(jakarta.ws.rs.PathParam.class, javax.ws.rs.PathParam.class);
        map.put(jakarta.ws.rs.QueryParam.class, javax.ws.rs.QueryParam.class);
        map.put(jakarta.ws.rs.container.Suspended.class, javax.ws.rs.container.Suspended.class);
        map.put(jakarta.ws.rs.core.Application.class, javax.ws.rs.core.Application.class);
        map.put(jakarta.ws.rs.core.Context.class, javax.ws.rs.core.Context.class);
        map.put(jakarta.ws.rs.core.GenericEntity.class, javax.ws.rs.core.GenericEntity.class);
        map.put(jakarta.ws.rs.core.MultivaluedMap.class, javax.ws.rs.core.MultivaluedMap.class);
        map.put(jakarta.ws.rs.core.Response.class, javax.ws.rs.core.Response.class);
        map.put(jakarta.ws.rs.core.StreamingOutput.class, javax.ws.rs.core.StreamingOutput.class);
        return map;
    });

}
