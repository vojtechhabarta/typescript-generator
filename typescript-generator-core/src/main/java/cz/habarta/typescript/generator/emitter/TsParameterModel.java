
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsParameter;
import cz.habarta.typescript.generator.TsType;


public class TsParameterModel extends TsParameter {

    private final TsAccessibilityModifier accessibilityModifier;

    public TsParameterModel(String name, TsType tsType) {
        this(null, name, tsType);
    }

    public TsParameterModel(TsAccessibilityModifier accessibilityModifier, String name, TsType tsType) {
        super(name, tsType);
        this.accessibilityModifier = accessibilityModifier;
    }

    public TsAccessibilityModifier getAccessibilityModifier() {
        return accessibilityModifier;
    }

}
