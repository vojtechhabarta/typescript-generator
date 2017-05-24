
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.compiler.EnumKind;
import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import cz.habarta.typescript.generator.compiler.Symbol;
import cz.habarta.typescript.generator.parser.EnumModel;
import java.util.List;


public class TsEnumModel extends TsDeclarationModel {

    private final EnumKind kind;
    private final List<EnumMemberModel> members;

    public TsEnumModel(Class<?> origin, Symbol name, EnumKind kind, List<EnumMemberModel> members, List<String> comments) {
        super(origin, null, name, comments);
        this.kind = kind;
        this.members = members;
    }

    public static TsEnumModel fromEnumModel(Symbol name, EnumModel enumModel) {
        return new TsEnumModel(enumModel.getOrigin(), name, enumModel.getKind(), enumModel.getMembers(), enumModel.getComments());
    }

    public EnumKind getKind() {
        return kind;
    }

    public List<EnumMemberModel> getMembers() {
        return members;
    }

    public TsEnumModel withMembers(List<EnumMemberModel> members) {
        return new TsEnumModel(origin, name, kind, members, comments);
    }

}
