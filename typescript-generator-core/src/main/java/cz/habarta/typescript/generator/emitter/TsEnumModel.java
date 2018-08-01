
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.compiler.EnumKind;
import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import cz.habarta.typescript.generator.compiler.Symbol;
import cz.habarta.typescript.generator.parser.EnumModel;
import java.util.List;


public class TsEnumModel extends TsDeclarationModel {

    private final EnumKind kind;
    private final List<EnumMemberModel> members;
    private final boolean isNonConstEnum;

    @Deprecated
    public TsEnumModel(Class<?> origin, Symbol name, EnumKind kind, List<EnumMemberModel> members, List<String> comments) {
        this(origin, name, kind, members, comments, false);
    }

    public TsEnumModel(Class<?> origin, Symbol name, EnumKind kind, List<EnumMemberModel> members, List<String> comments, boolean isNonConstEnum) {
        super(origin, null, name, comments);
        this.kind = kind;
        this.members = members;
        this.isNonConstEnum = isNonConstEnum;
    }

    public static TsEnumModel fromEnumModel(Symbol name, EnumModel enumModel, boolean isNonConstEnum) {
        return new TsEnumModel(enumModel.getOrigin(), name, enumModel.getKind(), enumModel.getMembers(), enumModel.getComments(), isNonConstEnum);
    }

    public EnumKind getKind() {
        return kind;
    }

    public List<EnumMemberModel> getMembers() {
        return members;
    }

    public boolean isNonConstEnum() {
        return isNonConstEnum;
    }

    public TsEnumModel withMembers(List<EnumMemberModel> members) {
        return new TsEnumModel(origin, name, kind, members, comments, isNonConstEnum);
    }

}
