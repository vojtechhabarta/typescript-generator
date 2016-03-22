
package cz.habarta.typescript.generator.emitter;

import java.util.*;


public class TsModel {

    private final List<TsBeanModel> beans = new ArrayList<>();
    private final List<TsEnumModel> enums = new ArrayList<>();
    private final LinkedHashSet<TsAliasModel> typeAliases = new LinkedHashSet<>();

    public List<TsBeanModel> getBeans() {
        return beans;
    }

    public List<TsEnumModel> getEnums() {
        return enums;
    }

    public LinkedHashSet<TsAliasModel> getTypeAliases() {
        return typeAliases;
    }

}
