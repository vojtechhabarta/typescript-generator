
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.compiler.Symbol;
import java.util.*;


public class TsAliasModel extends TsDeclarationModel {
    
    private final List<TsType.GenericVariableType> typeParameters;
    private final TsType definition;

    public TsAliasModel(Class<?> origin, Symbol name, List<TsType.GenericVariableType> typeParameters, TsType definition, List<String> comments) {
        super(origin, null, name, comments);
        this.typeParameters = typeParameters != null ? typeParameters : Collections.<TsType.GenericVariableType>emptyList();
        this.definition = definition;
    }

    public List<TsType.GenericVariableType> getTypeParameters() {
        return typeParameters;
    }

    public TsType getDefinition() {
        return definition;
    }

}
