
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Settings;


public class TsNumberLiteral extends TsExpression {

    private final Number literal;

    public TsNumberLiteral(Number literal) {
        this.literal = literal;
    }

    public Number getLiteral() {
        return literal;
    }

    @Override
    public String format(Settings settings) {
        return literal.toString();
    }

}
