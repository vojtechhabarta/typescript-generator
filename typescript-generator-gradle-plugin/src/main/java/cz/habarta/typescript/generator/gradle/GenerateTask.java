
package cz.habarta.typescript.generator.gradle;

import cz.habarta.typescript.generator.ClassMapping;
import cz.habarta.typescript.generator.DateMapping;
import cz.habarta.typescript.generator.EnumMapping;
import cz.habarta.typescript.generator.GsonConfiguration;
import cz.habarta.typescript.generator.IdentifierCasing;
import cz.habarta.typescript.generator.Jackson2Configuration;
import cz.habarta.typescript.generator.Jackson3Configuration;
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
import java.util.List;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;


/**
 * Task for generating TypeScript files from Java classes.
 * This implementation is compatible with Gradle 8.14+ and Configuration Cache.
 */
public abstract class GenerateTask extends DefaultTask {

    /**
     * Classpath containing compiled classes and dependencies.
     */
    @Classpath
    public abstract ConfigurableFileCollection getClasspath();

    /**
     * Build directory for default output location.
     */
    @Internal
    public abstract DirectoryProperty getBuildDirectory();

    /**
     * Project name for default output file naming.
     */
    @Input
    public abstract Property<String> getProjectName();

    // All configuration properties from Extension
    @Input
    @Optional
    public abstract Property<String> getOutputFile();

    @Input
    @Optional
    public abstract Property<TypeScriptFileType> getOutputFileType();

    @Input
    public abstract Property<TypeScriptOutputKind> getOutputKind();

    @Input
    @Optional
    public abstract Property<String> getModule();

    @Input
    @Optional
    public abstract Property<String> getNamespace();

    @Input
    @Optional
    public abstract Property<Boolean> getMapPackagesToNamespaces();

    @Input
    @Optional
    public abstract Property<String> getUmdNamespace();

    @Input
    @Optional
    public abstract ListProperty<ModuleDependency> getModuleDependencies();

    @Input
    @Optional
    public abstract ListProperty<String> getClasses();

    @Input
    @Optional
    public abstract ListProperty<String> getClassPatterns();

    @Input
    @Optional
    public abstract ListProperty<String> getClassesWithAnnotations();

    @Input
    @Optional
    public abstract ListProperty<String> getClassesImplementingInterfaces();

    @Input
    @Optional
    public abstract ListProperty<String> getClassesExtendingClasses();

    @Input
    @Optional
    public abstract Property<String> getClassesFromJaxrsApplication();

    @Input
    @Optional
    public abstract Property<Boolean> getClassesFromAutomaticJaxrsApplication();

    @Input
    @Optional
    public abstract ListProperty<String> getScanningAcceptedPackages();

    @Input
    @Optional
    public abstract ListProperty<String> getExcludeClasses();

    @Input
    @Optional
    public abstract ListProperty<String> getExcludeClassPatterns();

    @Input
    @Optional
    public abstract ListProperty<String> getIncludePropertyAnnotations();

    @Input
    @Optional
    public abstract ListProperty<String> getExcludePropertyAnnotations();

    @Input
    public abstract Property<JsonLibrary> getJsonLibrary();

    @Input
    @Optional
    public abstract Property<Jackson2Configuration> getJackson2Configuration();

    @Input
    @Optional
    public abstract Property<Jackson3Configuration> getJackson3Configuration();

    @Input
    @Optional
    public abstract Property<GsonConfiguration> getGsonConfiguration();

    @Input
    @Optional
    public abstract Property<JsonbConfiguration> getJsonbConfiguration();

    @Input
    @Optional
    public abstract ListProperty<String> getAdditionalDataLibraries();

    @Input
    @Optional
    public abstract Property<OptionalProperties> getOptionalProperties();

    @Input
    @Optional
    public abstract Property<OptionalPropertiesDeclaration> getOptionalPropertiesDeclaration();

    @Input
    @Optional
    public abstract Property<NullabilityDefinition> getNullabilityDefinition();

    @Input
    @Optional
    public abstract Property<Boolean> getDeclarePropertiesAsReadOnly();

