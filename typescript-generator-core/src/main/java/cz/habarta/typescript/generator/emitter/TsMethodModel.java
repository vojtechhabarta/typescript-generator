
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.util.Utils;
import java.util.List;


public class TsMethodModel extends TsCallableModel {

    protected final List<TsDecorator> decorators;

    public TsMethodModel(String name, TsModifierFlags modifiers, List<TsType.GenericVariableType> typeParameters, List<TsParameterModel> parameters, TsType returnType, List<TsStatement> body, List<String> comments) {
        this(name, null, modifiers, typeParameters, parameters, returnType, body, comments);
    }

    private TsMethodModel(String name, List<TsDecorator> decorators, TsModifierFlags modifiers, List<TsType.GenericVariableType> typeParameters, List<TsParameterModel> parameters, TsType returnType, List<TsStatement> body, List<String> comments) {
        super(name, modifiers, typeParameters, parameters, returnType, body, comments);
        this.decorators = Utils.listFromNullable(decorators);
    }

    public List<TsDecorator> getDecorators() {
        return decorators;
    }

    public TsMethodModel withDecorators(List<TsDecorator> decorators) {
        return new TsMethodModel(name, decorators, modifiers, typeParameters, parameters, returnType, body, comments);
    }

    public TsMethodModel withParameters(List<TsParameterModel> parameters) {
        return new TsMethodModel(name, decorators, modifiers, typeParameters, parameters, returnType, body, comments);
    }

}
