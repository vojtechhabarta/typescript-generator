
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsParameter;
import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.util.Utils;
import java.util.List;
import org.jspecify.annotations.Nullable;


public class TsParameterModel extends TsParameter {

    protected final List<TsDecorator> decorators;
    private final @Nullable TsAccessibilityModifier accessibilityModifier;

    public TsParameterModel(String name, @Nullable TsType tsType) {
        this(null, name, tsType);
    }

    public TsParameterModel(@Nullable TsAccessibilityModifier accessibilityModifier, String name, @Nullable TsType tsType) {
        this(null, accessibilityModifier, name, tsType);
    }

    private TsParameterModel(@Nullable List<TsDecorator> decorators, @Nullable TsAccessibilityModifier accessibilityModifier, String name, @Nullable TsType tsType) {
        super(name, tsType);
        this.decorators = Utils.listFromNullable(decorators);
        this.accessibilityModifier = accessibilityModifier;
    }

    public List<TsDecorator> getDecorators() {
        return decorators;
    }

    public @Nullable TsAccessibilityModifier getAccessibilityModifier() {
        return accessibilityModifier;
    }

    public TsParameterModel withTsType(@Nullable TsType tsType) {
        return new TsParameterModel(decorators, accessibilityModifier, name, tsType);
    }

    public TsParameterModel withDecorators(List<TsDecorator> decorators) {
        return new TsParameterModel(decorators, accessibilityModifier, name, tsType);
    }

}
