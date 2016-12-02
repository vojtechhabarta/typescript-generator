
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.compiler.Symbol;
import java.util.List;


public class TsAliasModel extends TsDeclarationModel {
    
    private final TsType definition;

    public TsAliasModel(Symbol name, TsType definition, List<String> comments) {
        super(name, comments);
        this.definition = definition;
    }

    public TsAliasModel(Class<?> origin, Symbol name, TsType definition, List<String> comments) {
        super(origin, name, comments);
        this.definition = definition;
    }

    public TsType getDefinition() {
        return definition;
    }

}
