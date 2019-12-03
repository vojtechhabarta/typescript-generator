
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.Symbol;
import cz.habarta.typescript.generator.compiler.SymbolTable;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import kotlin.reflect.KType;


public interface TypeProcessor {

    /**
     * @return <code>null</code> if this processor didn't process passed java type
     */
    public Result processType(Type javaType, Context context);

    public Result processType(Type javaType, KType kType, Context context);

    public default Result processTypeInTemporaryContext(Type type, KType kType, Object typeContext, Settings settings) {
        return processType(type, kType, new Context(new SymbolTable(settings), this, typeContext));
    }

    public default List<Class<?>> discoverClassesUsedInType(Type type, KType kType, Object typeContext, Settings settings) {
        final TypeProcessor.Result result = processTypeInTemporaryContext(type, kType, typeContext, settings);
        return result != null ? result.getDiscoveredClasses() : Collections.emptyList();
    }

    public default boolean isTypeExcluded(Type type, KType kType, Object typeContext, Settings settings) {
        final TypeProcessor.Result result = processTypeInTemporaryContext(type, kType, typeContext, settings);
        return result != null && result.tsType == TsType.Any;
    }

    public static class Context {

        private final SymbolTable symbolTable;
        private final TypeProcessor typeProcessor;
        private final Object typeContext;

        public Context(SymbolTable symbolTable, TypeProcessor typeProcessor, Object typeContext) {
            this.symbolTable = Objects.requireNonNull(symbolTable, "symbolTable");
            this.typeProcessor = Objects.requireNonNull(typeProcessor, "typeProcessor");
            this.typeContext = typeContext;
        }

        public Symbol getSymbol(Class<?> cls) {
            return symbolTable.getSymbol(cls);
        }

        public Result processType(Type javaType) {
            return typeProcessor.processType(javaType, this);
        }

        public Result processType(Type javaType, KType kType) {
            return typeProcessor.processType(javaType, kType,this);
        }

        public Object getTypeContext() {
            return typeContext;
        }

        public Context withTypeContext(Object typeContext) {
            return new Context(symbolTable, typeProcessor, typeContext);
        }

    }

    public static class Result {

        private final TsType tsType;
        private final List<Class<?>> discoveredClasses;

        public Result(TsType tsType, KType kType, List<Class<?>> discoveredClasses) {
            this.tsType = boxIfNullable(kType, tsType);
            this.discoveredClasses = discoveredClasses;
        }

        public Result(TsType tsType, KType kType, Class<?>... discoveredClasses) {
            this.tsType = boxIfNullable(kType, tsType);
            this.discoveredClasses = Arrays.asList(discoveredClasses);
        }

        public TsType getTsType() {
            return tsType;
        }

        public List<Class<?>> getDiscoveredClasses() {
            return discoveredClasses;
        }

        protected TsType boxIfNullable(KType kType, TsType tsType) {
            if (kType == null) {
                return tsType;
            }

            if (kType.isMarkedNullable()) {
                return new TsType.OptionalType(tsType);
            }

            return tsType;
        }

    }

    public static class Chain implements TypeProcessor {

        private final List<TypeProcessor> processors;

        public Chain(List<TypeProcessor> processors) {
            this.processors = processors;
        }

        public Chain(TypeProcessor... processors) {
            this.processors = Arrays.asList(processors);
        }

        @Override
        public Result processType(Type javaType, Context context) {
            return processType(javaType, null, context);
        }

        @Override
        public Result processType(Type javaType, KType kType, Context context) {
            for (TypeProcessor processor : processors) {
                final Result result = processor.processType(javaType, kType, context);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }
    }

}
