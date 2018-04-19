
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
     * Output file format, can be:
     * <ul>
     * <li><code>declarationFile</code> (.d.ts)</li>
     * <li><code>implementationFile</code> (.ts)</li>
     * </ul>
     * Setting this parameter to <code>implementationFile</code> allows to generate runnable TypeScript code.<br>
     * Default value is <code>declarationFile</code>.
     */
    @Parameter
    private TypeScriptFileType outputFileType;

    /**
     * Kind of generated TypeScript output. Allowed values are:
     * <ul>
     * <li><code>global</code> - means that declarations will be in global scope or namespace (no module)</li>
     * <li><code>module</code> - means that generated file will contain top-level <code>export</code> declarations</li>
     * <li><code>ambientModule</code> - means that generated declarations will be wrapped in <code>declare module "mod" { }</code> declaration</li>
     * </ul>
     * Required parameter.
     * For more information see <a href="http://vojtechhabarta.github.io/typescript-generator/doc/ModulesAndNamespaces.html">Modules and Namespaces</a> Wiki page.
     */
    @Parameter(required = true)
    public TypeScriptOutputKind outputKind;

    /**
     * Name of generated ambient module.<br>
     * Used when {@link #outputKind} is set to <code>ambientModule</code>.
     */
    @Parameter
    private String module;

    /**
     * Generates specified namespace.<br>
     * Not recommended to combine with modules.<br>
     * Default is no namespace.
     */
    @Parameter
    private String namespace;

    /**
     * Generates TypeScript namespaces from Java packages. Default is <code>false</code>.
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
     * <ul>
     * <li>Single <code>*</code> wildcard matches any character except for <code>.</code> and <code>$</code>.</li>
     * <li>Double <code>**</code> wildcard matches any character.</li>
     * </ul>
     * For more information and examples see <a href="https://github.com/vojtechhabarta/typescript-generator/wiki/Class-Names-Glob-Patterns">Class Names Glob Patterns</a> Wiki page.
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
     * For more information and examples see <a href="https://github.com/vojtechhabarta/typescript-generator/wiki/Class-Names-Glob-Patterns">Class Names Glob Patterns</a> Wiki page.
     */
    @Parameter
    private List<String> excludeClassPatterns;

    /**
     * If this list is not empty then TypeScript will only be generated for
     * methods with one of the annotations defined in this list.
     */
    @Parameter
    private List<String> includePropertyAnnotations;

    /**
     * Library used in JSON classes.
     * Supported values are:
     * <ul>
     * <li><code>jackson1</code> - annotations from `org.codehaus.jackson.annotate` package</li>
     * <li><code>jackson2</code> - annotations from `com.fasterxml.jackson.annotation` package</li>
     * <li><code>jaxb</code> - annotations from `javax.xml.bind.annotation` package<li>
     * </ul>
     * Required parameter, recommended value is <code>jackson2</code>.
     */
    @Parameter(required = true)
    private JsonLibrary jsonLibrary;

    /**
     * <b>Deprecated</b>, use {@link #optionalProperties} parameter.
     */
    @Deprecated
    @Parameter
    private boolean declarePropertiesAsOptional;

    /**
     * Specifies how properties are defined to be optional.
     * Supported values are:
     * <ul>
     * <li><code>useSpecifiedAnnotations</code> - annotations specified using {@link #optionalAnnotations} parameter</li>
     * <li><code>useLibraryDefinition</code> - examples: <code>@JsonProperty(required = false)</code> when using <code>jackson2</code> library
     *   or <code>@XmlElement(required = false)</code> when using <code>jaxb</code> library</li>
     * <li><code>all</code> - all properties are optional</li>
     * </ul>
     * Default value is <code>useSpecifiedAnnotations</code>.
     */
    @Parameter
    private OptionalProperties optionalProperties;

    /**
     * If <code>true</code> declared properties will be <code>readonly</code>.
     */
    @Parameter
    private boolean declarePropertiesAsReadOnly;

    /**
     * Prefix which will be removed from names of classes, interfaces, enums.
     * For example if set to <code>Json</code> then mapping for <code>JsonData</code> will be <code>Data</code>.
     */
    @Parameter
    private String removeTypeNamePrefix;

    /**
     * Suffix which will be removed from names of classes, interfaces, enums.
     * For example if set to <code>JSON</code> then mapping for <code>DataJSON</code> will be <code>Data</code>.
     */
    @Parameter
    private String removeTypeNameSuffix;

    /**
     * Prefix which will be added to names of classes, interfaces, enums.
     * For example if set to <code>I</code> then mapping for <code>Data</code> will be <code>IData</code>.
     */
    @Parameter
    private String addTypeNamePrefix;

    /**
     * Suffix which will be added to names of classes, interfaces, enums.
     * For example if set to <code>Data</code> then mapping for <code>Person</code> will be <code>PersonData</code>.
     */
    @Parameter
    private String addTypeNameSuffix;

    /**
     * Specifies custom TypeScript names for Java classes.
     * Multiple mappings can be specified, each using this format: <code>javaClassName:typescriptName</code>.
     * This takes precedence over other naming settings.
     */
    @Parameter
    private List<String> customTypeNaming;

    /**
     * Specifies JavaScript function for getting custom TypeScript names for Java classes.
     * Function can return undefined if default name should be used.<br>
     * Function signature: <code>function getName(className: string, classSimpleName: string): string | null | undefined;</code><br>
     * Example function: <code>function(name, simpleName) { if (name.startsWith('cz.')) return 'Test' + simpleName; }</code>
     */
    @Parameter
    private String customTypeNamingFunction;

    /**
     * List of files which will be referenced using triple-slash directive: <code>/// &lt;reference path="file" /></code>.
     * This can be used with {@link #customTypeMappings} to provide needed TypeScript types.
     */
    @Parameter
    private List<String> referencedFiles;

    /**
     * List of import declarations which will be added to generated output.
     * This can be used with {@link #customTypeMappings} to provide needed TypeScript types.
     */
    @Parameter
    private List<String> importDeclarations;

    /**
     * List of custom mappings.
     * Each item specifies TypeScript type which will be used for particular Java class.
     * Item format is: <code>javaClassName:typescriptType</code>.
     * For example mapping Joda-Time {@link org.joda.time.LocalDateTime} to <code>string</code> would be added as <code>org.joda.time.LocalDateTime:string</code>.
     */
    @Parameter
    private List<String> customTypeMappings;

    /**
     * Specifies how {@link java.util.Date} will be mapped.
     * Supported values are:
     * <ul>
     * <li><code>asDate</code> - type <code>Date</code></li>
     * <li><code>asNumber</code> - type <code>number</code></li>
     * <li><code>asString</code> - type <code>string</code></li>
     * </ul>
     * Default value is <code>asDate</code>.
     */
    @Parameter
    private DateMapping mapDate;

    /**
     * Specifies how enums will be mapped.
     * Supported values are:
     * <ul>
     * <li><code>asUnion</code> - creates type alias to union of string enum values</li>
     * <li><code>asInlineUnion</code> - creates union of enum values on places where the enum is used</li>
     * <li><code>asEnum</code> - creates string enum. Requires TypeScript 2.4</li>
     * <li><code>asNumberBasedEnum</code> - creates enum of named number values</li>
     * </ul>
     * Default value is <code>asUnion</code>.
     */
    @Parameter
    private EnumMapping mapEnum;

    /**
     * If <code>true</code> generated enums will not have <code>const</code> keyword.<br>
     * This can be used only in implementation files.
     */
    @Parameter
    private boolean nonConstEnums;

    /**
     * Specifies whether Java classes will be mapped to TypeScript classes or interfaces.
     * Java interfaces are always mapped as TypeScript interfaces.
     * Supported values are:
     * <ul>
     * <li><code>asInterfaces</code></li>
     * <li><code>asClasses</code></li>
     * </ul>
     * Default value is <code>asInterfaces</code>.<br>
     * Value <code>asClasses</code> can only be used in implementation files (.ts).
     * It is also possible to generate mix of classes and interfaces by setting this parameter to <code>asClasses</code> value
     * and specifying which classes should be mapped as classes using <code>mapClassesAsClassesPatterns</code> parameter.
     */
    @Parameter
    private ClassMapping mapClasses;

    /**
     * Specifies which Java classes should be mapped as TypeScript classes.
     * Classes which are matched by any of these patters are mapped as classes otherwise they are mapped as interfaces.
     * This parameter can only be used when <code>mapClasses</code> parameter is set to <code>asClasses</code> value.
     */
    @Parameter
    private List<String> mapClassesAsClassesPatterns;

    /**
     * If <code>true</code> tagged unions will not be generated for Jackson 2 polymorphic types.
     */
    @Parameter
    private boolean disableTaggedUnions;

    /**
     * If <code>true</code> Swagger annotations will not be used.
     */
    @Parameter
    private boolean ignoreSwaggerAnnotations;

    /**
     * If <code>true</code> interface for JAX-RS REST application will be generated.
     */
    @Parameter
    private boolean generateJaxrsApplicationInterface;

    /**
     * If <code>true</code> client for JAX-RS REST application will be generated.
     */
    @Parameter
    private boolean generateJaxrsApplicationClient;

    /**
     * Specifies how JAX-RS REST operations will be grouped into objects.
     * Supported values are:
     * <ul>
     * <li><code>singleObject</code> - means that one object with all operations will be generated</li>
     * <li><code>perResource</code> - means that for each root resource one object will be generated</li>
     * <li><code>byAnnotation</code> - means that operations will be grouped by annotation specified using {@link #jaxrsNamespacingAnnotation}</li>
     * </ul>
     * Default value is <code>singleObject</code>.
     */
    @Parameter
    private JaxrsNamespacing jaxrsNamespacing;

    /**
     * Specifies annotation used for grouping JAX-RS REST operations.
     * Format is <code>annotationClass#annotationElement</code> where
     * <code>annotationClass</code> is fully-qualified class name and <code>annotationElement</code> is element name and defaults to <code>value</code>.
     * Examples:
     * <ul>
     * <li><code>io.swagger.annotations.Api</code></li>
     * <li><code>io.swagger.annotations.Api#value</code></li>
     * </ul>
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
     * Can be specific (for example <code>AxiosRequestConfig</code>) or generic (for example <code>&lt;O></code>).
     */
    @Parameter
    private String restOptionsType;

    /**
     * Specifies custom class implementing {@link cz.habarta.typescript.generator.TypeProcessor}.
     * This allows to customize how Java types are mapped to TypeScript.
     * For example it is possible to implement TypeProcessor
     * for {@link com.google.common.base.Optional} from guava.
     */
    @Parameter
    private String customTypeProcessor;

    /**
     * If <code>true</code> TypeScript declarations (interfaces, properties) will be sorted alphabetically.
     */
    @Parameter
    private boolean sortDeclarations;

    /**
     * If <code>true</code> TypeScript type declarations (interfaces) will be sorted alphabetically.
     */
    @Parameter
    private boolean sortTypeDeclarations;

    /**
     * If <code>true</code> generated file will not contain comment at the top.
     * By default there is a comment with timestamp and typescript-generator version.
     * So it might be useful to suppress this comment if the file is in source control and is regenerated in build.
     */
    @Parameter
    private boolean noFileComment;

    /**
     * List of Javadoc XML files to search for documentation comments.
     * These files should be created using <code>com.github.markusbernhardt.xmldoclet.XmlDoclet</code> from <code>com.github.markusbernhardt:xml-doclet</code> artifact.
     * Javadoc comments are added to output declarations as JSDoc comments.
     * For more information see <a href="https://github.com/vojtechhabarta/typescript-generator/wiki/Javadoc">Javadoc</a> Wiki page.
     */
    @Parameter
    private List<File> javadocXmlFiles;

    /**
     * List of extensions specified as fully qualified class name.
     * Known extensions:
     * <ul>
     * <li><code>cz.habarta.typescript.generator.ext.AxiosClientExtension}</code>
     *   - generates client for JAX-RS service using Axios library, see <a href="https://github.com/vojtechhabarta/typescript-generator/wiki/JAX-RS-Application">JAX RS Application</a>Wiki page</li>
     * <li><code>cz.habarta.typescript.generator.ext.BeanPropertyPathExtension}</code>
     *   - generates type-safe property path getters</li>
     * <li><code>cz.habarta.typescript.generator.ext.TypeGuardsForJackson2PolymorphismExtension}</code></li>
     * </ul>
     * Parameter {@link #extensionsWithConfiguration} can be used in case extension needs some configuration.
     */
    @Parameter
    private List<String> extensions;

    /**
     * List of extensions with their configurations.
     * This parameter has the same purpose as {@link #extensions} parameter.
     * Each item of this list has
     * <ul>
     * <li><code>className</code> - required fully-qualified class name of the extension</li>
     * <li><code>configuration</code> - optional <code>Map</code> with <code>String</code> keys and <code>String</code> values</li>
     * </ul>
     */
    @Parameter
    private List<Settings.ConfiguredExtension> extensionsWithConfiguration;

    /**
     * The presence of any annotation in this list on a JSON property will cause
     * the typescript-generator to treat that property as optional when generating
     * the corresponding TypeScript interface.
     * Example optional annotation: <code>javax.annotation.Nullable</code>
     */
    @Parameter
    private List<String> optionalAnnotations;

    /**
     * If <code>true</code> optional property types will be generated as foo: T | null instead of as foo?: T
     */
    @Parameter
    private boolean optionalAsNull;

    /**
     * If <code>true</code> NPM <code>package.json</code> will be generated.
     * Only applicable when {@link #outputKind} is set to <code>module</code>.
     * NPM package name and version can be specified using {@link #npmName} and {@link #npmVersion} parameters.
     */
    @Parameter
    private boolean generateNpmPackageJson;

    /**
     * Specifies NPM package name.<br>
     * Only applicable when {@link #generateNpmPackageJson} parameter is <code>true</code>.<br>
     * Default value is <code>${project.artifactId}</code>.
     */
    @Parameter
    private String npmName;

    /**
     * Specifies NPM package version.<br>
     * Only applicable when {@link #generateNpmPackageJson} parameter is <code>true</code>.<br>
     * Default value is <code>1.0.0</code>.
     */
    @Parameter
    private String npmVersion;

    /**
     * Specifies how strings will be quoted.
     * Supported values are:
     * <ul>
     * <li><code>doubleQuotes</code></li>
     * <li><code>singleQuotes</code></li>
     * </ul>
     * Default value is <code>doubleQuotes</code>.
     */
    @Parameter
    private StringQuotes stringQuotes;

    /**
     * Specifies indentation string.
     */
    @Parameter
    private String indentString;

    /**
     * Display warnings when bean serializer is not found.
     */
    @Parameter(defaultValue = "true")
    private boolean displaySerializerWarning;

    /**
     * <b>Deprecated</b>, see {@link #jackson2ModuleDiscovery} and {@link #jackson2Modules} parameters.
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
            settings.optionalAsNull = optionalAsNull;
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
            settings.mapClassesAsClassesPatterns = mapClassesAsClassesPatterns;
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
            settings.loadExtensions(classLoader, extensions, extensionsWithConfiguration);
            settings.loadIncludePropertyAnnotations(classLoader, includePropertyAnnotations);
            settings.loadOptionalAnnotations(classLoader, optionalAnnotations);
            settings.generateNpmPackageJson = generateNpmPackageJson;
            settings.npmName = npmName == null && generateNpmPackageJson ? project.getArtifactId() : npmName;
            settings.npmVersion = npmVersion == null && generateNpmPackageJson ? settings.getDefaultNpmVersion() : npmVersion;
            settings.setStringQuotes(stringQuotes);
            settings.setIndentString(indentString);
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
