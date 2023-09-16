
package cz.habarta.typescript.generator.gradle;

import cz.habarta.typescript.generator.ClassMapping;
import cz.habarta.typescript.generator.DateMapping;
import cz.habarta.typescript.generator.EnumMapping;
import cz.habarta.typescript.generator.GsonConfiguration;
import cz.habarta.typescript.generator.IdentifierCasing;
import cz.habarta.typescript.generator.Input;
import cz.habarta.typescript.generator.Jackson2Configuration;
import cz.habarta.typescript.generator.JsonLibrary;
import cz.habarta.typescript.generator.JsonbConfiguration;
import cz.habarta.typescript.generator.Logger;
import cz.habarta.typescript.generator.MapMapping;
import cz.habarta.typescript.generator.ModuleDependency;
import cz.habarta.typescript.generator.NullabilityDefinition;
import cz.habarta.typescript.generator.OptionalProperties;
import cz.habarta.typescript.generator.OptionalPropertiesDeclaration;
import cz.habarta.typescript.generator.Output;
import cz.habarta.typescript.generator.RestNamespacing;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.StringQuotes;
import cz.habarta.typescript.generator.TypeScriptFileType;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.TypeScriptOutputKind;
import cz.habarta.typescript.generator.util.Utils;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskAction;


public class GenerateTask extends DefaultTask {

    public String outputFile;
    public TypeScriptFileType outputFileType;
    public TypeScriptOutputKind outputKind;
    public String module;
    public String namespace;
    public boolean mapPackagesToNamespaces;
    public String umdNamespace;
    public List<ModuleDependency> moduleDependencies;
    public List<String> classes;
    public List<String> classPatterns;
    public List<String> classesWithAnnotations;
    public List<String> classesImplementingInterfaces;
    public List<String> classesExtendingClasses;
    public String classesFromJaxrsApplication;
    public boolean classesFromAutomaticJaxrsApplication;
    public List<String> scanningAcceptedPackages;
    public List<String> excludeClasses;
    public List<String> excludeClassPatterns;
    public List<String> includePropertyAnnotations;
    public List<String> excludePropertyAnnotations;
    public JsonLibrary jsonLibrary;
    public Jackson2Configuration jackson2Configuration;
    public GsonConfiguration gsonConfiguration;
    public JsonbConfiguration jsonbConfiguration;
    public List<String> additionalDataLibraries;
    public OptionalProperties optionalProperties;
    public OptionalPropertiesDeclaration optionalPropertiesDeclaration;
    public NullabilityDefinition nullabilityDefinition;
    public boolean declarePropertiesAsReadOnly;
    public String removeTypeNamePrefix;
    public String removeTypeNameSuffix;
    public String addTypeNamePrefix;
    public String addTypeNameSuffix;
    public List<String> customTypeNaming;
    public String customTypeNamingFunction;
    public List<String> referencedFiles;
    public List<String> importDeclarations;
    public List<String> customTypeMappings;
    public List<String> customTypeAliases;
    public DateMapping mapDate;
    public MapMapping mapMap;
    public EnumMapping mapEnum;
    public IdentifierCasing enumMemberCasing;
    public boolean nonConstEnums;
    public List<String> nonConstEnumAnnotations;
    public ClassMapping mapClasses;
    public List<String> mapClassesAsClassesPatterns;
    public boolean generateConstructors;
    public List<String> disableTaggedUnionAnnotations;
    public boolean disableTaggedUnions;
    public boolean generateReadonlyAndWriteonlyJSDocTags;
    public boolean ignoreSwaggerAnnotations;
    public boolean generateJaxrsApplicationInterface;
    public boolean generateJaxrsApplicationClient;
    public boolean generateSpringApplicationInterface;
    public boolean generateSpringApplicationClient;
    public boolean scanSpringApplication;
    public RestNamespacing restNamespacing;
    public String restNamespacingAnnotation;
    public String restResponseType;
    public String restOptionsType;
    public String customRestMethodBuilder;
    public String customTypeProcessor;
    public boolean sortDeclarations;
    public boolean sortTypeDeclarations;
    public boolean noFileComment;
    public boolean noTslintDisable;
    public boolean noEslintDisable;
    public boolean tsNoCheck;
    public List<File> javadocXmlFiles;
    public List<String> extensionClasses;
    public List<String> extensions;
    public List<Settings.ConfiguredExtension> extensionsWithConfiguration;
    public List<String> optionalAnnotations;
    public List<String> requiredAnnotations;
    public List<String> nullableAnnotations;
    public boolean primitivePropertiesRequired;
    public boolean generateInfoJson;
    public boolean generateNpmPackageJson;
    public String npmName;
    public String npmVersion;
    public String npmTypescriptVersion;
    public String npmBuildScript;
    public List<String> npmDependencies;
    public List<String> npmDevDependencies;
    public List<String> npmPeerDependencies;
    public StringQuotes stringQuotes;
    public String indentString;
    public boolean jackson2ModuleDiscovery;
    public List<String> jackson2Modules;
    public Logger.Level loggingLevel;

