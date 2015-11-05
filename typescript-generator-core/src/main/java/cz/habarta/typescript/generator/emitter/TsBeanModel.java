
package cz.habarta.typescript.generator.emitter;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;


public class TsBeanModel {

    private final String name;
    private final String parent;
    private final List<TsPropertyModel> properties = new ArrayList<>();
    private final List<String> genericDeclarations;

    public TsBeanModel(String name, String parent) {
        this(name, parent, Lists.<String> newArrayList());
    }

    public TsBeanModel(String name, String parent, List<String> genericDeclarations) {
        this.name = name;
        this.parent = parent;
        this.genericDeclarations = genericDeclarations;
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

    public List<String> getGenericDeclarations() {
        return genericDeclarations;
    }

}
