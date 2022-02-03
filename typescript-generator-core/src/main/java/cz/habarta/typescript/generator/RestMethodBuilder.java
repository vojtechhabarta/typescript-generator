package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.MethodParameterModel;
import cz.habarta.typescript.generator.parser.RestMethodModel;
import cz.habarta.typescript.generator.parser.RestQueryParam;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

public interface RestMethodBuilder {

    RestMethodModel build(Class<?> originClass, String name, Type returnType, Method originalMethod,
                          Class<?> rootResource, String httpMethod, String path, List<MethodParameterModel> pathParams, List<RestQueryParam> queryParams, MethodParameterModel entityParam,
                          List<String> comments);

}