    private Settings createSettings(URLClassLoader classLoader) {
        final Settings settings = new Settings();
        if (outputFileType != null) {
            settings.outputFileType = outputFileType;
        }
        settings.outputKind = outputKind;
        settings.module = module;
        settings.namespace = namespace;
        settings.mapPackagesToNamespaces = mapPackagesToNamespaces;
        settings.umdNamespace = umdNamespace;
        settings.moduleDependencies = moduleDependencies;
        settings.setExcludeFilter(excludeClasses, excludeClassPatterns);
        settings.jsonLibrary = jsonLibrary;
        settings.setJackson2Configuration(classLoader, jackson2Configuration);
        settings.gsonConfiguration = gsonConfiguration;
        settings.jsonbConfiguration = jsonbConfiguration;
        settings.additionalDataLibraries = additionalDataLibraries;
        settings.optionalProperties = optionalProperties;
        settings.optionalPropertiesDeclaration = optionalPropertiesDeclaration;
        settings.nullabilityDefinition = nullabilityDefinition;
        settings.declarePropertiesAsReadOnly = declarePropertiesAsReadOnly;
        settings.removeTypeNamePrefix = removeTypeNamePrefix;
        settings.removeTypeNameSuffix = removeTypeNameSuffix;
        settings.addTypeNamePrefix = addTypeNamePrefix;
        settings.addTypeNameSuffix = addTypeNameSuffix;
        settings.customTypeNaming = Settings.convertToMap(customTypeNaming, "customTypeNaming");
        settings.customTypeNamingFunction = customTypeNamingFunction;
        settings.referencedFiles = referencedFiles;
        settings.importDeclarations = importDeclarations;
        settings.customTypeMappings = Settings.convertToMap(customTypeMappings, "customTypeMapping");
        settings.customTypeAliases = Settings.convertToMap(customTypeAliases, "customTypeAlias");
        settings.mapDate = mapDate;
        settings.mapMap = mapMap;
        settings.mapEnum = mapEnum;
        settings.enumMemberCasing = enumMemberCasing;
        settings.nonConstEnums = nonConstEnums;
        settings.loadNonConstEnumAnnotations(classLoader, nonConstEnumAnnotations);
        settings.mapClasses = mapClasses;
        settings.mapClassesAsClassesPatterns = mapClassesAsClassesPatterns;
        settings.generateConstructors = generateConstructors;
        settings.loadDisableTaggedUnionAnnotations(classLoader, disableTaggedUnionAnnotations);
        settings.disableTaggedUnions = disableTaggedUnions;
        settings.generateReadonlyAndWriteonlyJSDocTags = generateReadonlyAndWriteonlyJSDocTags;
        settings.ignoreSwaggerAnnotations = ignoreSwaggerAnnotations;
        settings.generateJaxrsApplicationInterface = generateJaxrsApplicationInterface;
        settings.generateJaxrsApplicationClient = generateJaxrsApplicationClient;
        settings.generateSpringApplicationInterface = generateSpringApplicationInterface;
        settings.generateSpringApplicationClient = generateSpringApplicationClient;
        settings.scanSpringApplication = scanSpringApplication;
        settings.restNamespacing = restNamespacing;
        settings.setRestNamespacingAnnotation(classLoader, restNamespacingAnnotation);
        settings.restResponseType = restResponseType;
        settings.setRestOptionsType(restOptionsType);
        settings.loadCustomRestMethodBuilder(classLoader, customRestMethodBuilder);
        settings.loadCustomTypeProcessor(classLoader, customTypeProcessor);
        settings.sortDeclarations = sortDeclarations;
        settings.sortTypeDeclarations = sortTypeDeclarations;
        settings.noFileComment = noFileComment;
        settings.noTslintDisable = noTslintDisable;
        settings.noEslintDisable = noEslintDisable;
        settings.tsNoCheck = tsNoCheck;
        settings.javadocXmlFiles = javadocXmlFiles;
        settings.loadExtensions(classLoader, Utils.concat(extensionClasses, extensions), extensionsWithConfiguration);
        settings.loadIncludePropertyAnnotations(classLoader, includePropertyAnnotations);
        settings.loadExcludePropertyAnnotations(classLoader, excludePropertyAnnotations);
        settings.loadOptionalAnnotations(classLoader, optionalAnnotations);
        settings.loadRequiredAnnotations(classLoader, requiredAnnotations);
        settings.loadNullableAnnotations(classLoader, nullableAnnotations);
        settings.primitivePropertiesRequired = primitivePropertiesRequired;
        settings.generateInfoJson = generateInfoJson;
        settings.generateNpmPackageJson = generateNpmPackageJson;
        settings.npmName = npmName == null && generateNpmPackageJson ? getProject().getName() : npmName;
        settings.npmVersion = npmVersion == null && generateNpmPackageJson ? settings.getDefaultNpmVersion() : npmVersion;
        settings.npmTypescriptVersion = npmTypescriptVersion;
        settings.npmBuildScript = npmBuildScript;
        settings.npmPackageDependencies = Settings.convertToMap(npmDependencies, "npmDependencies");
        settings.npmDevDependencies = Settings.convertToMap(npmDevDependencies, "npmDevDependencies");
        settings.npmPeerDependencies = Settings.convertToMap(npmPeerDependencies, "npmPeerDependencies");
        settings.setStringQuotes(stringQuotes);
        settings.setIndentString(indentString);
        settings.jackson2ModuleDiscovery = jackson2ModuleDiscovery;
        settings.loadJackson2Modules(classLoader, jackson2Modules);
        settings.classLoader = classLoader;
        return settings;
    }

