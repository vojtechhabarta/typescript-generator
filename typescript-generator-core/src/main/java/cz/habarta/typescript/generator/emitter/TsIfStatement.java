
package cz.habarta.typescript.generator.emitter;

import java.util.*;


public class TsIfStatement extends TsStatement {

    private final TsExpression expression;
    private final List<TsStatement> thenStatements;
    private final List<TsStatement> elseStatements;

    public TsIfStatement(TsExpression expression, List<TsStatement> thenStatements) {
        this(expression, thenStatements, null);
    }

    public TsIfStatement(TsExpression expression, List<TsStatement> thenStatements, List<TsStatement> elseStatements) {
        Objects.requireNonNull(expression);
        Objects.requireNonNull(thenStatements);
        this.expression = expression;
        this.thenStatements = thenStatements;
        this.elseStatements = elseStatements;
    }

    public TsExpression getExpression() {
        return expression;
    }

    public List<TsStatement> getThenStatements() {
        return thenStatements;
    }

    public List<TsStatement> getElseStatements() {
        return elseStatements;
    }

}
