
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Settings;
import java.util.List;


public class TsTemplateLiteral extends TsExpression {

    private final List<TsExpression/*|TsStringLiteral*/> spans;

    public TsTemplateLiteral(List<TsExpression> spans) {
        this.spans = spans;
    }

    public List<TsExpression> getSpans() {
        return spans;
    }

    @Override
    public String format(Settings settings) {
        final StringBuilder sb = new StringBuilder();
        sb.append("`");
        for (TsExpression span : spans) {
            if (span instanceof TsStringLiteral) {
                final TsStringLiteral literal = (TsStringLiteral) span;
                sb.append(literal.getLiteral());
            } else {
                sb.append("${");
                sb.append(span.format(settings));
                sb.append("}");
            }
        }
        sb.append("`");
        return sb.toString();
    }

}
