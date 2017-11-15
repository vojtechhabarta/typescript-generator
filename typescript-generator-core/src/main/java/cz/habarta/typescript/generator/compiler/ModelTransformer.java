
package cz.habarta.typescript.generator.compiler;

import cz.habarta.typescript.generator.emitter.TsModel;


public interface ModelTransformer {

    public TsModel transformModel(SymbolTable symbolTable, TsModel model);

}
