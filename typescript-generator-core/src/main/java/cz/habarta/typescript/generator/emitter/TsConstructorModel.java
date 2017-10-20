
package cz.habarta.typescript.generator.emitter;

import java.util.List;


public class TsConstructorModel extends TsCallableModel {
    
    public TsConstructorModel(TsModifierFlags modifiers, List<TsParameterModel> parameters, List<TsStatement> body, List<String> comments) {
        super("constructor", modifiers, null, parameters, null, body, comments);
    }

}
