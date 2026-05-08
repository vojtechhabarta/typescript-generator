
package cz.habarta.typescript.generator.emitter;

import java.util.List;
import org.jspecify.annotations.Nullable;


public class TsConstructorModel extends TsCallableModel {

    public TsConstructorModel(TsModifierFlags modifiers, List<TsParameterModel> parameters, @Nullable List<TsStatement> body, @Nullable List<String> comments) {
        super("constructor", modifiers, null, parameters, null, body, comments);
    }

    public TsConstructorModel withParameters(List<TsParameterModel> parameters) {
        return new TsConstructorModel(modifiers, parameters, body, comments);
    }

}
