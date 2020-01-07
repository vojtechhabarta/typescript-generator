
package cz.habarta.typescript.generator.type;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class JParameterizedType implements ParameterizedType {

    private final Type rawType;
    private final Type[] actualTypeArguments;
    private final Type ownerType;

    public JParameterizedType(Type rawType, Type[] actualTypeArguments, Type ownerType) {
        this.rawType = Objects.requireNonNull(rawType, "rawType");
        this.actualTypeArguments = actualTypeArguments != null ? actualTypeArguments : new Type[0];
        this.ownerType = ownerType;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return actualTypeArguments;
    }

    @Override
    public Type getRawType() {
        return rawType;
    }

    @Override
    public Type getOwnerType() {
        return ownerType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerType, rawType, actualTypeArguments);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ParameterizedType) {
            final ParameterizedType that = (ParameterizedType) obj;
            return Objects.equals(ownerType, that.getOwnerType()) &&
                    Objects.equals(rawType, that.getRawType()) &&
                    Arrays.equals(actualTypeArguments, that.getActualTypeArguments());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return (rawType instanceof Class ? ((Class<?>)rawType).getName() : rawType.getTypeName())
                + "<"
                + Stream.of(actualTypeArguments)
                .map(type -> type.getTypeName())
                .collect(Collectors.joining(", "))
                + ">";
    }

}
