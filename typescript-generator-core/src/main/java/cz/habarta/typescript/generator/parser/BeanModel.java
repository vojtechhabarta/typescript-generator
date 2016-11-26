
package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Type;
import java.util.*;


public class BeanModel extends DeclarationModel {

    private final Type parent;
    private final List<Class<?>> taggedUnionClasses;
    private final String discriminantProperty;
    private final String discriminantLiteral;
    private final List<Type> interfaces;
    private final List<PropertyModel> properties;

    public BeanModel(Class<?> origin, Type parent, List<Class<?>> taggedUnionClasses, String discriminantProperty, String discriminantLiteral, List<Type> interfaces, List<PropertyModel> properties, List<String> comments) {
        super(origin, comments);
        this.parent = parent;
        this.taggedUnionClasses = taggedUnionClasses;
        this.discriminantProperty = discriminantProperty;
        this.discriminantLiteral = discriminantLiteral;
        this.interfaces = interfaces;
        this.properties = properties;
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

    public BeanModel withProperties(List<PropertyModel> properties) {
        return new BeanModel(origin, parent, taggedUnionClasses, discriminantProperty, discriminantLiteral, interfaces, properties, comments);
    }

    @Override
    public BeanModel withComments(List<String> comments) {
        return new BeanModel(origin, parent, taggedUnionClasses, discriminantProperty, discriminantLiteral, interfaces, properties, comments);
    }

    @Override
    public String toString() {
        return "BeanModel{" + "origin=" + getOrigin() + ", properties=" + properties + '}';
    }

}
