
package cz.habarta.typescript.generator.emitter;

import java.util.ArrayList;
import java.util.List;


public class TsBeanModel {

    private final String name;
    private final String parent;
    private final List<TsPropertyModel> properties = new ArrayList<>();

    public TsBeanModel(String name, String parent) {
        this.name = name;
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public String getParent() {
        return parent;
    }

    public List<TsPropertyModel> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "TsBeanModel{" + "name=" + name + ", properties=" + properties + '}';
    }

}
