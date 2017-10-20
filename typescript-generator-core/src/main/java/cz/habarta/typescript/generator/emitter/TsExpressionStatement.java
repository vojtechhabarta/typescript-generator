
package cz.habarta.typescript.generator.emitter;

import java.util.*;


public class TsExpressionStatement extends TsStatement {

    private final TsExpression expression;

    public TsExpressionStatement(TsExpression expression) {
        Objects.requireNonNull(expression);
        this.expression = expression;
    }

    public TsExpression getExpression() {
        return expression;
    }

}
