
package cz.habarta.typescript.generator.maven;

import cz.habarta.typescript.generator.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.maven.artifact.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

/**
 * Generates TypeScript declaration file from specified java classes.
 * For more information see README and Wiki on GitHub.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateMojo extends AbstractMojo {

    /**
     * Output TypeScript declaration file.
     */
    @Parameter(required = true)
    private File outputFile;

    /**
     * JSON classes to process.
     */
    @Parameter
    private List<String> classes;

    /**
     * Scans specified JAX-RS {@link javax.ws.rs.core.Application} for JSON classes to process.
     * Parameter contains fully-qualified class name.
     * It is possible to exclude particular REST resource classes using {@link #excludeClasses} parameter.
     */
    @Parameter
    private String classesFromJaxrsApplication;

    /**
     * List of classes excluded from processing.
     */
    @Parameter
    private List<String> excludeClasses;

    /**
     * Library used in JSON classes.
     * Supported values are 'jackson1', 'jackson2'.
     * Default value is 'jackson1'.
     */
    @Parameter
    private JsonLibrary jsonLibrary;

    /**
     * Deprecated. Use "namespace" parameter.
     * @deprecated Use {@link #namespace} instead.
     */
    @Parameter
    @Deprecated
    private String moduleName;

    /**
     * TypeScript namespace (previously called "internal module") of generated declarations.
     */
    @Parameter
    private String namespace;

    /**
     * TypeScript module name (previously called "external module") of generated declarations.
     */
    @Parameter
    private String module;

    /**
     * If true declared properties will be optional.
     */
    @Parameter
    private boolean declarePropertiesAsOptional;

    /**
     * Prefix which will be removed from names of classes, interfaces, enums.
     * For example if set to "Json" then mapping for "JsonData" will be "Data".
     */
    @Parameter
    private String removeTypeNamePrefix;

    /**
     * Suffix which will be removed from names of classes, interfaces, enums.
     * For example if set to "JSON" then mapping for "DataJSON" will be "Data".
     */
    @Parameter
    private String removeTypeNameSuffix;

    /**
     * Prefix which will be added to names of classes, interfaces, enums.
     * For example if set to "I" then mapping for "Data" will be "IData".
     */
    @Parameter
    private String addTypeNamePrefix;

    /**
     * Suffix which will be added to names of classes, interfaces, enums.
     * For example if set to "Data" then mapping for "Person" will be "PersonData".
     */
    @Parameter
    private String addTypeNameSuffix;

    /**
     * Specifies how {@link java.util.Date} will be mapped.
     * Supported values are 'asDate', 'asNumber, 'asString'.
     * Default value is 'asDate'.
     */
    @Parameter
    private DateMapping mapDate;

    /**
     * Specifies custom class implementing {@link cz.habarta.typescript.generator.TypeProcessor}.
     * This allows to customize how Java types are mapped to TypeScript.
     * For example it is possible to implement TypeProcessor
     * for {@link com.google.common.base.Optional} from guava or for Java 8 date/time classes.
     */
    @Parameter
    private String customTypeProcessor;

    /**
     * If true TypeScript declarations (interfaces, properties) will be sorted alphabetically.
     */
    @Parameter
    private boolean sortDeclarations;

    /**
     * If true TypeScript type declarations (interfaces) will be sorted alphabetically.
     */
    @Parameter
    private boolean sortTypeDeclarations;

    /**
     * If true generated file will not contain comment at the top.
     * By default there is a comment with timestamp and typescript-generator version.
     * So it might be useful to suppress this comment if the file is in source control and is regenerated in build.
     */
    @Parameter
    private boolean noFileComment;


    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() {
        try {
            TypeScriptGenerator.printVersion();

            // class loader
            final List<URL> urls = new ArrayList<>();
            for (String element : project.getCompileClasspathElements()) {
                urls.add(new File(element).toURI().toURL());
            }
            final URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());

            // Settings
            final Settings settings = new Settings();
            settings.excludedClassNames = excludeClasses;
            settings.jsonLibrary = jsonLibrary;
            settings.namespace = namespace != null ? namespace : moduleName;
            settings.module = module;
            settings.declarePropertiesAsOptional = declarePropertiesAsOptional;
            settings.removeTypeNamePrefix = removeTypeNamePrefix;
            settings.removeTypeNameSuffix = removeTypeNameSuffix;
            settings.addTypeNamePrefix = addTypeNamePrefix;
            settings.addTypeNameSuffix = addTypeNameSuffix;
            settings.mapDate = mapDate;
            if (customTypeProcessor != null) {
                settings.customTypeProcessor = (TypeProcessor) classLoader.loadClass(customTypeProcessor).newInstance();
            }
            settings.sortDeclarations = sortDeclarations;
            settings.sortTypeDeclarations = sortTypeDeclarations;
            settings.noFileComment = noFileComment;

            // TypeScriptGenerator
            new TypeScriptGenerator(settings).generateTypeScript(
                    Input.fromClassNamesAndJaxrsApplication(classes, classesFromJaxrsApplication, excludeClasses, classLoader),
                    Output.to(outputFile)
            );

        } catch (DependencyResolutionRequiredException | ReflectiveOperationException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
