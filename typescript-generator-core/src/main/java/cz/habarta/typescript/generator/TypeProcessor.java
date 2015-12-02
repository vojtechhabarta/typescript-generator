
package cz.habarta.typescript.generator;

import java.lang.reflect.Type;
import java.util.*;


public interface TypeProcessor {

    /**
     * @return <code>null</code> if this processor didn't process passed java type
     */
    public Result processType(Type javaType, Context context);


    public static interface Context {
        public String getMappedName(Class<?> cls);
        public Result processType(Type javaType);
    }

    public static class Result {

        private final TsType tsType;
        private final List<Class<?>> discoveredClasses;

        public Result(TsType tsType, List<Class<?>> discoveredClasses) {
            this.tsType = tsType;
            this.discoveredClasses = discoveredClasses;
        }

        public Result(TsType tsType, Class<?>... discoveredClasses) {
            this.tsType = tsType;
            this.discoveredClasses = Arrays.asList(discoveredClasses);
        }

        public TsType getTsType() {
            return tsType;
        }

        public List<Class<?>> getDiscoveredClasses() {
            return discoveredClasses;
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
            for (TypeProcessor processor : processors) {
                final Result result = processor.processType(javaType, context);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }

    }

}
