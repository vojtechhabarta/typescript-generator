
package cz.habarta.typescript.generator.parser;

import java.util.List;


public class BeanModel {

    private final Class<?> beanClass;
    private final Class<?> parent;
    private final List<PropertyModel> properties;

    public BeanModel(Class<?> beanClass, Class<?> parent, List<PropertyModel> properties) {
        this.beanClass = beanClass;
        this.parent = parent;
        this.properties = properties;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public Class<?> getParent() {
        return parent;
    }

    public List<PropertyModel> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "BeanModel{" + "beanClass=" + beanClass + ", properties=" + properties + '}';
    }

}
