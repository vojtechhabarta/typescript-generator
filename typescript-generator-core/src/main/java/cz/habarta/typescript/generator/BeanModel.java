
package cz.habarta.typescript.generator;

import java.util.List;


public class BeanModel {

    private final String name;
    private final List<PropertyModel> properties;

    public BeanModel(String name, List<PropertyModel> properties) {
        this.name = name;
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public List<PropertyModel> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "BeanModel{" + "name=" + name + ", properties=" + properties + '}';
    }

}
