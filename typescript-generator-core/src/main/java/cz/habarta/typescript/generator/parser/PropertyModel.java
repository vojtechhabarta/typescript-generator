
package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Type;
import java.util.List;


public class PropertyModel {

    private final String name;
    private final Type type;
    private final boolean optional;
    private final List<String> comments;

    public PropertyModel(String name, Type type, boolean optional, List<String> comments) {
        this.name = name;
        this.type = type;
        this.optional = optional;
        this.comments = comments;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public boolean isOptional() {
        return optional;
    }

    public List<String> getComments() {
        return comments;
    }

    @Override
    public String toString() {
        return "PropertyModel{" + "name=" + name + ", type=" + type + "}";
    }

}
