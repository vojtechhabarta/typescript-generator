
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;
import java.util.List;


public class TsEnumModel extends TsDeclarationModel {
    
    private final List<String> values;

    public TsEnumModel(TsType name, List<String> comments, List<String> values) {
        super(name, comments);
        this.values = values;
    }

    public TsEnumModel(Class<?> origin, TsType name, List<String> comments, List<String> values) {
        super(origin, name, comments);
        this.values = values;
    }

    public List<String> getValues() {
        return values;
    }

}
