
package cz.habarta.typescript.generator.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class RestApplicationModel {

    private final RestApplicationType type;
    private String applicationPath;
    private String applicationName;
    private final List<RestMethodModel> methods;

    public RestApplicationModel(RestApplicationType type) {
        this.type = type;
        this.methods = new ArrayList<>();
    }

    public RestApplicationModel(RestApplicationType type, String applicationPath, String applicationName, List<RestMethodModel> methods) {
        this.type = Objects.requireNonNull(type);
        this.applicationPath = applicationPath;
        this.applicationName = applicationName;
        this.methods = Objects.requireNonNull(methods);
    }

    public RestApplicationType getType() {
        return type;
    }

    public String getApplicationPath() {
        return applicationPath;
    }

    public void setApplicationPath(String applicationPath) {
        this.applicationPath = applicationPath;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public List<RestMethodModel> getMethods() {
        return methods;
    }

    public RestApplicationModel withMethods(List<RestMethodModel> methods) {
        return new RestApplicationModel(type, applicationPath, applicationName, methods);
    }

}
