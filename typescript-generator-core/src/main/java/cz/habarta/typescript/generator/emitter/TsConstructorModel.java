
package cz.habarta.typescript.generator.emitter;

import java.util.List;


public class TsConstructorModel extends TsCallableModel {
    
    public TsConstructorModel(List<TsParameterModel> parameters, List<TsStatement> body, List<String> comments) {
        super("constructor", null, parameters, body, comments);
    }

}
