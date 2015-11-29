package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType.EnumType;

public class TsEnumBeanModel extends TsBeanModel {

    private final EnumType type;

    public TsEnumBeanModel(String name, EnumType type) {
        super(name, null);
        this.type = type;
    }

    public EnumType getType() {
        return type;
    }
}
