package cz.habarta.typescript.generator;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableRectangle.class)
@JsonDeserialize(as = ImmutableRectangle.class)
public abstract class Rectangle implements Shape {
    public abstract double width();
    public abstract double height();

    public static Rectangle.Builder builder() {
        return new Rectangle.Builder();
    }

    public static final class Builder extends ImmutableRectangle.Builder {}
}
