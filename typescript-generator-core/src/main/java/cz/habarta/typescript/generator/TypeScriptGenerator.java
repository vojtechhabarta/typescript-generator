
package cz.habarta.typescript.generator;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;


public class TypeScriptGenerator {

    public static void generateTypeScript(List<? extends Class<?>> classes, Settings settings, File outputDeclarationFile) {
        final Logger logger = Logger.getGlobal();

        final Jackson1Parser jacksonParser = new Jackson1Parser(logger, settings);
        final Model model = jacksonParser.parseModel(classes);

        Emitter.emit(logger, settings, outputDeclarationFile, model);
    }

}
