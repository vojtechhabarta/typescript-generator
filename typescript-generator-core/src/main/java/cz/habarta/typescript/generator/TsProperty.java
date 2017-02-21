
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.emitter.Emitter;


public class TsProperty {

    public final String name;
    public final TsType tsType;

    public TsProperty(String name, TsType tsType) {
        this.name = name;
        this.tsType = tsType;
    }

    public String getName() {
        return name;
    }

    public TsType getTsType() {
        return tsType;
    }

    public String format(Settings settings) {
        final String questionMark = (tsType instanceof TsType.OptionalType) ? "?" : "";
        return Emitter.quoteIfNeeded(name, settings) + questionMark + ": " + tsType.format(settings) + ";";
    }

}
