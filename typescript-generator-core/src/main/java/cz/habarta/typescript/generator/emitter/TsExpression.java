
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Settings;


public abstract class TsExpression implements Emittable {

    @Override
    public abstract String format(Settings settings);

}
