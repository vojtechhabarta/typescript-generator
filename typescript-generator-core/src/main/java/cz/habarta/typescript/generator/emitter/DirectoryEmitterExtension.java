package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.emitter.EmitterExtension.Writer;

public interface DirectoryEmitterExtension {

    void emitElement(Writer writer, Settings settings, boolean exportKeyword, TsModel model, TsDeclarationModel declaration);

}
