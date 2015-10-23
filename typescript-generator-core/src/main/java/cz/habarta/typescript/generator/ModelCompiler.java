
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.emitter.*;
import cz.habarta.typescript.generator.parser.*;


public class ModelCompiler {

    private final Settings settings;

    public ModelCompiler(Settings settings) {
        this.settings = settings;
    }

    public TsModel javaToTypescript(Model model) {
        final TsModel tsModel = new TsModel();
        for (BeanModel bean : model.getBeans()) {
            processBean(tsModel, bean);
        }
        return tsModel;
    }

    private void processBean(TsModel tsModel, BeanModel jBeanModel) {
        final TsBeanModel tsBeanModel = new TsBeanModel(jBeanModel.getName(), jBeanModel.getParent());
        tsModel.getBeans().add(tsBeanModel);
        for (PropertyModel jBean : jBeanModel.getProperties()) {
            processProperty(tsModel, tsBeanModel, jBean);
        }
    }

    private void processProperty(TsModel tsModel, TsBeanModel tsBeanModel, PropertyModel jPropertyModel) {
        final TsPropertyModel tsPropertyModel = new TsPropertyModel(jPropertyModel.getName(), jPropertyModel.getTsType(), jPropertyModel.getComments());
        tsBeanModel.getProperties().add(tsPropertyModel);
    }

}
