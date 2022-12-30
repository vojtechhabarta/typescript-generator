
package cz.habarta.typescript.generator.emitter;

import java.util.List;


public class TsSwitchCaseClause extends TsStatement {

    private final List<TsExpression> expressions;
    private final List<TsStatement> statements;

    public TsSwitchCaseClause(List<TsExpression> expressions, List<TsStatement> statements) {
        this.expressions = expressions;
        this.statements = statements;
    }

    public List<TsExpression> getExpressions() {
        return expressions;
    }

    public List<TsStatement> getStatements() {
        return statements;
    }

}
