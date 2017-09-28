
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
     * Path and name of generated TypeScript file.
     */
    @Parameter
    private File outputFile;

    /**
     * Output file format, can be 'declarationFile' (.d.ts) or 'implementationFile' (.ts).
     * Setting this parameter to 'implementationFile' allows to generate runnable TypeScript code.
     * Default value is 'declarationFile'.
     */
    @Parameter
    private TypeScriptFileType outputFileType;

    /**
     * Kind of generated TypeScript output, allowed values are 'global', 'module' or 'ambientModule'.
     * Value 'global' means that declarations will be in global scope or namespace (no module).
     * Value 'module' means that generated file will contain top-level 'export' declarations.
     * Value 'ambientModule' means that generated declarations will be wrapped in 'declare module "mod" { }' declaration.
     * Required parameter.
     * For more information see Wiki page 'http://vojtechhabarta.github.io/typescript-generator/doc/ModulesAndNamespaces.html'.
     */
    @Parameter(required = true)
    private TypeScriptOutputKind outputKind;

    /**
     * Name of generated ambient module.
     * Used when 'outputKind' is set to 'ambientModule'.
     */
    @Parameter
    private String module;

    /**
     * Generates specified namespace. Not recommended to combine with modules. Default is no namespace.
     */
    @Parameter
    private String namespace;

    /**
     * Generates TypeScript namespaces from Java packages. Default is false.
     */
    @Parameter
    private boolean mapPackagesToNamespaces;

    /**
     * Turns proper module into UMD (Universal Module Definition) with specified namespace.
     * Only applicable to declaration files.
     */
    @Parameter
    private String umdNamespace;

    /**
     * JSON classes to process.
     */
    @Parameter
    private List<String> classes;

    /**
     * JSON classes to process specified using glob patterns
     * so it is possible to specify package or class name suffix.
     * Glob patterns support two wildcards:
     * Single "*" wildcard matches any character except for "." and "$".
     * Double "**" wildcard matches any character.
     * For more information and examples see Wiki page 'https://github.com/vojtechhabarta/typescript-generator/wiki/Class-Names-Glob-Patterns'.
     */
    @Parameter
    private List<String> classPatterns;

    /**
     * Scans specified JAX-RS {@link javax.ws.rs.core.Application} for JSON classes to process.
     * Parameter contains fully-qualified class name.
     * It is possible to exclude particular REST resource classes using {@link #excludeClasses} parameter.
     */
    @Parameter
    private String classesFromJaxrsApplication;

    /**
     * Scans JAX-RS resources for JSON classes to process.
     * It is possible to exclude particular REST resource classes using {@link #excludeClasses} parameter.
     */
    @Parameter
    private boolean classesFromAutomaticJaxrsApplication;

    /**
     * List of classes excluded from processing.
     */
    @Parameter
    private List<String> excludeClasses;

    /**
     * Excluded classes specified using glob patterns.
     */
    @Parameter
    private List<String> excludeClassPatterns;

    /**
     * If this list is not empty then TypeScript will only be generated for
     * methods with one of the annotations defined in this list
     */
    @Parameter
    private List<String> includePropertyAnnotations;

    /**
     * Library used in JSON classes.
     * Supported values are
     * 'jackson1' (annotations from 'org.codehaus.jackson.annotate' package),
     * 'jackson2' (annotations from 'com.fasterxml.jackson.annotation' package),
     * 'jaxb' (annotations from 'javax.xml.bind.annotation' package).
     * Required parameter, recommended value is 'jackson2'.
     */
    @Parameter(required = true)
    private JsonLibrary jsonLibrary;

    /**
     * Deprecated, use <code>optionalProperties</code> parameter.
     */
    @Deprecated
    @Parameter
    private boolean declarePropertiesAsOptional;

    /**
     * Specifies how properties are defined to be optional.
     * Supported values are:
     * <ul>
     * <li><code>useSpecifiedAnnotations</code> - annotations specified using <code>optionalAnnotations</code> parameter</li>
     * <li><code>useLibraryDefinition</code> - examples: <code>@JsonProperty(required = false)</code> when using <code>jackson2</code> library
     *   or <code>@XmlElement(required = false)</code> when using <code>jaxb</code> library</li>
     * <li><code>all</code> - all properties are optional</li>
     * </ul>
     * Default value is <code>useSpecifiedAnnotations</code>.
     */
    @Parameter
    private OptionalProperties optionalProperties;

    /**
     * If true declared properties will be <code>readonly</code>.
     */
    @Parameter
    private boolean declarePropertiesAsReadOnly;

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
     * Specifies custom TypeScript names for Java classes.
     * Multiple mappings can be specified, each using this format: "javaClassName:typescriptName".
     * This takes precedence over other naming settings.
     */
    @Parameter
    private List<String> customTypeNaming;

    /**
     * Specifies JavaScript function for getting custom TypeScript names for Java classes.
     * Function can return undefined if default name should be used.
     * Function signature: <code>function getName(className: string, classSimpleName: string): string | null | undefined;</code>
     * Example function: <code>function(name, simpleName) { if (name.startsWith('cz.')) return 'Test' + simpleName; }</code>
     */
    @Parameter
    private String customTypeNamingFunction;

    /**
     * List of files which will be referenced using triple-slash directive: /// &lt;reference path="file" />.
     * This can be used with "customTypeMappings" to provide needed TypeScript types.
     */
    @Parameter
    private List<String> referencedFiles;

    /**
     * List of import declarations which will be added to generated output.
     * This can be used with "customTypeMappings" to provide needed TypeScript types.
     */
    @Parameter
    private List<String> importDeclarations;

    /**
     * List of custom mappings.
     * Each item specifies TypeScript type which will be used for particular Java class.
     * Item format is: "javaClass:typescriptType".
     * For example mapping "ZonedDateTime" to "string" would be added as "java.time.ZonedDateTime:string".
     */
    @Parameter
    private List<String> customTypeMappings;

    /**
     * Specifies how {@link java.util.Date} will be mapped.
     * Supported values are 'asDate', 'asNumber', 'asString'.
     * Default value is 'asDate'.
     */
    @Parameter
    private DateMapping mapDate;

    /**
     * Specifies how enums will be mapped.
     * Supported values are 'asUnion', 'asInlineUnion', 'asEnum', 'asNumberBasedEnum'.
     * Default value is 'asUnion'.
     * Value 'asUnion' creates type alias to union of string enum values.
     * Value 'asInlineUnion' creates union of enum values on places where the enum is used.
     * Value 'asEnum' creates string enum. Requires TypeScript 2.4.
     * Value 'asNumberBasedEnum' creates enum of named number values.
     */
    @Parameter
    private EnumMapping mapEnum;

    /**
     * If true generated enums will not have <code>const</code> keyword.
     * This can be used only in implementation files.
     */
    @Parameter
    private boolean nonConstEnums;

    /**
     * Specifies whether classes will be mapped to classes or interfaces.
     * Supported values are 'asInterfaces', 'asClasses'.
     * Default value is 'asInterfaces'.
     * Value 'asClasses' can only be used in implementation files (.ts).
     */
    @Parameter
    private ClassMapping mapClasses;

    /**
     * If true tagged unions will not be generated for Jackson 2 polymorphic types.
     */
    @Parameter
    private boolean disableTaggedUnions;

    /**
     * If true Swagger annotations will not be used.
     */
    @Parameter
    private boolean ignoreSwaggerAnnotations;

    /**
     * If true interface for JAX-RS REST application will be generated.
     */
    @Parameter
    private boolean generateJaxrsApplicationInterface;

    /**
     * If true client for JAX-RS REST application will be generated.
     */
    @Parameter
    private boolean generateJaxrsApplicationClient;

    /**
     * Specifies how JAX-RS REST operations will be grouped into objects.
     * Supported values are 'singleObject', 'perResource', 'byAnnotation'.
     * Default value is 'singleObject'.
     * Value 'singleObject' means that one object with all operations will be generated.
     * Value 'perResource' means that for each root resource one object will be generated.
     * Value 'byAnnotation' means that operations will be grouped by annotation specified using <code>jaxrsNamespacingAnnotation</code>.
     */
    @Parameter
    private JaxrsNamespacing jaxrsNamespacing;

    /**
     * Specifies annotation used for grouping JAX-RS REST operations.
     * Format is <code>annotationClass#annotationElement</code> where
     * annotationClass is fully-qualified class name and annotationElement is element name and defaults to 'value'.
     * Examples:
     * <code>io.swagger.annotations.Api</code>,
     * <code>io.swagger.annotations.Api#value</code>
     */
    @Parameter
    private String jaxrsNamespacingAnnotation;

    /**
     * Specifies HTTP response type in JAXRS application.
     * Default value is <code>Promise&lt;R></code> which means data object returned asynchronously.
     * This parameter is useful for example when underlying HTTP response object (like <code>XMLHttpRequest</code> or <code>AxiosPromise</code>)
     * is returned instead of actual response data.
     */
    @Parameter
    private String restResponseType;

    /**
     * Specifies HTTP request options type in JAXRS application.
     * By default no <code>options</code> parameter is generated.
     * Useful when passing additional parameters to underlying HTTP request method (like jQuery ajax settings or <code>AxiosRequestConfig</code>).
     * Can be specific (for example <code>AxiosRequestConfig</code>) or generic (for example <code>&ltO></code>).
     */
    @Parameter
    private String restOptionsType;

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

    /**
     * List of Javadoc XML files to search for documentation comments.
     * These files should be created using "com.github.markusbernhardt.xmldoclet.XmlDoclet" (com.github.markusbernhardt:xml-doclet).
     * Javadoc comments are added to output declarations as JSDoc comments.
     * For more information see Wiki page 'https://github.com/vojtechhabarta/typescript-generator/wiki/Javadoc'.
     */
    @Parameter
    private List<File> javadocXmlFiles;

    /**
     * List of extensions specified as fully qualified class name.
     * Known extensions:
     * cz.habarta.typescript.generator.ext.TypeGuardsForJackson2PolymorphismExtension
     */
    @Parameter
    private List<String> extensions;

    /**
     * The presence of any annotation in this list on a JSON property will cause
     * the typescript-generator to treat that property as optional when generating
     * the corresponding TypeScript interface.
     * Example optional annotation: @javax.annotation.Nullable
     */
    @Parameter
    private List<String> optionalAnnotations;

    /**
     * If true NPM package.json will be generated.
     * Only applicable when 'outputKind' is set to 'module'.
     * NPM package name and version can be specified using 'npmName' and 'npmVersion' parameters.
     */
    @Parameter
    private boolean generateNpmPackageJson;

    /**
     * Specifies NPM package name.
     * Only applicable when 'generateNpmPackageJson' parameter is 'true'.
     * Default value is <code>${project.artifactId}</code>.
     */
    @Parameter
    private String npmName;

    /**
     * Specifies NPM package version.
     * Only applicable when 'generateNpmPackageJson' parameter is 'true'.
     * Default value is <code>1.0.0</code>.
     */
    @Parameter
    private String npmVersion;

    /**
     * Specifies how strings will be quoted.
     * Supported values are 'doubleQuotes', 'singleQuotes'.
     * Default value is 'doubleQuotes'.
     */
    @Parameter
    private StringQuotes stringQuotes;

    /**
     * Display warnings when bean serializer is not found.
     */
    @Parameter(defaultValue = "true")
    private boolean displaySerializerWarning;

    /**
     * Deprecated, see <code>jackson2ModuleDiscovery</code> and <code>jackson2Modules</code> parameters.
     */
    @Deprecated
    @Parameter
    private boolean disableJackson2ModuleDiscovery;

    /**
     * Turns on Jackson2 automatic module discovery.
     */
    @Parameter
    private boolean jackson2ModuleDiscovery;

    /**
     * Specifies Jackson2 modules to use.
     */
    @Parameter
    private List<String> jackson2Modules;

    /**
     * Turns on verbose output for debugging purposes.
     */
    @Parameter
    private boolean debug;


    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
    private String projectBuildDirectory;

    @Override
    public void execute() {
        try {
            TypeScriptGenerator.printVersion();

            // class loader
            final List<URL> urls = new ArrayList<>();
            for (String element : project.getCompileClasspathElements()) {
                urls.add(new File(element).toURI().toURL());
            }
            final URLClassLoader classLoader = Settings.createClassLoader(project.getArtifactId(), urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());

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
            settings.setExcludeFilter(excludeClasses, excludeClassPatterns);
            settings.jsonLibrary = jsonLibrary;
            settings.declarePropertiesAsOptional = declarePropertiesAsOptional;
            settings.optionalProperties = optionalProperties;
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
            settings.mapClasses = mapClasses;
            settings.disableTaggedUnions = disableTaggedUnions;
            settings.ignoreSwaggerAnnotations = ignoreSwaggerAnnotations;
            settings.generateJaxrsApplicationInterface = generateJaxrsApplicationInterface;
            settings.generateJaxrsApplicationClient = generateJaxrsApplicationClient;
            settings.jaxrsNamespacing = jaxrsNamespacing;
            settings.setJaxrsNamespacingAnnotation(classLoader, jaxrsNamespacingAnnotation);
            settings.restResponseType = restResponseType;
            settings.setRestOptionsType(restOptionsType);
            settings.loadCustomTypeProcessor(classLoader, customTypeProcessor);
            settings.sortDeclarations = sortDeclarations;
            settings.sortTypeDeclarations = sortTypeDeclarations;
            settings.noFileComment = noFileComment;
            settings.javadocXmlFiles = javadocXmlFiles;
            settings.loadExtensions(classLoader, extensions);
            settings.loadIncludePropertyAnnotations(classLoader, includePropertyAnnotations);
            settings.loadOptionalAnnotations(classLoader, optionalAnnotations);
            settings.generateNpmPackageJson = generateNpmPackageJson;
            settings.npmName = npmName == null && generateNpmPackageJson ? project.getArtifactId() : npmName;
            settings.npmVersion = npmVersion == null && generateNpmPackageJson ? settings.getDefaultNpmVersion() : npmVersion;
            settings.setStringQuotes(stringQuotes);
            settings.displaySerializerWarning = displaySerializerWarning;
            settings.disableJackson2ModuleDiscovery = disableJackson2ModuleDiscovery;
            settings.jackson2ModuleDiscovery = jackson2ModuleDiscovery;
            settings.loadJackson2Modules(classLoader, jackson2Modules);
            settings.classLoader = classLoader;
            final File output = outputFile != null
                    ? outputFile
                    : new File(new File(projectBuildDirectory, "typescript-generator"), project.getArtifactId() + settings.getExtension());
            settings.validateFileName(output);

            // TypeScriptGenerator
            new TypeScriptGenerator(settings).generateTypeScript(
                    Input.fromClassNamesAndJaxrsApplication(classes, classPatterns, classesFromJaxrsApplication, classesFromAutomaticJaxrsApplication, settings.getExcludeFilter(), classLoader, debug),
                    Output.to(output)
            );

        } catch (DependencyResolutionRequiredException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
