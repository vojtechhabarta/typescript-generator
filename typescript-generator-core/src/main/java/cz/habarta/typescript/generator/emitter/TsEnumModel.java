
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.parser.EnumModel;
import java.util.List;


public class TsEnumModel implements Comparable<TsEnumModel> {
    
    private final EnumModel enumModel;
    private final TsType name;
    private final List<String> comments;
    private final List<String> values;

    public TsEnumModel(EnumModel enumModel, TsType name, List<String> comments, List<String> values) {
        this.enumModel = enumModel;
        this.name = name;
        this.comments = comments;
        this.values = values;
    }

    public EnumModel getEnumModel() {
        return enumModel;
    }

    public TsType getName() {
        return name;
    }

    public List<String> getComments() {
        return comments;
    }

    public List<String> getValues() {
        return values;
    }

    @Override
    public int compareTo(TsEnumModel o) {
        return name.toString().compareTo(o.name.toString());
    }

}
