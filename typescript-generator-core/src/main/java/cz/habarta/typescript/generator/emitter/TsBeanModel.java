
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.*;
import java.util.*;


public class TsBeanModel extends TsDeclarationModel {

    private final TsType parent;
    private final List<TsPropertyModel> properties;

    public TsBeanModel(TsType name, TsType parent, List<TsPropertyModel> properties, List<String> comments) {
        super(name, comments);
        this.parent = parent;
        this.properties = properties;
    }

    public TsBeanModel(Class<?> origin, TsType name, TsType parent, List<TsPropertyModel> properties, List<String> comments) {
        super(origin, name, comments);
        this.parent = parent;
        this.properties = properties;
    }

    public TsType getParent() {
        return parent;
    }

    public List<TsPropertyModel> getProperties() {
        return properties;
    }

    public TsBeanModel setProperties(List<TsPropertyModel> properties) {
        return new TsBeanModel(origin, name, parent, properties, comments);
    }

}