    @Input
    @Optional
    public abstract Property<String> getRemoveTypeNamePrefix();

    @Input
    @Optional
    public abstract Property<String> getRemoveTypeNameSuffix();

    @Input
    @Optional
    public abstract Property<String> getAddTypeNamePrefix();

    @Input
    @Optional
    public abstract Property<String> getAddTypeNameSuffix();

    @Input
    @Optional
    public abstract ListProperty<String> getCustomTypeNaming();

    @Input
    @Optional
    public abstract Property<String> getCustomTypeNamingFunction();

    @Input
    @Optional
    public abstract ListProperty<String> getReferencedFiles();

    @Input
    @Optional
    public abstract ListProperty<String> getImportDeclarations();

    @Input
    @Optional
    public abstract ListProperty<String> getCustomTypeMappings();

    @Input
    @Optional
    public abstract ListProperty<String> getCustomTypeAliases();

    @Input
    @Optional
    public abstract Property<DateMapping> getMapDate();

    @Input
    @Optional
    public abstract Property<MapMapping> getMapMap();

    @Input
    @Optional
    public abstract Property<EnumMapping> getMapEnum();

    @Input
    @Optional
    public abstract Property<IdentifierCasing> getEnumMemberCasing();

    @Input
    @Optional
    public abstract Property<Boolean> getNonConstEnums();

    @Input
    @Optional
    public abstract ListProperty<String> getNonConstEnumAnnotations();

    @Input
    @Optional
    public abstract Property<ClassMapping> getMapClasses();

    @Input
    @Optional
    public abstract ListProperty<String> getMapClassesAsClassesPatterns();

    @Input
    @Optional
    public abstract Property<Boolean> getGenerateConstructors();

    @Input
    @Optional
    public abstract ListProperty<String> getDisableTaggedUnionAnnotations();

    @Input
    @Optional
    public abstract Property<Boolean> getDisableTaggedUnions();

    @Input
    @Optional
    public abstract Property<Boolean> getGenerateReadonlyAndWriteonlyJSDocTags();

    @Input
    @Optional
    public abstract Property<Boolean> getIgnoreSwaggerAnnotations();

    @Input
    @Optional
    public abstract Property<Boolean> getGenerateJaxrsApplicationInterface();

    @Input
    @Optional
    public abstract Property<Boolean> getGenerateJaxrsApplicationClient();

    @Input
    @Optional
    public abstract Property<Boolean> getGenerateSpringApplicationInterface();

    @Input
    @Optional
    public abstract Property<Boolean> getGenerateSpringApplicationClient();

    @Input
    @Optional
    public abstract Property<Boolean> getScanSpringApplication();

    @Input
    @Optional
    public abstract Property<RestNamespacing> getRestNamespacing();

    @Input
    @Optional
    public abstract Property<String> getRestNamespacingAnnotation();

    @Input
    @Optional
    public abstract Property<String> getRestResponseType();

    @Input
    @Optional
    public abstract Property<String> getRestOptionsType();

    @Input
    @Optional
    public abstract Property<String> getCustomTypeProcessor();

    @Input
    @Optional
    public abstract Property<Boolean> getSortDeclarations();

    @Input
    @Optional
    public abstract Property<Boolean> getSortTypeDeclarations();

    @Input
    @Optional
    public abstract Property<Boolean> getNoFileComment();

    @Input
    @Optional
    public abstract Property<Boolean> getNoTslintDisable();

    @Input
    @Optional
    public abstract Property<Boolean> getNoEslintDisable();

    @Input
    @Optional
    public abstract Property<Boolean> getTsNoCheck();

    @InputFiles
    @Optional
    public abstract ConfigurableFileCollection getJavadocXmlFiles();

    @Input
    @Optional
    public abstract ListProperty<String> getExtensionClasses();

    @Input
    @Optional
    public abstract ListProperty<String> getExtensionsList();

    @Input
    @Optional
    public abstract ListProperty<Settings.ConfiguredExtension> getExtensionsWithConfiguration();

    @Input
    @Optional
    public abstract ListProperty<String> getOptionalAnnotations();

