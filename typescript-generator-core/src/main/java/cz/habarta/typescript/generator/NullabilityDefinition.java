
package cz.habarta.typescript.generator;

import java.util.Arrays;
import java.util.List;


public enum NullabilityDefinition {

    nullAndUndefinedUnion       (false, TsType.Null, TsType.Undefined),
    nullUnion                   (false, TsType.Null),
    undefinedUnion              (false, TsType.Undefined),
    nullAndUndefinedInlineUnion (true,  TsType.Null, TsType.Undefined),
    nullInlineUnion             (true,  TsType.Null),
    undefinedInlineUnion        (true,  TsType.Undefined);

    private final boolean isInline;
    private final List<TsType> types;

    private NullabilityDefinition(boolean isInline, TsType... types) {
        this.isInline = isInline;
        this.types = Arrays.asList(types);
    }

    public boolean isInline() {
        return isInline;
    }

    public List<TsType> getTypes() {
        return types;
    }

    public boolean containsUndefined() {
        return types.contains(TsType.Undefined);
    }

    public boolean containsNull() {
        return types.contains(TsType.Null);
    }

}
