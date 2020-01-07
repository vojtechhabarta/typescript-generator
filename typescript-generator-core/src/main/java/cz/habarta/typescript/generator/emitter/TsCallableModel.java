
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;
import java.util.Collections;
import java.util.List;


public class TsCallableModel {
    
    protected final String name;
    protected final TsModifierFlags modifiers;
    protected final List<TsType.GenericVariableType> typeParameters;
    protected final List<TsParameterModel> parameters;
    protected final TsType returnType;
    protected final List<TsStatement> body;
    protected final List<String> comments;

    public TsCallableModel(String name, TsModifierFlags modifiers, List<TsType.GenericVariableType> typeParameters,
            List<TsParameterModel> parameters, TsType returnType, List<TsStatement> body, List<String> comments) {
        this.name = name;
        this.modifiers = modifiers != null ? modifiers : TsModifierFlags.None;
        this.typeParameters = typeParameters != null ? typeParameters : Collections.<TsType.GenericVariableType>emptyList();
        this.parameters = parameters;
        this.returnType = returnType;
        this.body = body;
        this.comments = comments;
    }

    public String getName() {
        return name;
    }

    public TsModifierFlags getModifiers() {
        return modifiers;
    }

    public List<TsType.GenericVariableType> getTypeParameters() {
        return typeParameters;
    }

    public List<TsParameterModel> getParameters() {
        return parameters;
    }

    public TsType getReturnType() {
        return returnType;
    }

    public List<TsStatement> getBody() {
        return body;
    }

    public List<String> getComments() {
        return comments;
    }

}
