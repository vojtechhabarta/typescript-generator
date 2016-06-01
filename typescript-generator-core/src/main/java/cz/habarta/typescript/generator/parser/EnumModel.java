
package cz.habarta.typescript.generator.parser;

import java.util.*;


public class EnumModel extends DeclarationModel {
    
    private final List<String> values;

    public EnumModel(Class<?> origin, List<String> values, List<String> comments) {
        super (origin, comments);
        this.values = values;
    }

    public List<String> getValues() {
        return values;
    }

}
