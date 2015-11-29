package cz.habarta.typescript.generator.parser;

import com.google.api.client.util.Lists;

public class EnumBeanModel extends BeanModel {

    public EnumBeanModel(Class<?> beanClass) {
        super(beanClass, null, Lists.<PropertyModel> newArrayList());
    }
}
