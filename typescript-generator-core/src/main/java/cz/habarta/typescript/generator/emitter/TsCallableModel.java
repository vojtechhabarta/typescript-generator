
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;
import java.util.List;


public class TsCallableModel {
    
    private final String name;
    private final TsType returnType;
    private final List<TsParameterModel> parameters;
    private final List<TsStatement> body;
    private final List<String> comments;

    public TsCallableModel(String name, TsType returnType, List<TsParameterModel> parameters, List<TsStatement> body, List<String> comments) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
        this.body = body;
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

    public List<TsStatement> getBody() {
        return body;
    }

    public List<String> getComments() {
        return comments;
    }

}
