
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Settings;


public class TsSuperExpression extends TsExpression {

    @Override
    public String format(Settings settings) {
        return "super";
    }

}
