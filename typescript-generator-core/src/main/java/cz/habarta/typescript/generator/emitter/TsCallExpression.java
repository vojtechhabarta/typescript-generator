
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TsType;
import java.util.*;


public class TsCallExpression extends TsExpression {

    private final TsExpression expression;
    private final List<TsType> typeArguments;
    private final List<TsExpression> arguments;

    public TsCallExpression(TsExpression expression, TsExpression... arguments) {
        this(expression, null, Arrays.asList(arguments));
    }

    public TsCallExpression(TsExpression expression, List<TsType> typeArguments, List<TsExpression> arguments) {
        this.expression = expression;
        this.typeArguments = typeArguments != null ? typeArguments : Collections.<TsType>emptyList();
        this.arguments = arguments;
    }

    public TsExpression getExpression() {
        return expression;
    }

    public List<TsType> getTypeArguments() {
        return typeArguments;
    }

    public List<TsExpression> getArguments() {
        return arguments;
    }

    @Override
    public String format(Settings settings) {
        final String typeArgumentsString = typeArguments.isEmpty() ? "" : "<" + Emitter.formatList(settings, typeArguments) + ">";
        return expression.format(settings) + typeArgumentsString + "(" + Emitter.formatList(settings, arguments) + ")";
    }

}
