
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Settings;


public class TsBooleanLiteral extends TsExpression {

    private final boolean literal;

    public TsBooleanLiteral(boolean literal) {
        this.literal = literal;
    }

    public boolean getLiteral() {
        return literal;
    }

    @Override
    public String format(Settings settings) {
        return String.valueOf(literal);
    }

}
