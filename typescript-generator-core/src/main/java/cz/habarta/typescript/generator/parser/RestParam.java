
package cz.habarta.typescript.generator.parser;


public abstract class RestParam {

    public boolean required;

    RestParam(boolean required) {
        this.required = required;
    }

    public static class Single extends RestParam {
        private final MethodParameterModel restParam;

        public Single(MethodParameterModel restParam, boolean required) {
            super(required);
            this.restParam = restParam;
        }

        public MethodParameterModel getRestParam() {
            return restParam;
        }
    }

    public static class Bean extends RestParam {
        private final BeanModel bean;

        // Only used in JAX-Rs, so optional
        public Bean(BeanModel bean) {
            super(false);
            this.bean = bean;
        }

        public BeanModel getBean() {
            return bean;
        }
    }

    public static class Map extends RestParam {
        public Map(boolean required) {
            super(required);
        }
    }

}
