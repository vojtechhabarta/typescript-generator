
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;
import java.util.List;


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

    public TsPropertyModel setTsType(TsType type) {
        return new TsPropertyModel(name, type, comments);
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
