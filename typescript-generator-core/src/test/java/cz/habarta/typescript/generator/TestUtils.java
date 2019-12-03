
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.ModelCompiler;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;


public class TestUtils {

    private TestUtils() {
    }

    public static Settings settings() {
        final Settings settings = new Settings();
        settings.outputKind = TypeScriptOutputKind.global;
        settings.jsonLibrary = JsonLibrary.jackson2;
        settings.noFileComment = true;
        settings.noTslintDisable = true;
        settings.noEslintDisable = true;
        settings.newline = "\n";
        settings.classLoader = Thread.currentThread().getContextClassLoader();
        return settings;
    }

    public static TsType compileType(Settings settings, Type type) {
        final ModelCompiler modelCompiler = new TypeScriptGenerator(settings).getModelCompiler();
        return modelCompiler.javaToTypeScript(type, null);
    }

    public static String readFile(String file) {
        try {
            return String.join("\n", Files.readAllLines(Paths.get(file)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
