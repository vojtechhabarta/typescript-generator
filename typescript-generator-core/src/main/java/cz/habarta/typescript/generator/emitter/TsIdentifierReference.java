
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Settings;


public class TsIdentifierReference extends TsExpression {

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
