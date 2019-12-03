package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Type;
import kotlin.reflect.KType;

public class ReturnTypeModel {

    private final Type type;
    private final KType ktype;

    public ReturnTypeModel(Type type) {
        this.type = type;
        this.ktype = null;
    }

    public ReturnTypeModel(Type type, KType ktype) {
        this.ktype = ktype;
        this.type = type;
    }

    public KType getKType() {
        return ktype;
    }

    public Type getType() {
        return type;
    }
}
