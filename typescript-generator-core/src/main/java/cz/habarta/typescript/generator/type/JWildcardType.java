
package cz.habarta.typescript.generator.type;


import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JWildcardType implements WildcardType {

    private final Type[] upperBounds;
    private final Type[] lowerBounds;

    public JWildcardType() {
        this(null, null);
    }

    public JWildcardType(Type[] upperBounds, Type[] lowerBounds) {
        this.upperBounds = upperBounds != null ? upperBounds : new Type[0];
        this.lowerBounds = lowerBounds != null ? lowerBounds : new Type[0];
    }

    @Override
    public Type[] getUpperBounds() {
        return upperBounds;
    }

    @Override
    public Type[] getLowerBounds() {
        return lowerBounds;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(lowerBounds) ^ Arrays.hashCode(upperBounds);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof WildcardType) {
            final WildcardType that = (WildcardType) obj;
            return Arrays.equals(lowerBounds, that.getLowerBounds()) &&
                    Arrays.equals(upperBounds, that.getUpperBounds());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        final String upper = upperBounds.length > 0 && !Objects.equals(upperBounds[0], Object.class)
                ? " extends " + Stream.of(upperBounds).map(Type::getTypeName).collect(Collectors.joining(" & "))
                : "";
        final String lower = lowerBounds.length > 0
                ? " extends " + Stream.of(lowerBounds).map(Type::getTypeName).collect(Collectors.joining(" & "))
                : "";
        return "?" + upper + lower;
    }

}
