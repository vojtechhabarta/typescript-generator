
package cz.habarta.typescript.generator.emitter;

import java.util.List;

import cz.habarta.typescript.generator.TsType;


public class TsPropertyModel implements Comparable<TsPropertyModel> {

    private final String name;
    private final TsType tsType;
    private final List<String> comments;

    public TsPropertyModel(String name, TsType tsType, List<String> comments) {
        this.name = name;
        this.tsType = tsType;
        this.comments = comments;
    }

    public String getName() {
        return name;
    }

    public TsType getTsType() {
        return tsType;
    }

    public List<String> getComments() {
        return comments;
    }

    @Override
    public String toString() {
        return "TsPropertyModel{" + "name=" + name + ", tsType=" + tsType + '}';
    }

    @Override
    public int compareTo(TsPropertyModel o) {
        return name.compareTo(o.getName());
    }

}
