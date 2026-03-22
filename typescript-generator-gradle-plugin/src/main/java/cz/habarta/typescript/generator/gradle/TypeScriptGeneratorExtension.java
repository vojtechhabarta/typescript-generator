
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
import cz.habarta.typescript.generator.RestNamespacing;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.StringQuotes;
import cz.habarta.typescript.generator.TypeScriptFileType;
import cz.habarta.typescript.generator.TypeScriptOutputKind;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import java.io.File;


/**
 * Extension for configuring the TypeScript generator plugin.
 * All properties use Gradle's Property API for Configuration Cache compatibility.
 */
public abstract class TypeScriptGeneratorExtension {

    @Input
    @Optional
    public abstract Property<String> getOutputFile();

    @Input
    @Optional
    public abstract Property<TypeScriptFileType> getOutputFileType();

    @Input
    @Optional
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
    @Optional
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

    @Input
    @Optional
    public abstract ListProperty<File> getJavadocXmlFiles();

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

    public TypeScriptGeneratorExtension() {
        // Set default values
        getMapPackagesToNamespaces().convention(false);
        getClassesFromAutomaticJaxrsApplication().convention(false);
        getDeclarePropertiesAsReadOnly().convention(false);
        getNonConstEnums().convention(false);
        getGenerateConstructors().convention(false);
        getDisableTaggedUnions().convention(false);
        getGenerateReadonlyAndWriteonlyJSDocTags().convention(false);
        getIgnoreSwaggerAnnotations().convention(false);
        getGenerateJaxrsApplicationInterface().convention(false);
        getGenerateJaxrsApplicationClient().convention(false);
        getGenerateSpringApplicationInterface().convention(false);
        getGenerateSpringApplicationClient().convention(false);
        getScanSpringApplication().convention(false);
        getSortDeclarations().convention(false);
        getSortTypeDeclarations().convention(false);
        getNoFileComment().convention(false);
        getNoTslintDisable().convention(false);
        getNoEslintDisable().convention(false);
        getTsNoCheck().convention(false);
        getPrimitivePropertiesRequired().convention(false);
        getGenerateInfoJson().convention(false);
        getGenerateNpmPackageJson().convention(false);
        getJackson2ModuleDiscovery().convention(false);
        getJackson3ModuleDiscovery().convention(false);
        getLoggingLevel().convention(Logger.Level.Info);
    }
}
