
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.compiler.EnumKind;
import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import cz.habarta.typescript.generator.compiler.Symbol;
import cz.habarta.typescript.generator.parser.EnumModel;
import java.util.List;


// T extends String | Number
public class TsEnumModel<T> extends TsDeclarationModel {
    
    private final EnumKind<T> kind;
    private final List<EnumMemberModel<T>> members;

    public TsEnumModel(Class<?> origin, Symbol name, EnumKind<T> kind, List<EnumMemberModel<T>> members, List<String> comments) {
        super(origin, null, name, comments);
        this.kind = kind;
        this.members = members;
    }

    public static <T> TsEnumModel<T> fromEnumModel(Symbol name, EnumModel<T> enumModel) {
        return new TsEnumModel<>(enumModel.getOrigin(), name, enumModel.getKind(), enumModel.getMembers(), enumModel.getComments());
    }

    public EnumKind<T> getKind() {
        return kind;
    }

    public List<EnumMemberModel<T>> getMembers() {
        return members;
    }

}
