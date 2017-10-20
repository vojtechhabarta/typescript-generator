
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;
import java.util.*;


public class TsCallableModel {
    
    private final String name;
    private final TsModifierFlags modifiers;
    private final List<TsType.GenericVariableType> typeParameters;
    private final List<TsParameterModel> parameters;
    private final TsType returnType;
    private final List<TsStatement> body;
    private final List<String> comments;

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
