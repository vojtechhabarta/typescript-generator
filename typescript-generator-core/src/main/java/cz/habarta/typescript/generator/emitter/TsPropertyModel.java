
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsProperty;
import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.util.Utils;
import java.util.List;


public class TsPropertyModel extends TsProperty implements Comparable<TsProperty> {

    public final List<TsDecorator> decorators;
    public final TsModifierFlags modifiers;
    public final boolean ownProperty; // property exists directly on the bean, should not be inherited
    public final TsExpression defaultValue;
    public final List<String> comments;

    public TsPropertyModel(String name, TsType tsType, TsModifierFlags modifiers, boolean ownProperty, List<String> comments) {
        this(name, tsType, /*decorators*/ null, modifiers, ownProperty, /*defaultValue*/ null, comments);
    }

    public TsPropertyModel(String name, TsType tsType, List<TsDecorator> decorators, TsModifierFlags modifiers, boolean ownProperty, TsExpression defaultValue, List<String> comments) {
        super(name, tsType);
        this.decorators = Utils.listFromNullable(decorators);
        this.modifiers = modifiers != null ? modifiers : TsModifierFlags.None;
        this.comments = comments;
        this.defaultValue = defaultValue;
        this.ownProperty = ownProperty;
    }

    public List<TsDecorator> getDecorators() {
        return decorators;
    }

    public TsPropertyModel withDecorators(List<TsDecorator> decorators) {
        return new TsPropertyModel(getName(), tsType, decorators, modifiers, ownProperty, defaultValue, getComments());
    }

    public TsModifierFlags getModifiers() {
        return modifiers;
    }

    public boolean isOwnProperty() {
        return ownProperty;
    }

    public TsExpression getDefaultValue() {
        return defaultValue;
    }

    public TsPropertyModel withDefaultValue(TsExpression defaultValue) {
        return new TsPropertyModel(name, tsType, decorators, modifiers, ownProperty, defaultValue, comments);
    }

    public List<String> getComments() {
        return comments;
    }

    public TsPropertyModel withTsType(TsType tsType) {
        return new TsPropertyModel(name, tsType, decorators, modifiers, ownProperty, defaultValue, comments);
    }

    @Override
    public int compareTo(TsProperty o) {
        return name.compareTo(o.getName());
    }

    @Override
    public String toString() {
        return "TsPropertyModel{" + "name=" + name + ", tsType=" + tsType + '}';
    }

}
