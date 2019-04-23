
package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Type;
import java.util.*;


public class RestMethodModel extends MethodModel {

    private final Class<?> rootResource;
    private final String httpMethod;
    private final String path;
    private final List<MethodParameterModel> pathParams;
    private final List<RestQueryParam> queryParams;
    private final MethodParameterModel entityParam;

    public RestMethodModel(Class<?> originClass, String name, Type returnType,
            Class<?> rootResource, String httpMethod, String path, List<MethodParameterModel> pathParams, List<RestQueryParam> queryParams, MethodParameterModel entityParam,
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

    public List<RestQueryParam> getQueryParams() {
        return queryParams;
    }

    public MethodParameterModel getEntityParam() {
        return entityParam;
    }

}
