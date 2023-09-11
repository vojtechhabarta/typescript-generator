
package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;


public class RestMethodModel extends MethodModel {

    private final Class<?> rootResource;
    private final String httpMethod;
    private final String path;
    private final List<MethodParameterModel> pathParams;
    private final List<RestParam> queryParams;
    private final MethodParameterModel entityParam;
    private final List<RestParam> headers;

    public RestMethodModel(Class<?> originClass, String name, Type returnType, Method originalMethod,
                           Class<?> rootResource, String httpMethod, String path, List<MethodParameterModel> pathParams, List<RestParam> queryParams, MethodParameterModel entityParam,
                           List<String> comments, List<RestParam> headers) {
        super(originClass, name, null, returnType, originalMethod, comments);
        this.rootResource = rootResource;
        this.httpMethod = httpMethod;
        this.path = path;
        this.pathParams = pathParams;
        this.queryParams = queryParams;
        this.entityParam = entityParam;
        this.headers = headers;
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

    public List<RestParam> getQueryParams() {
        return queryParams;
    }

    public MethodParameterModel getEntityParam() {
        return entityParam;
    }

    public List<RestParam> getHeaders() {
        return headers;
    }

    @Override
    public RestMethodModel withComments(List<String> comments) {
        return new RestMethodModel(originClass, name, returnType, originalMethod, rootResource, httpMethod, path, pathParams, queryParams, entityParam, comments, headers);
    }

}
