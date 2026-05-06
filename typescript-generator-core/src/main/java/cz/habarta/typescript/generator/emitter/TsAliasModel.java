
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.compiler.Symbol;
import cz.habarta.typescript.generator.util.Utils;
import java.util.List;
import org.jspecify.annotations.Nullable;


public class TsAliasModel extends TsDeclarationModel {

    private final List<TsType.GenericVariableType> typeParameters;
    private final TsType definition;

    public TsAliasModel(@Nullable Class<?> origin, Symbol name, @Nullable List<TsType.GenericVariableType> typeParameters, TsType definition, @Nullable List<String> comments) {
        super(origin, null, name, comments);
        this.typeParameters = Utils.listFromNullable(typeParameters);
        this.definition = definition;
    }

    public List<TsType.GenericVariableType> getTypeParameters() {
        return typeParameters;
    }

    public TsType getDefinition() {
        return definition;
    }

}
