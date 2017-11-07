
package cz.habarta.typescript.generator;


public class TsParameter {

    public final String name;
    public final TsType tsType;

    public TsParameter(String name, TsType tsType) {
        this.name = name;
        this.tsType = tsType;
    }

    public String getName() {
        return name;
    }

    public TsType getTsType() {
        return tsType;
    }

}
