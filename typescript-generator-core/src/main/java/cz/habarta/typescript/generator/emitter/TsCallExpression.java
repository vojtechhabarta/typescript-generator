
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


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
        final List<String> args = new ArrayList<>();
        for (TsExpression argument : arguments) {
            args.add(argument.format(settings));
        }
        return expression.format(settings) + "(" + Utils.join(args, ", ") + ")";
    }

}
