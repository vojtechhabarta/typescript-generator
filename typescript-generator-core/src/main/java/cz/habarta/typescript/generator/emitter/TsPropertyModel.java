
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsProperty;
import cz.habarta.typescript.generator.TsType;
import java.util.List;


public class TsPropertyModel extends TsProperty implements Comparable<TsProperty> {

    public final TsModifierFlags modifiers;
    public final boolean ownProperty; // property exists directly on the bean, should not be inherited
    public final List<String> comments;

    public TsPropertyModel(String name, TsType tsType, TsModifierFlags modifiers, boolean ownProperty, List<String> comments) {
        super(name, tsType);
        this.modifiers = modifiers != null ? modifiers : TsModifierFlags.None;
        this.comments = comments;
        this.ownProperty = ownProperty;
    }

    public boolean isOwnProperty() {
        return ownProperty;
    }

    public List<String> getComments() {
        return comments;
    }

    public TsPropertyModel setTsType(TsType type) {
        return new TsPropertyModel(getName(), type, modifiers, ownProperty, getComments());
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
