
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.util.Utils;
import java.util.List;
import org.jspecify.annotations.Nullable;


public class TsCallableModel {

    protected final String name;
    protected final TsModifierFlags modifiers;
    protected final List<TsType.GenericVariableType> typeParameters;
    protected final List<TsParameterModel> parameters;
    protected final @Nullable TsType returnType;
    protected final @Nullable List<TsStatement> body;
    protected final @Nullable List<String> comments;

    public TsCallableModel(
        String name,
        @Nullable TsModifierFlags modifiers,
        @Nullable List<TsType.GenericVariableType> typeParameters,
        List<TsParameterModel> parameters,
        @Nullable TsType returnType,
        @Nullable List<TsStatement> body,
        @Nullable List<String> comments
    ) {
        this.name = name;
        this.modifiers = modifiers != null ? modifiers : TsModifierFlags.None;
        this.typeParameters = Utils.listFromNullable(typeParameters);
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

    public @Nullable TsType getReturnType() {
        return returnType;
    }

    public @Nullable List<TsStatement> getBody() {
        return body;
    }

    public @Nullable List<String> getComments() {
        return comments;
    }

}
