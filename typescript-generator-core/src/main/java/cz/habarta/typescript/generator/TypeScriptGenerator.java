
package cz.habarta.typescript.generator;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;


public class TypeScriptGenerator {

    public static void generateTypeScript(List<? extends Class<?>> classes, Settings settings, File outputDeclarationFile) {
        final Logger logger = Logger.getGlobal();

        final ModelParser modelParser;
        if (settings.jsonLibrary == JsonLibrary.jackson2) {
            modelParser = new Jackson2Parser(logger, settings);
        } else {
            modelParser = new Jackson1Parser(logger, settings);
        }
        final Model model = modelParser.parseModel(classes);

        Emitter.emit(logger, settings, outputDeclarationFile, model);
    }

}
