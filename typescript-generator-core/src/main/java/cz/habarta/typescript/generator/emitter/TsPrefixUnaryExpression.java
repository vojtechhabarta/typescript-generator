
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Settings;


public class TsPrefixUnaryExpression extends TsExpression {

    private final TsUnaryOperator operator;
    private final TsExpression operand;

    public TsPrefixUnaryExpression(TsUnaryOperator operator, TsExpression operand) {
        this.operator = operator;
        this.operand = operand;
    }

    public TsUnaryOperator getOperator() {
        return operator;
    }

    public TsExpression getOperand() {
        return operand;
    }

    @Override
    public String format(Settings settings) {
        return operator.format(settings) + operand.format(settings);
    }
    
}
