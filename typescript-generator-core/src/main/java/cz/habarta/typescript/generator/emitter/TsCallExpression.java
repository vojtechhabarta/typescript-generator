
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Settings;
import java.util.*;


public class TsCallExpression extends TsExpression {

    private final TsExpression expression;
    private final List<TsExpression> arguments;

    public TsCallExpression(TsExpression expression, TsExpression... arguments) {
        this(expression, Arrays.asList(arguments));
    }

    public TsCallExpression(TsExpression expression, List<TsExpression> arguments) {
        this.expression = expression;
        this.arguments = arguments;
    }

    public TsExpression getExpression() {
        return expression;
    }

    public List<TsExpression> getArguments() {
        return arguments;
    }

    @Override
    public String format(Settings settings) {
        return expression.format(settings) + "(" + Emitter.formatList(settings, arguments) + ")";
    }

}
