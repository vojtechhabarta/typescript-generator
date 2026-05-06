
package cz.habarta.typescript.generator;

import org.jspecify.annotations.Nullable;


public class TsParameter {

    public final String name;
    public final @Nullable TsType tsType;

    public TsParameter(String name, @Nullable TsType tsType) {
        this.name = name;
        this.tsType = tsType;
    }

    public String getName() {
        return name;
    }

    public @Nullable TsType getTsType() {
        return tsType;
    }

}
