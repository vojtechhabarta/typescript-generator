package cz.habarta.typescript.generator;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableCircle.class)
@JsonDeserialize(as = ImmutableCircle.class)
public interface Circle extends Shape {
    double radius();

    final class Builder extends ImmutableCircle.Builder {}
}
