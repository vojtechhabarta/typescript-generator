
package cz.habarta.typescript.generator;

import java.util.*;


public class Model {

    private final List<BeanModel> beans;

    public Model(List<BeanModel> beans) {
        this.beans = beans;
    }

    public List<BeanModel> getBeans() {
        return beans;
    }

}
