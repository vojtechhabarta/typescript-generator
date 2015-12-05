
package cz.habarta.typescript.generator.emitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class TsBeanModel implements Comparable<TsBeanModel> {

    private final String name;
    private final String parent;
    private final List<TsPropertyModel> properties = new ArrayList<>();
    private final List<String> genericDeclarations;

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

    @Override
    public int compareTo(TsBeanModel o) {
        return name.compareTo(o.name);
    }

    public void sort() {
        Collections.sort(properties);
    }

    public List<String> getGenericDeclarations() {
        return genericDeclarations;
    }
}
