
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Settings;
import java.util.List;


public class TsTaggedTemplateLiteral extends TsTemplateLiteral {

    private final TsExpression tagFunction;

    public TsTaggedTemplateLiteral(TsExpression tagFunction, List<TsExpression/*|TsStringLiteral*/> spans) {
        super(spans);
        this.tagFunction = tagFunction;
    }

    public TsExpression getTagFunction() {
        return tagFunction;
    }

    @Override
    public String format(Settings settings) {
        return tagFunction.format(settings) + super.format(settings);
    }

}
