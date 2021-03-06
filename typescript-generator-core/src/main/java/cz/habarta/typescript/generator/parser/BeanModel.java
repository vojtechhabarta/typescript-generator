
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.util.Utils;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class BeanModel extends DeclarationModel {

    private final Type parent;
    private final List<Class<?>> taggedUnionClasses;
    private final String discriminantProperty;
    private final String discriminantLiteral;
    private final List<String> additionalDiscriminantLiterals;
    private final List<Type> interfaces;
    private final List<PropertyModel> properties;

    public BeanModel(Class<?> origin, Type parent,
            List<Class<?>> taggedUnionClasses, String discriminantProperty, String discriminantLiteral, List<String> additionalDiscriminantLiterals,
            List<Type> interfaces, List<PropertyModel> properties, List<String> comments) {
        super(origin, comments);
        this.parent = parent;
        this.taggedUnionClasses = taggedUnionClasses;
        this.discriminantProperty = discriminantProperty;
        this.discriminantLiteral = discriminantLiteral;
        this.additionalDiscriminantLiterals = additionalDiscriminantLiterals;
        this.interfaces = Utils.listFromNullable(interfaces);
        this.properties = properties;
    }

    public BeanModel(Class<?> origin, Type parent, List<Type> interfaces, List<PropertyModel> properties, List<String> comments) {
        this(origin, parent, null, null, null, null, interfaces, properties, comments);
    }

    public Type getParent() {
        return parent;
    }

    public List<Class<?>> getTaggedUnionClasses() {
        return taggedUnionClasses;
    }

    public String getDiscriminantProperty() {
        return discriminantProperty;
    }

    public String getDiscriminantLiteral() {
        return discriminantLiteral;
    }

    public List<String> getAdditionalDiscriminantLiterals() {
        return additionalDiscriminantLiterals;
    }

    public BeanModel withTaggedUnion(List<Class<?>> taggedUnionClasses, String discriminantProperty, String discriminantLiteral, List<String> additionalDiscriminantLiterals) {
        return new BeanModel(origin, parent, taggedUnionClasses, discriminantProperty, discriminantLiteral, additionalDiscriminantLiterals, interfaces, properties, comments);
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

    public PropertyModel getProperty(String name) {
        return properties.stream()
                .filter(property -> Objects.equals(property.getName(), name))
                .findFirst()
                .orElse(null);
    }

    public BeanModel withProperties(List<PropertyModel> properties) {
        return new BeanModel(origin, parent, taggedUnionClasses, discriminantProperty, discriminantLiteral, additionalDiscriminantLiterals, interfaces, properties, comments);
    }

    @Override
    public BeanModel withComments(List<String> comments) {
        return new BeanModel(origin, parent, taggedUnionClasses, discriminantProperty, discriminantLiteral, additionalDiscriminantLiterals, interfaces, properties, comments);
    }

    @Override
    public String toString() {
        return "BeanModel{" + "origin=" + getOrigin() + ", properties=" + properties + '}';
    }

}
