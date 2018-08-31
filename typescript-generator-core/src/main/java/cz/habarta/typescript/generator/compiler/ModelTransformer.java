
package cz.habarta.typescript.generator.compiler;

import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.parser.Model;


public interface ModelTransformer {

    public TsModel transformModel(SymbolTable symbolTable, TsModel model);

    default Model transformModel(SymbolTable symbolTable, Model model) {
        return model;
    }

}
