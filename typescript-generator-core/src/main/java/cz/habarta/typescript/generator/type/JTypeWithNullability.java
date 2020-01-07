
package cz.habarta.typescript.generator.type;

import cz.habarta.typescript.generator.util.Utils;
import java.lang.reflect.Type;
import java.util.Objects;


public class JTypeWithNullability implements Type {

    private final Type type;
    private final boolean isNullable;

    public JTypeWithNullability(Type type, boolean isNullable) {
        this.type = Objects.requireNonNull(type, "type");
        this.isNullable = isNullable;
    }

    public Type getType() {
        return type;
    }

    public boolean isNullable() {
        return isNullable;
    }

    // shallow
    public static Type getPlainType(Type type) {
        if (type instanceof JTypeWithNullability) {
            final JTypeWithNullability typeWithNullability = (JTypeWithNullability) type;
            return typeWithNullability.getType();
        } else {
            return type;
        }
    }

    // deep
    public static Type removeNullability(Type type) {
        if (type instanceof JTypeWithNullability) {
            final JTypeWithNullability typeWithNullability = (JTypeWithNullability) type;
            return removeNullability(typeWithNullability.getType());
        }
        return Utils.transformContainedTypes(type, JTypeWithNullability::removeNullability);
    }

    @Override
    public String toString() {
        return type + (isNullable ? "?" : "!");
    }

}
