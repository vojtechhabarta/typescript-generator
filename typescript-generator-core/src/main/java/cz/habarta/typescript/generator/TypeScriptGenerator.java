
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.emitter.*;
import cz.habarta.typescript.generator.parser.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.logging.Logger;


public class TypeScriptGenerator {

    public static final String Version = getVersion();

    public static void generateTypeScript(List<? extends Class<?>> classes, Settings settings, File file) {
        try {
            generateTypeScript(classes, settings, new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void generateTypeScript(List<? extends Class<?>> classes, Settings settings, OutputStream output) {
        generateTypeScript(classes, settings, new OutputStreamWriter(output, Charset.forName("UTF-8")));
    }

    public static void generateTypeScript(List<? extends Class<?>> classes, Settings settings, Writer output) {
        generateTypeScript(classes, settings, output, false, 0);
    }

    public static void generateEmbeddableTypeScript(List<? extends Class<?>> classes, Settings settings, OutputStream output, boolean addExportKeyword, int initialIndentationLevel) {
        generateEmbeddableTypeScript(classes, settings, new OutputStreamWriter(output, Charset.forName("UTF-8")), addExportKeyword, initialIndentationLevel);
    }

    public static void generateEmbeddableTypeScript(List<? extends Class<?>> classes, Settings settings, Writer output, boolean addExportKeyword, int initialIndentationLevel) {
        generateTypeScript(classes, settings, output, addExportKeyword, initialIndentationLevel);
    }

    private static void generateTypeScript(List<? extends Class<?>> classes, Settings settings, Writer output, boolean forceExportKeyword, int initialIndentationLevel) {
        final Logger logger = Logger.getGlobal();
        logger.info("Running TypeScriptGenerator version " + Version);
        final TypeProcessor typeProcessor = createTypeProcessor(settings);

        final ModelParser modelParser;
        if (settings.jsonLibrary == JsonLibrary.jackson2) {
            modelParser = new Jackson2Parser(logger, settings, typeProcessor);
        } else {
            modelParser = new Jackson1Parser(logger, settings, typeProcessor);
        }
        final Model model = modelParser.parseModel(classes);

        final ModelCompiler compiler = new ModelCompiler(logger, settings, typeProcessor);
        final TsModel tsModel = compiler.javaToTypeScript(model);

        Emitter.emit(logger, settings, output, tsModel, forceExportKeyword, initialIndentationLevel);
    }

    static TypeProcessor createTypeProcessor(Settings settings) {
        if (settings.customTypeProcessor != null) {
            return new TypeProcessor.Chain(settings.customTypeProcessor, new DefaultTypeProcessor());
        } else {
            return new DefaultTypeProcessor();
        }
    }

    private static String getVersion() {
        try {
            final InputStream inputStream = TypeScriptGenerator.class.getResourceAsStream(
                    "/META-INF/maven/cz.habarta.typescript-generator/typescript-generator-core/pom.properties");
            if (inputStream != null) {
                final Properties properties = new Properties();
                properties.load(inputStream);
                return (String) properties.get("version");
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

}
