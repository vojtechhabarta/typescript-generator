
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Settings;


public class TsBinaryExpression extends TsExpression {
    
    private final TsExpression left;
    private final TsBinaryOperator operator;
    private final TsExpression right;

    public TsBinaryExpression(TsExpression left, TsBinaryOperator operator, TsExpression right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public TsExpression getLeft() {
        return left;
    }

    public TsBinaryOperator getOperator() {
        return operator;
    }

    public TsExpression getRight() {
        return right;
    }

    @Override
    public String format(Settings settings) {
        return left.format(settings) + " " + operator.format(settings) + " " + right.format(settings);
    }

}
