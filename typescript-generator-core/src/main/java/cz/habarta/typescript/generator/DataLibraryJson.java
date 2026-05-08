
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonValue;
import cz.habarta.typescript.generator.util.Utils;
import java.util.List;
import org.jspecify.annotations.Nullable;


public class DataLibraryJson {

    public List<ClassMapping> classMappings;
    public List<TypeAlias> typeAliases;

    public DataLibraryJson(
        @Nullable List<ClassMapping> classMappings,
        @Nullable List<TypeAlias> typeAliases
    ) {
        this.classMappings = Utils.listFromNullable(classMappings);
        this.typeAliases = Utils.listFromNullable(typeAliases);
    }

    public static class ClassMapping {
        public String className;
        public SemanticType semanticType;
        public String customType;

        public ClassMapping(String className, SemanticType semanticType, String customType) {
            this.className = className;
            this.semanticType = semanticType;
            this.customType = customType;
        }
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

        public TypeAlias(String name, String definition) {
            this.name = name;
            this.definition = definition;
        }
    }

}
