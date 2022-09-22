
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Settings;


public class TsIdentifierReference extends TsExpression {

    public static final TsIdentifierReference Undefined = new TsIdentifierReference("undefined");
    public static final TsIdentifierReference Null = new TsIdentifierReference("null");

    private final String identifier;

    public TsIdentifierReference(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String format(Settings settings) {
        return identifier;
    }

}
