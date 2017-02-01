
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;


public class TsParameterModel {

    private final String name;
    private final TsType tsType;

    public TsParameterModel(String name, TsType tsType) {
        this.name = name;
        this.tsType = tsType;
    }

    public String getName() {
        return name;
    }

    public TsType getTsType() {
        return tsType;
    }

}
