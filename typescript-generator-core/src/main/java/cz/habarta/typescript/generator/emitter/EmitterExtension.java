
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Settings;


public abstract class EmitterExtension {

    public abstract boolean generatesRuntimeCode();

    public void emitObjects(Writer writer, Settings settings, TsModel model) {
    }

    public static interface Writer {
        public void writeIndentedLine(String line);
    }

}
