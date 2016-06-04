
package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.List;


public class PropertyModel {

    private final String name;
    private final Type type;
    private final boolean optional;
    private final Member originalMember;
    private final List<String> comments;

    public PropertyModel(String name, Type type, boolean optional, Member originalMember, List<String> comments) {
        this.name = name;
        this.type = type;
        this.optional = optional;
        this.originalMember = originalMember;
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

    public Member getOriginalMember() {
        return originalMember;
    }

    public PropertyModel originalMember(Member originalMember) {
        return new PropertyModel(name, type, optional, originalMember, comments);
    }

    public List<String> getComments() {
        return comments;
    }

    public PropertyModel withComments(List<String> comments) {
        return new PropertyModel(name, type, optional, originalMember, comments);
    }

    @Override
    public String toString() {
        return "PropertyModel{" + "name=" + name + ", type=" + type + "}";
    }

}
