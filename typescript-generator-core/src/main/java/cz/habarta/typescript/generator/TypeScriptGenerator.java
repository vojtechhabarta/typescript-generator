
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.*;
import cz.habarta.typescript.generator.emitter.*;
import cz.habarta.typescript.generator.parser.*;
import cz.habarta.typescript.generator.util.Utils;
import java.io.*;
import java.util.*;


public class TypeScriptGenerator {

    public static final String Version = getVersion();

    private static Logger logger = new Logger();

    private final Settings settings;
    private TypeProcessor typeProcessor = null;
    private ModelParser modelParser = null;
    private ModelCompiler modelCompiler = null;
    private Emitter emitter = null;
    private InfoJsonEmitter infoJsonEmitter = null;
    private NpmPackageJsonEmitter npmPackageJsonEmitter = null;

    public static Logger getLogger() {
        return logger;
    }

    public static void setLogger(Logger logger) {
        TypeScriptGenerator.logger = logger;
    }

    public TypeScriptGenerator() {
        this (new Settings());
    }

    public TypeScriptGenerator(Settings settings) {
        this.settings = settings;
        settings.validate();
    }

    public static void printVersion() {
        TypeScriptGenerator.getLogger().info("Running TypeScriptGenerator version " + Version);
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
        generateTypeScript(tsModel, output, forceExportKeyword, initialIndentationLevel);
        generateInfoJson(tsModel, output);
        generateNpmPackageJson(output);
    }

    private void generateTypeScript(TsModel tsModel, Output output, boolean forceExportKeyword, int initialIndentationLevel) {
        getEmitter().emit(tsModel, output.getWriter(), output.getName(), output.shouldCloseWriter(), forceExportKeyword, initialIndentationLevel);
    }

    private void generateInfoJson(TsModel tsModel, Output output) {
        if (settings.generateInfoJson) {
            if (output.getName() == null) {
                throw new RuntimeException("Generating info JSON can only be used when output is specified using file name");
            }
            final File outputFile = new File(output.getName());
            final Output out = Output.to(new File(outputFile.getParent(), "typescript-generator-info.json"));
            getInfoJsonEmitter().emit(tsModel, out.getWriter(), out.getName(), out.shouldCloseWriter());
        }
    }

    private void generateNpmPackageJson(Output output) {
        if (settings.generateNpmPackageJson) {
            if (output.getName() == null) {
                throw new RuntimeException("Generating NPM package.json can only be used when output is specified using file name");
            }
            final File outputFile = new File(output.getName());
            final Output npmOutput = Output.to(new File(outputFile.getParent(), "package.json"));
            final NpmPackageJson npmPackageJson = new NpmPackageJson();
            npmPackageJson.name = settings.npmName;
            npmPackageJson.version = settings.npmVersion;
            npmPackageJson.types = outputFile.getName();
            npmPackageJson.dependencies = new LinkedHashMap<>();
            if (settings.moduleDependencies != null) {
                for (ModuleDependency dependency : settings.moduleDependencies) {
                    npmPackageJson.dependencies.put(dependency.npmPackageName, dependency.npmVersionRange);
                }
            }
            if (settings.outputFileType == TypeScriptFileType.implementationFile) {
                npmPackageJson.types = Utils.replaceExtension(outputFile, ".d.ts").getName();
                npmPackageJson.main = Utils.replaceExtension(outputFile, ".js").getName();
                npmPackageJson.dependencies.putAll(settings.npmPackageDependencies);
                npmPackageJson.devDependencies = Collections.singletonMap("typescript", settings.typescriptVersion);
                npmPackageJson.scripts = Collections.singletonMap("build", "tsc --module umd --moduleResolution node --target es5 --lib es6 --declaration --sourceMap " + outputFile.getName());
            }
            if (npmPackageJson.dependencies.isEmpty()) {
                npmPackageJson.dependencies = null;
            }
            getNpmPackageJsonEmitter().emit(npmPackageJson, npmOutput.getWriter(), npmOutput.getName(), npmOutput.shouldCloseWriter());
        }
    }

    public TypeProcessor getTypeProcessor() {
        if (typeProcessor == null) {
            final List<TypeProcessor> processors = new ArrayList<>();
            processors.add(new ExcludingTypeProcessor(settings.getExcludeFilter()));
            if (settings.customTypeProcessor != null) {
                processors.add(settings.customTypeProcessor);
            }
            processors.add(new CustomMappingTypeProcessor(settings.customTypeMappings));
            processors.add(new DefaultTypeProcessor());
            typeProcessor = new TypeProcessor.Chain(processors);
        }
        return typeProcessor;
    }

    public ModelParser getModelParser() {
        if (modelParser == null) {
            modelParser = createModelParser();
        }
        return modelParser;
    }

    private ModelParser createModelParser() {
        switch (settings.jsonLibrary) {
            case jackson1:
                return new Jackson1Parser(settings, getTypeProcessor());
            case jackson2:
                return new Jackson2Parser(settings, getTypeProcessor());
            case jaxb:
                return new Jackson2Parser(settings, getTypeProcessor(), /*useJaxbAnnotations*/ true);
            default:
                throw new RuntimeException();
        }
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

    public InfoJsonEmitter getInfoJsonEmitter() {
        if (infoJsonEmitter == null) {
            infoJsonEmitter = new InfoJsonEmitter();
        }
        return infoJsonEmitter;
    }

    public NpmPackageJsonEmitter getNpmPackageJsonEmitter() {
        if (npmPackageJsonEmitter == null) {
            npmPackageJsonEmitter = new NpmPackageJsonEmitter();
        }
        return npmPackageJsonEmitter;
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
