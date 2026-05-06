
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.util.Utils;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;


public class BeanModel extends DeclarationModel {

    private final @Nullable Type parent;
    private final @Nullable List<Class<?>> taggedUnionClasses;
    private final @Nullable String discriminantProperty;
    private final @Nullable String discriminantLiteral;
    private final List<Type> interfaces;
    private final List<PropertyModel> properties;

    public BeanModel(
        Class<?> origin,
        @Nullable Type parent,
        @Nullable List<Class<?>> taggedUnionClasses,
        @Nullable String discriminantProperty,
        @Nullable String discriminantLiteral,
        @Nullable List<Type> interfaces,
        @Nullable List<PropertyModel> properties,
        @Nullable List<String> comments
    ) {
        super(origin, comments);
        this.parent = parent;
        this.taggedUnionClasses = taggedUnionClasses;
        this.discriminantProperty = discriminantProperty;
        this.discriminantLiteral = discriminantLiteral;
        this.interfaces = Utils.listFromNullable(interfaces);
        this.properties = Utils.listFromNullable(properties);
    }

    public @Nullable Type getParent() {
        return parent;
    }

    public @Nullable List<Class<?>> getTaggedUnionClasses() {
        return taggedUnionClasses;
    }

    public @Nullable String getDiscriminantProperty() {
        return discriminantProperty;
    }

    public @Nullable String getDiscriminantLiteral() {
        return discriminantLiteral;
    }

    public List<Type> getInterfaces() {
        return interfaces;
    }

    public List<Type> getParentAndInterfaces() {
        final List<Type> ancestors = new ArrayList<>();
        if (parent != null) {
            ancestors.add(parent);
        }
        ancestors.addAll(interfaces);
        return ancestors;
    }

    public List<PropertyModel> getProperties() {
        return properties;
    }

    public @Nullable PropertyModel getProperty(String name) {
        return properties.stream()
            .filter(property -> Objects.equals(property.getName(), name))
            .findFirst()
            .orElse(null);
    }

    public BeanModel withProperties(List<PropertyModel> properties) {
        return new BeanModel(origin, parent, taggedUnionClasses, discriminantProperty, discriminantLiteral, interfaces, properties, comments);
    }

    @Override
    public BeanModel withComments(@Nullable List<String> comments) {
        return new BeanModel(origin, parent, taggedUnionClasses, discriminantProperty, discriminantLiteral, interfaces, properties, comments);
    }

    @Override
    public String toString() {
        return "BeanModel{" + "origin=" + getOrigin() + ", properties=" + properties + '}';
    }

}
