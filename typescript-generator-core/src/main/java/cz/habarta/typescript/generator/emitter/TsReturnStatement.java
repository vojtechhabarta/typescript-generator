
package cz.habarta.typescript.generator.emitter;


public class TsReturnStatement extends TsStatement {

    private final TsExpression expression;

    public TsReturnStatement() {
        this(null);
    }

    public TsReturnStatement(TsExpression expression) {
        this.expression = expression;
    }

    public TsExpression getExpression() {
        return expression;
    }

}
