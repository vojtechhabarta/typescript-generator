
package cz.habarta.typescript.generator.parser;

import java.util.List;
import java.util.Objects;


public class Model {

    private final List<BeanModel> beans;
    private final List<EnumModel> enums;
    private final List<RestApplicationModel> restApplications;

    public Model(List<BeanModel> beans, List<EnumModel> enums, List<RestApplicationModel> restApplications) {
        this.beans = Objects.requireNonNull(beans);
        this.enums = Objects.requireNonNull(enums);
        this.restApplications = restApplications;
    }

    public List<BeanModel> getBeans() {
        return beans;
    }

    public BeanModel getBean(Class<?> beanClass) {
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

    public List<RestApplicationModel> getRestApplications() {
        return restApplications;
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
