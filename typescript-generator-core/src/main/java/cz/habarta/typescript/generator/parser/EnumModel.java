
package cz.habarta.typescript.generator.parser;

import java.util.*;


public class EnumModel {

    private final Class<?> enumClass;
    private final List<String> values;
    private final List<String> comments;

    private final BeanModel valueModel;

    public EnumModel(Class<?> enumClass, List<String> values, List<String> comments, BeanModel valueModel) {
        this.enumClass = enumClass;
        this.values = values;
        this.comments = comments;
        this.valueModel = valueModel;
    }

    public Class<?> getEnumClass() {
        return enumClass;
    }

    public List<String> getValues() {
        return values;
    }

    public List<String> getComments() {
        return comments;
    }

    public BeanModel getValueModel() {
        return valueModel;
    }

}
