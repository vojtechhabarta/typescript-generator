
package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Type;
import kotlin.reflect.KType;


public class MethodParameterModel {

    private final String name;
    private final Type type;
    private final KType kType;
    public final boolean isRequired;

    public MethodParameterModel(String name, Type type, KType kType, boolean isRequired) {
        this.name = name;
        this.type = type;
        this.kType = kType;
        this.isRequired = isRequired;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public KType getkType() {
        return kType;
    }

    public boolean isNullable() {
        return !isRequired || (kType != null && kType.isMarkedNullable());
    }
}
