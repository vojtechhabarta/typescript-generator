package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Type;

public class ReturnTypeModel {

    private final Type type;
    private final boolean optional;

    public ReturnTypeModel(Type type, boolean optional) {
        this.type = type;
        this.optional = optional;
    }

    public Type getType() {
        return type;
    }

    public boolean isOptional() {
        return optional;
    }
}
