
package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Type;
import java.util.List;


public class BeanModel extends DeclarationModel {

    private final Type parent;
    private final List<Type> interfaces;
    private final List<PropertyModel> properties;

    public BeanModel(Class<?> origin, Type parent, List<Type> interfaces, List<PropertyModel> properties) {
        this(origin, parent, interfaces, properties, null);
    }

    public BeanModel(Class<?> origin, Type parent, List<Type> interfaces, List<PropertyModel> properties, List<String> comments) {
        super (origin, comments);
        this.parent = parent;
        this.interfaces = interfaces;
        this.properties = properties;
    }

    public Type getParent() {
        return parent;
    }

    public List<Type> getInterfaces() {
        return interfaces;
    }

    public List<PropertyModel> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "BeanModel{" + "origin=" + getOrigin() + ", properties=" + properties + '}';
    }

}
