
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.emitter.*;
import cz.habarta.typescript.generator.parser.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.logging.Logger;


public class TypeScriptGenerator {

    public static final String Version = getVersion();

    private final Logger logger = Logger.getGlobal();
    private final Settings settings;
    private TypeProcessor typeProcessor = null;
    private ModelParser modelParser = null;
    private ModelCompiler modelCompiler = null;
    private Emitter emitter = null;

    public TypeScriptGenerator() {
        this (new Settings());
    }

    public TypeScriptGenerator(Settings settings) {
        this.settings = settings;
    }

    public void generateTypeScript(List<? extends Class<?>> classes, File file) {
        try {
            generateTypeScript(classes, new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void generateTypeScript(List<? extends Class<?>> classes, OutputStream output) {
        generateTypeScript(classes, new OutputStreamWriter(output, Charset.forName("UTF-8")));
    }

    public void generateTypeScript(List<? extends Class<?>> classes, Writer output) {
        generateTypeScript(classes, output, false, 0);
    }

    public void generateEmbeddableTypeScript(List<? extends Class<?>> classes, OutputStream output, boolean addExportKeyword, int initialIndentationLevel) {
        generateEmbeddableTypeScript(classes, new OutputStreamWriter(output, Charset.forName("UTF-8")), addExportKeyword, initialIndentationLevel);
    }

    public void generateEmbeddableTypeScript(List<? extends Class<?>> classes, Writer output, boolean addExportKeyword, int initialIndentationLevel) {
        generateTypeScript(classes, output, addExportKeyword, initialIndentationLevel);
    }

    private void generateTypeScript(List<? extends Class<?>> classes, Writer output, boolean forceExportKeyword, int initialIndentationLevel) {
        logger.info("Running TypeScriptGenerator version " + Version);
        final Model model = getModelParser().parseModel(classes);
        final TsModel tsModel = getModelCompiler().javaToTypeScript(model);
        getEmitter().emit(tsModel, output, forceExportKeyword, initialIndentationLevel);
    }

    public TypeProcessor getTypeProcessor() {
        if (typeProcessor == null) {
            if (settings.customTypeProcessor != null) {
                typeProcessor = new TypeProcessor.Chain(settings.customTypeProcessor, new DefaultTypeProcessor());
            } else {
                typeProcessor = new DefaultTypeProcessor();
            }
        }
        return typeProcessor;
    }

    public ModelParser getModelParser() {
        if (modelParser == null) {
            if (settings.jsonLibrary == JsonLibrary.jackson2) {
                modelParser = new Jackson2Parser(logger, settings, getTypeProcessor());
            } else {
                modelParser = new Jackson1Parser(logger, settings, getTypeProcessor());
            }
        }
        return modelParser;
    }

    public ModelCompiler getModelCompiler() {
        if (modelCompiler == null) {
            modelCompiler = new ModelCompiler(logger, settings, getTypeProcessor());
        }
        return modelCompiler;
    }

    public Emitter getEmitter() {
        if (emitter == null) {
            emitter = new Emitter(logger, settings);
        }
        return emitter;
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
