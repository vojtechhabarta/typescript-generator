
package cz.habarta.typescript.generator.ext;

import cz.habarta.typescript.generator.Extension;
import cz.habarta.typescript.generator.compiler.EnumKind;
import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.compiler.ModelTransformer;
import cz.habarta.typescript.generator.compiler.SymbolTable;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsBeanModel;
import cz.habarta.typescript.generator.emitter.TsEnumModel;
import cz.habarta.typescript.generator.emitter.TsModel;
import java.lang.reflect.Field;
import java.util.*;


public class ClassEnumExtension extends Extension {

    public static final String CFG_CLASS_ENUM_PATTERN = "classEnumPattern";

    private String classEnumPattern = "ClassEnum";

    @Override
    public EmitterExtensionFeatures getFeatures() {
        return new EmitterExtensionFeatures();
    }

    @Override
    public void setConfiguration(Map<String, String> configuration) throws RuntimeException {
        if (configuration.containsKey(CFG_CLASS_ENUM_PATTERN)) {
            classEnumPattern = configuration.get(CFG_CLASS_ENUM_PATTERN);
        }
    }

    @Override
    public List<TransformerDefinition> getTransformers() {
        return Arrays.asList(new TransformerDefinition(ModelCompiler.TransformationPhase.BeforeEnums, new ModelTransformer() {
            @Override
            public TsModel transformModel(SymbolTable symbolTable, TsModel model) {
                List<TsBeanModel> beans = model.getBeans();
                List<TsBeanModel> classEnums = new ArrayList<>();
                for (TsBeanModel bean : beans) {
                    if (bean.getName().getSimpleName().contains(classEnumPattern))
                        classEnums.add(bean);
                }

                List<TsEnumModel> stringEnums = new ArrayList<>();
                for (TsBeanModel tsBeanModel : classEnums) {
                    List<EnumMemberModel> members = new ArrayList<>();
                    for (Field declaredField : tsBeanModel.getOrigin().getDeclaredFields()) {
                        if (declaredField.getType().getName().equals(tsBeanModel.getOrigin().getName())) {
                            members.add(new EnumMemberModel(declaredField.getName(), declaredField.getName(), null));
                        }
                    }
                    TsEnumModel temp = new TsEnumModel(
                            tsBeanModel.getOrigin(),
                            tsBeanModel.getName(),
                            EnumKind.StringBased,
                            members,
                            null,
                            false
                    );
                    stringEnums.add(temp);
                }

                stringEnums.addAll(model.getEnums());
                return model.withEnums(stringEnums).withoutBeans(classEnums);
            }
        }));
    }
}
