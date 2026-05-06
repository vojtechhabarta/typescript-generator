
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.util.Utils;
import java.util.List;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;


public class Model {

    private final List<BeanModel> beans;
    private final List<EnumModel> enums;
    private final List<RestApplicationModel> restApplications;

    public Model(List<BeanModel> beans, List<EnumModel> enums, @Nullable List<RestApplicationModel> restApplications) {
        this.beans = requireNonNull(beans);
        this.enums = requireNonNull(enums);
        this.restApplications = Utils.listFromNullable(restApplications);
    }

    public List<BeanModel> getBeans() {
        return beans;
    }

    public BeanModel getBeanNonNull(@Nullable Class<?> beanClass) {
        return requireNonNull(getBean(beanClass));
    }

    public @Nullable BeanModel getBean(@Nullable Class<?> beanClass) {
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
