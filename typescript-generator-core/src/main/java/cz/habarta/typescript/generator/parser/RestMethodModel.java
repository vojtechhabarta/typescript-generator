
package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import org.jspecify.annotations.Nullable;


public class RestMethodModel extends MethodModel {

    private final Class<?> rootResource;
    private final String httpMethod;
    private final @Nullable String path;
    private final List<MethodParameterModel> pathParams;
    private final List<RestQueryParam> queryParams;
    private final @Nullable MethodParameterModel entityParam;

    public RestMethodModel(
        Class<?> originClass,
        String name,
        Type returnType,
        Method originalMethod,
        Class<?> rootResource,
        String httpMethod,
        @Nullable String path,
        List<MethodParameterModel> pathParams,
        List<RestQueryParam> queryParams,
        @Nullable MethodParameterModel entityParam,
        @Nullable List<String> comments
    ) {
        super(originClass, name, null, returnType, originalMethod, comments);
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

    public @Nullable String getPath() {
        return path;
    }

    public List<MethodParameterModel> getPathParams() {
        return pathParams;
    }

    public List<RestQueryParam> getQueryParams() {
        return queryParams;
    }

    public @Nullable MethodParameterModel getEntityParam() {
        return entityParam;
    }

    @Override
    public RestMethodModel withComments(@Nullable List<String> comments) {
        return new RestMethodModel(originClass, name, returnType, originalMethod, rootResource, httpMethod, path, pathParams, queryParams, entityParam, comments);
    }

}
