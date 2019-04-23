
package cz.habarta.typescript.generator.parser;


public abstract class RestQueryParam {

    public static class Single extends RestQueryParam {
        private final MethodParameterModel queryParam;

        public Single(MethodParameterModel queryParam) {
            this.queryParam = queryParam;
        }

        public MethodParameterModel getQueryParam() {
            return queryParam;
        }
    }

    public static class Bean extends RestQueryParam {
        private final BeanModel bean;

        public Bean(BeanModel bean) {
            this.bean = bean;
        }

        public BeanModel getBean() {
            return bean;
        }
    }

}
