
package cz.habarta.typescript.generator.emitter;


public class TsAliasModel implements Comparable<TsAliasModel> {
    
    public static final TsAliasModel DateAsNumber = new TsAliasModel("DateAsNumber", "type DateAsNumber = number;");
    public static final TsAliasModel DateAsString = new TsAliasModel("DateAsString", "type DateAsString = string;");

    public final String name;
    public final String definition;

    public TsAliasModel(String name, String definition) {
        this.name = name;
        this.definition = definition;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(TsAliasModel o) {
        return name.compareTo(o.name);
    }

}
