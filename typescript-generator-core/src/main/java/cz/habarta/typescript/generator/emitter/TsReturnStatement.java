
package cz.habarta.typescript.generator.emitter;

import org.jspecify.annotations.Nullable;


public class TsReturnStatement extends TsStatement {

    private final @Nullable TsExpression expression;

    public TsReturnStatement() {
        this(null);
    }

    public TsReturnStatement(@Nullable TsExpression expression) {
        this.expression = expression;
    }

    public @Nullable TsExpression getExpression() {
        return expression;
    }

}
