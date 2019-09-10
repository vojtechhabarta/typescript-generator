
package cz.habarta.typescript.generator.spring;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.TypeProcessor;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.parser.JaxrsApplicationParser;
import cz.habarta.typescript.generator.parser.MethodParameterModel;
import cz.habarta.typescript.generator.parser.PathTemplate;
import cz.habarta.typescript.generator.parser.RestApplicationModel;
import cz.habarta.typescript.generator.parser.RestApplicationParser;
import cz.habarta.typescript.generator.parser.RestApplicationType;
import cz.habarta.typescript.generator.parser.RestMethodModel;
import cz.habarta.typescript.generator.parser.RestQueryParam;
import cz.habarta.typescript.generator.parser.SourceType;
import cz.habarta.typescript.generator.util.Utils;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


public class SpringApplicationParser extends RestApplicationParser {

    // This factory class is instantiated using reflections!
    public static class Factory extends RestApplicationParser.Factory {

        @Override
        public TypeProcessor getSpecificTypeProcessor() {
            return (javaType, context) -> {
                final Class<?> rawClass = Utils.getRawClassOrNull(javaType);
                if (rawClass != null) {
                    for (Map.Entry<Class<?>, TsType> entry : getStandardEntityClassesMapping().entrySet()) {
                        final Class<?> cls = entry.getKey();
                        final TsType type = entry.getValue();
                        if (cls.isAssignableFrom(rawClass) && type != null) {
                            return new TypeProcessor.Result(type);
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
        public RestApplicationParser create(Settings settings, TypeProcessor commonTypeProcessor) {
            return new SpringApplicationParser(settings, commonTypeProcessor);
        }

    };

    public SpringApplicationParser(Settings settings, TypeProcessor commonTypeProcessor) {
        super(settings, commonTypeProcessor, new RestApplicationModel(RestApplicationType.Spring));
    }

    @Override
    public JaxrsApplicationParser.Result tryParse(SourceType<?> sourceType) {
        if (!(sourceType.type instanceof Class<?>)) {
            return null;
        }
        final Class<?> cls = (Class<?>) sourceType.type;

        // application
        final SpringBootApplication app = AnnotationUtils.findAnnotation(cls, SpringBootApplication.class);
        if (app != null) {
            if (settings.scanSpringApplication) {
                TypeScriptGenerator.getLogger().verbose("Scanning Spring application: " + cls.getName());
                final ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(settings.classLoader);
                    final SpringApplicationHelper springApplicationHelper = new SpringApplicationHelper(settings.classLoader, cls);
                    final List<Class<?>> restControllers = springApplicationHelper.findRestControllers();
                    return new JaxrsApplicationParser.Result(restControllers.stream()
                            .map(controller -> new SourceType<Type>(controller, cls, "<scanned>"))
                            .collect(Collectors.toList())
                    );
                } finally {
                    Thread.currentThread().setContextClassLoader(originalContextClassLoader);
                }
            } else {
                return null;
            }
        }

        // controller
        final RestController controller = AnnotationUtils.findAnnotation(cls, RestController.class);
        if (controller != null) {
            TypeScriptGenerator.getLogger().verbose("Parsing Spring RestController: " + cls.getName());
            final JaxrsApplicationParser.Result result = new JaxrsApplicationParser.Result();
            final RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(cls, RequestMapping.class);
            final String path = requestMapping != null && requestMapping.path() != null ? requestMapping.path()[0] : null;
            final JaxrsApplicationParser.ResourceContext context = new JaxrsApplicationParser.ResourceContext(cls, path);
            parseController(result, context, cls);
            return result;
        }
        
        return null;
    }

    private class SpringApplicationHelper extends SpringApplication {

        private final ClassLoader classLoader;

        public SpringApplicationHelper(ClassLoader classLoader, Class<?>... primarySources) {
            super(primarySources);
            this.classLoader = classLoader;
        }

        public List<Class<?>> findRestControllers() {
            try (ConfigurableApplicationContext context = createApplicationContext()) {
                load(context, getAllSources().toArray());
                withSystemProperty("server.port", "0", () -> {
                    context.refresh();
                });
                final List<Class<?>> classes = Stream.of(context.getBeanDefinitionNames())
                        .map(beanName -> context.getBeanFactory().getBeanDefinition(beanName).getBeanClassName())
                        .filter(Objects::nonNull)
                        .filter(className -> isClassNameExcluded == null || !isClassNameExcluded.test(className))
                        .map(className -> {
                            try {
                                return classLoader.loadClass(className);
                            } catch (ClassNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .filter(instance -> AnnotationUtils.findAnnotation(instance, RestController.class) != null)
                        .collect(Collectors.toList());
                return classes;
            }
        }

    }

    private static void withSystemProperty(String name, String value, Runnable runnable) {
        final String original = System.getProperty(name);
        try {
            System.setProperty(name, value);
            runnable.run();
        } finally {
            if (original != null) {
                System.setProperty(name, original);
            } else {
                System.getProperties().remove(name);
            }
        }
    }

    private void parseController(JaxrsApplicationParser.Result result, JaxrsApplicationParser.ResourceContext context, Class<?> controllerClass) {
        // parse controller methods
        final List<Method> methods = Utils.getAllMethods(controllerClass);
        methods.sort(Utils.methodComparator());
        for (Method method : methods) {
            parseControllerMethod(result, context, controllerClass, method);
        }
    }

    // https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-ann-methods
    private void parseControllerMethod(JaxrsApplicationParser.Result result, JaxrsApplicationParser.ResourceContext context, Class<?> controllerClass, Method method) {
        final RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (requestMapping != null) {

            // subContext
            context = context.subPath(requestMapping.path().length == 0 ? "" : requestMapping.path()[0]);
            final Map<String, Type> pathParamTypes = new LinkedHashMap<>();
            for (Parameter parameter : method.getParameters()) {
                final PathVariable pathVariableAnnotation = AnnotationUtils.findAnnotation(parameter, PathVariable.class);
                if (pathVariableAnnotation != null) {
                    pathParamTypes.put(pathVariableAnnotation.value(), parameter.getParameterizedType());
                }
            }
            context = context.subPathParamTypes(pathParamTypes);
            final RequestMethod httpMethod = requestMapping.method().length == 0 ? RequestMethod.GET : requestMapping.method()[0];

            // path parameters
            final PathTemplate pathTemplate = PathTemplate.parse(context.path);
            final Map<String, Type> contextPathParamTypes = context.pathParamTypes;
            final List<MethodParameterModel> pathParams = pathTemplate.getParts().stream()
                    .filter(PathTemplate.Parameter.class::isInstance)
                    .map(PathTemplate.Parameter.class::cast)
                    .map(parameter -> {
                        final Type type = contextPathParamTypes.get(parameter.getOriginalName());
                        final Type paramType = type != null ? type : String.class;
                        foundType(result, paramType, controllerClass, method.getName());
                        return new MethodParameterModel(parameter.getValidName(), paramType);
                    })
                    .collect(Collectors.toList());

            // query parameters
            final List<RestQueryParam> queryParams = new ArrayList<>();
            for (Parameter parameter : method.getParameters()) {
                if(parameter.getType() == Pageable.class) {
                    queryParams.add(new RestQueryParam.Single(new MethodParameterModel("page", Long.class)));
                    foundType(result, Long.class, controllerClass, method.getName());

                    queryParams.add(new RestQueryParam.Single(new MethodParameterModel("size", Long.class)));
                    foundType(result, Long.class, controllerClass, method.getName());

                    queryParams.add(new RestQueryParam.Single(new MethodParameterModel("sort", String.class)));
                    foundType(result, String.class, controllerClass, method.getName());
                } else {
                    final RequestParam requestParamAnnotation = AnnotationUtils.findAnnotation(parameter, RequestParam.class);
                    if (requestParamAnnotation != null) {
                        queryParams.add(new RestQueryParam.Single(new MethodParameterModel(firstOf(
                            requestParamAnnotation.value(),
                            parameter.getName()
                        ), parameter.getParameterizedType())));
                        foundType(result, parameter.getParameterizedType(), controllerClass, method.getName());
                    }
                }
            }

            // entity parameter
            final MethodParameterModel entityParameter = getEntityParameter(controllerClass, method);
            if (entityParameter != null) {
                foundType(result, entityParameter.getType(), controllerClass, method.getName());
            }

            final Type modelReturnType = parseReturnType(controllerClass, method);
            foundType(result, modelReturnType, controllerClass, method.getName());

            model.getMethods().add(new RestMethodModel(controllerClass, method.getName(), modelReturnType,
                    controllerClass, httpMethod.name(), context.path, pathParams, queryParams, entityParameter, null));
        }
    }

    private Type parseReturnType(Class<?> controllerClass, Method method) {
        final Type modelReturnType = method.getGenericReturnType();
        if(modelReturnType instanceof TypeVariable) {
            return GenericTypeResolver.resolveReturnType(method, controllerClass);
        } else if (modelReturnType instanceof  ParameterizedType && ((ParameterizedType) modelReturnType).getRawType() == ResponseEntity.class) {
            return ((ParameterizedType) modelReturnType).getActualTypeArguments()[0];
        } else {
            return modelReturnType;
        }
    }

    private static MethodParameterModel getEntityParameter(Class<?> controller, Method method) {
        for(int index = 0; index < method.getParameterCount(); ++index) {
            Parameter parameter =  method.getParameters()[index];
            final RequestBody requestBodyAnnotation = AnnotationUtils.findAnnotation(parameter, RequestBody.class);
            if (requestBodyAnnotation != null) {
                Type modelParameterType = parameter.getParameterizedType();
                if(modelParameterType instanceof TypeVariable) {
                    final MethodParameter methodParameter = new MethodParameter(method, index);
                    modelParameterType = GenericTypeResolver.resolveParameterType(methodParameter, controller);
                }
                return new MethodParameterModel(parameter.getName(), modelParameterType);
            }
        }
        return null;
    }

    private static Map<Class<?>, TsType> getStandardEntityClassesMapping() {
        if (standardEntityClassesMapping == null) {
            final Map<Class<?>, TsType> map = new LinkedHashMap<>();
            standardEntityClassesMapping = map;
        }
        return standardEntityClassesMapping;
    }

    private static Map<Class<?>, TsType> standardEntityClassesMapping;

    private static List<String> getDefaultExcludedClassNames() {
        return Arrays.asList(
        );
    }

    private static String firstOf(String... values) {
        return Stream.of(values).filter(it -> it != null && !it.isEmpty()).findFirst().orElse("");
    }
}
