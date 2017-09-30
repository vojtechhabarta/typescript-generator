
package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Type;
import java.util.*;


public class Model {

    private final List<BeanModel> beans;
    private final List<EnumModel> enums;
    private final JaxrsApplicationModel jaxrsApplication;

    public Model(List<BeanModel> beans, List<EnumModel> enums, JaxrsApplicationModel jaxrsApplication) {
        if (beans == null) throw new NullPointerException();
        if (enums == null) throw new NullPointerException();
        this.beans = beans;
        this.enums = enums;
        this.jaxrsApplication = jaxrsApplication;
    }

    public List<BeanModel> getBeans() {
        return beans;
    }

    public BeanModel getBean(Type beanClass) {
        for (BeanModel bean : beans) {
            if (bean.getOrigin().equals(beanClass)) {
                return bean;
            }
        }
        return null;
    }

    public List<EnumModel> getEnums() {
        return enums;
    }

    public JaxrsApplicationModel getJaxrsApplication() {
        return jaxrsApplication;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Model{");
        sb.append(String.format("%n"));
        for (BeanModel bean : beans) {
            sb.append("  ");
            sb.append(bean);
            sb.append(String.format("%n"));
        }
        for (EnumModel enumModel : enums) {
            sb.append("  ");
            sb.append(enumModel);
            sb.append(String.format("%n"));
        }
        sb.append('}');
        return sb.toString();
    }

}
