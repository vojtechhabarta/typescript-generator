
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.util.Utils;
import java.util.*;


public class TsSwitchStatement extends TsStatement {

    private final TsExpression expression;
    private final List<TsSwitchCaseClause> caseClauses;
    private final List<TsStatement> defaultClause;

    public TsSwitchStatement(TsExpression expression, List<TsSwitchCaseClause> caseClauses, List<TsStatement> defaultClause) {
        this.expression = expression;
        this.caseClauses = Utils.listFromNullable(caseClauses);
        this.defaultClause = defaultClause;
    }

    public TsExpression getExpression() {
        return expression;
    }

    public List<TsSwitchCaseClause> getCaseClauses() {
        return caseClauses;
    }

    public List<TsStatement> getDefaultClause() {
        return defaultClause;
    }

}