    @TaskAction
    public void generate() throws Exception {
        if (outputKind == null) {
            throw new RuntimeException("Please specify 'outputKind' property.");
        }
        if (jsonLibrary == null) {
            throw new RuntimeException("Please specify 'jsonLibrary' property.");
        }

        TypeScriptGenerator.setLogger(new Logger(loggingLevel));
        TypeScriptGenerator.printVersion();

        // class loader
        final Set<URL> urls = new LinkedHashSet<>();
        for (Task task : getProject().getTasks()) {
            if (task.getName().startsWith("compile") && !task.getName().startsWith("compileTest")) {
                for (File file : task.getOutputs().getFiles()) {
                    urls.add(file.toURI().toURL());
                }
            }
        }
        urls.addAll(getFilesFromConfiguration("compileClasspath"));

        try (URLClassLoader classLoader = Settings.createClassLoader(getProject().getName(), urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader())) {

            final Settings settings = createSettings(classLoader);

            final Input.Parameters parameters = new Input.Parameters();
            parameters.classNames = classes;
            parameters.classNamePatterns = classPatterns;
            parameters.classesWithAnnotations = classesWithAnnotations;
            parameters.classesImplementingInterfaces = classesImplementingInterfaces;
            parameters.classesExtendingClasses = classesExtendingClasses;
            parameters.jaxrsApplicationClassName = classesFromJaxrsApplication;
            parameters.automaticJaxrsApplication = classesFromAutomaticJaxrsApplication;
            parameters.isClassNameExcluded = settings.getExcludeFilter();
            parameters.classLoader = classLoader;
            parameters.scanningAcceptedPackages = scanningAcceptedPackages;
            parameters.debug = loggingLevel == Logger.Level.Debug;

            final File output = outputFile != null
                    ? getProject().file(outputFile)
                    : new File(new File(getProject().getBuildDir(), "typescript-generator"), getProject().getName() + settings.getExtension());
            settings.validateFileName(output);

            new TypeScriptGenerator(settings).generateTypeScript(Input.from(parameters), Output.to(output));
        }
    }

    private List<URL> getFilesFromConfiguration(String configuration) {
        try {
            final List<URL> urls = new ArrayList<>();
            for (File file : getProject().getConfigurations().getAt(configuration).getFiles()) {
                urls.add(file.toURI().toURL());
            }
            return urls;
        } catch (Exception e) {
            TypeScriptGenerator.getLogger().warning(String.format("Cannot get file names from configuration '%s': %s", configuration, e.getMessage()));
            return Collections.emptyList();
        }
    }

}
