
package cz.habarta.typescript.generator.parser;

import java.util.List;


public class BeanModel {

    private final String name;
    private final String parent;
    private final List<PropertyModel> properties;

    public BeanModel(String name, String parent, List<PropertyModel> properties) {
        this.name = name;
        this.parent = parent;
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public String getParent() {
        return parent;
    }

    public List<PropertyModel> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "BeanModel{" + "name=" + name + ", properties=" + properties + '}';
    }

}
