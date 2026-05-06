
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.emitter.Emitter;
import cz.habarta.typescript.generator.emitter.InfoJsonEmitter;
import cz.habarta.typescript.generator.emitter.NpmPackageJson;
import cz.habarta.typescript.generator.emitter.NpmPackageJsonEmitter;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.parser.GsonParser;
import cz.habarta.typescript.generator.parser.Jackson2Parser;
import cz.habarta.typescript.generator.parser.Jackson3Parser;
import cz.habarta.typescript.generator.parser.JsonbParser;
import cz.habarta.typescript.generator.parser.Model;
import cz.habarta.typescript.generator.parser.ModelParser;
import cz.habarta.typescript.generator.parser.RestApplicationParser;
import cz.habarta.typescript.generator.util.Utils;
import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;


public class TypeScriptGenerator {

    public static final @Nullable String Version = getVersion();

    private static Logger logger = new Logger();

    private final Settings settings;
    private @Nullable TypeProcessor commonTypeProcessor = null;
    private @Nullable ModelParser modelParser = null;
    private @Nullable ModelCompiler modelCompiler = null;

    public static Logger getLogger() {
        return logger;
    }

    public static void setLogger(Logger logger) {
        TypeScriptGenerator.logger = logger;
    }

    public TypeScriptGenerator() {
        this(new Settings());
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
        final Model model = getModelParser().parseModel(input.getSourceTypes());
        final TsModel tsModel = getModelCompiler().javaToTypeScript(model);
        generateTypeScript(tsModel, output);
        generateInfoJson(tsModel, output);
        generateNpmPackageJson(output);
    }

    private void generateTypeScript(TsModel tsModel, Output output) {
        new Emitter(settings, output.getWriter(), output.getName())
            .emit(tsModel, output.shouldCloseWriter());
    }

    private void generateInfoJson(TsModel tsModel, Output output) {
        if (settings.generateInfoJson) {
            if (output.getName() == null) {
                throw new RuntimeException("Generating info JSON can only be used when output is specified using file name");
            }
            final File outputFile = new File(output.getName());
            final Output out = Output.to(new File(outputFile.getParent(), "typescript-generator-info.json"));
            new InfoJsonEmitter(out.getWriter(), out.getName())
                .emit(tsModel, out.shouldCloseWriter());
        }
    }

    private void generateNpmPackageJson(Output output) {
        if (settings.generateNpmPackageJson) {
            if (output.getName() == null) {
                throw new RuntimeException("Generating NPM package.json can only be used when output is specified using file name");
            }
            final File outputFile = new File(output.getName());
            final Output npmOutput = Output.to(new File(outputFile.getParent(), "package.json"));
            final String types;
            final String main;
            final Map<String, String> dependencies = new LinkedHashMap<>();
            final Map<String, String> devDependencies = new LinkedHashMap<>();
            final Map<String, String> peerDependencies = new LinkedHashMap<>();
            final Map<String, String> scripts;
            if (settings.moduleDependencies != null) {
                for (ModuleDependency dependency : settings.moduleDependencies) {
                    if (dependency.peerDependency) {
                        peerDependencies.put(dependency.npmPackageName, dependency.npmVersionRange);
                    } else {
                        dependencies.put(dependency.npmPackageName, dependency.npmVersionRange);
                    }
                }
            }
            if (settings.outputFileType == TypeScriptFileType.implementationFile) {
                types = Utils.replaceExtension(outputFile, ".d.ts").getName();
                main = Utils.replaceExtension(outputFile, ".js").getName();
                dependencies.putAll(settings.npmPackageDependencies);
                devDependencies.putAll(settings.npmDevDependencies);
                peerDependencies.putAll(settings.npmPeerDependencies);
                final String typescriptVersion = settings.npmTypescriptVersion != null ? settings.npmTypescriptVersion : settings.typescriptVersion;
                devDependencies.put("typescript", typescriptVersion);
                final String npmBuildScript = settings.npmBuildScript != null
                    ? settings.npmBuildScript
                    : "tsc --module umd --moduleResolution node --typeRoots --target es5 --lib es6 --declaration --sourceMap $outputFile";
                final String build = npmBuildScript.replaceAll(Pattern.quote("$outputFile"), outputFile.getName());
                scripts = Collections.singletonMap("build", build);
            } else {
                types = outputFile.getName();
                main = null;
                scripts = null;
            }
            final NpmPackageJson npmPackageJson = new NpmPackageJson(
                settings.npmName,
                settings.npmVersion,
                types,
                main,
                dependencies,
                devDependencies,
                peerDependencies,
                scripts
            );
            new NpmPackageJsonEmitter(npmOutput.getWriter(), npmOutput.getName())
                .emit(npmPackageJson, npmOutput.shouldCloseWriter());
        }
    }

    public TypeProcessor getCommonTypeProcessor() {
        if (commonTypeProcessor == null) {
            final List<RestApplicationParser.Factory> restFactories = settings.getRestApplicationParserFactories();
            final ModelParser.Factory modelParserFactory = getModelParserFactory();
            final List<TypeProcessor> specificTypeProcessors = Stream
                .concat(
                    restFactories.stream().map(factory -> factory.getSpecificTypeProcessor()),
                    Stream.of(modelParserFactory.getSpecificTypeProcessor())
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            commonTypeProcessor = createTypeProcessor(specificTypeProcessors);
        }
        return commonTypeProcessor;
    }

    private TypeProcessor createTypeProcessor(List<TypeProcessor> specificTypeProcessors) {
        final List<TypeProcessor> processors = new ArrayList<>();
        processors.add(new ExcludingTypeProcessor(settings.getExcludeFilter()));
        if (settings.customTypeProcessor != null) {
            processors.add(settings.customTypeProcessor);
        }
        processors.add(new CustomMappingTypeProcessor(settings.getValidatedCustomTypeMappings()));
        processors.addAll(specificTypeProcessors);
        processors.add(new DefaultTypeProcessor(settings.getLoadedDataLibraries()));
        final TypeProcessor typeProcessor = new TypeProcessor.Chain(processors);
        return typeProcessor;
    }

    public ModelParser getModelParser() {
        if (modelParser == null) {
            modelParser = createModelParser();
        }
        return modelParser;
    }

    private ModelParser createModelParser() {
        final List<RestApplicationParser.Factory> factories = settings.getRestApplicationParserFactories();
        final List<RestApplicationParser> restApplicationParsers = factories.stream()
            .map(factory -> factory.create(settings, getCommonTypeProcessor()))
            .collect(Collectors.toList());
        return getModelParserFactory().create(settings, getCommonTypeProcessor(), restApplicationParsers);
    }

    private ModelParser.Factory getModelParserFactory() {
        if (settings.jsonLibrary == null) {
            throw new RuntimeException();
        }
        return switch (settings.jsonLibrary) {
            case jackson3 -> new Jackson3Parser.Jackson3ParserFactory();
            case jackson2 -> new Jackson2Parser.Jackson2ParserFactory();
            case jaxb -> new Jackson3Parser.JaxbParserFactory();
            case gson -> new GsonParser.Factory();
            case jsonb -> new JsonbParser.Factory();
        };
    }

    public ModelCompiler getModelCompiler() {
        if (modelCompiler == null) {
            modelCompiler = new ModelCompiler(settings, getCommonTypeProcessor());
        }
        return modelCompiler;
    }

    private static @Nullable String getVersion() {
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
