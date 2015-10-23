
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;
import java.util.List;


public class TsPropertyModel {

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

}
