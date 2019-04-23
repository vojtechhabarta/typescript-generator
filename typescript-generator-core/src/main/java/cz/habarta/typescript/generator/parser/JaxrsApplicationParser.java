
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.JaxrsApplicationScanner;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.TypeProcessor;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.util.Utils;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
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
import java.util.stream.Stream;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.BeanParam;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

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
            final PathParam pathParamAnnotation = field.getAnnotation(PathParam.class);
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
        final Path pathAnnotation = method.getAnnotation(Path.class);
        // subContext
        context = context.subPath(pathAnnotation != null ? pathAnnotation.value() : null);
        final Map<String, Type> pathParamTypes = new LinkedHashMap<>();
        for (Parameter parameter : method.getParameters()) {
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
                    final Type type = context.pathParamTypes.get(parameter.getOriginalName());
                    final Type paramType = type != null ? type : String.class;
                    pathParams.add(new MethodParameterModel(parameter.getValidName(), paramType));
                    foundType(result, paramType, resourceClass, method.getName());
                }
            }
            // query parameters
            final List<RestQueryParam> queryParams = new ArrayList<>();
            for (Parameter param : method.getParameters()) {
                final QueryParam queryParamAnnotation = param.getAnnotation(QueryParam.class);
                if (queryParamAnnotation != null) {
                    queryParams.add(new RestQueryParam.Single(new MethodParameterModel(queryParamAnnotation.value(), param.getParameterizedType())));
                    foundType(result, param.getParameterizedType(), resourceClass, method.getName());
                }
                final BeanParam beanParamAnnotation = param.getAnnotation(BeanParam.class);
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
            final MethodParameterModel entityParameter = getEntityParameter(method);
            if (entityParameter != null) {
                foundType(result, entityParameter.getType(), resourceClass, method.getName());
            }
            // JAX-RS specification - 3.3.3 Return Type
            final Class<?> returnType = method.getReturnType();
            final Type genericReturnType = method.getGenericReturnType();
            final Type modelReturnType;
            if (returnType == void.class) {
                //for async response also use swagger
                if (hasAnyAnnotation(method.getParameters(), Collections.singletonList(Suspended.class))) {
                    if (swaggerOperation.responseType != null) {
                        modelReturnType = swaggerOperation.responseType;
                        foundType(result, modelReturnType, resourceClass, method.getName());
                    } else {
                        modelReturnType = Object.class;
                    }
                } else {
                    modelReturnType = returnType;
                }
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
            model.getMethods().add(new RestMethodModel(resourceClass, method.getName(), modelReturnType,
                    context.rootResource, httpMethod.value(), context.path, pathParams, queryParams, entityParameter, comments));
        }
        // JAX-RS specification - 3.4.1 Sub Resources
        if (pathAnnotation != null && httpMethod == null) {
            parseResource(result, context, method.getReturnType());
        }
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

    private static BeanModel getQueryParameters(Class<?> paramBean) {
        final List<PropertyModel> properties = new ArrayList<>();
        final List<Field> fields = Utils.getAllFields(paramBean);
        for (Field field : fields) {
            final QueryParam annotation = field.getAnnotation(QueryParam.class);
            if (annotation != null) {
                properties.add(new PropertyModel(annotation.value(), field.getGenericType(), /*optional*/true, field, null, null, null));
            }
        }
        try {
            final BeanInfo beanInfo = Introspector.getBeanInfo(paramBean);
            for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
                final Method writeMethod = propertyDescriptor.getWriteMethod();
                if (writeMethod != null) {
                    final QueryParam annotation = writeMethod.getAnnotation(QueryParam.class);
                    if (annotation != null) {
                        properties.add(new PropertyModel(annotation.value(), propertyDescriptor.getPropertyType(), /*optional*/true, writeMethod, null, null, null));
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

    private static MethodParameterModel getEntityParameter(Method method) {
        for (Parameter parameter : method.getParameters()) {
            if (!Utils.hasAnyAnnotation(parameter::getAnnotation, Arrays.asList(
                    MatrixParam.class,
                    QueryParam.class,
                    PathParam.class,
                    CookieParam.class,
                    HeaderParam.class,
                    Suspended.class,
                    Context.class,
                    FormParam.class,
                    BeanParam.class
            ))) {
                return new MethodParameterModel(parameter.getName(), parameter.getParameterizedType());
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
            map.put(javax.xml.bind.JAXBElement.class, null);
            map.put(MultivaluedMap.class, TsType.Any);
            map.put(StreamingOutput.class, TsType.Any);
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

}
