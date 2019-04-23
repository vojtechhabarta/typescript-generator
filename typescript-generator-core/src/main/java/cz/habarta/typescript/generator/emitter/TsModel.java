
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.compiler.EnumKind;
import cz.habarta.typescript.generator.compiler.Symbol;
import cz.habarta.typescript.generator.util.Utils;
import java.util.*;


public class TsModel {

    private final List<TsBeanModel> beans;
    private final List<TsEnumModel> enums;
    private final List<TsEnumModel> originalStringEnums;
    private final List<TsAliasModel> typeAliases;
    private final List<TsHelper> helpers;

    public TsModel() {
        this (new ArrayList<TsBeanModel>(), new ArrayList<TsEnumModel>(), new ArrayList<TsEnumModel>(), new ArrayList<TsAliasModel>(), new ArrayList<TsHelper>());
    }

    public TsModel(List<TsBeanModel> beans, List<TsEnumModel> enums, List<TsEnumModel> originalStringEnums, List<TsAliasModel> typeAliases, List<TsHelper> helpers) {
        if (beans == null) throw new NullPointerException();
        if (enums == null) throw new NullPointerException();
        if (typeAliases == null) throw new NullPointerException();
        this.beans = beans;
        this.enums = enums;
        this.originalStringEnums = originalStringEnums;
        this.typeAliases = typeAliases;
        this.helpers = helpers;
    }

    public List<TsBeanModel> getBeans() {
        return beans;
    }

    public TsBeanModel getBean(Class<?> origin) {
        if (origin != null) {
            for (TsBeanModel bean : beans) {
                if (Objects.equals(bean.getOrigin(), origin)) {
                    return bean;
                }
            }
        }
        return null;
    }

    public TsBeanModel getBean(Symbol name) {
        if (name != null) {
            for (TsBeanModel bean : beans) {
                if (Objects.equals(bean.getName(), name)) {
                    return bean;
                }
            }
        }
        return null;
    }

    public TsModel withBeans(List<TsBeanModel> beans) {
        return new TsModel(beans, enums, originalStringEnums, typeAliases, helpers);
    }

    public TsModel withoutBeans(List<TsBeanModel> beans) {
        return new TsModel(Utils.removeAll(this.beans, beans), enums, originalStringEnums, typeAliases, helpers);
    }

    public List<TsEnumModel> getEnums() {
        return enums;
    }

    @SuppressWarnings("unchecked")
    public List<TsEnumModel> getEnums(EnumKind enumKind) {
        final List<TsEnumModel> result = new ArrayList<>();
        for (TsEnumModel enumModel : enums) {
            if (enumModel.getKind() == enumKind) {
                result.add(enumModel);
            }
        }
        return result;
    }

    public TsModel withEnums(List<TsEnumModel> enums) {
        return new TsModel(beans, enums, originalStringEnums, typeAliases, helpers);
    }

    public TsModel withoutEnums(List<TsEnumModel> enums) {
        return new TsModel(beans, Utils.removeAll(this.enums, enums), originalStringEnums, typeAliases, helpers);
    }

    public List<TsEnumModel> getOriginalStringEnums() {
        return originalStringEnums;
    }

    public TsModel withOriginalStringEnums(List<TsEnumModel> originalStringEnums) {
        return new TsModel(beans, enums, originalStringEnums, typeAliases, helpers);
    }

    public List<TsAliasModel> getTypeAliases() {
        return typeAliases;
    }

    public TsAliasModel getTypeAlias(Class<?> origin) {
        if (origin != null) {
            for (TsAliasModel alias : typeAliases) {
                if (Objects.equals(alias.getOrigin(), origin)) {
                    return alias;
                }
            }
        }
        return null;
    }

    public TsModel withTypeAliases(List<TsAliasModel> typeAliases) {
        return new TsModel(beans, enums, originalStringEnums, typeAliases, helpers);
    }

    public TsModel withoutTypeAliases(List<TsAliasModel> typeAliases) {
        return new TsModel(beans, enums, originalStringEnums, Utils.removeAll(this.typeAliases, typeAliases), helpers);
    }

    public List<TsHelper> getHelpers() {
        return helpers;
    }

}
