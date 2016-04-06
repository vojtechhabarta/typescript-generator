
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.ModelCompiler;
import java.lang.reflect.Type;


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
        final ModelCompiler modelCompiler = new TypeScriptGenerator(settings).getModelCompiler();
        return modelCompiler.javaToTypeScript(type);
    }

}
