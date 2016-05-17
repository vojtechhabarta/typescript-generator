
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;
import java.util.List;


public class TsEnumModel extends TsDeclarationModel {

    private final List<String> values;
    private final TsBeanModel valueModel;

    public TsEnumModel(TsType name, List<String> comments, List<String> values, TsBeanModel valueModel) {
        super(name, comments);
        this.values = values;
        this.valueModel = valueModel;
    }

    public TsEnumModel(Class<?> origin, TsType name, List<String> comments, List<String> values, TsBeanModel beanModel) {
        super(origin, name, comments);
        this.values = values;
        this.valueModel = beanModel;
    }

    public List<String> getValues() {
        return values;
    }

    public TsBeanModel getValueModel() {
        return valueModel;
    }

}
