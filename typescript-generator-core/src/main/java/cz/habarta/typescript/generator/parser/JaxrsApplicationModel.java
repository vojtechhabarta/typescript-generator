
package cz.habarta.typescript.generator.parser;

import java.util.*;


public class JaxrsApplicationModel {

    private String applicationPath;
    private String applicationName;
    private final List<JaxrsMethodModel> methods = new ArrayList<>();

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

    public List<JaxrsMethodModel> getMethods() {
        return methods;
    }

}
