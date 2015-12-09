
package cz.habarta.typescript.generator;

import java.util.*;


public abstract class TsType {

    public static final TsType Any = new BasicType("any");
    public static final TsType Boolean = new BasicType("boolean");
    public static final TsType Number = new BasicType("number");
    public static final TsType String = new BasicType("string");
    public static final TsType Date = new BasicType("Date");
    public static final TsType Void = new BasicType("void");

    public static final AliasType DateAsNumber = new AliasType("DateAsNumber", "type DateAsNumber = number;");
    public static final AliasType DateAsString = new AliasType("DateAsString", "type DateAsString = string;");

    public TsType.OptionalType optional() {
        return new TsType.OptionalType(this);
    }

    public static class BasicType extends TsType {

        public final String name;

        public BasicType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    public static class AliasType extends TsType {

        public final String name;
        public final String definition;

        public AliasType(String name, String definition) {
            this.name = name;
            this.definition = definition;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    public static class BasicArrayType extends TsType {

        public final TsType elementType;

        public BasicArrayType(TsType elementType) {
            this.elementType = elementType;
        }

        @Override
        public String toString() {
            return elementType + "[]";
        }

    }

    public static class IndexedArrayType extends TsType {

        public final TsType indexType;
        public final TsType elementType;

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

        public final String name;

        public StructuralType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    public static class EnumType extends TsType {

        public final String name;
        public final List<String> values;

        public EnumType(java.lang.String name, List<String> values) {
            this.name = name;
            this.values = values;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    public static class OptionalType extends TsType {

        public final TsType type;

        public OptionalType(TsType type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type.toString();
        }

    }

}
