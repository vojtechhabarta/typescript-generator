
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;
import java.util.List;


public class TsMethodModel extends TsCallableModel {
    
    public TsMethodModel(String name, TsType returnType, List<TsParameterModel> parameters, List<TsStatement> body, List<String> comments) {
        super(name, returnType, parameters, body, comments);
    }

}
