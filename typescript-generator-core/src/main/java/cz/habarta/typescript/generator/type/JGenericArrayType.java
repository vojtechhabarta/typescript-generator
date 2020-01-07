
package cz.habarta.typescript.generator.type;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Objects;


public class JGenericArrayType implements GenericArrayType {

    private final Type genericComponentType;

    public JGenericArrayType(Type genericComponentType) {
        this.genericComponentType = Objects.requireNonNull(genericComponentType, "genericComponentType");
    }

    public static JGenericArrayType of(Class<?> arrayClass) {
        Objects.requireNonNull(arrayClass);
        if (!arrayClass.isArray()) {
            throw new IllegalArgumentException("Class is not array: " + arrayClass);
        }
        return new JGenericArrayType(arrayClass.getComponentType());
    }

    @Override
    public Type getGenericComponentType() {
        return genericComponentType;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(genericComponentType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof GenericArrayType) {
            final GenericArrayType that = (GenericArrayType) obj;
            return Objects.equals(genericComponentType, that.getGenericComponentType());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return getGenericComponentType().getTypeName() + "[]";
    }

}
