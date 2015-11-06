
package cz.habarta.typescript.generator.emitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import cz.habarta.typescript.generator.TsType;


public class TsModel {

    private final List<TsBeanModel> beans = new ArrayList<>();
    private final LinkedHashSet<TsType.AliasType> typeAliases = new LinkedHashSet<>();

    public List<TsBeanModel> getBeans() {
        return beans;
    }

    public LinkedHashSet<TsType.AliasType> getTypeAliases() {
        return typeAliases;
    }

    public void sort() {
        for (TsBeanModel beanModel : beans) {
            beanModel.sort();
        }
        Collections.sort(beans);
    }
}
