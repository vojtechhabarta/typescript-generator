
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.util.Utils;
import java.util.List;
import org.jspecify.annotations.Nullable;


public class TsSwitchStatement extends TsStatement {

    private final TsExpression expression;
    private final List<TsSwitchCaseClause> caseClauses;
    private final @Nullable List<TsStatement> defaultClause;

    public TsSwitchStatement(TsExpression expression, List<TsSwitchCaseClause> caseClauses, @Nullable List<TsStatement> defaultClause) {
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

    public @Nullable List<TsStatement> getDefaultClause() {
        return defaultClause;
    }

}
