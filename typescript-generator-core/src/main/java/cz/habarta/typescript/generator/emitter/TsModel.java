
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.*;
import java.util.*;


public class TsModel {

    private final List<TsBeanModel> beans = new ArrayList<>();
    private final List<TsEnumModel> enums = new ArrayList<>();
    private final LinkedHashSet<TsType.AliasType> typeAliases = new LinkedHashSet<>();

    public List<TsBeanModel> getBeans() {
        return beans;
    }

    public List<TsEnumModel> getEnums() {
        return enums;
    }

    public LinkedHashSet<TsType.AliasType> getTypeAliases() {
        return typeAliases;
    }

}