    @Input
    @Optional
    public abstract ListProperty<String> getRequiredAnnotations();

    @Input
    @Optional
    public abstract ListProperty<String> getNullableAnnotations();

    @Input
    @Optional
    public abstract Property<Boolean> getPrimitivePropertiesRequired();

    @Input
    @Optional
    public abstract Property<Boolean> getGenerateInfoJson();

    @Input
    @Optional
    public abstract Property<Boolean> getGenerateNpmPackageJson();

    @Input
    @Optional
    public abstract Property<String> getNpmName();

    @Input
    @Optional
    public abstract Property<String> getNpmVersion();

    @Input
    @Optional
    public abstract Property<String> getNpmTypescriptVersion();

    @Input
    @Optional
    public abstract Property<String> getNpmBuildScript();

    @Input
    @Optional
    public abstract ListProperty<String> getNpmDependencies();

    @Input
    @Optional
    public abstract ListProperty<String> getNpmDevDependencies();

    @Input
    @Optional
    public abstract ListProperty<String> getNpmPeerDependencies();

    @Input
    @Optional
    public abstract Property<StringQuotes> getStringQuotes();

    @Input
    @Optional
    public abstract Property<String> getIndentString();

    @Input
    @Optional
    public abstract Property<Boolean> getJackson2ModuleDiscovery();

    @Input
    @Optional
    public abstract Property<Boolean> getJackson3ModuleDiscovery();

    @Input
    @Optional
    public abstract ListProperty<String> getJackson2Modules();

    @Input
    @Optional
    public abstract ListProperty<String> getJackson3Modules();

    @Input
    @Optional
    public abstract Property<Logger.Level> getLoggingLevel();

    @OutputFile
    public abstract RegularFileProperty getOutputFileProperty();

    @TaskAction
    public void generate() throws Exception {
        Logger.Level logLevel = getLoggingLevel().getOrElse(Logger.Level.Info);
        TypeScriptGenerator.setLogger(new Logger(logLevel));
        TypeScriptGenerator.printVersion();

        // Create class loader from classpath
        final List<URL> urls = new ArrayList<>();
        for (File file : getClasspath().getFiles()) {
            urls.add(file.toURI().toURL());
        }

        try (
            URLClassLoader classLoader = Settings.createClassLoader(getProjectName().get(), urls.toArray(new URL[0]),
                Thread.currentThread().getContextClassLoader())
        ) {
            final Settings settings = createSettings(classLoader);

            final cz.habarta.typescript.generator.Input.Parameters parameters = new cz.habarta.typescript.generator.Input.Parameters();
            parameters.classNames = getClasses().getOrElse(Collections.emptyList());
            parameters.classNamePatterns = getClassPatterns().getOrElse(Collections.emptyList());
            parameters.classesWithAnnotations = getClassesWithAnnotations().getOrElse(Collections.emptyList());
            parameters.classesImplementingInterfaces = getClassesImplementingInterfaces()
                .getOrElse(Collections.emptyList());
            parameters.classesExtendingClasses = getClassesExtendingClasses().getOrElse(Collections.emptyList());
            parameters.jaxrsApplicationClassName = getClassesFromJaxrsApplication().getOrNull();
            parameters.automaticJaxrsApplication = getClassesFromAutomaticJaxrsApplication().getOrElse(false);
            parameters.isClassNameExcluded = settings.getExcludeFilter();
            parameters.classLoader = classLoader;
            parameters.scanningAcceptedPackages = getScanningAcceptedPackages().getOrElse(Collections.emptyList());
            parameters.debug = logLevel == Logger.Level.Debug;

            final File output = getOutputFileProperty().getAsFile().get();
            settings.validateFileName(output);

            new TypeScriptGenerator(settings).generateTypeScript(cz.habarta.typescript.generator.Input.from(parameters),
                Output.to(output));
        }
    }

