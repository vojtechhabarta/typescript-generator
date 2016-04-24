
package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Type;
import java.util.List;


public class BeanModel {

    private final Class<?> beanClass;
    private final Type parent;
    private final List<Type> interfaces;
    private final List<PropertyModel> properties;
    private final List<String> comments;

    public BeanModel(Class<?> beanClass, Type parent, List<Type> interfaces, List<PropertyModel> properties) {
        this(beanClass, parent, interfaces, properties, null);
    }

    public BeanModel(Class<?> beanClass, Type parent, List<Type> interfaces, List<PropertyModel> properties, List<String> comments) {
        this.beanClass = beanClass;
        this.parent = parent;
        this.interfaces = interfaces;
        this.properties = properties;
        this.comments = comments;
    }

    public Class<?> getBeanClass() {
        return beanClass;
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

    public List<String> getComments() {
        return comments;
    }

    @Override
    public String toString() {
        return "BeanModel{" + "beanClass=" + beanClass + ", properties=" + properties + '}';
    }

}
