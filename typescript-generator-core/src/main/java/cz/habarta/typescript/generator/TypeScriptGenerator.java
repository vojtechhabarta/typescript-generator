
package cz.habarta.typescript.generator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import cz.habarta.typescript.generator.emitter.Emitter;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.parser.Jackson1Parser;
import cz.habarta.typescript.generator.parser.Jackson2Parser;
import cz.habarta.typescript.generator.parser.Model;
import cz.habarta.typescript.generator.parser.ModelParser;


public class TypeScriptGenerator {

    public static Map<Type, TsType> generateTypeScript(List<? extends Class<?>> classes, Settings settings, File file) {
        try {
            return generateTypeScript(classes, settings, new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    public static Map<Type, TsType> generateTypeScript(List<? extends Class<?>> classes, Settings settings, OutputStream output) {
        final Logger logger = Logger.getGlobal();
        final ModelCompiler compiler = new ModelCompiler(logger, settings);

        final ModelParser modelParser;
        if (settings.jsonLibrary == JsonLibrary.jackson2) {
            modelParser = new Jackson2Parser(logger, settings, compiler);
        } else {
            modelParser = new Jackson1Parser(logger, settings, compiler);
        }
        final Model model = modelParser.parseModel(classes);

        final TsModel tsModel = compiler.javaToTypescript(model);

        Emitter.emit(logger, settings, output, tsModel);
        return compiler.getJavaToTypescriptTypeMap();
    }

}
