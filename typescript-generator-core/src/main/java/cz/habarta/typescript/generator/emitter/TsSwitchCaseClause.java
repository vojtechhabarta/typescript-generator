
package cz.habarta.typescript.generator.emitter;

import java.util.*;


public class TsSwitchCaseClause extends TsStatement {

    private final TsExpression expression;
    private final List<TsStatement> statements;

    public TsSwitchCaseClause(TsExpression expression, List<TsStatement> statements) {
        this.expression = expression;
        this.statements = statements;
    }

    public TsExpression getExpression() {
        return expression;
    }

    public List<TsStatement> getStatements() {
        return statements;
    }

}
