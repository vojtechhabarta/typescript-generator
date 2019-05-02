
package cz.habarta.typescript.generator.gradle;

import cz.habarta.typescript.generator.*;
import cz.habarta.typescript.generator.Input;
import cz.habarta.typescript.generator.util.Utils;
import java.io.*;
import java.net.*;
import java.util.*;
import org.gradle.api.*;
import org.gradle.api.tasks.*;


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
    public List<String> excludeClasses;
    public List<String> excludeClassPatterns;
    public List<String> includePropertyAnnotations;
    public List<String> excludePropertyAnnotations;
    public JsonLibrary jsonLibrary;
    public Jackson2Configuration jackson2Configuration;
    @Deprecated public boolean declarePropertiesAsOptional;
    public OptionalProperties optionalProperties;
    public OptionalPropertiesDeclaration optionalPropertiesDeclaration;
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
    public DateMapping mapDate;
    public EnumMapping mapEnum;
    public boolean nonConstEnums;
    public List<String> nonConstEnumAnnotations;
    public ClassMapping mapClasses;
    public List<String> mapClassesAsClassesPatterns;
    public boolean disableTaggedUnions;
    public boolean ignoreSwaggerAnnotations;
    public boolean generateJaxrsApplicationInterface;
    public boolean generateJaxrsApplicationClient;
    public boolean generateSpringApplicationInterface;
    public boolean generateSpringApplicationClient;
    public boolean scanSpringApplication;
    @Deprecated public RestNamespacing jaxrsNamespacing;
    @Deprecated public String jaxrsNamespacingAnnotation;
    public RestNamespacing restNamespacing;
    public String restNamespacingAnnotation;
    public String restResponseType;
    public String restOptionsType;
    public String customTypeProcessor;
    public boolean sortDeclarations;
    public boolean sortTypeDeclarations;
    public boolean noFileComment;
    public boolean noTslintDisable;
    public boolean noEslintDisable;
    public List<File> javadocXmlFiles;
    public List<String> extensionClasses;
    public List<String> extensions;
    public List<Settings.ConfiguredExtension> extensionsWithConfiguration;
    public List<String> optionalAnnotations;
    public boolean generateInfoJson;
    public boolean generateNpmPackageJson;
    public String npmName;
    public String npmVersion;
    public String npmBuildScript;
    public StringQuotes stringQuotes;
    public String indentString;
    @Deprecated public boolean displaySerializerWarning;
    @Deprecated public boolean disableJackson2ModuleDiscovery;
    public boolean jackson2ModuleDiscovery;
    public List<String> jackson2Modules;
    @Deprecated public boolean debug;
    public Logger.Level loggingLevel;

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
            if (task.getName().startsWith("compile")) {
                for (File file : task.getOutputs().getFiles()) {
                    urls.add(file.toURI().toURL());
                }
            }
        }
        urls.addAll(getFilesFromConfiguration("compile"));
        urls.addAll(getFilesFromConfiguration("compileClasspath"));

        try (URLClassLoader classLoader = Settings.createClassLoader(getProject().getName(), urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader())) {

            // Settings
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
            settings.declarePropertiesAsOptional = declarePropertiesAsOptional;
            settings.optionalProperties = optionalProperties;
            settings.optionalPropertiesDeclaration = optionalPropertiesDeclaration;
            settings.declarePropertiesAsReadOnly = declarePropertiesAsReadOnly;
            settings.removeTypeNamePrefix = removeTypeNamePrefix;
            settings.removeTypeNameSuffix = removeTypeNameSuffix;
            settings.addTypeNamePrefix = addTypeNamePrefix;
            settings.addTypeNameSuffix = addTypeNameSuffix;
            settings.customTypeNaming = Settings.convertToMap(customTypeNaming);
            settings.customTypeNamingFunction = customTypeNamingFunction;
            settings.referencedFiles = referencedFiles;
            settings.importDeclarations = importDeclarations;
            settings.customTypeMappings = Settings.convertToMap(customTypeMappings);
            settings.mapDate = mapDate;
            settings.mapEnum = mapEnum;
            settings.nonConstEnums = nonConstEnums;
            settings.loadNonConstEnumAnnotations(classLoader, nonConstEnumAnnotations);
            settings.mapClasses = mapClasses;
            settings.mapClassesAsClassesPatterns = mapClassesAsClassesPatterns;
            settings.disableTaggedUnions = disableTaggedUnions;
            settings.ignoreSwaggerAnnotations = ignoreSwaggerAnnotations;
            settings.generateJaxrsApplicationInterface = generateJaxrsApplicationInterface;
            settings.generateJaxrsApplicationClient = generateJaxrsApplicationClient;
            settings.generateSpringApplicationInterface = generateSpringApplicationInterface;
            settings.generateSpringApplicationClient = generateSpringApplicationClient;
            settings.scanSpringApplication = scanSpringApplication;
            settings.jaxrsNamespacing = jaxrsNamespacing;
            settings.setJaxrsNamespacingAnnotation(classLoader, jaxrsNamespacingAnnotation);
            settings.restNamespacing = restNamespacing;
            settings.setRestNamespacingAnnotation(classLoader, restNamespacingAnnotation);
            settings.restResponseType = restResponseType;
            settings.setRestOptionsType(restOptionsType);
            settings.loadCustomTypeProcessor(classLoader, customTypeProcessor);
            settings.sortDeclarations = sortDeclarations;
            settings.sortTypeDeclarations = sortTypeDeclarations;
            settings.noFileComment = noFileComment;
            settings.noTslintDisable = noTslintDisable;
            settings.noEslintDisable = noEslintDisable;
            settings.javadocXmlFiles = javadocXmlFiles;
            settings.loadExtensions(classLoader, Utils.concat(extensionClasses, extensions), extensionsWithConfiguration);
            settings.loadIncludePropertyAnnotations(classLoader, includePropertyAnnotations);
            settings.loadExcludePropertyAnnotations(classLoader, excludePropertyAnnotations);
            settings.loadOptionalAnnotations(classLoader, optionalAnnotations);
            settings.generateInfoJson = generateInfoJson;
            settings.generateNpmPackageJson = generateNpmPackageJson;
            settings.npmName = npmName == null && generateNpmPackageJson ? getProject().getName() : npmName;
            settings.npmVersion = npmVersion == null && generateNpmPackageJson ? settings.getDefaultNpmVersion() : npmVersion;
            settings.npmBuildScript = npmBuildScript;
            settings.setStringQuotes(stringQuotes);
            settings.setIndentString(indentString);
            settings.displaySerializerWarning = displaySerializerWarning;
            settings.debug = debug;
            settings.disableJackson2ModuleDiscovery = disableJackson2ModuleDiscovery;
            settings.jackson2ModuleDiscovery = jackson2ModuleDiscovery;
            settings.loadJackson2Modules(classLoader, jackson2Modules);
            settings.classLoader = classLoader;
            final File output = outputFile != null
                    ? getProject().file(outputFile)
                    : new File(new File(getProject().getBuildDir(), "typescript-generator"), getProject().getName() + settings.getExtension());
            settings.validateFileName(output);

            // TypeScriptGenerator
            new TypeScriptGenerator(settings).generateTypeScript(
                    Input.fromClassNamesAndJaxrsApplication(classes, classPatterns, classesWithAnnotations,
                        classesImplementingInterfaces, classesExtendingClasses,
                        classesFromJaxrsApplication,
                        classesFromAutomaticJaxrsApplication, settings.getExcludeFilter(),
                        classLoader, loggingLevel == Logger.Level.Debug),
                    Output.to(output)
            );
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
