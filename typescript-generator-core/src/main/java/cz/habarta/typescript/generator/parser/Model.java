
package cz.habarta.typescript.generator.parser;

import java.util.*;


public class Model {

    private final List<BeanModel> beans;

    public Model(List<BeanModel> beans) {
        this.beans = beans;
    }

    public List<BeanModel> getBeans() {
        return beans;
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
        sb.append('}');
        return sb.toString();
    }

}
