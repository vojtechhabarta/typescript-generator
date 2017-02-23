
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.Symbol;
import cz.habarta.typescript.generator.util.Utils;
import java.util.*;


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

    public abstract String format(Settings settings);

    protected static List<String> format(List<TsType> types, Settings settings) {
        final List<String> formatted = new ArrayList<>();
        for (TsType type : types) {
            formatted.add(type.format(settings));
        }
        return formatted;
    }

    @Override
    public String toString() {
        return format(new Settings());
    }

    public static class BasicType extends TsType {

        public final String name;

        public BasicType(String name) {
            this.name = name;
        }

        @Override
        public String format(Settings settings) {
            return name;
        }
    }

    public static class VerbatimType extends TsType {

        public final String verbatimType;

        public VerbatimType(String verbatimType) {
            this.verbatimType = verbatimType;
        }

        @Override
        public String format(Settings settings) {
            return verbatimType;
        }
    }

    /**
     * Identifier which references some type, for example interface or type alias.
     */
    public static class ReferenceType extends TsType {

        public final Symbol symbol;

        public ReferenceType(Symbol symbol) {
            this.symbol = symbol;
        }

        @Override
        public String format(Settings settings) {
            return symbol.toString();
        }

    }

    public static class GenericReferenceType extends TsType.ReferenceType {

        public final List<TsType> typeArguments;

        public GenericReferenceType(Symbol symbol, TsType... typeArguments) {
            this(symbol, Arrays.asList(typeArguments));
        }

        public GenericReferenceType(Symbol symbol, List<TsType> typeArguments) {
            super(symbol);
            this.typeArguments = typeArguments;
        }

        @Override
        public String format(Settings settings) {
            return symbol + "<" + Utils.join(format(typeArguments, settings), ", ") + ">";
        }
    }
    
    public static class GenericVariableType extends TsType.BasicType {

        public GenericVariableType(String name) {
            super(name);
        }

    }

    public static class EnumReferenceType extends ReferenceType {
        public EnumReferenceType(Symbol symbol) {
            super(symbol);
        }
    }

    public static class BasicArrayType extends TsType {

        public final TsType elementType;

        public BasicArrayType(TsType elementType) {
            this.elementType = elementType;
        }

        @Override
        public String format(Settings settings) {
            // https://github.com/Microsoft/TypeScript/pull/914
            // TypeScript Specification A.1
            return elementType instanceof UnionType
                    ? "(" + elementType.format(settings) + ")" + "[]"
                    : elementType.format(settings) + "[]";
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
        public String format(Settings settings) {
            return "{ [index: " + indexType.format(settings) + "]: " + elementType.format(settings) + " }";
        }

    }

    public static class UnionType extends TsType {

        public final List<TsType> types;

        public UnionType(List<? extends TsType> types) {
            this.types = new ArrayList<TsType>(types);
        }

        @Override
        public String format(Settings settings) {
            return types.isEmpty()
                    ? "never"
                    : Utils.join(format(types, settings), " | ");
        }

    }

    public static class StringLiteralType extends TsType {

        public final String literal;

        public StringLiteralType(String literal) {
            this.literal = literal;
        }

        @Override
        public String format(Settings settings) {
            return settings.quotes + literal + settings.quotes;
        }

    }

    public static class OptionalType extends TsType {

        public final TsType type;

        public OptionalType(TsType type) {
            this.type = type;
        }

        @Override
        public String format(Settings settings) {
            return type.format(settings);
        }

    }

    public static class ObjectType extends TsType {

        public final List<TsProperty> properties;

        public ObjectType(TsProperty... properties) {
            this(Utils.removeNulls(Arrays.asList(properties)));
        }

        public ObjectType(List<TsProperty> properties) {
            this.properties = properties;
        }

        @Override
        public String format(Settings settings) {
            final List<String> props = new ArrayList<>();
            for (TsProperty property : properties) {
                props.add(property.format(settings));
            }
            if (props.isEmpty()) {
                return "{}";
            } else {
                return "{ " + Utils.join(props, " ") + " }";
            }
        }

    }

    public static TsType transformTsType(TsType tsType, Transformer transformer) {
        final TsType type = transformer.transform(tsType);
        if (type instanceof TsType.OptionalType) {
            final TsType.OptionalType optionalType = (TsType.OptionalType) type;
            return new TsType.OptionalType(transformTsType(optionalType.type, transformer));
        }
        if (type instanceof TsType.BasicArrayType) {
            final TsType.BasicArrayType basicArrayType = (TsType.BasicArrayType) type;
            return new TsType.BasicArrayType(transformTsType(basicArrayType.elementType, transformer));
        }
        if (type instanceof TsType.IndexedArrayType) {
            final TsType.IndexedArrayType indexedArrayType = (TsType.IndexedArrayType) type;
            return new TsType.IndexedArrayType(
                    transformTsType(indexedArrayType.indexType, transformer),
                    transformTsType(indexedArrayType.elementType, transformer));
        }
        if (type instanceof TsType.ObjectType) {
            final TsType.ObjectType objectType = (TsType.ObjectType) type;
            final List<TsProperty> properties = new ArrayList<>();
            for (TsProperty property : objectType.properties) {
                properties.add(new TsProperty(property.name, transformTsType(property.tsType, transformer)));
            }
            return new TsType.ObjectType(properties);
        }
        return type;
    }

    public static interface Transformer {
        public TsType transform(TsType tsType);
    }

}
