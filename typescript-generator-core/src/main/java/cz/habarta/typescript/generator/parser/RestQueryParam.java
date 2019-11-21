
package cz.habarta.typescript.generator.parser;


public abstract class RestQueryParam {

    public boolean required;

    RestQueryParam(boolean required) {
        this.required = required;
    }

    public static class Single extends RestQueryParam {
        private final MethodParameterModel queryParam;

        public Single(MethodParameterModel queryParam, boolean required) {
            super(required);
            this.queryParam = queryParam;
        }

        public MethodParameterModel getQueryParam() {
            return queryParam;
        }
    }

    public static class Bean extends RestQueryParam {
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

}
