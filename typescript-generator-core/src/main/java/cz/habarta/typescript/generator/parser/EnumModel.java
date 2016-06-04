
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import cz.habarta.typescript.generator.compiler.EnumKind;
import java.util.*;


// T extends String | Number
public class EnumModel<T> extends DeclarationModel {

    private final EnumKind<T> kind;
    private final List<EnumMemberModel<T>> members;

    public EnumModel(Class<?> origin, EnumKind<T> kind, List<EnumMemberModel<T>> members, List<String> comments) {
        super (origin, comments);
        this.kind = kind;
        this.members = members;
    }

    public EnumKind<T> getKind() {
        return kind;
    }

    public List<EnumMemberModel<T>> getMembers() {
        return members;
    }

    public EnumModel<T> withMembers(List<EnumMemberModel<T>> members) {
        return new EnumModel<>(origin, kind, members, comments);
    }

    @Override
    public EnumModel<T> withComments(List<String> comments) {
        return new EnumModel<>(origin, kind, members, comments);
    }

}
