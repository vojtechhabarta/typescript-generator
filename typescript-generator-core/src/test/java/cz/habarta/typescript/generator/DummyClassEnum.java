package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

public class DummyClassEnum {

    public static final DummyClassEnum ATYPE  = new DummyClassEnum("ATYPE");
    public static final DummyClassEnum BTYPE  = new DummyClassEnum("BTYPE");
    public static final DummyClassEnum CTYPE  = new DummyClassEnum("CTYPE");

    private final String value;

    @JsonCreator
    public DummyClassEnum(String value) {
        this.value = Objects.requireNonNull(value);
    }

    @JsonValue
    public String getValue() { return value; }

    @Override
    public String toString() { return value; }

}
