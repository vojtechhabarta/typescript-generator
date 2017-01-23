
package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Type;


public class MethodParameterModel {

    private final String name;
    private final Type type;

    public MethodParameterModel(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

}
