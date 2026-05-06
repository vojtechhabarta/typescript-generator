
package cz.habarta.typescript.generator.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;


public class RestApplicationModel {

    private final RestApplicationType type;
    private @Nullable String applicationPath;
    private @Nullable String applicationName;
    private final List<RestMethodModel> methods;

    public RestApplicationModel(RestApplicationType type) {
        this.type = type;
        this.methods = new ArrayList<>();
    }

    public RestApplicationModel(
        RestApplicationType type,
        @Nullable String applicationPath,
        @Nullable String applicationName,
        List<RestMethodModel> methods
    ) {
        this.type = Objects.requireNonNull(type);
        this.applicationPath = applicationPath;
        this.applicationName = applicationName;
        this.methods = Objects.requireNonNull(methods);
    }

    public RestApplicationType getType() {
        return type;
    }

    public @Nullable String getApplicationPath() {
        return applicationPath;
    }

    public void setApplicationPath(@Nullable String applicationPath) {
        this.applicationPath = applicationPath;
    }

    public @Nullable String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(@Nullable String applicationName) {
        this.applicationName = applicationName;
    }

    public List<RestMethodModel> getMethods() {
        return methods;
    }

    public RestApplicationModel withMethods(List<RestMethodModel> methods) {
        return new RestApplicationModel(type, applicationPath, applicationName, methods);
    }

}
