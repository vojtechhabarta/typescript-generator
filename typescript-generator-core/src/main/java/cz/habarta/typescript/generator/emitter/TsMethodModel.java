
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;
import java.util.List;


public class TsMethodModel {
    
    private final String name;
    private final TsType returnType;
    private final List<TsParameterModel> parameters;
    private final List<String> comments;

    public TsMethodModel(String name, TsType returnType, List<TsParameterModel> parameters, List<String> comments) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
        this.comments = comments;
    }

    public String getName() {
        return name;
    }

    public TsType getReturnType() {
        return returnType;
    }

    public List<TsParameterModel> getParameters() {
        return parameters;
    }

    public List<String> getComments() {
        return comments;
    }

}
