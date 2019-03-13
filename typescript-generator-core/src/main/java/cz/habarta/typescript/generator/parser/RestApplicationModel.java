
package cz.habarta.typescript.generator.parser;

import java.util.*;


public class RestApplicationModel {

    private final RestApplicationType type;
    private String applicationPath;
    private String applicationName;
    private final List<RestMethodModel> methods = new ArrayList<>();

    public RestApplicationModel(RestApplicationType type) {
        this.type = type;
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

}
