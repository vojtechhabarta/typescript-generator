
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.*;
import java.util.*;


public class TsBeanModel implements Comparable<TsBeanModel> {

    private final TsType name;
    private final TsType parent;
    private final List<TsPropertyModel> properties = new ArrayList<>();

    public TsBeanModel(TsType name, TsType parent) {
        this.name = name;
        this.parent = parent;
    }

    public TsType getName() {
        return name;
    }

    public TsType getParent() {
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
        return name.toString().compareTo(o.name.toString());
    }

}
