
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.Symbol;
import cz.habarta.typescript.generator.emitter.Emittable;
import cz.habarta.typescript.generator.emitter.Emitter;
import cz.habarta.typescript.generator.util.Utils;
import java.util.*;


/**
 * Represents TypeScript type.
 * That means something which can appear in type position (after ":" character).
 */
public abstract class TsType implements Emittable {

    public static final TsType Any = new BasicType("any");
    public static final TsType Boolean = new BasicType("boolean");
    public static final TsType Number = new BasicType("number");
    public static final TsType String = new BasicType("string");
    public static final TsType Date = new BasicType("Date");
    public static final TsType Void = new BasicType("void");
    public static final TsType Undefined = new BasicType("undefined");
    public static final TsType Null = new BasicType("null");
    public static final TsType Never = new BasicType("never");
    public static final TsType Unknown = new BasicType("unknown");

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
    public abstract String format(Settings settings);

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

    public static class GenericBasicType extends TsType.BasicType {

        public final List<TsType> typeArguments;

        public GenericBasicType(String name, TsType... typeArguments) {
            this(name, Arrays.asList(typeArguments));
        }

        public GenericBasicType(String name, List<? extends TsType> typeArguments) {
            super(name);
            this.typeArguments = new ArrayList<>(typeArguments);
        }

        @Override
        public String format(Settings settings) {
            return name + "<" + Emitter.formatList(settings, typeArguments) + ">";
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
            return symbol.getFullName();
        }

    }

    public static class GenericReferenceType extends TsType.ReferenceType {

        public final List<TsType> typeArguments;

        public GenericReferenceType(Symbol symbol, TsType... typeArguments) {
            this(symbol, Arrays.asList(typeArguments));
        }

        public GenericReferenceType(Symbol symbol, List<? extends TsType> typeArguments) {
            super(symbol);
            this.typeArguments = new ArrayList<>(typeArguments);
        }

        @Override
        public String format(Settings settings) {
            return symbol.getFullName() + "<" + Emitter.formatList(settings, typeArguments) + ">";
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

        public UnionType(TsType... types) {
            this(Arrays.asList(types));
        }

        public UnionType(List<? extends TsType> types) {
            this.types = new ArrayList<>(new LinkedHashSet<>(types));
        }

        @Override
        public String format(Settings settings) {
            return types.isEmpty()
                    ? Never.format(settings)
                    : Emitter.formatList(settings, types, " | ");
        }

    }

    public static class IntersectionType extends TsType {

        public final List<TsType> types;

        public IntersectionType(TsType... types) {
            this(Arrays.asList(types));
        }

        public IntersectionType(List<? extends TsType> types) {
            this.types = new ArrayList<>(new LinkedHashSet<>(types));
        }

        @Override
        public String format(Settings settings) {
            return types.isEmpty()
                    ? Unknown.format(settings)
                    : Emitter.formatList(settings, types, " & ");
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
            this.properties = new ArrayList<>(properties);
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

    public static class FunctionType extends TsType {

        public final List<TsParameter> parameters;
        public final TsType type;

        public FunctionType(List<TsParameter> parameters, TsType type) {
            this.parameters = new ArrayList<>(parameters);
            this.type = type;
        }

        @Override
        public String format(Settings settings) {
            return Emitter.formatParameterList(parameters, false) + " => " + type.format(settings);
        }

    }

    public static TsType transformTsType(Context context, TsType tsType, Transformer transformer) {
        final TsType type = transformer.transform(context, tsType);
        if (type instanceof TsType.GenericBasicType) {
            final GenericBasicType genericBasicType = (TsType.GenericBasicType) type;
            final List<TsType> typeArguments = new ArrayList<>();
            for (TsType typeArgument : genericBasicType.typeArguments) {
                typeArguments.add(transformTsType(context, typeArgument, transformer));
            }
            return new TsType.GenericBasicType(genericBasicType.name, typeArguments);
        }
        if (type instanceof TsType.GenericReferenceType) {
            final GenericReferenceType genericReferenceType = (TsType.GenericReferenceType) type;
            final List<TsType> typeArguments = new ArrayList<>();
            for (TsType typeArgument : genericReferenceType.typeArguments) {
                typeArguments.add(transformTsType(context, typeArgument, transformer));
            }
            return new TsType.GenericReferenceType(genericReferenceType.symbol, typeArguments);
        }
        if (type instanceof TsType.OptionalType) {
            final TsType.OptionalType optionalType = (TsType.OptionalType) type;
            return new TsType.OptionalType(transformTsType(context, optionalType.type, transformer));
        }
        if (type instanceof TsType.BasicArrayType) {
            final TsType.BasicArrayType basicArrayType = (TsType.BasicArrayType) type;
            return new TsType.BasicArrayType(transformTsType(context, basicArrayType.elementType, transformer));
        }
        if (type instanceof TsType.IndexedArrayType) {
            final TsType.IndexedArrayType indexedArrayType = (TsType.IndexedArrayType) type;
            return new TsType.IndexedArrayType(
                    transformTsType(context, indexedArrayType.indexType, transformer),
                    transformTsType(context, indexedArrayType.elementType, transformer));
        }
        if (type instanceof TsType.UnionType) {
            final TsType.UnionType unionType = (TsType.UnionType) type;
            final List<TsType> types = new ArrayList<>();
            for (TsType constituentType : unionType.types) {
                types.add(transformTsType(context, constituentType, transformer));
            }
            return new TsType.UnionType(types);
        }
        if (type instanceof TsType.IntersectionType) {
            final TsType.IntersectionType intersectionType = (TsType.IntersectionType) type;
            final List<TsType> types = new ArrayList<>();
            for (TsType constituent : intersectionType.types) {
                types.add(transformTsType(context, constituent, transformer));
            }
            return new TsType.IntersectionType(types);
        }
        if (type instanceof TsType.ObjectType) {
            final TsType.ObjectType objectType = (TsType.ObjectType) type;
            final List<TsProperty> properties = new ArrayList<>();
            for (TsProperty property : objectType.properties) {
                properties.add(new TsProperty(property.name, transformTsType(context, property.tsType, transformer)));
            }
            return new TsType.ObjectType(properties);
        }
        if (type instanceof TsType.FunctionType) {
            final TsType.FunctionType functionType = (TsType.FunctionType) type;
            final List<TsParameter> parameters = new ArrayList<>();
            for (TsParameter parameter : functionType.parameters) {
                parameters.add(new TsParameter(parameter.name, transformTsType(context, parameter.tsType, transformer)));
            }
            return new TsType.FunctionType(parameters, transformTsType(context, functionType.type, transformer));
        }
        return type;
    }

    public static class Context {
        // currently not needed
    }

    public static interface Transformer {
        public TsType transform(Context context, TsType tsType);
    }

}
