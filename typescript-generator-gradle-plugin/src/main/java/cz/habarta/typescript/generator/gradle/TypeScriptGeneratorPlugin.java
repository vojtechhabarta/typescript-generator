
package cz.habarta.typescript.generator.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;


/**
 * Gradle plugin for generating TypeScript files from Java classes.
 */
public class TypeScriptGeneratorPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // Create extension for user configuration
        TypeScriptGeneratorExtension extension = project.getExtensions().create(
            "generateTypeScript",
            TypeScriptGeneratorExtension.class);

        // Register the task using TaskProvider for lazy configuration
        TaskProvider<GenerateTask> generateTaskProvider = project.getTasks().register(
            "generateTypeScript",
            GenerateTask.class,
            task -> configureTask(project, task, extension));

        // Configure task dependencies when Java plugin is applied
        project.getPlugins().withId("java", plugin -> {
            // Make generateTypeScript depend on compileJava
            generateTaskProvider.configure(task -> {
                task.dependsOn(project.getTasks().named("compileJava"));
            });
        });

        project.getPlugins().withId("java-library", plugin -> {
            // Make generateTypeScript depend on compileJava
            generateTaskProvider.configure(task -> {
                task.dependsOn(project.getTasks().named("compileJava"));
            });
        });

        // Support for Kotlin
        project.getPlugins().withId("org.jetbrains.kotlin.jvm", plugin -> {
            generateTaskProvider.configure(task -> {
                task.dependsOn(project.getTasks().named("compileKotlin"));
            });
        });

        // Support for Scala
        project.getPlugins().withId("scala", plugin -> {
            generateTaskProvider.configure(task -> {
                task.dependsOn(project.getTasks().named("compileScala"));
            });
        });

        // Support for Groovy
        project.getPlugins().withId("groovy", plugin -> {
            generateTaskProvider.configure(task -> {
                task.dependsOn(project.getTasks().named("compileGroovy"));
            });
        });
    }

    private void configureTask(Project project, GenerateTask task, TypeScriptGeneratorExtension extension) {
        // Set task group and description
        task.setGroup("build");
        task.setDescription("Generates TypeScript declaration files from Java classes");

        // Configure project-level properties (these are safe to access during configuration time)
        task.getProjectName().set(project.getName());
        task.getBuildDirectory().set(project.getLayout().getBuildDirectory());

        // Configure classpath by collecting compile classpath and output directories
        ConfigurableFileCollection classpath = project.getObjects().fileCollection();

        // Add main source set output and classpath when Java plugin is applied
        project.getPlugins().withId("java", plugin -> {
            JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            SourceSetContainer sourceSets = javaExtension.getSourceSets();
            SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);

            // Add compiled classes directory
            classpath.from(mainSourceSet.getOutput().getClassesDirs());

            // Add compile classpath
            classpath.from(mainSourceSet.getCompileClasspath());
        });

        task.getClasspath().from(classpath);

        // Copy all properties from extension to task
        task.getOutputFile().set(extension.getOutputFile());
        task.getOutputFileType().set(extension.getOutputFileType());
        task.getOutputKind().set(extension.getOutputKind());
        task.getModule().set(extension.getModule());
        task.getNamespace().set(extension.getNamespace());
        task.getMapPackagesToNamespaces().set(extension.getMapPackagesToNamespaces());
        task.getUmdNamespace().set(extension.getUmdNamespace());
        task.getModuleDependencies().set(extension.getModuleDependencies());
        task.getClasses().set(extension.getClasses());
        task.getClassPatterns().set(extension.getClassPatterns());
        task.getClassesWithAnnotations().set(extension.getClassesWithAnnotations());
        task.getClassesImplementingInterfaces().set(extension.getClassesImplementingInterfaces());
        task.getClassesExtendingClasses().set(extension.getClassesExtendingClasses());
        task.getClassesFromJaxrsApplication().set(extension.getClassesFromJaxrsApplication());
        task.getClassesFromAutomaticJaxrsApplication().set(extension.getClassesFromAutomaticJaxrsApplication());
        task.getScanningAcceptedPackages().set(extension.getScanningAcceptedPackages());
        task.getExcludeClasses().set(extension.getExcludeClasses());
        task.getExcludeClassPatterns().set(extension.getExcludeClassPatterns());
        task.getIncludePropertyAnnotations().set(extension.getIncludePropertyAnnotations());
        task.getExcludePropertyAnnotations().set(extension.getExcludePropertyAnnotations());
        task.getJsonLibrary().set(extension.getJsonLibrary());
        task.getJackson2Configuration().set(extension.getJackson2Configuration());
        task.getJackson3Configuration().set(extension.getJackson3Configuration());
        task.getGsonConfiguration().set(extension.getGsonConfiguration());
        task.getJsonbConfiguration().set(extension.getJsonbConfiguration());
        task.getAdditionalDataLibraries().set(extension.getAdditionalDataLibraries());
        task.getOptionalProperties().set(extension.getOptionalProperties());
        task.getOptionalPropertiesDeclaration().set(extension.getOptionalPropertiesDeclaration());
        task.getNullabilityDefinition().set(extension.getNullabilityDefinition());
        task.getDeclarePropertiesAsReadOnly().set(extension.getDeclarePropertiesAsReadOnly());
        task.getRemoveTypeNamePrefix().set(extension.getRemoveTypeNamePrefix());
        task.getRemoveTypeNameSuffix().set(extension.getRemoveTypeNameSuffix());
        task.getAddTypeNamePrefix().set(extension.getAddTypeNamePrefix());
        task.getAddTypeNameSuffix().set(extension.getAddTypeNameSuffix());
        task.getCustomTypeNaming().set(extension.getCustomTypeNaming());
        task.getCustomTypeNamingFunction().set(extension.getCustomTypeNamingFunction());
        task.getReferencedFiles().set(extension.getReferencedFiles());
        task.getImportDeclarations().set(extension.getImportDeclarations());
        task.getCustomTypeMappings().set(extension.getCustomTypeMappings());
        task.getCustomTypeAliases().set(extension.getCustomTypeAliases());
        task.getMapDate().set(extension.getMapDate());
        task.getMapMap().set(extension.getMapMap());
        task.getMapEnum().set(extension.getMapEnum());
        task.getEnumMemberCasing().set(extension.getEnumMemberCasing());
        task.getNonConstEnums().set(extension.getNonConstEnums());
        task.getNonConstEnumAnnotations().set(extension.getNonConstEnumAnnotations());
        task.getMapClasses().set(extension.getMapClasses());
        task.getMapClassesAsClassesPatterns().set(extension.getMapClassesAsClassesPatterns());
        task.getGenerateConstructors().set(extension.getGenerateConstructors());
        task.getDisableTaggedUnionAnnotations().set(extension.getDisableTaggedUnionAnnotations());
        task.getDisableTaggedUnions().set(extension.getDisableTaggedUnions());
        task.getGenerateReadonlyAndWriteonlyJSDocTags().set(extension.getGenerateReadonlyAndWriteonlyJSDocTags());
        task.getIgnoreSwaggerAnnotations().set(extension.getIgnoreSwaggerAnnotations());
        task.getGenerateJaxrsApplicationInterface().set(extension.getGenerateJaxrsApplicationInterface());
        task.getGenerateJaxrsApplicationClient().set(extension.getGenerateJaxrsApplicationClient());
        task.getGenerateSpringApplicationInterface().set(extension.getGenerateSpringApplicationInterface());
        task.getGenerateSpringApplicationClient().set(extension.getGenerateSpringApplicationClient());
        task.getScanSpringApplication().set(extension.getScanSpringApplication());
        task.getRestNamespacing().set(extension.getRestNamespacing());
        task.getRestNamespacingAnnotation().set(extension.getRestNamespacingAnnotation());
        task.getRestResponseType().set(extension.getRestResponseType());
        task.getRestOptionsType().set(extension.getRestOptionsType());
        task.getCustomTypeProcessor().set(extension.getCustomTypeProcessor());
        task.getSortDeclarations().set(extension.getSortDeclarations());
        task.getSortTypeDeclarations().set(extension.getSortTypeDeclarations());
        task.getNoFileComment().set(extension.getNoFileComment());
        task.getNoTslintDisable().set(extension.getNoTslintDisable());
        task.getNoEslintDisable().set(extension.getNoEslintDisable());
        task.getTsNoCheck().set(extension.getTsNoCheck());
        task.getJavadocXmlFiles().from(extension.getJavadocXmlFiles());
        task.getExtensionClasses().set(extension.getExtensionClasses());
        task.getExtensionsList().set(extension.getExtensionsList());
        task.getExtensionsWithConfiguration().set(extension.getExtensionsWithConfiguration());
        task.getOptionalAnnotations().set(extension.getOptionalAnnotations());
        task.getRequiredAnnotations().set(extension.getRequiredAnnotations());
        task.getNullableAnnotations().set(extension.getNullableAnnotations());
        task.getPrimitivePropertiesRequired().set(extension.getPrimitivePropertiesRequired());
        task.getGenerateInfoJson().set(extension.getGenerateInfoJson());
        task.getGenerateNpmPackageJson().set(extension.getGenerateNpmPackageJson());
        task.getNpmName().set(extension.getNpmName());
        task.getNpmVersion().set(extension.getNpmVersion());
        task.getNpmTypescriptVersion().set(extension.getNpmTypescriptVersion());
        task.getNpmBuildScript().set(extension.getNpmBuildScript());
        task.getNpmDependencies().set(extension.getNpmDependencies());
        task.getNpmDevDependencies().set(extension.getNpmDevDependencies());
        task.getNpmPeerDependencies().set(extension.getNpmPeerDependencies());
        task.getStringQuotes().set(extension.getStringQuotes());
        task.getIndentString().set(extension.getIndentString());
        task.getJackson2ModuleDiscovery().set(extension.getJackson2ModuleDiscovery());
        task.getJackson3ModuleDiscovery().set(extension.getJackson3ModuleDiscovery());
        task.getJackson2Modules().set(extension.getJackson2Modules());
        task.getJackson3Modules().set(extension.getJackson3Modules());
        task.getLoggingLevel().set(extension.getLoggingLevel());
    }
}
