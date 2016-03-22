
package cz.habarta.typescript.generator;


/**
 * Represents TypeScript type.
 * That means something which can appear in type position (after ":" character).
 */
public abstract class TsType {

    public static final TsType Any = new BasicType("any");
    public static final TsType Boolean = new BasicType("boolean");
    public static final TsType Number = new BasicType("number");
    public static final TsType String = new BasicType("string");
    public static final TsType Date = new BasicType("Date");
    public static final TsType Void = new BasicType("void");

    @Override
    public boolean equals(Object rhs) {
        return rhs != null && this.getClass() == rhs.getClass() && this.toString().equals(rhs.toString());
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    public TsType.OptionalType optional() {
        return new TsType.OptionalType(this);
    }

    @Override
    public abstract String toString();

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

    /**
     * Identifier which references some type, for example interface or type alias.
     */
    public static class ReferenceType extends TsType {

        public final String name;

        public ReferenceType(String name) {
            this.name = name;
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
