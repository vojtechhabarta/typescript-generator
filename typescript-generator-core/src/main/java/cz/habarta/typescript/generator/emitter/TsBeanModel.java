
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.*;
import cz.habarta.typescript.generator.parser.BeanModel;
import java.util.*;


public class TsBeanModel implements Comparable<TsBeanModel> {

    private final BeanModel javaModel;
    private final TsType name;
    private final TsType parent;
    private final List<String> comments;
    private final List<TsPropertyModel> properties = new ArrayList<>();

    public TsBeanModel(BeanModel javaModel, TsType name, TsType parent, List<String> comments) {
        this.javaModel = javaModel;
        this.name = name;
        this.parent = parent;
        this.comments = comments;
    }

    public BeanModel getJavaModel() {
        return javaModel;
    }

    public TsType getName() {
        return name;
    }

    public TsType getParent() {
        return parent;
    }

    public List<String> getComments() {
        return comments;
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
