
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.compiler.EnumKind;
import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import java.util.List;
import org.jspecify.annotations.Nullable;


public class EnumModel extends DeclarationModel {

    private final EnumKind kind;
    private final List<EnumMemberModel> members;

    public EnumModel(Class<?> origin, EnumKind kind, List<EnumMemberModel> members, @Nullable List<String> comments) {
        super(origin, comments);
        this.kind = kind;
        this.members = members;
    }

    public EnumKind getKind() {
        return kind;
    }

    public List<EnumMemberModel> getMembers() {
        return members;
    }

    public EnumModel withMembers(List<EnumMemberModel> members) {
        return new EnumModel(origin, kind, members, comments);
    }

    @Override
    public EnumModel withComments(@Nullable List<String> comments) {
        return new EnumModel(origin, kind, members, comments);
    }

}
