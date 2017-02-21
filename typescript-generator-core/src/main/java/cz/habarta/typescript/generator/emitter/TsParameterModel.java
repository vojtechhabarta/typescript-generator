
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;


public class TsParameterModel {

    private final TsAccessibilityModifier accessibilityModifier;
    private final String name;
    private final TsType tsType;

    public TsParameterModel(String name, TsType tsType) {
        this(null, name, tsType);
    }

    public TsParameterModel(TsAccessibilityModifier accessibilityModifier, String name, TsType tsType) {
        this.accessibilityModifier = accessibilityModifier;
        this.name = name;
        this.tsType = tsType;
    }

    public TsAccessibilityModifier getAccessibilityModifier() {
        return accessibilityModifier;
    }

    public String getName() {
        return name;
    }

    public TsType getTsType() {
        return tsType;
    }

}
