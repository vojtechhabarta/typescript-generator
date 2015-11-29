
package cz.habarta.typescript.generator;

import java.util.List;

import com.google.common.base.Joiner;

public abstract class TsType {

    public static final TsType Any = new BasicType("any");
    public static final TsType Boolean = new BasicType("boolean");
    public static final TsType Number = new BasicType("number");
    public static final TsType String = new BasicType("string");
    public static final TsType Date = new BasicType("Date");
    public static final TsType Void = new BasicType("void");

    public static final AliasType DateAsNumber = new AliasType("DateAsNumber", "type DateAsNumber = number;");
    public static final AliasType DateAsString = new AliasType("DateAsString", "type DateAsString = string;");

    protected boolean optional = false;

    public abstract TsType getOptionalReference();

    public boolean getOptional() {
        return optional;
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

        @Override
        public TsType getOptionalReference() {
            BasicType ret = new BasicType(name);
            ret.optional = true;
            return ret;
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

        @Override
        public TsType getOptionalReference() {
            AliasType ret = new AliasType(name, definition);
            ret.optional = true;
            return ret;
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

        @Override
        public TsType getOptionalReference() {
            BasicArrayType ret = new BasicArrayType(elementType);
            ret.optional = true;
            return ret;
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

        @Override
        public TsType getOptionalReference() {
            IndexedArrayType ret = new IndexedArrayType(indexType, elementType);
            ret.optional = true;
            return ret;
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

        @Override
        public TsType getOptionalReference() {
            StructuralType ret = new StructuralType(name);
            ret.optional = true;
            return ret;
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
            return "string";
        }

        @Override
        public TsType getOptionalReference() {
            EnumType ret = new EnumType(name, values);
            ret.optional = true;
            return ret;
        }

        public String getName() {
            return name;
        }
    }

    public static class GenericParamType extends TsType {

        public final String name;

        public GenericParamType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public TsType getOptionalReference() {
            GenericParamType ret = new GenericParamType(name);
            ret.optional = false;
            return ret;
        }
    }
    public static class GenericInstanceType extends TsType {

        public final TsType base;
        public final List<TsType> childGenericInstances;

        public GenericInstanceType(TsType base, List<TsType> childGenericInstances) {
            this.base = base;
            this.childGenericInstances = childGenericInstances;
        }

        @Override
        public String toString() {
            String genericString = "<" + Joiner.on(", ").join(childGenericInstances) + ">";
            return base + genericString;
        }

        @Override
        public TsType getOptionalReference() {
            TsType ret = new GenericInstanceType(base, childGenericInstances);
            ret.optional = true;
            return ret;
        }

    }
}
