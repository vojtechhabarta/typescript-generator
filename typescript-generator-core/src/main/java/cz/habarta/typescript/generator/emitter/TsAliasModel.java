
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;
import java.util.List;


public class TsAliasModel extends TsDeclarationModel {
    
    private final TsType definition;

    public TsAliasModel(TsType name, TsType definition, List<String> comments) {
        super(name, comments);
        this.definition = definition;
    }

    public TsAliasModel(Class<?> origin, TsType name, TsType definition, List<String> comments) {
        super(origin, name, comments);
        this.definition = definition;
    }

    public TsType getDefinition() {
        return definition;
    }

}
