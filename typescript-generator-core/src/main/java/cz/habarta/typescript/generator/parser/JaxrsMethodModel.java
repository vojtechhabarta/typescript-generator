
package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Type;
import java.util.*;


public class JaxrsMethodModel extends MethodModel {

    private final Class<?> rootResource;
    private final String httpMethod;
    private final String path;
    private final List<MethodParameterModel> pathParams;
    private final List<MethodParameterModel> queryParams;
    private final MethodParameterModel entityParam;

    public JaxrsMethodModel(Class<?> originClass, String name, Type returnType,
            Class<?> rootResource, String httpMethod, String path, List<MethodParameterModel> pathParams, List<MethodParameterModel> queryParams, MethodParameterModel entityParam,
            List<String> comments) {
        super(originClass, name, null, returnType, comments);
        this.rootResource = rootResource;
        this.httpMethod = httpMethod;
        this.path = path;
        this.pathParams = pathParams;
        this.queryParams = queryParams;
        this.entityParam = entityParam;
    }

    public Class<?> getRootResource() {
        return rootResource;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public List<MethodParameterModel> getPathParams() {
        return pathParams;
    }

    public List<MethodParameterModel> getQueryParams() {
        return queryParams;
    }

    public MethodParameterModel getEntityParam() {
        return entityParam;
    }

}
