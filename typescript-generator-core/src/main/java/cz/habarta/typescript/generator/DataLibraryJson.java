
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;


public class DataLibraryJson {

    public List<ClassMapping> classMappings;
    public List<TypeAlias> typeAliases;

    public static class ClassMapping {
        public String className;
        public SemanticType semanticType;
        public String customType;
    }

    public enum SemanticType {
        String("string"),
        Number("number"),
        Boolean("boolean"),
        Date("date"),
        Any("any"),
        Void("void"),
        List("list"),
        Map("map"),
        Optional("optional"),
        Wrapper("wrapper"),
        ;

        private final String name;

        private SemanticType(String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }

    }

    public static class TypeAlias {
        public String name;
        public String definition;
    }

}