    private Settings createSettings(URLClassLoader classLoader) {
        final Settings settings = new Settings();

        if (getOutputFileType().isPresent()) {
            settings.outputFileType = getOutputFileType().get();
        }
        settings.outputKind = getOutputKind().get();
        settings.module = getModule().getOrNull();
        settings.namespace = getNamespace().getOrNull();
        settings.mapPackagesToNamespaces = getMapPackagesToNamespaces().getOrElse(false);
        settings.umdNamespace = getUmdNamespace().getOrNull();
        settings.moduleDependencies = getModuleDependencies().getOrNull();
        settings.setExcludeFilter(
            getExcludeClasses().getOrElse(Collections.emptyList()),
            getExcludeClassPatterns().getOrElse(Collections.emptyList()));
        settings.jsonLibrary = getJsonLibrary().get();
        settings.setJackson2Configuration(classLoader, getJackson2Configuration().getOrNull());
        settings.setJackson3Configuration(classLoader, getJackson3Configuration().getOrNull());
        settings.gsonConfiguration = getGsonConfiguration().getOrNull();
        settings.jsonbConfiguration = getJsonbConfiguration().getOrNull();
        settings.additionalDataLibraries = nullableList(getAdditionalDataLibraries());
        settings.optionalProperties = getOptionalProperties().getOrNull();
        settings.optionalPropertiesDeclaration = getOptionalPropertiesDeclaration().getOrNull();
        settings.nullabilityDefinition = getNullabilityDefinition().getOrNull();
        settings.declarePropertiesAsReadOnly = getDeclarePropertiesAsReadOnly().getOrElse(false);
        settings.removeTypeNamePrefix = getRemoveTypeNamePrefix().getOrNull();
        settings.removeTypeNameSuffix = getRemoveTypeNameSuffix().getOrNull();
        settings.addTypeNamePrefix = getAddTypeNamePrefix().getOrNull();
        settings.addTypeNameSuffix = getAddTypeNameSuffix().getOrNull();
        settings.customTypeNaming = Settings.convertToMap(getCustomTypeNaming().getOrElse(Collections.emptyList()),
            "customTypeNaming");
        settings.customTypeNamingFunction = getCustomTypeNamingFunction().getOrNull();
        settings.referencedFiles = nullableList(getReferencedFiles());
        settings.importDeclarations = nullableList(getImportDeclarations());
        settings.customTypeMappings = Settings.convertToMap(getCustomTypeMappings().getOrElse(Collections.emptyList()),
            "customTypeMapping");
        settings.customTypeAliases = Settings.convertToMap(getCustomTypeAliases().getOrElse(Collections.emptyList()),
            "customTypeAlias");
        settings.mapDate = getMapDate().getOrNull();
        settings.mapMap = getMapMap().getOrNull();
        settings.mapEnum = getMapEnum().getOrNull();
        settings.enumMemberCasing = getEnumMemberCasing().getOrNull();
        settings.nonConstEnums = getNonConstEnums().getOrElse(false);
        settings.loadNonConstEnumAnnotations(classLoader, nullableList(getNonConstEnumAnnotations()));
        settings.mapClasses = getMapClasses().getOrNull();
        settings.mapClassesAsClassesPatterns = nullableList(getMapClassesAsClassesPatterns());
        settings.generateConstructors = getGenerateConstructors().getOrElse(false);
        settings.loadDisableTaggedUnionAnnotations(classLoader, nullableList(getDisableTaggedUnionAnnotations()));
        settings.disableTaggedUnions = getDisableTaggedUnions().getOrElse(false);
        settings.generateReadonlyAndWriteonlyJSDocTags = getGenerateReadonlyAndWriteonlyJSDocTags().getOrElse(false);
        settings.ignoreSwaggerAnnotations = getIgnoreSwaggerAnnotations().getOrElse(false);
        settings.generateJaxrsApplicationInterface = getGenerateJaxrsApplicationInterface().getOrElse(false);
        settings.generateJaxrsApplicationClient = getGenerateJaxrsApplicationClient().getOrElse(false);
        settings.generateSpringApplicationInterface = getGenerateSpringApplicationInterface().getOrElse(false);
        settings.generateSpringApplicationClient = getGenerateSpringApplicationClient().getOrElse(false);
        settings.scanSpringApplication = getScanSpringApplication().getOrElse(false);
        settings.restNamespacing = getRestNamespacing().getOrNull();
        settings.setRestNamespacingAnnotation(classLoader, getRestNamespacingAnnotation().getOrNull());
        settings.restResponseType = getRestResponseType().getOrNull();
        settings.setRestOptionsType(getRestOptionsType().getOrNull());
        settings.loadCustomTypeProcessor(classLoader, getCustomTypeProcessor().getOrNull());
        settings.sortDeclarations = getSortDeclarations().getOrElse(false);
        settings.sortTypeDeclarations = getSortTypeDeclarations().getOrElse(false);
        settings.noFileComment = getNoFileComment().getOrElse(false);
        settings.noTslintDisable = getNoTslintDisable().getOrElse(false);
        settings.noEslintDisable = getNoEslintDisable().getOrElse(false);
        settings.tsNoCheck = getTsNoCheck().getOrElse(false);

        List<File> javadocFiles = new ArrayList<>();
        if (getJavadocXmlFiles().getFiles() != null) {
            javadocFiles.addAll(getJavadocXmlFiles().getFiles());
        }
        settings.javadocXmlFiles = javadocFiles;

        settings.loadExtensions(
            classLoader,
            Utils.concat(
                getExtensionClasses().getOrElse(Collections.emptyList()),
                getExtensionsList().getOrElse(Collections.emptyList())),
            getExtensionsWithConfiguration().getOrNull());
        settings.loadIncludePropertyAnnotations(classLoader, nullableList(getIncludePropertyAnnotations()));
        settings.loadExcludePropertyAnnotations(classLoader, nullableList(getExcludePropertyAnnotations()));
        settings.loadOptionalAnnotations(classLoader, nullableList(getOptionalAnnotations()));
        settings.loadRequiredAnnotations(classLoader, nullableList(getRequiredAnnotations()));
        settings.loadNullableAnnotations(classLoader, nullableList(getNullableAnnotations()));
        settings.primitivePropertiesRequired = getPrimitivePropertiesRequired().getOrElse(false);
        settings.generateInfoJson = getGenerateInfoJson().getOrElse(false);
        settings.generateNpmPackageJson = getGenerateNpmPackageJson().getOrElse(false);
        settings.npmName = getNpmName().isPresent() ? getNpmName().get()
            : (getGenerateNpmPackageJson().getOrElse(false) ? getProjectName().get() : null);
        settings.npmVersion = getNpmVersion().isPresent() ? getNpmVersion().get()
            : (getGenerateNpmPackageJson().getOrElse(false) ? settings.getDefaultNpmVersion() : null);
        settings.npmTypescriptVersion = getNpmTypescriptVersion().getOrNull();
        settings.npmBuildScript = getNpmBuildScript().getOrNull();
        settings.npmPackageDependencies = Settings.convertToMap(getNpmDependencies().getOrElse(Collections.emptyList()),
            "npmDependencies");
        settings.npmDevDependencies = Settings.convertToMap(getNpmDevDependencies().getOrElse(Collections.emptyList()),
            "npmDevDependencies");
        settings.npmPeerDependencies = Settings
            .convertToMap(getNpmPeerDependencies().getOrElse(Collections.emptyList()), "npmPeerDependencies");
        settings.setStringQuotes(getStringQuotes().getOrNull());
        settings.setIndentString(getIndentString().getOrNull());
        settings.jackson2ModuleDiscovery = getJackson2ModuleDiscovery().getOrElse(false);
        settings.jackson3ModuleDiscovery = getJackson3ModuleDiscovery().getOrElse(false);
        settings.loadJackson2Modules(classLoader, nullableList(getJackson2Modules()));
        settings.loadJackson3Modules(classLoader, nullableList(getJackson3Modules()));
        settings.classLoader = classLoader;

        return settings;
    }

    /**
     * @return null if the list is null or if the list is empty (common case for unspecified Gradle ListProperty-s)
     */
    private static List<String> nullableList(ListProperty<String> listProperty) {
        List<String> list = listProperty.getOrNull();
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list;
    }
}
