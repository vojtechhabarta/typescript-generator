
package cz.habarta.typescript.generator.emitter;

import java.util.*;


public class TsModel {

    private final List<TsBeanModel> beans;
    private final List<TsEnumModel> enums;
    private final List<TsAliasModel> typeAliases;

    public TsModel() {
        this (new ArrayList<TsBeanModel>(), new ArrayList<TsEnumModel>(), new ArrayList<TsAliasModel>());
    }

    public TsModel(List<TsBeanModel> beans, List<TsEnumModel> enums, List<TsAliasModel> typeAliases) {
        if (beans == null) throw new NullPointerException();
        if (enums == null) throw new NullPointerException();
        if (typeAliases == null) throw new NullPointerException();
        this.beans = beans;
        this.enums = enums;
        this.typeAliases = typeAliases;
    }

    public List<TsBeanModel> getBeans() {
        return beans;
    }

    public TsModel setBeans(List<TsBeanModel> beans) {
        return new TsModel(beans, enums, typeAliases);
    }

    public List<TsEnumModel> getEnums() {
        return enums;
    }

    public TsModel setEnums(List<TsEnumModel> enums) {
        return new TsModel(beans, enums, typeAliases);
    }

    public List<TsAliasModel> getTypeAliases() {
        return typeAliases;
    }

    public TsModel setTypeAliases(List<TsAliasModel> typeAliases) {
        return new TsModel(beans, enums, typeAliases);
    }

}
