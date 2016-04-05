
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.*;
import cz.habarta.typescript.generator.emitter.*;
import cz.habarta.typescript.generator.parser.*;
import java.io.*;
import java.util.*;


public class TypeScriptGenerator {

    public static final String Version = getVersion();

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
        settings.validate();
    }

    public static void printVersion() {
        System.out.println("Running TypeScriptGenerator version " + Version);
    }

    public String generateTypeScript(Input input) {
        final StringWriter stringWriter = new StringWriter();
        generateTypeScript(input, Output.to(stringWriter));
        return stringWriter.toString();
    }

    public void generateTypeScript(Input input, Output output) {
        generateTypeScript(input, output, false, 0);
    }

    public void generateEmbeddableTypeScript(Input input, Output output, boolean addExportKeyword, int initialIndentationLevel) {
        generateTypeScript(input, output, addExportKeyword, initialIndentationLevel);
    }

    private void generateTypeScript(Input input, Output output, boolean forceExportKeyword, int initialIndentationLevel) {
        final Model model = getModelParser().parseModel(input.getSourceTypes());
        final TsModel tsModel = getModelCompiler().javaToTypeScript(model);
        getEmitter().emit(tsModel, output.getWriter(), output.getName(), output.shouldCloseWriter(), forceExportKeyword, initialIndentationLevel);
    }

    public TypeProcessor getTypeProcessor() {
        if (typeProcessor == null) {
            final List<TypeProcessor> processors = new ArrayList<>();
            processors.add(new ExcludingTypeProcessor(settings.excludedClassNames));
            if (settings.customTypeProcessor != null) {
                processors.add(settings.customTypeProcessor);
            }
            processors.add(new GenericsTypeProcessor());
            processors.add(new DefaultTypeProcessor());
            typeProcessor = new TypeProcessor.Chain(processors);
        }
        return typeProcessor;
    }

    public ModelParser getModelParser() {
        if (modelParser == null) {
            if (settings.jsonLibrary == JsonLibrary.jackson2) {
                modelParser = new Jackson2Parser(settings, getTypeProcessor());
            } else {
                modelParser = new Jackson1Parser(settings, getTypeProcessor());
            }
        }
        return modelParser;
    }

    public ModelCompiler getModelCompiler() {
        if (modelCompiler == null) {
            modelCompiler = new ModelCompiler(settings, getTypeProcessor());
        }
        return modelCompiler;
    }

    public Emitter getEmitter() {
        if (emitter == null) {
            emitter = new Emitter(settings);
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
