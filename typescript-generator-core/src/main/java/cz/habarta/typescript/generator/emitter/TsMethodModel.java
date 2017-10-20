
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;
import java.util.List;


public class TsMethodModel extends TsCallableModel {
    
    public TsMethodModel(String name, TsModifierFlags modifiers, List<TsType.GenericVariableType> typeParameters, List<TsParameterModel> parameters, TsType returnType, List<TsStatement> body, List<String> comments) {
        super(name, modifiers, typeParameters, parameters, returnType, body, comments);
    }

}
