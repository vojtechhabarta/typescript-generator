
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.compiler.EnumKind;
import java.util.*;


public class TsModel {

    private final List<TsBeanModel> beans;
    private final List<TsEnumModel<?>> enums;
    private final List<TsAliasModel> typeAliases;

    public TsModel() {
        this (new ArrayList<TsBeanModel>(), new ArrayList<TsEnumModel<?>>(), new ArrayList<TsAliasModel>());
    }

    public TsModel(List<TsBeanModel> beans, List<TsEnumModel<?>> enums, List<TsAliasModel> typeAliases) {
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

    public List<TsEnumModel<?>> getEnums() {
        return enums;
    }

    @SuppressWarnings("unchecked")
    public <T> List<TsEnumModel<T>> getEnums(EnumKind<T> enumKind) {
        final List<TsEnumModel<T>> result = new ArrayList<>();
        for (TsEnumModel<?> enumModel : enums) {
            if (enumModel.getKind() == enumKind) {
                result.add((TsEnumModel<T>) enumModel);
            }
        }
        return result;
    }

    public TsModel setEnums(List<TsEnumModel<?>> enums) {
        return new TsModel(beans, enums, typeAliases);
    }

    public List<TsAliasModel> getTypeAliases() {
        return typeAliases;
    }

    public TsModel setTypeAliases(List<TsAliasModel> typeAliases) {
        return new TsModel(beans, enums, typeAliases);
    }

}
