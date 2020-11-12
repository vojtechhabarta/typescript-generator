
package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;


public class PropertyModel {

    private final String name;
    private final Type type;
    private final boolean optional;
    private final PropertyAccess access;
    private final Member originalMember;
    private final PullProperties pullProperties;
    private final Object context;
    private final List<String> comments;

    public static class PullProperties {
        public final String prefix;
        public final String suffix;

        public PullProperties(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }
    }

    public PropertyModel(String name, Type type, boolean optional, PropertyAccess access, Member originalMember, PullProperties pullProperties, Object context, List<String> comments) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
        this.optional = optional;
        this.access = access;
        this.originalMember = originalMember;
        this.pullProperties = pullProperties;
        this.context = context;
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

    public PropertyAccess getAccess() {
        return access;
    }

    public Member getOriginalMember() {
        return originalMember;
    }

    public PropertyModel originalMember(Member originalMember) {
        return new PropertyModel(name, type, optional, access, originalMember, pullProperties, context, comments);
    }

    public PullProperties getPullProperties() {
        return pullProperties;
    }

    public Object getContext() {
        return context;
    }

    public List<String> getComments() {
        return comments;
    }

    public PropertyModel withComments(List<String> comments) {
        return new PropertyModel(name, type, optional, access, originalMember, pullProperties, context, comments);
    }

    public PropertyModel withType(Type type) {
        return new PropertyModel(name, type, optional, access, originalMember, pullProperties, context, comments);
    }

    public PropertyModel withOptional(boolean optional) {
        return new PropertyModel(name, type, optional, access, originalMember, pullProperties, context, comments);
    }

    @Override
    public String toString() {
        return "PropertyModel{" + "name=" + name + ", type=" + type + "}";
    }

}
