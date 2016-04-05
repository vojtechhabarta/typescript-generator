
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.parser.*;
import java.lang.reflect.Type;
import java.util.*;


public class TestUtils {

    private TestUtils() {
    }

    public static Settings settings() {
        final Settings settings = new Settings();
        settings.outputKind = TypeScriptOutputKind.global;
        settings.jsonLibrary = JsonLibrary.jackson2;
        settings.noFileComment = true;
        settings.newline = "\n";
        return settings;
    }

    public static TsType compileType(Settings settings, Type type) {
        final BeanModel beanModel = new BeanModel(Object.class, Object.class, Collections.singletonList(new PropertyModel("test", type, false, null)));
        final Model model = new Model(Collections.singletonList(beanModel), Collections.<EnumModel>emptyList());
        final ModelCompiler modelCompiler = new TypeScriptGenerator(settings).getModelCompiler();
        final TsModel tsModel = modelCompiler.javaToTypeScript(model);
        return tsModel.getBeans().get(0).getProperties().get(0).getTsType();
    }

}
