
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.*;
import java.util.*;


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
        Collections.sort(beans);
    }
}
