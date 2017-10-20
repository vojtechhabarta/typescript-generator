
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TsType;
import java.util.*;


public class TsNewExpression extends TsExpression {

    private final TsExpression expression;
    private final List<TsType> typeArguments;
    private final List<TsExpression> arguments;

    public TsNewExpression(TsExpression expression, List<? extends TsExpression> arguments) {
        this(expression, null, arguments);
    }

    public TsNewExpression(TsExpression expression, List<? extends TsType> typeArguments, List<? extends TsExpression> arguments) {
        this.expression = expression;
        this.typeArguments = typeArguments != null ? new ArrayList<TsType>(typeArguments) : Collections.<TsType>emptyList();
        this.arguments = arguments != null ? new ArrayList<TsExpression>(arguments) : Collections.<TsExpression>emptyList();
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
        return "new "
                + expression.format(settings)
                + (typeArguments.isEmpty() ? "" : "<" + Emitter.formatList(settings, typeArguments) + ">")
                + "(" + Emitter.formatList(settings, arguments)
                + ")";
    }

}
