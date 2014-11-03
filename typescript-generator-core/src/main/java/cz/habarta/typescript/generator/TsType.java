
package cz.habarta.typescript.generator;

import java.util.LinkedHashSet;
import java.util.List;


public class TsType {

    public static final TsType Any = new BasicType("any");
    public static final TsType Boolean = new BasicType("boolean");
    public static final TsType Number = new BasicType("number");
    public static final TsType String = new BasicType("string");
    public static final TsType Date = new BasicType("Date");

    public static class BasicType extends TsType {

        private final String name;

        public BasicType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    public static class BasicArrayType extends TsType {

        private final TsType elementType;

        public BasicArrayType(TsType elementType) {
            this.elementType = elementType;
        }

        @Override
        public String toString() {
            return elementType + "[]";
        }

    }

    public static class IndexedArrayType extends TsType {

        private final TsType indexType;
        private final TsType elementType;

        public IndexedArrayType(TsType indexType, TsType elementType) {
            this.indexType = indexType;
            this.elementType = elementType;
        }

        @Override
        public String toString() {
            return "{ [index: " + indexType + "]: " + elementType + " }";
        }

    }

    public static class StructuralType extends TsType {

        private final String name;

        public StructuralType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    public static class EnumType extends TsType {

        private final String name;
        private final List<String> values;

        public EnumType(java.lang.String name, List<java.lang.String> values) {
            this.name = name;
            this.values = values;
        }

        public List<java.lang.String> getValues() {
            return values;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    public static TsType replaceEnumsWithStrings(TsType type, LinkedHashSet<EnumType> replacedEnums) {
        if (type instanceof EnumType) {
            final EnumType enumType = (EnumType) type;
            replacedEnums.add(enumType);
            return TsType.String;
        }
        if (type instanceof BasicArrayType) {
            final BasicArrayType basicArrayType = (BasicArrayType) type;
            return new BasicArrayType(replaceEnumsWithStrings(basicArrayType.elementType, replacedEnums));
        }
        if (type instanceof IndexedArrayType) {
            final IndexedArrayType indexedArrayType = (IndexedArrayType) type;
            return new IndexedArrayType(replaceEnumsWithStrings(indexedArrayType.indexType, replacedEnums),
                    replaceEnumsWithStrings(indexedArrayType.elementType, replacedEnums));
        }
        return type;
    }

}
